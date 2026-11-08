package com.nolleo.app.core.speech

import com.nolleo.app.core.Clock
import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withTimeoutOrNull

/** 대본 한 줄: [delayMs] 뒤에 [answer] 를 말한다. answer == null 이면 무응답(타임아웃). */
data class ScriptEntry(val delayMs: Long, val answer: String?)

/** [driverOnly] 이면 대본은 운전자 차례에만 쓰고, 동승자는 평소 속도(1.2~1.8초 정답)로 답한다. */
data class AnswerScript(val name: String, val entries: List<ScriptEntry>, val driverOnly: Boolean = false) {
    companion object {
        const val CORRECT = "{correct}"
        const val WRONG = "{wrong}"
    }
}

/**
 * 데모 녹화·테스트용 엔진.
 *  - speak: 화면 자막(세션 상태가 표시)만 쓰고 소리 없이 글자 수에 비례해 기다린다.
 *  - listen: 대본(assets/demo/answers_*.json)의 답을 지정된 지연 뒤에 돌려준다. "{correct}"·"{wrong}" 토큰 지원.
 *  - 디버그 패널의 직접 입력/정답·오답·무응답 버튼은 [submitManual] 로 현재 듣기에 즉시 답한다.
 */
class ScriptedSpeechEngine(private val clock: Clock) : SpeechEngine {

    override val kind = SpeechEngineKind.SCRIPTED

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Available)
    override val ttsStatus: StateFlow<SdkStatus> = _status.asStateFlow()
    override val sttStatus: StateFlow<SdkStatus> = _status.asStateFlow()

    private val _script = MutableStateFlow<AnswerScript?>(null)
    val script: StateFlow<AnswerScript?> = _script.asStateFlow()

    private var cursor = 0

    /** 대본 없이 수동 입력만 기다리는 모드 */
    private val _manualOnly = MutableStateFlow(false)
    val manualOnly: StateFlow<Boolean> = _manualOnly.asStateFlow()

    private val manualAnswers = Channel<ManualAnswer>(Channel.CONFLATED)

    private val _listening = MutableStateFlow(false)
    val listening: StateFlow<Boolean> = _listening.asStateFlow()

    /** 직전 hint (수동 정답/오답 버튼용) */
    @Volatile private var currentHint: ListenHint = ListenHint()

    /** 발화 속도: 글자당 ms (데모 녹화가 늘어지지 않게 실제 TTS 보다 약간 빠르게) */
    var msPerChar: Long = 55L

    sealed interface ManualAnswer {
        data class Text(val text: String) : ManualAnswer
        data object Correct : ManualAnswer
        data object Wrong : ManualAnswer
        data object Silence : ManualAnswer
    }

    fun setScript(script: AnswerScript?) {
        _script.value = script
        _manualOnly.value = script == null
        rewind()
        AppLog.i(TAG, "script = ${script?.name ?: "manual"}")
    }

    /** 대본을 처음부터 (새 세션을 시작할 때 — 시연 대본은 출발부터 순서대로 간다) */
    fun rewind() {
        cursor = 0
        passengerCursor = 0
    }

    fun submitManual(answer: ManualAnswer) {
        manualAnswers.trySend(answer)
    }

    override suspend fun speak(text: String) {
        delay((text.length * msPerChar).coerceIn(700L, 4_000L))
    }

    override fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent> = channelFlow {
        currentHint = hint
        _listening.value = true
        // 이전 듣기에서 남은 수동 입력은 버린다
        while (manualAnswers.tryReceive().isSuccess) Unit
        try {
            val start = clock.elapsedMs()
            send(SttEvent.Listening(start))
            val script = _script.value
            val entry = when {
                // 네/아니요 질문은 대본 순서를 쓰지 않는다 (수동 버튼: 정답=네, 오답=아니요, 무응답)
                hint.yesNo && script != null -> ScriptEntry(YES_NO_DELAY_MS, AnswerScript.CORRECT)
                script != null && script.driverOnly && !hint.forDriver ->
                    ScriptEntry(PASSENGER_DELAYS[passengerCursor++ % PASSENGER_DELAYS.size], AnswerScript.CORRECT)
                else -> nextEntry()
            }
            val scriptedDelay = entry?.delayMs
            // 대본 지연과 수동 입력 중 먼저 오는 것. 대본이 없으면 수동 입력만 (타임아웃은 AnswerCollector 가 처리)
            val manual: ManualAnswer? = if (scriptedDelay != null) {
                withTimeoutOrNull(scriptedDelay) { manualAnswers.receive() }
            } else {
                manualAnswers.receive()
            }
            val answer: String? = when (manual) {
                is ManualAnswer.Text -> manual.text
                ManualAnswer.Correct -> resolve(AnswerScript.CORRECT, hint)
                ManualAnswer.Wrong -> resolve(AnswerScript.WRONG, hint)
                ManualAnswer.Silence -> null
                null -> entry?.answer?.let { resolve(it, hint) }
            }
            if (answer.isNullOrBlank()) {
                // 무응답: 아무것도 보내지 않고 수집기의 타임아웃(8초)이 취소할 때까지 기다린다
                awaitCancellation()
            }
            val detectedAt = clock.elapsedMs()
            send(SttEvent.SpeechDetected(detectedAt))
            send(SttEvent.Partial(answer, detectedAt))
            delay(FINALIZE_MS)
            send(SttEvent.Final(answer, clock.elapsedMs()))
        } finally {
            _listening.value = false
        }
    }

    private fun nextEntry(): ScriptEntry? {
        val entries = _script.value?.entries ?: return null
        if (entries.isEmpty()) return null
        val entry = entries[cursor % entries.size]
        cursor++
        return entry
    }

    private fun resolve(token: String, hint: ListenHint): String? = when (token) {
        AnswerScript.CORRECT -> hint.correctAnswer ?: "모르겠어요"
        AnswerScript.WRONG -> hint.wrongAnswer ?: "음 글쎄요"
        else -> token
    }

    override fun stop() = Unit

    override fun release() = Unit

    private var passengerCursor = 0

    companion object {
        private const val TAG = "ScriptedSpeech"
        private const val FINALIZE_MS = 250L
        private const val YES_NO_DELAY_MS = 1_500L
        private val PASSENGER_DELAYS = listOf(1_200L, 1_600L, 1_400L, 1_800L)
    }
}
