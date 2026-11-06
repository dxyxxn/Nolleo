package com.gildongmu.app.domain.engine

import com.gildongmu.app.core.Clock
import com.gildongmu.app.core.speech.SttEvent
import com.gildongmu.app.domain.drowsy.DrowsinessMonitor
import com.gildongmu.app.domain.drowsy.ResponseSample
import com.gildongmu.app.domain.games.Hangul
import com.gildongmu.app.domain.games.Judgement
import com.gildongmu.app.domain.games.Outcome
import com.gildongmu.app.domain.games.RegionQuizGame
import com.gildongmu.app.domain.games.Turn
import com.gildongmu.app.domain.games.TurnContext
import com.gildongmu.app.domain.games.VoiceGame
import com.gildongmu.app.domain.games.WordChainGame
import com.gildongmu.app.domain.games.WordDictionary
import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameContent
import com.gildongmu.app.domain.model.GameKind
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.domain.model.Player
import com.gildongmu.app.domain.model.RoundPlan
import com.gildongmu.app.domain.model.SessionPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 게임 루프 (지시서 §3·§6·§7): 판 시작 대기 → 출제(TTS) → 듣기(STT) → 판정 → 결과(TTS) → 다음 차례.
 *
 *  - PAUSED 진입 시 진행 중인 TTS/STT 를 즉시 멈추고(현재 문제 보존), 해제되면 1초 뒤 같은 문제를 다시 낸다.
 *  - 정차(화면 게임 가능)하면 진행 중인 차례만 끝내고 판을 마무리한다 → ModeArbiter 가 SCREEN 으로 전환.
 *  - 남은 시간 ≤ 5분이면 진행 중인 판을 마무리하고 결과를 정리한다.
 *  - 운전자 차례 응답의 지연을 DrowsinessMonitor 에 넘긴다 (판 첫 문제·일시정지 직후 첫 응답 제외).
 * 모든 대기는 StateFlow 이벤트로 깨어난다 (폴링·타이머 루프 없음).
 */
class GameSession(
    private val speech: SpeechPort,
    private val clock: Clock,
    private val content: GameContent,
    private val dictionary: WordDictionary,
    val players: List<Player>,
    initialPlan: SessionPlan,
    private val choice: GameChoice,
    private val demo: Boolean,
    val drowsy: DrowsinessMonitor,
    private val inputs: SessionInputs,
    private val callbacks: SessionCallbacks,
    val destinationName: String,
    val presetId: String?,
    private val random: Random = Random.Default,
    restore: SessionSnapshot? = null,
) {
    private var plan: SessionPlan = restore?.plan ?: initialPlan
    private var roundIndex = restore?.roundIndex ?: 0
    private var rotationIndex = restore?.rotationIndex ?: 0
    private var resumeTurnInRound = restore?.turnInRound ?: 0
    private var pendingQuestionId: String? = restore?.pendingQuestionId
    private var pendingTurn: Turn? = null
    private var currentGame: VoiceGame? = null
    private var restoredChainState: Map<String, String>? = restore?.chainState?.takeIf { it.isNotEmpty() }
    private var longestChain = 0

    private val quiz = RegionQuizGame(content, random).also { q -> restore?.quizState?.let { q.restoreState(it) } }
    private var chain: WordChainGame? = null

    private var planBasisRemainSec: Int = plan.basisRemainSec
    private var planBasisAtMs: Long = clock.elapsedMs()

    private val skipWait = MutableStateFlow(false)

    private val _state = MutableStateFlow(
        SessionState(
            plan = plan,
            roundIndex = roundIndex,
            scores = restore?.scores ?: players.associate { it.id to 0 },
            correctCounts = restore?.correctCounts ?: players.associate { it.id to 0 },
            regionsVisited = restore?.regionsVisited.orEmpty(),
            roundsPlayed = restore?.roundsPlayed.orEmpty(),
            difficultyCap = drowsy.difficultyCap,
        ),
    )
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val driver: Player get() = players.first { it.isDriver }

    /** "다음 판 지금 시작" 버튼 */
    fun requestNextRoundNow() {
        skipWait.value = true
    }

    /**
     * 남은 시간이 기대값 대비 ±20% 이상 바뀌면 남은 판만 다시 계산한다 (진행 중인 판은 유지).
     * @param timeScale 여정 초 / 실제 초 (시뮬레이션 압축 배율)
     */
    fun onTripUpdate(remainSec: Int, timeScale: Float) {
        val elapsedTripSec = ((clock.elapsedMs() - planBasisAtMs) / 1000.0 * timeScale).toInt()
        val expected = planBasisRemainSec - elapsedTripSec
        if (expected <= SessionPlanner.END_BUFFER_SEC) return
        if (!SessionPlanner.needsReplan(expected, remainSec)) return
        val keep = roundIndex + if (currentGame != null) 1 else 0
        val newPlan = SessionPlanner.replanRemaining(plan, keep, remainSec, choice, demo)
        callbacks.log("SessionPlanner: remain ${expected}s → ${remainSec}s (±20%↑) → ${plan.roundCount}판 → ${newPlan.roundCount}판")
        plan = newPlan
        planBasisRemainSec = remainSec
        planBasisAtMs = clock.elapsedMs()
        _state.update { it.copy(plan = newPlan) }
    }

    suspend fun run() = coroutineScope {
        callbacks.log("session start: ${plan.roundCount} rounds, players=${players.size}, choice=$choice, demo=$demo")
        val regionJob = launch {
            inputs.regionCode.filterNotNull().collect { code ->
                _state.update { s -> if (s.regionsVisited.lastOrNull() == code) s else s.copy(regionsVisited = s.regionsVisited + code) }
            }
        }
        // "PAUSED 직후 첫 응답"을 졸음 지표에서 빼기 위해, 일시정지가 있었는지 기억한다
        val pauseJob = launch {
            inputs.mode.collect { m -> if (m == GameMode.PAUSED) pausedSinceDriverAnswer = true }
        }
        try {
            mainLoop()
            finish()
        } finally {
            regionJob.cancel()
            pauseJob.cancel()
            callbacks.onVoiceRoundActive(false)
        }
    }

    @Volatile private var pausedSinceDriverAnswer = false

    private suspend fun mainLoop() {
        while (true) {
            if (arrivalImminent()) return
            val round = plan.rounds.getOrNull(roundIndex)
            if (round == null) {
                setPhase(SessionPhase.ALL_DONE)
                _state.update { it.copy(nextRoundAtRemainSec = null) }
                val (_, _, skip) = waitFor { remain, _, skip -> arrival(remain) || skip }
                if (!skip) return
                skipWait.value = false
                // 준비한 판을 다 했어도 가족이 원하면 보너스 판
                val bonus = RoundPlan(
                    index = plan.roundCount,
                    game = SessionPlanner.gameFor(plan.roundCount, choice),
                    targetDurationSec = SessionPlanner.MIN_ROUND_SEC,
                    questionCount = if (demo) SessionPlanner.DEMO_QUESTION_COUNT else 8,
                    startAtRemainSec = Int.MAX_VALUE,
                )
                plan = plan.copy(rounds = plan.rounds + bonus)
                _state.update { it.copy(plan = plan) }
                continue
            }
            if (!waitForRound(round)) return
            playRound(round)
            roundIndex++
            rotationIndex = 0
            resumeTurnInRound = 0
            _state.update { it.copy(roundIndex = roundIndex) }
        }
    }

    // ── 대기 ────────────────────────────────────────────────────

    private data class WaitResult(val remain: Int?, val mode: GameMode, val skip: Boolean)

    private suspend fun waitFor(predicate: (Int?, GameMode, Boolean) -> Boolean): WaitResult =
        combine(inputs.remainSec, inputs.mode, skipWait) { r, m, s -> WaitResult(r, m, s) }
            .first { predicate(it.remain, it.mode, it.skip) }

    private suspend fun waitForRound(round: RoundPlan): Boolean {
        val first = round.index == 0
        setPhase(if (first) SessionPhase.WAITING else SessionPhase.RESTING)
        _state.update {
            it.copy(
                nextRoundAtRemainSec = if (first) null else round.startAtRemainSec,
                caption = null,
                display = null,
                roundIndex = round.index,
                roundGame = round.game,
                turnInRound = 0,
                turnsInRound = round.questionCount,
            )
        }
        val result = waitFor { remain, mode, skip ->
            arrival(remain) || (mode == GameMode.VOICE && (skip || first || remain == null || remain <= round.startAtRemainSec))
        }
        skipWait.value = false
        return !arrival(result.remain)
    }

    /** VOICE 가 될 때까지 기다린다. 일시정지 중 정차(화면 게임 가능)·도착이면 false → 판 마무리 */
    private suspend fun awaitVoice(): Boolean {
        if (inputs.mode.value == GameMode.VOICE) return true
        _state.update { it.copy(mic = MicState.IDLE) }
        val result = combine(inputs.mode, inputs.screenAvailable, inputs.remainSec) { m, s, r -> Triple(m, s, r) }
            .first { (m, s, r) -> m == GameMode.VOICE || s || arrival(r) }
        return result.first == GameMode.VOICE && !arrival(result.third)
    }

    private fun arrival(remain: Int?): Boolean = remain != null && remain <= SessionPlanner.END_BUFFER_SEC

    private fun arrivalImminent(): Boolean = arrival(inputs.remainSec.value)

    // ── 판 진행 ──────────────────────────────────────────────────

    private suspend fun playRound(round: RoundPlan) {
        val game: VoiceGame = when (round.game) {
            GameKind.WORD_CHAIN -> WordChainGame(dictionary, random) { word -> callbacks.log("WordChain: lenient accept '$word'") }
                .also { g ->
                    restoredChainState?.let { g.restoreState(it) }
                    restoredChainState = null
                    chain = g
                }
            else -> quiz
        }
        currentGame = game
        val rotation = game.rotation(players)
        callbacks.onVoiceRoundActive(true)
        speech.markNewStream()
        _state.update {
            it.copy(roundIndex = round.index, roundGame = round.game, turnsInRound = round.questionCount, turnInRound = resumeTurnInRound)
        }
        callbacks.log("round ${round.index + 1}/${plan.roundCount} start: ${round.game.label} x${round.questionCount}")

        val introLine = if (resumeTurnInRound > 0) "이어서 할게요!" else "${round.index + 1}번째 판! ${game.intro(players, round.questionCount)}"
        say(introLine, SessionPhase.INTRO)

        var humanTurns = resumeTurnInRound
        var firstHumanTurn = resumeTurnInRound == 0
        resumeTurnInRound = 0
        var wrapLine: String? = null

        while (humanTurns < round.questionCount) {
            if (inputs.screenAvailable.value) {
                wrapLine = "차가 멈췄네요. 이번 판은 여기까지 할게요! 화면 게임이 열렸어요."
                break
            }
            if (arrivalImminent()) {
                wrapLine = "곧 도착이에요. 이번 판은 여기까지 할게요!"
                break
            }
            if (!awaitVoice()) break

            val player = rotation[rotationIndex % rotation.size]
            val next = rotation[(rotationIndex + 1) % rotation.size]
            val restoredQuizTurn = pendingQuestionId?.takeIf { game === quiz && player.id != Player.APP.id }
                ?.let { id -> quiz.turnFor(id, player) }
            pendingQuestionId = null
            val turn = pendingTurn
                ?: restoredQuizTurn
                ?: game.nextTurn(TurnContext(player, next, inputs.regionCode.value, drowsy.difficultyCap))
                ?: break
            pendingTurn = turn

            if (!turn.expectsAnswer) {
                _state.update { it.copy(currentPlayer = player, display = turn.display, shortDisplay = turn.shortDisplay, partial = null) }
                // 앱 차례(끝말잇기 낱말)가 일시정지로 끊기면 재개 후 다시 말한다 — 다음 사람이 낱말을 들어야 한다
                if (!sayFully(turn.prompt)) break
                pendingTurn = null
                rotationIndex++
                if (turn.endsRoundAfter) break
                continue
            }

            val asked = askAndListen(turn) ?: break
            val (first, interruptedTurn) = asked
            val interrupted = interruptedTurn || (player.isDriver && pausedSinceDriverAnswer)
            if (player.isDriver) pausedSinceDriverAnswer = false
            var judgement = game.judge(turn, first.text, 1)
            var heard = first.text
            if (judgement.outcome == Outcome.RETRY) {
                say(judgement.feedback, SessionPhase.FEEDBACK)
                val second = listenOnly(turn)
                heard = second?.text
                judgement = game.judge(turn, second?.text, 2)
            }
            pendingTurn = null
            heard?.let { callbacks.logSensitive("heard(${player.name})", it) }
            applyScore(player, judgement)
            _state.update { it.copy(lastAnswer = AnswerLine(player.name, heard, judgement.label), partial = null) }

            val action = if (player.isDriver) recordDrowsy(first, judgement, turn, eligible = !firstHumanTurn && !interrupted) else DrowsinessMonitor.Action.None

            say(judgement.feedback, SessionPhase.FEEDBACK)
            if (action is DrowsinessMonitor.Action.Stage1) {
                callbacks.onStage1(player, action.newDifficultyCap)
                _state.update { it.copy(difficultyCap = action.newDifficultyCap) }
                say("${player.name}, 운전 오래 하셨죠? 다음 문제는 조금 쉬운 걸로 가 볼게요.", SessionPhase.FEEDBACK)
            }

            humanTurns++
            firstHumanTurn = false
            rotationIndex++
            _state.update { it.copy(turnInRound = humanTurns) }
            (game as? WordChainGame)?.let { longestChain = maxOf(longestChain, it.chainLength) }
            callbacks.onSnapshot(snapshot(turnInRound = humanTurns))
            if (judgement.endsRound) break
        }

        wrapLine?.let { say(it, SessionPhase.ROUND_END) }
        if (inputs.mode.value == GameMode.VOICE) say("${game.outro()} ${scoreLine()}", SessionPhase.ROUND_END)
        (game as? WordChainGame)?.let { longestChain = maxOf(longestChain, it.chainLength) }
        _state.update { it.copy(roundsPlayed = it.roundsPlayed + round.game, currentPlayer = null, display = null, shortDisplay = null) }
        currentGame = null
        chain = null
        pendingTurn = null
        callbacks.onVoiceRoundActive(false)
        callbacks.log("round ${round.index + 1} end")
        callbacks.onSnapshot(snapshot(turnInRound = 0, nextRound = true))
    }

    /** 출제 → 듣기. 일시정지로 끊기면 같은 문제를 다시 낸다. @return (첫 응답, 일시정지로 끊긴 적 있음) / null = 판 마무리 */
    private suspend fun askAndListen(turn: Turn): Pair<AnswerResult, Boolean>? {
        var interrupted = false
        var fatalRetries = 0
        while (true) {
            if (!awaitVoice()) return null
            if (interrupted) {
                delay(RESUME_DELAY_MS)
                if (inputs.mode.value != GameMode.VOICE) continue
            }
            val prompt = if (interrupted) turn.prompt.ifBlank { turn.reprompt } else turn.prompt
            _state.update { it.copy(currentPlayer = turn.player, display = turn.display, shortDisplay = turn.shortDisplay, partial = null) }
            val result = interruptible {
                if (prompt.isNotBlank()) {
                    _state.update { it.copy(phase = SessionPhase.ASKING, caption = prompt, mic = MicState.SPEAKING) }
                    speech.speak(prompt)
                }
                listenNow(turn)
            }
            _state.update { it.copy(mic = MicState.IDLE) }
            if (result == null) {
                interrupted = true
                callbacks.log("turn interrupted (mode=${inputs.mode.value}) → 재개 시 같은 문제")
                continue
            }
            if (result.fatalError && fatalRetries < 2) {
                fatalRetries++
                callbacks.log("STT fatal error ${result.error} → 폴백 엔진으로 같은 문제 다시")
                continue
            }
            return result to interrupted
        }
    }

    private suspend fun listenOnly(turn: Turn): AnswerResult? {
        if (!awaitVoice()) return null
        val result = interruptible { listenNow(turn) }
        _state.update { it.copy(mic = MicState.IDLE) }
        return result
    }

    private suspend fun listenNow(turn: Turn): AnswerResult {
        val t0 = clock.elapsedMs()
        _state.update { it.copy(phase = SessionPhase.LISTENING, mic = MicState.LISTENING) }
        val events = speech.listen(AnswerCollector.FIRST_SPEECH_TIMEOUT_MS, turn.hint).onEach { e ->
            if (e is SttEvent.Partial) _state.update { it.copy(partial = e.text) }
        }
        val result = AnswerCollector.collect(events, t0, clock)
        callbacks.log("answer ${turn.player.name}: latency=${result.latencyMs}ms timedOut=${result.timedOut}${result.error?.let { " error=$it" } ?: ""}")
        return result
    }

    /** 일시정지(모드가 VOICE 가 아니게 됨)되면 진행 중인 블록을 취소하고 TTS/STT 를 즉시 멈춘다. null = 끊김 */
    private suspend fun <T : Any> interruptible(block: suspend () -> T): T? {
        if (inputs.mode.value != GameMode.VOICE) return null
        val result: T? = coroutineScope {
            val work = async { block() }
            val watcher = launch {
                inputs.mode.first { it != GameMode.VOICE }
                work.cancel(CancellationException("paused"))
            }
            val value = try {
                work.await()
            } catch (e: CancellationException) {
                null
            }
            watcher.cancel()
            value
        }
        currentCoroutineContext().ensureActive()
        if (result == null) speech.stop()
        return result
    }

    /** 끝까지 말할 때까지(일시정지되면 재개 후 처음부터) 반복. 판 마무리가 필요하면 false */
    private suspend fun sayFully(text: String): Boolean {
        if (text.isBlank()) return true
        var resumed = false
        while (true) {
            if (!awaitVoice()) return false
            if (resumed) delay(RESUME_DELAY_MS)
            if (inputs.mode.value != GameMode.VOICE) continue
            _state.update { it.copy(phase = SessionPhase.ASKING, caption = text, mic = MicState.SPEAKING) }
            val done = interruptible { speech.speak(text) }
            _state.update { it.copy(mic = MicState.IDLE) }
            if (done != null) return true
            resumed = true
        }
    }

    private suspend fun say(text: String, phase: SessionPhase) {
        if (text.isBlank() || inputs.mode.value != GameMode.VOICE) return
        _state.update { it.copy(phase = phase, caption = text, mic = MicState.SPEAKING) }
        interruptible { speech.speak(text) }
        _state.update { it.copy(mic = MicState.IDLE) }
    }

    private fun setPhase(phase: SessionPhase) = _state.update { it.copy(phase = phase) }

    private fun applyScore(player: Player, judgement: Judgement) {
        if (judgement.scoreDelta == 0 && judgement.correct != true) return
        _state.update { s ->
            s.copy(
                scores = s.scores + (player.id to (s.scores[player.id] ?: 0) + judgement.scoreDelta),
                correctCounts = if (judgement.correct == true) s.correctCounts + (player.id to (s.correctCounts[player.id] ?: 0) + 1) else s.correctCounts,
            )
        }
    }

    private fun recordDrowsy(result: AnswerResult, judgement: Judgement, turn: Turn, eligible: Boolean): DrowsinessMonitor.Action {
        if (!eligible) {
            callbacks.log("Drowsiness: 샘플 제외(판 첫 문제 또는 일시정지 직후)")
            return DrowsinessMonitor.Action.None
        }
        val before = drowsy.snapshot()
        val sample = ResponseSample(
            latencyMs = result.latencyMs,
            correct = judgement.correct,
            difficulty = turn.difficulty,
            atEpochMs = clock.wallMs(),
            driveState = inputs.driveState.value,
        )
        val action = drowsy.record(sample, clock.wallMs())
        val after = drowsy.snapshot()
        if (before.calibrating) {
            callbacks.log("Drowsiness: 기준을 만드는 중 (${if (after.calibrating) after.calibrationCount else 5}/5)" +
                (after.baselineMs?.let { " → 기준 ${it}ms" } ?: ""))
        } else {
            callbacks.log("Drowsiness: latency=${result.latencyMs}ms baseline=${after.baselineMs}ms slow=${after.slowCount}/5 cap=${after.difficultyCap}")
        }
        if (action is DrowsinessMonitor.Action.Stage1) callbacks.log("Drowsiness: Stage1 → 말 걸기 + 난이도 상한 ${action.newDifficultyCap}")
        return action
    }

    private fun scoreLine(): String {
        val s = _state.value
        val ranked = players.sortedByDescending { s.scores[it.id] ?: 0 }
        val top = ranked.firstOrNull() ?: return ""
        val topScore = s.scores[top.id] ?: 0
        return if (topScore == 0) "다음 판에 더 잘해 봐요!" else "지금까지 ${Hangul.iGa(top.name)} ${topScore}점으로 1등이에요."
    }

    private suspend fun finish() {
        setPhase(SessionPhase.FINISHING)
        val summary = buildSummary()
        if (inputs.mode.value == GameMode.VOICE) {
            val winner = summary.winner
            val line = buildString {
                append("곧 도착이에요! 오늘 길동무는 여기까지예요. ")
                if (winner != null) append("오늘의 1등은 ${Hangul.ieyo(winner.name)}. ")
                append("모두 ${summary.totalCorrect}번 맞혔어요. 즐거운 여행 되세요!")
            }
            say(line, SessionPhase.FINISHING)
        }
        setPhase(SessionPhase.FINISHED)
        callbacks.log("session finished: correct=${summary.totalCorrect}, regions=${summary.regionsVisited}")
        callbacks.onFinished(summary)
    }

    fun buildSummary(): SessionSummary {
        val s = _state.value
        return SessionSummary(
            destinationName = destinationName,
            players = players,
            scores = s.scores,
            correctCounts = s.correctCounts,
            regionsVisited = s.regionsVisited,
            roundsPlayed = s.roundsPlayed,
            stage1Count = drowsy.snapshot().stage1Count,
            longestChain = maxOf(longestChain, chain?.chainLength ?: 0),
        )
    }

    fun snapshot(turnInRound: Int = _state.value.turnInRound, nextRound: Boolean = false): SessionSnapshot {
        val s = _state.value
        val d = drowsy.snapshot()
        return SessionSnapshot(
            destinationName = destinationName,
            presetId = presetId,
            players = players,
            choice = choice,
            demo = demo,
            plan = plan,
            roundIndex = if (nextRound) roundIndex + 1 else roundIndex,
            turnInRound = turnInRound,
            rotationIndex = if (nextRound) 0 else rotationIndex,
            scores = s.scores,
            correctCounts = s.correctCounts,
            regionsVisited = s.regionsVisited,
            roundsPlayed = s.roundsPlayed,
            quizState = quiz.saveState(),
            chainState = chain?.saveState().orEmpty(),
            pendingQuestionId = pendingTurn?.questionId,
            drowsyWindow = drowsy.windowSamples,
            difficultyCap = d.difficultyCap,
            lastTriggerAtMs = d.lastTriggerAtMs,
            stage1Count = d.stage1Count,
            savedAtWallMs = clock.wallMs(),
        )
    }

    companion object {
        /** 일시정지 해제(GAIN 등) 후 같은 문제를 다시 낼 때까지 (지시서 §5.4: 1초 뒤 재출제) */
        const val RESUME_DELAY_MS = 1_000L
    }
}
