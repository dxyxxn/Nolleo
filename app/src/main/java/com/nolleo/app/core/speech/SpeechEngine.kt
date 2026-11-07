package com.nolleo.app.core.speech

import com.nolleo.app.core.SdkStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class SpeechEngineKind(val label: String) {
    PLEOS("Pleos"),
    ANDROID("Android"),
    SCRIPTED("대본"),
}

/** STT 이벤트. tMs = Clock.elapsedMs() 기준 시각. */
sealed interface SttEvent {
    /** 마이크가 열리고 인식이 시작됨 */
    data class Listening(val tMs: Long) : SttEvent

    /** 말소리 감지 (EPD on / onBeginningOfSpeech) */
    data class SpeechDetected(val tMs: Long) : SttEvent

    data class Partial(val text: String, val tMs: Long) : SttEvent

    /** 최종 결과. 빈 문자열이면 말이 없었다는 뜻 */
    data class Final(val text: String, val tMs: Long) : SttEvent

    data object Timeout : SttEvent

    /** [fatal] = 이 엔진으로는 더 들을 수 없음(마이크·퍼미션·미지원) → 라우터가 폴백한다 */
    data class Error(val reason: String, val fatal: Boolean = false) : SttEvent
}

/**
 * 대본 엔진이 "{correct}"/"{wrong}" 토큰을 실제 답으로 바꾸는 데 쓰는 힌트.
 * 실제 엔진(Pleos/Android)은 무시한다.
 */
data class ListenHint(
    val correctAnswer: String? = null,
    val wrongAnswer: String? = null,
    /** 운전자 차례인지 — 대본이 driverOnly 이면 운전자 차례에만 대본 지연을 적용한다 */
    val forDriver: Boolean = false,
    /** 네/아니요 질문 (쉬어 가기 수락). 대본 엔진은 대본과 따로 1.5초 뒤 "네"로 답한다 */
    val yesNo: Boolean = false,
)

/** 지시서 §5.4 의 SpeechEngine 인터페이스 (+ 대본용 hint, 상태). */
interface SpeechEngine {
    val kind: SpeechEngineKind
    val ttsStatus: StateFlow<SdkStatus>
    val sttStatus: StateFlow<SdkStatus>

    /** onDone 또는 onError 에서 리턴. 코루틴 취소 시 즉시 발화를 멈춘다. */
    suspend fun speak(text: String)

    /** 수집을 취소하면 인식을 멈춘다. 타임아웃 정책은 [AnswerCollector] 가 맡는다. */
    fun listen(timeoutMs: Long, hint: ListenHint = ListenHint()): Flow<SttEvent>

    fun stop()

    fun release()
}
