package com.gildongmu.app.app

import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.core.speech.FocusChange
import com.gildongmu.app.domain.games.IdiomQuizGame
import com.gildongmu.app.domain.games.IdiomSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 정차 중 화면 게임 "사자성어".
 *
 *  1. 문제(뜻)를 화면에 보여 주고 TTS 로 읽어 준다.
 *  2. 네 개 중에서 고르면 맞았는지 보여 주고 정답 사자성어를 읽어 준다. 한자 한 글자씩 훈·음은 화면에 보여 준다.
 *  3. 유래가 있으면 이어서 읽어 주고, 다음 문제로 넘어가 다시 1부터 — 고른 때부터 적어도 10초, 다 읽은 뒤 적어도 2.5초.
 *
 * 오디오 (DEV_GUIDE §7.2–7.3): 읽어 주는 동안만 AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK 을 잡고 다 읽으면 반납한다.
 * 출발(SCREEN 이탈)·닫기·백그라운드·세션 종료·음성 게임의 발화·포커스 손실이면 즉시 멈추고,
 * 멈춘 음성은 저절로 이어 읽지 않는다 (사용자가 "다시 듣기"·"다음 문제"를 누른다).
 * 점수는 세션 동안 유지하고 새 세션에서 처음부터 시작한다.
 */
class IdiomQuizController(private val c: AppContainer, private val scope: CoroutineScope) {

    data class UiState(
        val question: IdiomQuizGame.IdiomQuestion? = null,
        /** 고른 보기 (null = 아직 안 고름) */
        val picked: Int? = null,
        val speaking: Boolean = false,
        /** 포커스를 얻지 못했거나 잃어서 읽어 주기가 멈춤 — 다음 문제로 저절로 넘어가지 않는다 */
        val voiceStopped: Boolean = false,
        /** 다 읽고 다음 문제로 넘어가기를 기다리는 중 */
        val advancing: Boolean = false,
        val score: Int = 0,
        val answered: Int = 0,
        val correct: Int = 0,
    ) {
        val revealed: Boolean get() = picked != null
        val pickedCorrect: Boolean get() = picked != null && picked == question?.answerIndex
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var game: IdiomQuizGame? = null
    private var speechJob: Job? = null
    /** 끊긴 발화의 finally 가 새 발화의 상태를 덮어쓰지 않게 하는 세대 번호 */
    private var generation = 0

    /** 이 게임이 오디오 포커스를 요청해 들고 있다 (포커스 변경은 [onFocusChange] 로 받는다) */
    var holdsFocus: Boolean = false
        private set

    private fun gameOrNull(): IdiomQuizGame? = game
        ?: c.content.content.value?.idioms
            ?.takeIf { it.size >= IdiomQuizGame.CHOICES }
            ?.let { IdiomQuizGame(it).also { g -> game = g } }

    /** 콘텐츠가 있어 게임을 열 수 있는지 */
    val isAvailable: Boolean get() = gameOrNull() != null

    // ── 사용자 동작 ──────────────────────────────────────────────

    /** 카드를 눌러 열기: 고르기 전 문제가 있으면 그 문제를 다시 읽고, 없거나 이미 골랐으면 새 문제를 낸다 */
    fun open() {
        val g = gameOrNull() ?: return
        val s = _state.value
        AppLog.i(TAG, "open (answered=${s.answered})")
        if (s.question == null || s.revealed) showNext(g) else speakQuestion()
    }

    /**
     * 화면이 다시 그려졌는데 문제가 없을 때(프로세스 재생성 뒤 복원 등) 소리 없이 문제만 준비한다.
     * 사용자가 누른 게 아니므로 읽어 주지 않는다 (자동 재생 금지).
     */
    fun prepare() {
        if (_state.value.question != null) return
        val q = gameOrNull()?.next() ?: return
        _state.update { it.copy(question = q, picked = null, voiceStopped = false, advancing = false) }
    }

    fun choose(index: Int) {
        val g = game ?: return
        val s = _state.value
        val q = s.question ?: return
        if (s.revealed || index !in q.choices.indices) return
        cancelSpeech()
        val ok = g.answer(q, index)
        AppLog.i(TAG, "answer ${q.item.id}: ${if (ok) "정답" else "오답"} (score=${g.score}, ${g.correct}/${g.answered})")
        _state.update {
            it.copy(picked = index, score = g.score, answered = g.answered, correct = g.correct, voiceStopped = false, advancing = false)
        }
        speakReveal(ok)
    }

    /** "다음 문제": 읽는 중이어도 바로 넘어간다 */
    fun next() {
        val g = gameOrNull() ?: return
        cancelSpeech()
        showNext(g)
    }

    /** "다시 듣기": 고르기 전이면 문제를, 고른 뒤면 정답·유래를 다시 읽는다 */
    fun replay() {
        val s = _state.value
        val q = s.question ?: return
        if (s.revealed) speakReveal(s.pickedCorrect) else speakQuestion()
    }

    /** "닫기"·뒤로: 읽기를 멈추고 카드 화면으로 (점수·풀던 문제는 남긴다) */
    fun close() = stop("close")

    // ── 코디네이터가 부르는 정리 ────────────────────────────────

    /** 읽기·자동 넘김을 멈추고 포커스를 반납한다. 아무것도 안 하고 있으면 그냥 돌아간다. */
    fun stop(reason: String) {
        // 끝난 Job 이 남아 있어도 "활동 중"으로 보지 않는다 — 음성 게임이 막 시작한 발화를 stopAll 로 끊지 않게
        val active = speechJob?.isActive == true
        if (!active && !holdsFocus) return
        AppLog.i(TAG, "stop ($reason)")
        cancelSpeech()
        if (active) c.speechRouter.stopAll()
        releaseFocus()
    }

    /** 새 세션: 점수와 문제를 처음부터 */
    fun reset() {
        stop("reset")
        game = null
        _state.value = UiState()
    }

    /** 이 게임이 포커스를 들고 있을 때의 포커스 변경. LOSS 류는 즉시 멈추고, GAIN 이 와도 저절로 이어 읽지 않는다. */
    fun onFocusChange(change: FocusChange, ownTts: Boolean) {
        when (change) {
            FocusChange.LOSS_TRANSIENT, FocusChange.LOSS -> {
                if (ownTts) {
                    // Pleos TTS 는 Speech Service 가 따로 포커스를 잡는다 → 우리 발화로 생긴 손실은 무시
                    c.audioFocus.markLost()
                    return
                }
                AppLog.i(TAG, "focus ${change.name} (통화·Gleo 등) → 읽기 즉시 중단, 자동 재개 없음")
                cancelSpeech()
                c.speechRouter.stopAll()
                releaseFocus()
                _state.update { it.copy(voiceStopped = true) }
            }
            FocusChange.GAIN, FocusChange.LOSS_TRANSIENT_CAN_DUCK -> Unit
        }
    }

    // ── 내부 ──────────────────────────────────────────────────

    private fun showNext(g: IdiomQuizGame) {
        val q = g.next() ?: return
        _state.update { it.copy(question = q, picked = null, voiceStopped = false, advancing = false) }
        speakQuestion()
    }

    private fun speakQuestion() {
        val q = _state.value.question ?: return
        speak(listOf(IdiomSpeech.question(q.item)), autoNext = false)
    }

    /** 고른 뒤: 정답(·유래)을 읽고, 한자 풀이를 고른 때부터 적어도 [REVEAL_MIN_MS] 보여 준 뒤 다음 문제로 */
    private fun speakReveal(correct: Boolean) {
        val q = _state.value.question ?: return
        speak(IdiomSpeech.afterAnswer(q.item, correct), autoNext = true, minTotalMs = REVEAL_MIN_MS)
    }

    /**
     * @param autoNext 다 읽으면 다음 문제로 — 다 읽은 뒤 적어도 [NEXT_DELAY_MS], 읽기 시작부터 적어도 [minTotalMs] 뒤
     */
    private fun speak(lines: List<String>, autoNext: Boolean, minTotalMs: Long = 0) {
        cancelSpeech()
        val gen = generation
        val startedAt = c.clock.elapsedMs()
        speechJob = scope.launch {
            _state.update { it.copy(speaking = true, voiceStopped = false, advancing = false) }
            var spoken = true
            try {
                for (line in lines) {
                    if (!ensureFocus()) {
                        spoken = false
                        break
                    }
                    c.speechRouter.speak(line)
                }
            } finally {
                if (gen == generation) _state.update { it.copy(speaking = false) }
            }
            releaseFocus()
            if (!spoken) {
                _state.update { it.copy(voiceStopped = true) }
                speechJob = null
                return@launch
            }
            if (autoNext) {
                _state.update { it.copy(advancing = true) }
                delay(maxOf(NEXT_DELAY_MS, minTotalMs - (c.clock.elapsedMs() - startedAt)))
                speechJob = null
                gameOrNull()?.let(::showNext)
            } else {
                speechJob = null
            }
        }
    }

    private fun cancelSpeech() {
        generation++
        speechJob?.cancel()
        speechJob = null
        _state.update { if (it.speaking || it.advancing) it.copy(speaking = false, advancing = false) else it }
    }

    private fun ensureFocus(): Boolean {
        if (c.audioFocus.hasFocus.value) {
            holdsFocus = true
            return true
        }
        val granted = c.audioFocus.request()
        if (granted) {
            // 처음 잡을 때만 새 스트림(선행 묵음) — 자기 Pleos TTS 때문에 잠깐 잃었다 다시 잡을 때는 아니다
            if (!holdsFocus) c.speechRouter.markNewStream()
            holdsFocus = true
        } else {
            AppLog.w(TAG, "audio focus denied → 읽지 않고 화면만 (자동 넘김 없음)")
        }
        return granted
    }

    private fun releaseFocus() {
        if (!holdsFocus) return
        holdsFocus = false
        c.audioFocus.abandon(force = true)
    }

    companion object {
        private const val TAG = "IdiomQuiz"

        /** 다 읽고 다음 문제로 넘어가기 전 최소 쉬는 시간 */
        const val NEXT_DELAY_MS = 2_500L

        /** 고른 뒤 정답·한자 풀이를 보여 주는 최소 시간 (유래가 없으면 정답만 읽어 금방 끝나므로 — 11/7 사용자 요청 "총 10초 정도") */
        const val REVEAL_MIN_MS = 10_000L
    }
}
