package com.nolleo.app.domain.engine

import com.nolleo.app.core.Clock
import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.domain.drowsy.DrowsinessMonitor
import com.nolleo.app.domain.drowsy.ResponseSample
import com.nolleo.app.domain.games.Hangul
import com.nolleo.app.domain.games.Judgement
import com.nolleo.app.domain.games.Outcome
import com.nolleo.app.domain.games.RegionQuizGame
import com.nolleo.app.domain.games.Turn
import com.nolleo.app.domain.games.TurnContext
import com.nolleo.app.domain.games.VoiceGame
import com.nolleo.app.domain.games.WordChainGame
import com.nolleo.app.domain.games.WordDictionary
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.RoundPlan
import com.nolleo.app.domain.model.SessionPlan
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 게임 루프: 판 시작 대기 → 출제(TTS) → 듣기(STT) → 판정 → 결과(TTS) → 다음 차례.
 *
 *  - PAUSED 진입 시 진행 중인 TTS/STT 를 즉시 멈추고(현재 문제 보존), 해제되면 1초 뒤 같은 문제를 다시 낸다.
 *  - 정차(화면 게임 가능)하면 진행 중인 차례만 끝내고 판을 마무리한다 → ModeArbiter 가 SCREEN 으로 전환.
 *  - 남은 시간 ≤ 5분이면 진행 중인 판을 마무리하고 결과를 정리한다 (목적지 없이 정한 시간만큼 노는 세션도 같은 기준).
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
    val demo: Boolean,
    val drowsy: DrowsinessMonitor,
    private val inputs: SessionInputs,
    private val callbacks: SessionCallbacks,
    val destinationName: String,
    val presetId: String?,
    /** 목적지 없이 정한 시간만큼 논다 — 끝맺는 말이 "곧 도착이에요" 대신 "정한 시간이 다 됐어요" */
    val noDestination: Boolean = false,
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
    /** 끝말잇기 최장 = 한 판에 이은 낱말 수(다른 말로 다시 시작한 전후 합산, [WordChainGame.chainLength]) */
    private var longestChain = restore?.longestChain ?: 0

    private val quiz = RegionQuizGame(content, random).also { q -> restore?.quizState?.let { q.restoreState(it) } }
    private var chain: WordChainGame? = null

    // 시뮬 여정을 이어서 하면 저장할 때의 남은 시간이 기준 — 판 일정이 원래 시간표 그대로 이어진다
    private var planBasisRemainSec: Int = restore?.simRemainSec ?: plan.basisRemainSec
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

    // ── 졸음 케어 2단계 (휴식 권유) ─────────────────────────────

    private val restDecision = MutableStateFlow<Boolean?>(null)
    @Volatile private var awaitingRestDecision = false
    @Volatile private var forceStage2 = false
    /** 한 번 말할 안내(졸음쉼터 도착·다시 출발). 판 사이 쉬는 시간에도 VOICE 가 되면 바로 말한다 */
    private data class Announcement(val line: String, val holdAfter: Boolean)
    private val announcement = MutableStateFlow<Announcement?>(null)

    /** 쉼터 도착 안내 뒤 "쉬는 중" — 다시 출발([onRestFinished])하거나 계속([releaseRestHold])을 누를 때까지 문제를 내지 않는다 */
    private val restHold = MutableStateFlow(false)

    /** @return 도착 안내 뒤 쉬었다 왔으면 true — 호출한 쪽은 정차·도착 여부를 다시 확인한다 */
    private suspend fun sayPendingAnnouncement(): Boolean {
        val a = announcement.value ?: return false
        announcement.value = null
        say(a.line, SessionPhase.FEEDBACK)
        if (a.holdAfter) holdForRest()
        return a.holdAfter
    }

    /**
     * 쉼터에 도착했다고 말한 직후 바로 다음 문제를 내지 않도록 기다린다. 정차해서 화면 게임이 열리면(판 마무리) 또는
     * 곧 도착이면 기다림을 끝내 호출한 쪽이 판을 정리하게 둔다. 다시 출발해서 풀리면 "잘 쉬셨나요?"부터 말한다.
     */
    private suspend fun holdForRest() {
        restHold.value = true
        callbacks.onRestHold()
        callbacks.log("RestHold: 쉼터 도착 안내 뒤 다시 출발할 때까지 대기")
        val (held, _) = combine(restHold, inputs.screenAvailable, inputs.remainSec) { h, s, r -> h to (s || arrival(r)) }
            .first { (h, leave) -> !h || leave }
        if (!held && awaitVoice()) sayPendingAnnouncement()
    }

    /** 디버그 패널 "2단계 시험": 다음 운전자 답 뒤에 2단계를 낸다 */
    fun requestStage2() {
        forceStage2 = true
        callbacks.log("Drowsiness: 2단계 시험 예약 (다음 운전자 답 뒤)")
    }

    /** 화면의 "쉬어 가기"/"괜찮아요" 버튼. 음성 질문을 기다리는 중이면 그 답으로 쓰고 true */
    fun answerRestSuggestion(accept: Boolean): Boolean {
        if (!awaitingRestDecision) return false
        restDecision.value = accept
        return true
    }

    /**
     * 다음 차례 앞에 한 번 말할 안내 (마지막 것만 남긴다).
     * @param holdAfter 말한 뒤 다시 출발할 때까지 게임을 쉰다 (졸음쉼터 도착 안내)
     */
    fun announce(line: String, holdAfter: Boolean = false) {
        announcement.value = Announcement(line, holdAfter)
    }

    /** 졸음쉼터에서 쉬고 다시 출발: 응답 창·난이도를 처음으로 되돌리고 인사한다 */
    fun onRestFinished() {
        drowsy.resetAfterRest()
        _state.update { it.copy(difficultyCap = drowsy.difficultyCap) }
        announce("${driver.name}, 잘 쉬셨나요? 다시 출발해요!")
        restHold.value = false
        callbacks.log("Drowsiness: 휴식 후 다시 출발 → 응답 창·난이도 초기화")
    }

    /** 쉼터에서 "계속"을 눌러 바로 이어 할 때 */
    fun releaseRestHold() {
        if (!restHold.value) return
        restHold.value = false
        callbacks.log("RestHold: 계속 → 바로 이어서")
    }

    private suspend fun offerRest(driver: Player, action: DrowsinessMonitor.Action.Stage2) {
        callbacks.log("Drowsiness: Stage2 (${action.reason.label}) → 휴식 권유 + 경로 위 휴게소·졸음쉼터")
        callbacks.headingRestStop()?.let { target ->
            // 이미 쉼 장소로 가는 중("계속"으로 게임을 이어 감) — 다른 곳을 묻지 않는다. 화면 카드와 말이 같은 곳을 가리키게
            callbacks.log("Stage2: 이미 쉼 장소로 가는 중 → 새로 묻지 않고 다시 알림")
            callbacks.onStage2(driver, target)
            say("${driver.name}, 반응이 계속 느려졌어요. ${target.name}에서 꼭 쉬어 가요.", SessionPhase.FEEDBACK)
            return
        }
        val suggestion = callbacks.suggestRestStop()
        callbacks.onStage2(driver, suggestion)
        if (suggestion == null) {
            say("${driver.name}, 반응이 계속 느려졌어요. 가까운 졸음쉼터나 휴게소에서 잠깐 쉬어 가요.", SessionPhase.FEEDBACK)
            callbacks.onRestDecision(null, false)
            return
        }
        val accepted = askYesNo("${driver.name}, 반응이 계속 느려졌어요. ${suggestion.spokenTarget} 거기서 잠깐 쉬어 갈까요?")
        callbacks.log("Stage2 응답: ${when (accepted) { true -> "네"; false -> "아니요"; null -> "응답 없음" }}")
        // 수락하면 곧 게임이 쉬므로(PauseReason.REST_STOP) 안내 말을 먼저 하고 결정을 넘긴다
        if (accepted == true) {
            say("좋아요. ${suggestion.name}에서 쉬어 가요. 도착할 때까지 게임은 쉴게요.", SessionPhase.FEEDBACK)
        } else {
            say("알겠어요. 대신 다음 문제는 쉬운 걸로 갈게요. 피곤하면 언제든 쉬어 가요.", SessionPhase.FEEDBACK)
        }
        callbacks.onRestDecision(suggestion, accepted == true)
    }

    private data class Decision(val value: Boolean?)

    /**
     * 네/아니요 질문: 음성(STT) 또는 화면 버튼 중 먼저 온 답. 모르면 null.
     * 일시정지로 끊기면 문제 차례와 같이 재개 1초 뒤 다시 묻는다(그동안 누른 버튼 답은 받는다).
     * 정차·도착으로 판을 마무리하게 되면 그때까지의 버튼 답(없으면 null).
     */
    private suspend fun askYesNo(prompt: String): Boolean? {
        restDecision.value = null
        awaitingRestDecision = true
        try {
            var interrupted = false
            while (true) {
                val voice = awaitVoiceOrRestDecision()
                restDecision.value?.let { return it }
                if (!voice) return null
                if (interrupted) {
                    delay(RESUME_DELAY_MS)
                    if (inputs.mode.value != GameMode.VOICE) continue
                }
                val result = interruptible { askYesNoOnce(prompt) }
                // speech.stop() 은 부르지 않는다: Pleos TTS 의 stop 은 비동기라 바로 이어지는 안내 말("좋아요…")을 끊는다.
                // 버튼이 먼저 온 경우의 듣기는 merge 가 흐름을 취소하면서 멈춘다.
                _state.update { it.copy(mic = MicState.IDLE, partial = null) }
                if (result != null) return result.value ?: restDecision.value
                interrupted = true
                callbacks.log("쉬어 가기 질문이 끊김 (mode=${inputs.mode.value}) → 재개 시 다시 묻기")
            }
        } finally {
            awaitingRestDecision = false
        }
    }

    private suspend fun askYesNoOnce(prompt: String): Decision {
        _state.update { it.copy(phase = SessionPhase.ASKING, caption = prompt, mic = MicState.SPEAKING) }
        speech.speak(prompt)
        restDecision.value?.let { return Decision(it) }
        val t0 = clock.elapsedMs()
        _state.update { it.copy(phase = SessionPhase.LISTENING, mic = MicState.LISTENING) }
        val heard = flow {
            val r = AnswerCollector.collect(speech.listen(YES_NO_HINT), t0, clock)
            r.text?.let { callbacks.logSensitive("heard(쉼터 안내)", it) }
            emit(YesNo.parse(r.text))
        }
        return Decision(merge(heard, restDecision.filterNotNull()).first())
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
        val inRound = currentGame != null
        val keep = roundIndex + if (inRound) 1 else 0
        val newPlan = SessionPlanner.replanRemaining(plan, keep, remainSec, choice, demo)
        callbacks.log("SessionPlanner: remain ${expected}s → ${remainSec}s (±20%↑) → ${plan.roundCount}판 → ${newPlan.roundCount}판")
        applyPlan(newPlan, remainSec)
        // 진행 중인 판이 언제 끝날지 모르므로, 새 첫 판의 시작은 그 판이 끝난 때 다시 정한다 (끝나자마자 다음 판이 이어지지 않게)
        if (inRound) replanAtRoundEnd = true
    }

    /** 계획이 바뀔 때마다 올린다 — 판을 기다리는 동안(쉬는 시간·모두 마침)에도 새 계획을 다시 읽게 한다 */
    private val planVersion = MutableStateFlow(0)

    /** 판 진행 중에 다시 계획했다 → 판이 끝나면 그때의 남은 시간으로 한 번 더 계획한다 */
    private var replanAtRoundEnd = false

    private fun applyPlan(newPlan: SessionPlan, basisRemainSec: Int) {
        plan = newPlan
        planBasisRemainSec = basisRemainSec
        planBasisAtMs = clock.elapsedMs()
        _state.update { it.copy(plan = newPlan) }
        planVersion.value++
    }

    /** 판 진행 중에 다시 계획했으면: 판이 끝난 지금의 남은 시간으로 다음 판들을 다시 잡는다 — 새 첫 판은 최소 휴지(2분) 뒤 */
    private fun replanAfterRound() {
        if (!replanAtRoundEnd) return
        replanAtRoundEnd = false
        val remain = inputs.remainSec.value ?: return
        if (arrival(remain)) return
        val newPlan = SessionPlanner.replanRemaining(plan, roundIndex + 1, remain, choice, demo)
        callbacks.log("SessionPlanner: 판 진행 중 다시 계획함 → 판이 끝난 지금(${remain}s) 기준으로 다음 판 다시 잡기 (${newPlan.roundCount}판)")
        applyPlan(newPlan, remain)
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
                when (waitAllDone()) {
                    WaitEnd.ARRIVED -> return
                    // 남은 시간이 늘어 판이 더해졌으면 그 판부터 (없으면 다시 "모두 했어요")
                    WaitEnd.PLAN_CHANGED -> continue
                    WaitEnd.START -> addBonusRound()
                }
                continue
            }
            when (waitForRound(round)) {
                WaitEnd.ARRIVED -> return
                // 기다리는 동안 계획이 바뀜 → 새 계획의 판(시작 시점·문제 수)을 다시 기다린다
                WaitEnd.PLAN_CHANGED -> continue
                WaitEnd.START -> Unit
            }
            playRound(round)
            roundIndex++
            rotationIndex = 0
            resumeTurnInRound = 0
            _state.update { it.copy(roundIndex = roundIndex) }
        }
    }

    /** 준비한 판을 다 했어도 가족이 원하면("한 판 더") 보너스 판 */
    private fun addBonusRound() {
        val count = if (demo) SessionPlanner.DEMO_QUESTION_COUNT else BONUS_QUESTION_COUNT
        val bonus = RoundPlan(
            index = plan.roundCount,
            game = SessionPlanner.gameFor(plan.roundCount, choice),
            targetDurationSec = count * SessionPlanner.SEC_PER_QUESTION,
            questionCount = count,
            startAtRemainSec = Int.MAX_VALUE,
        )
        plan = plan.copy(rounds = plan.rounds + bonus)
        _state.update { it.copy(plan = plan) }
    }

    // ── 대기 ────────────────────────────────────────────────────

    /** 판을 기다린 결과: 시작 / 기다리는 동안 계획이 바뀜 / 곧 도착 */
    private enum class WaitEnd { START, PLAN_CHANGED, ARRIVED }

    /** 오늘 준비한 판을 다 함: 도착·"한 판 더"·계획 변경(남은 시간이 늘어 판이 더해짐) 중 먼저 오는 것 */
    private suspend fun waitAllDone(): WaitEnd {
        setPhase(SessionPhase.ALL_DONE)
        _state.update { it.copy(nextRoundAtRemainSec = null) }
        val version = planVersion.value
        val (remain, skip, v) = combine(inputs.remainSec, skipWait, planVersion) { r, s, v -> Triple(r, s, v) }
            .first { (r, s, v) -> arrival(r) || s || v != version }
        return when {
            arrival(remain) -> WaitEnd.ARRIVED
            v != version && !skip -> WaitEnd.PLAN_CHANGED
            else -> {
                skipWait.value = false
                WaitEnd.START
            }
        }
    }

    private suspend fun waitForRound(round: RoundPlan): WaitEnd {
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
        val waitPhase = if (first) SessionPhase.WAITING else SessionPhase.RESTING
        val version = planVersion.value
        while (true) {
            val r = combine(inputs.remainSec, inputs.mode, skipWait, announcement, planVersion) { remain, mode, skip, line, v ->
                RoundWait(remain, mode, skip, line, v)
            }.first { w ->
                arrival(w.remain) || w.planVersion != version ||
                    (w.mode == GameMode.VOICE && (w.line != null || w.skip || first || w.remain == null || w.remain <= round.startAtRemainSec))
            }
            if (arrival(r.remain)) return WaitEnd.ARRIVED
            if (r.planVersion != version) return WaitEnd.PLAN_CHANGED
            // 판 사이에는 게임으로 말을 걸지 않지만, 졸음쉼터 도착·다시 출발 안내 한 줄은 바로 한다
            if (r.line != null) {
                sayPendingAnnouncement()
                setPhase(waitPhase)
                callbacks.onVoiceRoundActive(false) // 쉬는 동안 오디오 포커스를 붙잡지 않는다
            }
            if (r.skip || first || r.remain == null || r.remain <= round.startAtRemainSec) break
        }
        skipWait.value = false
        return if (arrival(inputs.remainSec.value)) WaitEnd.ARRIVED else WaitEnd.START
    }

    private data class RoundWait(val remain: Int?, val mode: GameMode, val skip: Boolean, val line: Announcement?, val planVersion: Int)

    /** VOICE 가 될 때까지 기다린다. 일시정지 중 정차(화면 게임 가능)·도착이면 false → 판 마무리 */
    private suspend fun awaitVoice(): Boolean {
        if (inputs.mode.value == GameMode.VOICE) return true
        _state.update { it.copy(mic = MicState.IDLE) }
        val result = combine(inputs.mode, inputs.screenAvailable, inputs.remainSec) { m, s, r -> Triple(m, s, r) }
            .first { (m, s, r) -> m == GameMode.VOICE || s || arrival(r) }
        return result.first == GameMode.VOICE && !arrival(result.third)
    }

    /** [awaitVoice] 와 같되, 멈춘 동안 화면의 "쉬어 가기"/"괜찮아요" 버튼 답이 오면 바로 돌아간다 */
    private suspend fun awaitVoiceOrRestDecision(): Boolean {
        if (inputs.mode.value == GameMode.VOICE) return true
        _state.update { it.copy(mic = MicState.IDLE) }
        val (m, remain) = combine(inputs.mode, inputs.screenAvailable, inputs.remainSec, restDecision) { m, s, r, d ->
            Triple(m, r, s || arrival(r) || d != null)
        }.first { (m, _, leave) -> m == GameMode.VOICE || leave }
        return m == GameMode.VOICE && !arrival(remain)
    }

    private fun arrival(remain: Int?): Boolean = remain != null && remain <= SessionPlanner.END_BUFFER_SEC

    private fun arrivalImminent(): Boolean = arrival(inputs.remainSec.value)

    // ── 판 진행 ──────────────────────────────────────────────────

    private suspend fun playRound(round: RoundPlan) {
        val game: VoiceGame = when (round.game) {
            GameKind.WORD_CHAIN -> WordChainGame(dictionary, random) { word -> callbacks.logSensitive("WordChain lenient accept", word) }
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
            it.copy(roundIndex = round.index, roundGame = round.game, turnsInRound = round.questionCount, turnInRound = resumeTurnInRound, lastAnswer = null)
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
                wrapLine = if (noDestination) "정한 시간이 거의 다 됐어요. 이번 판은 여기까지 할게요!" else "곧 도착이에요. 이번 판은 여기까지 할게요!"
                break
            }
            if (!awaitVoice()) break
            // 졸음쉼터 도착·다시 출발 같은 안내는 다음 차례 앞에 한 번 말한다. 쉼터에서 쉬었다 왔으면 정차·도착부터 다시 본다
            if (sayPendingAnnouncement()) continue

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
                val second = listenAgain(turn)
                heard = second?.text
                judgement = game.judge(turn, second?.text, 2)
            }
            pendingTurn = null
            heard?.let { callbacks.logSensitive("heard(${player.name})", it) }
            applyScore(player, judgement)
            _state.update { it.copy(lastAnswer = AnswerLine(player.name, heard, judgement.label), partial = null) }

            var action = if (player.isDriver) recordDrowsy(first, judgement, eligible = !firstHumanTurn && !interrupted) else DrowsinessMonitor.Action.None
            if (player.isDriver && forceStage2) {
                forceStage2 = false
                action = drowsy.forceStage2(clock.wallMs())
            }

            say(judgement.feedback, SessionPhase.FEEDBACK)
            when (action) {
                is DrowsinessMonitor.Action.Stage1 -> {
                    callbacks.onStage1(player, action.newDifficultyCap)
                    _state.update { it.copy(difficultyCap = action.newDifficultyCap) }
                    say("${player.name}, 운전 오래 하셨죠? 다음 문제는 조금 쉬운 걸로 가 볼게요.", SessionPhase.FEEDBACK)
                }
                is DrowsinessMonitor.Action.Stage2 -> {
                    _state.update { it.copy(difficultyCap = action.newDifficultyCap) }
                    offerRest(player, action)
                }
                DrowsinessMonitor.Action.None -> Unit
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
        replanAfterRound()
        callbacks.log("round ${round.index + 1} end")
        callbacks.onSnapshot(snapshot(turnInRound = 0, nextRound = true))
    }

    /**
     * 출제 → 듣기. 일시정지로 끊기면 같은 문제를 다시 낸다. @return (첫 응답, 일시정지로 끊긴 적 있음) / null = 판 마무리
     * @param alreadyInterrupted 이미 끊긴 뒤에 부름 — 재개 1초 뒤 문제부터 다시 낸다
     */
    private suspend fun askAndListen(turn: Turn, alreadyInterrupted: Boolean = false): Pair<AnswerResult, Boolean>? {
        var interrupted = alreadyInterrupted
        var fatalRetries = 0
        while (true) {
            if (!awaitVoice()) return null
            if (interrupted) {
                delay(RESUME_DELAY_MS)
                if (inputs.mode.value != GameMode.VOICE) continue
                // 쉬는 동안 생긴 안내(졸음쉼터 도착 등)는 같은 문제를 다시 내기 전에 말한다
                if (sayPendingAnnouncement() && (inputs.screenAvailable.value || arrivalImminent())) return null
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

    /** 재질문("다시 한번 말해 줄래요?") 뒤 두 번째 듣기. 끊기면 재개 뒤 같은 문제를 다시 내고 듣는다. null = 판 마무리 */
    private suspend fun listenAgain(turn: Turn): AnswerResult? {
        if (!awaitVoice()) return null
        val result = interruptible { listenNow(turn) }
        _state.update { it.copy(mic = MicState.IDLE) }
        if (result != null) return result
        callbacks.log("두 번째 듣기가 끊김 (mode=${inputs.mode.value}) → 재개 시 같은 문제")
        return askAndListen(turn, alreadyInterrupted = true)?.first
    }

    private suspend fun listenNow(turn: Turn): AnswerResult {
        val t0 = clock.elapsedMs()
        _state.update { it.copy(phase = SessionPhase.LISTENING, mic = MicState.LISTENING) }
        val events = speech.listen(turn.hint).onEach { e ->
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

    private fun recordDrowsy(result: AnswerResult, judgement: Judgement, eligible: Boolean): DrowsinessMonitor.Action {
        if (!eligible) {
            callbacks.log("Drowsiness: 샘플 제외(판 첫 문제 또는 일시정지 직후)")
            return DrowsinessMonitor.Action.None
        }
        // 인식 오류(네트워크·오디오 등)는 운전자의 반응이 아니다 — 8초 무응답과 달리 느린 응답으로 세지 않는다
        if (result.error != null) {
            callbacks.log("Drowsiness: 샘플 제외(인식 오류 ${result.error})")
            return DrowsinessMonitor.Action.None
        }
        val before = drowsy.snapshot()
        val action = drowsy.record(ResponseSample(result.latencyMs, judgement.correct), clock.wallMs())
        val after = drowsy.snapshot()
        if (before.calibrating) {
            val samples = drowsy.calibrationSamples
            callbacks.log("Drowsiness: 기준을 만드는 중 (${if (after.calibrating) after.calibrationCount else samples}/$samples)" +
                (after.baselineMs?.let { " → 기준 ${it}ms" } ?: ""))
        } else {
            callbacks.log("Drowsiness: latency=${result.latencyMs}ms baseline=${after.baselineMs}ms slow=${after.slowCount}/${drowsy.windowSize} cap=${after.difficultyCap}")
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
                append(if (noDestination) "정한 시간이 다 됐어요! 오늘 놀레오는 여기까지예요. " else "곧 도착이에요! 오늘 놀레오는 여기까지예요. ")
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
        val d = drowsy.snapshot()
        return SessionSummary(
            destinationName = destinationName,
            players = players,
            scores = s.scores,
            correctCounts = s.correctCounts,
            regionsVisited = s.regionsVisited,
            roundsPlayed = s.roundsPlayed,
            stage1Count = d.stage1Count,
            longestChain = maxOf(longestChain, chain?.chainLength ?: 0),
            stage2Count = d.stage2Count,
            noDestination = noDestination,
        )
    }

    fun snapshot(turnInRound: Int = _state.value.turnInRound, nextRound: Boolean = false): SessionSnapshot {
        val s = _state.value
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
            drowsy = drowsy.saveState(),
            longestChain = maxOf(longestChain, chain?.chainLength ?: 0),
            noDestination = noDestination,
            savedAtWallMs = clock.wallMs(),
        )
    }

    companion object {
        /** 일시정지 해제(GAIN 등) 후 같은 문제를 다시 낼 때까지 (1초 뒤 재출제) */
        const val RESUME_DELAY_MS = 1_000L

        private val YES_NO_HINT = ListenHint(correctAnswer = "네", wrongAnswer = "아니요", forDriver = true, yesNo = true)

        /** "한 판 더" 보너스 판의 문제 수 (데모 모드는 [SessionPlanner.DEMO_QUESTION_COUNT]) */
        private const val BONUS_QUESTION_COUNT = 8
    }
}
