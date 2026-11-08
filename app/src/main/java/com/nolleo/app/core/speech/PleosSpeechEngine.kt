package com.nolleo.app.core.speech

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import com.nolleo.app.core.Clock
import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import ai.pleos.playground.stt.constant.ErrorType as SttErrorType
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInitListener
import ai.pleos.playground.stt.listener.OnServerConnectionListener as SttServerListener
import ai.pleos.playground.tts.constant.ErrorType as TtsErrorType
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInitListener
import ai.pleos.playground.tts.listener.OnServerConnectionListener as TtsServerListener

/**
 * Pleos TextToSpeech / SpeechToText SDK (DEV_GUIDE §8.3–8.4).
 *
 *  TTS: TextToSpeech(appContext, mode) → initialize(OnInitializeListener) → addEventListener → setLocale(KOREA)
 *       → (자격증명 있을 때만) registerApp → speak(text, isFinal=true) → onDone
 *  STT: SpeechToText(appContext, mode) → initialize → addListener(ResultListener) **request() 전에** → setLocale
 *       → request() → onUpdated(text, completed) → stop()
 *  mode: BuildConfig 에 Client ID/Secret 이 있으면 HYBRID, 없으면 ON_DEVICE.
 *  ErrorType when 에는 반드시 else 분기 (신규 타입 추가 대비).
 */
class PleosSpeechEngine(
    context: Context,
    private val clientId: String,
    private val clientSecret: String,
    private val clock: Clock,
) : SpeechEngine {

    private val appContext = context.applicationContext

    override val kind = SpeechEngineKind.PLEOS

    private val useServer = clientId.isNotBlank() && clientSecret.isNotBlank()
    val modeLabel: String get() = if (useServer) "HYBRID" else "ON_DEVICE"

    private val _ttsStatus = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    override val ttsStatus: StateFlow<SdkStatus> = _ttsStatus.asStateFlow()

    private val _sttStatus = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    override val sttStatus: StateFlow<SdkStatus> = _sttStatus.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)

    /** 오디오 포커스 손실이 우리 TTS(Speech Service)에서 비롯됐는지 판단할 때 쓴다 */
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var tts: TextToSpeech? = null
    private var stt: SpeechToText? = null

    private val pendingSpeak = AtomicReference<CancellableContinuation<Unit>?>(null)
    private val sttSink = AtomicReference<ProducerScope<SttEvent>?>(null)

    /**
     * 우리가 끊은 발화의 종료 콜백(onStop·onDone·onError)이 아직 오지 않은 수.
     * Pleos `stop()` 은 비동기라, 끊은 발화의 onStop 이 곧바로 시작한 다음 발화의 대기를 끝내 버릴 수 있다 →
     * 그만큼은 무시한다. 다음 발화의 onStart 가 오면 0 으로 되돌린다 (그 뒤에 오는 종료 콜백은 새 발화의 것).
     */
    private val staleCallbacks = AtomicInteger(0)

    @Volatile private var stopRequested = false
    @Volatile private var needsPreRoll = true
    @Volatile private var speechDetected = false
    @Volatile private var lastPartial: String? = null
    @Volatile private var finalSent = false

    private val ttsListener = object : EventListener {
        override fun onReady() = Unit

        override fun onStart() {
            staleCallbacks.set(0)
            _isSpeaking.value = true
        }

        override fun onUpdatedRms(rms: Double) = Unit

        override fun onDone() = finishSpeak("done")

        override fun onStop() = finishSpeak(if (stopRequested) "stopped" else "interrupted")

        override fun onError(errorType: TtsErrorType) {
            when (errorType) {
                TtsErrorType.PERMISSION_DENIED -> AppLog.e(TAG, "TTS PERMISSION_DENIED — 매니페스트 TTS_SERVICE 확인")
                TtsErrorType.UNAUTHENTICATED -> AppLog.e(TAG, "TTS UNAUTHENTICATED — Client ID/Secret 확인")
                TtsErrorType.NETWORK_ERROR, TtsErrorType.TIMEOUT -> AppLog.w(TAG, "TTS ${errorType.name} (네트워크)")
                else -> AppLog.w(TAG, "TTS error ${errorType.name}")
            }
            finishSpeak("error:${errorType.name}")
        }
    }

    private val resultListener = object : ResultListener {
        override fun onReady() = Unit

        override fun onStartedRecognition() {
            sttSink.get()?.trySend(SttEvent.Listening(clock.elapsedMs()))
        }

        override fun onUpdated(stt: String, completed: Boolean) {
            val sink = sttSink.get() ?: return
            val now = clock.elapsedMs()
            if (completed) {
                finalSent = true
                sink.trySend(SttEvent.Final(stt, now))
                sink.close()
            } else if (stt.isNotBlank()) {
                lastPartial = stt
                sink.trySend(SttEvent.Partial(stt, now))
            }
        }

        override fun onUpdatedEpdData(on: Long, off: Long) {
            if (speechDetected) return
            speechDetected = true
            sttSink.get()?.trySend(SttEvent.SpeechDetected(clock.elapsedMs()))
        }

        override fun onUpdatedRms(rms: Double) = Unit

        override fun onEndedRecognition() {
            val sink = sttSink.get() ?: return
            if (!finalSent) sink.trySend(SttEvent.Final(lastPartial.orEmpty(), clock.elapsedMs()))
            sink.close()
        }

        override fun onError(errorType: SttErrorType) {
            val fatal = when (errorType) {
                SttErrorType.MIC_ERROR, SttErrorType.PERMISSION_DENIED, SttErrorType.NOT_SUPPORTED_API,
                SttErrorType.UNAUTHENTICATED, SttErrorType.ON_DEVICE_ENGINE_ERROR,
                SttErrorType.UNAVAILABLE_LANGUAGE -> true
                SttErrorType.TIMEOUT, SttErrorType.NETWORK_ERROR -> false
                else -> false
            }
            AppLog.w(TAG, "STT error ${errorType.name} (fatal=$fatal)")
            if (fatal) _sttStatus.value = SdkStatus.Unavailable(errorType.name)
            sttSink.get()?.let {
                it.trySend(SttEvent.Error(errorType.name, fatal))
                it.close()
            }
        }
    }

    fun initialize() {
        initTts()
        initStt()
    }

    /**
     * 실패한 엔진(초기화 실패·STT 치명 오류)을 버리고 다시 초기화한다 — 세션 시작·앱 복귀 때.
     * 그러지 않으면 한 번 실패한 엔진이 프로세스를 다시 띄우기 전까지 돌아오지 않는다. 정상인 엔진은 건드리지 않는다.
     */
    fun recoverIfUnavailable() {
        if (_ttsStatus.value is SdkStatus.Unavailable) {
            AppLog.i(TAG, "TTS unavailable (${_ttsStatus.value.label}) → 다시 초기화")
            releaseTts()
            initTts()
        }
        if (_sttStatus.value is SdkStatus.Unavailable) {
            AppLog.i(TAG, "STT unavailable (${_sttStatus.value.label}) → 다시 초기화")
            releaseStt()
            initStt()
        }
    }

    private fun initTts() {
        if (tts != null) return
        _ttsStatus.value = SdkStatus.Connecting
        try {
            val engine = TextToSpeech(appContext, if (useServer) TtsMode.HYBRID else TtsMode.ON_DEVICE)
            tts = engine
            engine.initialize(object : TtsInitListener {
                override fun onSuccess() {
                    runCatching {
                        engine.addEventListener(ttsListener)
                        engine.setLocale(Locale.KOREA)
                    }.onFailure { AppLog.w(TAG, "TTS setup failed", it) }
                    if (useServer) {
                        engine.registerApp(clientId, clientSecret, object : TtsServerListener {
                            override fun onConnected() = AppLog.i(TAG, "TTS server connected (HYBRID)")
                            override fun onFailed(errorType: TtsErrorType) =
                                AppLog.w(TAG, "TTS registerApp failed ${errorType.name} → on-device 로 동작")
                        })
                    }
                    AppLog.i(TAG, "TTS initialized ($modeLabel)")
                    _ttsStatus.value = SdkStatus.Available
                }

                override fun onFailure(errorType: TtsErrorType) {
                    AppLog.w(TAG, "TTS init failed ${errorType.name}")
                    _ttsStatus.value = SdkStatus.Unavailable(errorType.name)
                }
            })
        } catch (t: Throwable) {
            AppLog.w(TAG, "TTS construct failed", t)
            tts = null
            _ttsStatus.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    private fun initStt() {
        if (stt != null) return
        _sttStatus.value = SdkStatus.Connecting
        try {
            val engine = SpeechToText(appContext, if (useServer) SttMode.HYBRID else SttMode.ON_DEVICE)
            stt = engine
            engine.initialize(object : SttInitListener {
                override fun onSuccess() {
                    runCatching {
                        // ⚠️ request() 전에 등록 (DEV_GUIDE §8.4)
                        engine.addListener(resultListener)
                        engine.setLocale(Locale.KOREA)
                    }.onFailure { AppLog.w(TAG, "STT setup failed", it) }
                    if (useServer) {
                        engine.registerApp(clientId, clientSecret, object : SttServerListener {
                            override fun onConnected() = AppLog.i(TAG, "STT server connected (HYBRID)")
                            override fun onFailed(errorType: SttErrorType) =
                                AppLog.w(TAG, "STT registerApp failed ${errorType.name} → on-device 로 동작")
                        })
                    }
                    AppLog.i(TAG, "STT initialized ($modeLabel)")
                    _sttStatus.value = SdkStatus.Available
                }

                override fun onFailure(errorType: SttErrorType) {
                    AppLog.w(TAG, "STT init failed ${errorType.name}")
                    _sttStatus.value = SdkStatus.Unavailable(errorType.name)
                }
            })
        } catch (t: Throwable) {
            AppLog.w(TAG, "STT construct failed", t)
            stt = null
            _sttStatus.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    /** 세션 시작·일시정지 해제 등 "새 스트림"이면 첫 발화 앞에 선행 묵음을 다시 둔다 (DEV_GUIDE §7.7). */
    fun markNewStream() {
        needsPreRoll = true
    }

    override suspend fun speak(text: String) {
        val engine = tts ?: return
        if (_ttsStatus.value != SdkStatus.Available) return
        if (needsPreRoll) {
            // TODO(verify): SDK/Speech Service 가 선행 묵음을 넣는지 확인 불가(청취 필요) → 재생 지연(③ 방식)으로 보장
            delay(PRE_ROLL_MS)
            needsPreRoll = false
        }
        // 시간 제한에 걸리면 아래 invokeOnCancellation 이 발화를 멈춘다
        val finished = withTimeoutOrNull(SpeechTiming.ttsTimeoutMs(text)) {
            suspendCancellableCoroutine { cont ->
                stopRequested = false
                pendingSpeak.set(cont)
                cont.invokeOnCancellation {
                    stopRequested = true
                    if (pendingSpeak.compareAndSet(cont, null)) stopEngine(engine)
                    _isSpeaking.value = false
                }
                try {
                    engine.speak(text, true)
                } catch (t: Throwable) {
                    AppLog.w(TAG, "speak threw", t)
                    pendingSpeak.getAndSet(null)?.resume(Unit)
                }
            }
        }
        if (finished == null) AppLog.w(TAG, "TTS onDone not received in time → stopped")
    }

    /** 말하는 중인 발화를 끊는다 — 그 발화의 종료 콜백은 늦게 오므로 [staleCallbacks] 로 걸러 낸다 */
    private fun stopEngine(engine: TextToSpeech) {
        staleCallbacks.incrementAndGet()
        runCatching { engine.stop() }
    }

    private fun finishSpeak(reason: String) {
        if (staleCallbacks.getAndUpdate { if (it > 0) it - 1 else 0 } > 0) {
            AppLog.i(TAG, "TTS $reason from an utterance we stopped → ignored")
            return
        }
        _isSpeaking.value = false
        if (reason == "interrupted") AppLog.w(TAG, "TTS interrupted by system")
        val cont = pendingSpeak.getAndSet(null) ?: return
        if (cont.isActive) cont.resume(Unit)
    }

    override fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent> = callbackFlow {
        val engine = stt
        if (engine == null || _sttStatus.value != SdkStatus.Available) {
            trySend(SttEvent.Error("STT 미초기화", fatal = true))
            close()
            awaitClose { }
            return@callbackFlow
        }
        speechDetected = false
        lastPartial = null
        finalSent = false
        sttSink.getAndSet(this)?.close()
        try {
            engine.request()
        } catch (t: Throwable) {
            AppLog.w(TAG, "STT request threw", t)
            val fatal = t is SecurityException
            // 치명 오류면 상태도 내린다 — 라우터는 상태만 보고 엔진을 고르므로, 그래야 같은 문제를 다른 엔진으로 다시 낸다
            if (fatal) _sttStatus.value = SdkStatus.Unavailable(t.javaClass.simpleName)
            trySend(SttEvent.Error(t.javaClass.simpleName, fatal))
            close()
        }
        awaitClose {
            sttSink.compareAndSet(this, null)
            runCatching { engine.stop() }
        }
    }

    override fun stop() {
        stopRequested = true
        val pending = pendingSpeak.getAndSet(null)
        val engine = tts
        if (pending != null && engine != null) stopEngine(engine) else runCatching { engine?.stop() }
        runCatching { stt?.stop() }
        pending?.let { if (it.isActive) it.resume(Unit) }
        sttSink.getAndSet(null)?.close()
        _isSpeaking.value = false
    }

    override fun release() {
        stop()
        releaseTts()
        releaseStt()
        _ttsStatus.value = SdkStatus.Idle
        _sttStatus.value = SdkStatus.Idle
    }

    private fun releaseTts() {
        tts?.let { engine ->
            runCatching { engine.removeEventListener(ttsListener) }
            runCatching { engine.release() }
        }
        tts = null
    }

    private fun releaseStt() {
        stt?.let { engine ->
            runCatching { engine.removeListener(resultListener) }
            runCatching { engine.release() }
        }
        stt = null
    }

    companion object {
        private const val TAG = "PleosSpeech"

        /** DEV_GUIDE §7.7: 150~200 ms */
        const val PRE_ROLL_MS = 180L
    }
}
