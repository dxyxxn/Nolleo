package com.nolleo.app.domain.engine

import com.nolleo.app.core.Clock
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.domain.games.Hangul
import com.nolleo.app.domain.games.Outcome
import com.nolleo.app.domain.games.Turn
import com.nolleo.app.domain.games.TurnContext
import com.nolleo.app.domain.games.WordChainGame
import com.nolleo.app.domain.games.WordDictionary
import com.nolleo.app.domain.model.Player
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlin.random.Random

/**
 * 정차 중 끝말잇기 — 주행 중 음성 판과 같은 규칙([WordChainGame])으로 **말로** 잇는다
 * (11/8 사용자 요청: 넷 중에 고르기 대신 진짜 끝말잇기).
 *
 *  - 놀레오 → 운전자 → 동승자 순서로 돈다. 판 길이·졸음 지표는 없다(정차 중 화면 게임은 음성 판과 따로 논다).
 *  - 사람 차례는 [AnswerCollector.FIRST_SPEECH_TIMEOUT_MS](8초) 안에 말을 시작해야 한다 — 화면에 남은 초를 보인다([State.listenDeadlineMs]).
 *  - 놀레오가 이을 말이 없어 지거나([End.APP_LOST]), 사람 차례가 잇달아 대답이 없으면([End.NO_ANSWER]) 끝난다 —
 *    사람 수만큼(적어도 2번) 잇달아 무응답이면. 아무도 하지 않는데 마이크를 계속 열어 두지 않게.
 *  - 들은 말은 [logSensitive] 로만 남긴다 (릴리스에서 가림).
 *  - [run] 을 부른 코루틴을 취소하면 그 자리에서 멈춘다. 다시 [run] 을 부르면 같은 차례를 다시 낸다(이어진 낱말은 그대로).
 */
class WordChainPlay(
    dictionary: WordDictionary,
    private val players: List<Player>,
    private val speech: SpeechPort,
    private val clock: Clock,
    random: Random = Random.Default,
    private val log: (String) -> Unit = {},
    private val logSensitive: (label: String, value: String) -> Unit = { _, _ -> },
) {
    enum class Phase { READY, SPEAKING, LISTENING, ENDED }

    enum class End(val title: String) {
        APP_LOST("놀레오가 졌어요"),
        NO_ANSWER("대답이 없어서 멈췄어요"),
    }

    data class State(
        val phase: Phase = Phase.READY,
        /** 지금까지 이은 낱말 (다른 말로 다시 시작한 것 포함) */
        val words: List<String> = emptyList(),
        /** 지금 차례 (놀레오 포함) */
        val player: Player? = null,
        /** "'차'로 시작하는 말" — 아직 낱말이 없으면 null */
        val needed: String? = null,
        /** 답을 시작해야 하는 마감 시각([Clock.elapsedMs] 기준). 말을 시작하면 null */
        val listenDeadlineMs: Long? = null,
        val partial: String? = null,
        val lastAnswer: AnswerLine? = null,
        val score: Int = 0,
        val answered: Int = 0,
        val correct: Int = 0,
        val ended: End? = null,
    )

    private val game = WordChainGame(dictionary, random) { word -> logSensitive("lenient accept", word) }
    private val rotation = game.rotation(players)
    private val silentLimit = maxOf(SILENT_TURNS_MIN, players.size)
    private var rotationIndex = 0
    private var pendingTurn: Turn? = null
    private var started = false
    private var silentInARow = 0

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    suspend fun run() {
        if (_state.value.ended != null) return
        if (!started) {
            say(game.intro(players, 0))
            started = true
        }
        while (true) {
            val player = rotation[rotationIndex % rotation.size]
            val next = rotation[(rotationIndex + 1) % rotation.size]
            val resumed = pendingTurn != null
            val turn = pendingTurn ?: game.nextTurn(TurnContext(player, next, null, MAX_DIFFICULTY)) ?: return end(End.APP_LOST)
            pendingTurn = turn
            // 새 차례에는 앞사람 판정 줄을 지운다 (방금 답은 칭찬 뒤 [WordChainGame.TURN_HOLD_MS] 동안만 보인다)
            _state.update { it.copy(player = player, words = game.words, needed = neededPhrase(), lastAnswer = null) }

            if (!turn.expectsAnswer) {
                // 놀레오 차례: 낱말을 말하고 다음 사람에게 (끊겼다 이어도 같은 말을 다시 한다)
                say(turn.prompt)
                pendingTurn = null
                rotationIndex++
                _state.update { it.copy(words = game.words, needed = neededPhrase()) }
                if (turn.endsRoundAfter) return end(End.APP_LOST)
                // 놀레오도 한 차례 — 사람 차례처럼 잠깐 쉬고 다음 사람에게 (11/11)
                hold()
                continue
            }

            say(if (resumed) turn.prompt.ifBlank { turn.reprompt } else turn.prompt)
            var result = listen(turn)
            var heard = result.text
            var judgement = game.judge(turn, heard, 1)
            if (judgement.outcome == Outcome.RETRY) {
                say(judgement.feedback)
                result = listen(turn)
                heard = result.text
                judgement = game.judge(turn, heard, 2)
            }
            pendingTurn = null
            rotationIndex++
            silentInARow = if (judgement.outcome == Outcome.NO_ANSWER) silentInARow + 1 else 0
            _state.update { s ->
                s.copy(
                    words = game.words,
                    needed = neededPhrase(),
                    lastAnswer = AnswerLine(player.name, heard, judgement.label),
                    score = s.score + if (judgement.correct == true) judgement.scoreDelta else 0,
                    answered = s.answered + 1,
                    correct = s.correct + if (judgement.correct == true) 1 else 0,
                )
            }
            heard?.let { logSensitive("heard(${player.name})", it) }
            log("${player.name}: ${judgement.outcome.name} (${game.chainLength}개)")
            // 잇달아 대답이 없으면 "다음 사람이 이어 볼까요?" 대신 끝을 말한다
            if (silentInARow >= silentLimit) return end(End.NO_ANSWER)
            say(judgement.feedback)
            hold()
        }
    }

    /** 한 차례 뒤 쉬는 시간 — 말하지도 듣지도 않는다 (사람 답이면 판정 줄이 그동안 보인다) */
    private suspend fun hold() {
        _state.update { it.copy(phase = Phase.READY) }
        delay(WordChainGame.TURN_HOLD_MS)
    }

    private suspend fun listen(turn: Turn): AnswerResult {
        val t0 = clock.elapsedMs()
        _state.update { it.copy(phase = Phase.LISTENING, listenDeadlineMs = t0 + AnswerCollector.FIRST_SPEECH_TIMEOUT_MS, partial = null) }
        try {
            val events = speech.listen(turn.hint).onEach { e ->
                when (e) {
                    is SttEvent.SpeechDetected -> _state.update { it.copy(listenDeadlineMs = null) }
                    is SttEvent.Partial -> if (e.text.isNotBlank()) _state.update { it.copy(listenDeadlineMs = null, partial = e.text) }
                    else -> Unit
                }
            }
            return AnswerCollector.collect(events, t0, clock).also { r ->
                r.error?.let { log("STT error $it") }
            }
        } finally {
            _state.update { it.copy(listenDeadlineMs = null, partial = null) }
        }
    }

    private suspend fun say(text: String) {
        if (text.isBlank()) return
        _state.update { it.copy(phase = Phase.SPEAKING) }
        speech.speak(text)
    }

    private suspend fun end(reason: End) {
        val line = when (reason) {
            End.APP_LOST -> game.outro()
            End.NO_ANSWER -> "대답이 없어서 끝말잇기는 여기까지 할게요. 모두 ${game.chainLength}개 낱말을 이었어요."
        }
        log("end: ${reason.name} (${game.chainLength}개)")
        say(line)
        _state.update { it.copy(phase = Phase.ENDED, ended = reason, player = null) }
    }

    private fun neededPhrase(): String? = game.currentWord?.last()?.let { "${Hangul.startsWithPhrase(it)} 시작하는 말" }

    companion object {
        /** 정차 중에는 졸음 케어가 없으므로 난이도 상한을 두지 않는다 */
        private const val MAX_DIFFICULTY = 3

        /** 이만큼(사람이 더 많으면 사람 수만큼) 잇달아 대답이 없으면 끝낸다 */
        const val SILENT_TURNS_MIN = 2
    }
}
