package com.nolleo.app.core.speech

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/** 디버그 패널: 음성 엔진 강제 선택 (auto|pleos|android|scripted) */
enum class SpeechMode(val label: String) { AUTO("자동"), PLEOS("Pleos"), ANDROID("Android"), SCRIPTED("대본") }

/** 응답 입력: 마이크(STT) 또는 대본 */
enum class AnswerInput(val label: String, val scriptAsset: String?) {
    MIC("마이크", null),
    SCRIPT_NORMAL("대본 normal", "demo/answers_normal.json"),
    SCRIPT_SLEEPY("대본 sleepy", "demo/answers_sleepy.json"),
    MANUAL("직접 입력", null),
}

/**
 * TTS 와 STT 를 각각 Pleos → Android → Scripted 순서로 자동 선택한다.
 * 실제 SDK 경로가 1순위이고, 대본은 빈 곳만 메운다. 선택 결과는 [ttsKind]/[sttKind] 로 UI·로그에 드러난다.
 */
class SpeechRouter(
    val pleos: PleosSpeechEngine,
    val android: AndroidSpeechEngine,
    val scripted: ScriptedSpeechEngine,
    scope: CoroutineScope,
) {
    val mode = MutableStateFlow(SpeechMode.AUTO)
    val answerInput = MutableStateFlow(AnswerInput.MIC)

    val ttsKind: StateFlow<SpeechEngineKind> = combine(
        mode, pleos.ttsStatus, android.ttsStatus,
    ) { m, pleosStatus, androidStatus ->
        pick(m, pleosStatus, androidStatus)
    }.stateIn(scope, SharingStarted.Eagerly, SpeechEngineKind.SCRIPTED)

    val sttKind: StateFlow<SpeechEngineKind> = combine(
        mode, answerInput, pleos.sttStatus, android.sttStatus,
    ) { m, input, pleosStatus, androidStatus ->
        if (input != AnswerInput.MIC) SpeechEngineKind.SCRIPTED else pick(m, pleosStatus, androidStatus)
    }.stateIn(scope, SharingStarted.Eagerly, SpeechEngineKind.SCRIPTED)

    init {
        ttsKind.onEach { AppLog.i(TAG, "TTS engine = ${it.label}") }.launchIn(scope)
        sttKind.onEach { AppLog.i(TAG, "STT engine = ${it.label} (input=${answerInput.value.label})") }.launchIn(scope)
    }

    private fun pick(m: SpeechMode, pleosStatus: SdkStatus, androidStatus: SdkStatus): SpeechEngineKind = when (m) {
        SpeechMode.SCRIPTED -> SpeechEngineKind.SCRIPTED
        SpeechMode.ANDROID -> if (androidStatus.isAvailable) SpeechEngineKind.ANDROID else SpeechEngineKind.SCRIPTED
        SpeechMode.PLEOS -> if (pleosStatus.isAvailable) SpeechEngineKind.PLEOS else SpeechEngineKind.SCRIPTED
        SpeechMode.AUTO -> when {
            pleosStatus.isAvailable -> SpeechEngineKind.PLEOS
            androidStatus.isAvailable -> SpeechEngineKind.ANDROID
            else -> SpeechEngineKind.SCRIPTED
        }
    }

    private fun engine(kind: SpeechEngineKind): SpeechEngine = when (kind) {
        SpeechEngineKind.PLEOS -> pleos
        SpeechEngineKind.ANDROID -> android
        SpeechEngineKind.SCRIPTED -> scripted
    }

    fun initialize() {
        pleos.initialize()
        android.initialize()
    }

    /** 세션 시작·앱 복귀 때: 실패해 내려간 STT/TTS 를 한 번 다시 살려 본다 */
    fun recoverUnavailableEngines() {
        pleos.recoverIfUnavailable()
        android.refreshSttAvailability()
    }

    suspend fun speak(text: String) = engine(ttsKind.value).speak(text)

    fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent> = engine(sttKind.value).listen(timeoutMs, hint)

    /** 현재 발화가 Pleos Speech Service 경유인지 (자체 포커스 손실 판별용) */
    val pleosSpeaking: StateFlow<Boolean> get() = pleos.isSpeaking

    fun markNewStream() = pleos.markNewStream()

    fun stopAll() {
        pleos.stop()
        android.stop()
        scripted.stop()
    }

    fun release() {
        pleos.release()
        android.release()
        scripted.release()
    }

    companion object {
        private const val TAG = "SpeechRouter"
    }
}
