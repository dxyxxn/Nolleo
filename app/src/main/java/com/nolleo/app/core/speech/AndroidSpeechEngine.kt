package com.nolleo.app.core.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import com.nolleo.app.core.Clock
import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/**
 * Android 플랫폼 TTS/SpeechRecognizer 폴백 (지시서 §5.4).
 * 에뮬레이터에 엔진이 없을 수 있으므로 엔진 목록·언어·isRecognitionAvailable 로 감지해 없으면 건너뛴다.
 * SpeechRecognizer 는 RECORD_AUDIO 런타임 퍼미션이 필요하다 — 이 경로가 선택될 때만 요청한다.
 */
class AndroidSpeechEngine(
    context: Context,
    private val clock: Clock,
) : SpeechEngine {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    override val kind = SpeechEngineKind.ANDROID

    private val _ttsStatus = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    override val ttsStatus: StateFlow<SdkStatus> = _ttsStatus.asStateFlow()

    private val _sttStatus = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    override val sttStatus: StateFlow<SdkStatus> = _sttStatus.asStateFlow()

    /** 인식 엔진은 있으나 RECORD_AUDIO 가 없어 퍼미션 요청이 필요한 상태 */
    private val _needsMicPermission = MutableStateFlow(false)
    val needsMicPermission: StateFlow<Boolean> = _needsMicPermission.asStateFlow()

    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val pending = AtomicReference<CancellableContinuation<Unit>?>(null)
    private val utteranceCounter = AtomicInteger(0)

    fun initialize() {
        initTts()
        refreshSttAvailability()
    }

    private fun initTts() {
        if (tts != null) return
        _ttsStatus.value = SdkStatus.Connecting
        try {
            var created: TextToSpeech? = null
            created = TextToSpeech(appContext) { status ->
                val engine = created
                if (status != TextToSpeech.SUCCESS || engine == null) {
                    _ttsStatus.value = SdkStatus.Unavailable("엔진 없음")
                    return@TextToSpeech
                }
                val engines = runCatching { engine.engines }.getOrDefault(emptyList())
                val lang = runCatching { engine.setLanguage(Locale.KOREA) }.getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                if (engines.isEmpty() || lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                    AppLog.i(TAG, "Android TTS unusable (engines=${engines.size}, lang=$lang)")
                    _ttsStatus.value = SdkStatus.Unavailable("한국어 미지원")
                    return@TextToSpeech
                }
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) = finish()

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = finish()
                    override fun onError(utteranceId: String?, errorCode: Int) = finish()
                    override fun onStop(utteranceId: String?, interrupted: Boolean) = finish()
                })
                AppLog.i(TAG, "Android TTS ready (${engines.joinToString { it.name }})")
                _ttsStatus.value = SdkStatus.Available
            }
            tts = created
        } catch (t: Throwable) {
            AppLog.w(TAG, "Android TTS failed", t)
            _ttsStatus.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    /** 퍼미션 결과가 바뀌면 다시 호출한다. */
    fun refreshSttAvailability() {
        val available = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)
        if (!available) {
            _needsMicPermission.value = false
            _sttStatus.value = SdkStatus.Unavailable("인식 엔진 없음")
            return
        }
        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        _needsMicPermission.value = !granted
        _sttStatus.value = if (granted) SdkStatus.Available else SdkStatus.Unavailable("마이크 퍼미션 필요")
    }

    private fun finish() {
        val cont = pending.getAndSet(null) ?: return
        if (cont.isActive) cont.resume(Unit)
    }

    override suspend fun speak(text: String) {
        val engine = tts ?: return
        if (_ttsStatus.value != SdkStatus.Available) return
        val done = withTimeoutOrNull(6_000L + text.length * 250L) {
            suspendCancellableCoroutine<Unit> { cont ->
                pending.set(cont)
                cont.invokeOnCancellation {
                    pending.compareAndSet(cont, null)
                    runCatching { engine.stop() }
                }
                val id = "gd-${utteranceCounter.incrementAndGet()}"
                val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, Bundle(), id)
                if (result != TextToSpeech.SUCCESS) finish()
            }
        }
        if (done == null) runCatching { engine.stop() }
    }

    override fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent> = callbackFlow {
        if (_sttStatus.value != SdkStatus.Available) {
            trySend(SttEvent.Error("Android STT 불가", fatal = true))
            close()
            awaitClose { }
            return@callbackFlow
        }
        val r = recognizer ?: SpeechRecognizer.createSpeechRecognizer(appContext).also { recognizer = it }
        var detected = false
        var lastPartial = ""
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SttEvent.Listening(clock.elapsedMs()))
            }

            override fun onBeginningOfSpeech() {
                if (!detected) {
                    detected = true
                    trySend(SttEvent.SpeechDetected(clock.elapsedMs()))
                }
            }

            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        trySend(SttEvent.Final(lastPartial, clock.elapsedMs()))
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        _sttStatus.value = SdkStatus.Unavailable("마이크 퍼미션")
                        trySend(SttEvent.Error("INSUFFICIENT_PERMISSIONS", fatal = true))
                    }
                    else -> trySend(SttEvent.Error("ANDROID_STT_$error", fatal = error == SpeechRecognizer.ERROR_CLIENT))
                }
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                trySend(SttEvent.Final(text, clock.elapsedMs()))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    lastPartial = text
                    trySend(SttEvent.Partial(text, clock.elapsedMs()))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            r.startListening(intent)
        } catch (t: Throwable) {
            trySend(SttEvent.Error(t.javaClass.simpleName, fatal = true))
            close()
        }
        awaitClose { mainHandler.post { runCatching { r.cancel() } } }
    }.flowOn(Dispatchers.Main)

    override fun stop() {
        runCatching { tts?.stop() }
        mainHandler.post { runCatching { recognizer?.cancel() } }
        finish()
    }

    override fun release() {
        stop()
        runCatching { tts?.shutdown() }
        mainHandler.post {
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
        tts = null
        _ttsStatus.value = SdkStatus.Idle
    }

    companion object {
        private const val TAG = "AndroidSpeech"
    }
}
