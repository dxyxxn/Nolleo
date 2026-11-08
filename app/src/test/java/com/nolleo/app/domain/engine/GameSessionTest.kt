package com.nolleo.app.domain.engine

import com.nolleo.app.core.Clock
import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.domain.drowsy.DriverBaseline
import com.nolleo.app.domain.drowsy.DrowsinessMonitor
import com.nolleo.app.domain.games.WordDictionary
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.Question
import com.nolleo.app.domain.model.RegionInfo
import com.nolleo.app.domain.model.RoundPlan
import com.nolleo.app.domain.model.SessionPlan
import com.nolleo.app.domain.rest.RestStopKind
import com.nolleo.app.domain.rest.RestStopSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * GameSession 통합 테스트 — 가상 시간 + 가짜 음성(대본) 으로 한 판을 끝까지 돌린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionTest {

    private val driver = Player(1, "아빠", isDriver = true)
    private val kid = Player(2, "조수석", isDriver = false)

    private val questions = (1..12).map { i ->
        Question("cb-$i", "chungbuk", difficulty = (i % 3) + 1, category = "특산물", text = "충북 문제 $i 는 무엇일까요?", answers = listOf("정답$i"), hint = "")
    }
    private val content = GameContent(
        regions = listOf(RegionInfo("chungbuk", "충청북도", "충북", 36.0, 37.2, 127.2, 128.6, 36.8, 127.7, null, emptyList(), emptyList())),
        regionQuestions = mapOf("chungbuk" to questions),
        generalQuestions = (1..5).map { Question("g-$it", null, 1, "동물", "일반 문제 $it?", listOf("일반$it"), "") },
        idioms = emptyList(),
    )
    private val dictionary = WordDictionary(
        listOf("기차", "차표", "표지판", "판다", "다리미", "미역", "역사", "사과", "과자", "자동차", "차도", "도토리", "리본", "본관",
            "관광", "광장", "장미", "미소", "소나무", "무지개", "개미", "미술", "술래", "내일", "일기", "기린", "인형", "형제", "제비", "비누",
            "누나", "나무", "무릎", "나비", "비행기", "기러기", "토끼", "끼니", "니트", "트럭", "럭비", "비둘기", "구름", "우산", "산책"),
    )

    /** 대본 가짜 음성: 지연 목록을 순서대로 쓰고 answer 함수로 답을 만든다. 지연 ≥ 8000 이면 무응답 */
    private class FakeSpeech(
        private val clock: Clock,
        private val delays: List<Long>,
        private val answer: (ListenHint) -> String?,
    ) : SpeechPort {
        val spoken = mutableListOf<String>()
        var stops = 0
        private var cursor = 0

        override suspend fun speak(text: String) {
            spoken += text
            delay(400)
        }

        override fun listen(hint: ListenHint): Flow<SttEvent> = flow {
            val d = delays[cursor++ % delays.size]
            emit(SttEvent.Listening(clock.elapsedMs()))
            if (d >= AnswerCollector.FIRST_SPEECH_TIMEOUT_MS) awaitCancellation()
            delay(d)
            val text = answer(hint) ?: awaitCancellation()
            emit(SttEvent.SpeechDetected(clock.elapsedMs()))
            delay(200)
            emit(SttEvent.Final(text, clock.elapsedMs()))
        }

        override fun stop() {
            stops++
        }

        override fun markNewStream() = Unit
    }

    /** 듣기마다 인식 오류(네트워크 등)로 끝나는 가짜 음성 */
    private class ErrorSpeech : SpeechPort {
        override suspend fun speak(text: String) = delay(400)
        override fun listen(hint: ListenHint): Flow<SttEvent> = flow {
            delay(300)
            emit(SttEvent.Error("NETWORK_ERROR"))
        }
        override fun stop() = Unit
        override fun markNewStream() = Unit
    }

    private class Recorder : SessionCallbacks {
        val voiceRound = mutableListOf<Boolean>()
        var stage1 = 0
        var summary: SessionSummary? = null
        val logs = mutableListOf<String>()
        var restSuggestion: RestStopSuggestion? = null
        val stage2Offers = mutableListOf<RestStopSuggestion?>()
        val restDecisions = mutableListOf<Boolean>()
        var heading: RestStopSuggestion? = null
        override fun suggestRestStop(): RestStopSuggestion? = restSuggestion
        override fun headingRestStop(): RestStopSuggestion? = heading
        override fun onStage2(driver: Player, suggestion: RestStopSuggestion?) {
            stage2Offers += suggestion
        }
        override fun onRestDecision(suggestion: RestStopSuggestion?, accepted: Boolean) {
            restDecisions += accepted
        }
        var restHolds = 0
        override fun onRestHold() {
            restHolds++
        }
        override fun onVoiceRoundActive(active: Boolean) {
            voiceRound += active
        }
        override fun onStage1(driver: Player, newDifficultyCap: Int) {
            stage1++
        }
        override fun onSnapshot(snapshot: SessionSnapshot) = Unit
        override fun onFinished(summary: SessionSummary) {
            this.summary = summary
        }
        override fun log(message: String) {
            logs += message
        }
        override fun logSensitive(label: String, value: String) = Unit
    }

    private class Env(scope: TestScope) {
        val clock = object : Clock {
            override fun elapsedMs() = scope.testScheduler.currentTime
            override fun wallMs() = scope.testScheduler.currentTime
        }
        val mode = MutableStateFlow(GameMode.VOICE)
        val screen = MutableStateFlow(false)
        val remain = MutableStateFlow<Int?>(3 * 3600)
        val region = MutableStateFlow<String?>("chungbuk")
        val inputs = SessionInputs(mode, screen, remain, region)
    }

    private fun plan(game: GameKind, count: Int) =
        SessionPlan(listOf(RoundPlan(0, game, 360, count, 3 * 3600)), basisRemainSec = 3 * 3600)

    private fun session(env: Env, speech: SpeechPort, rec: Recorder, players: List<Player>, plan: SessionPlan, baseline: DriverBaseline? = null) =
        GameSession(
            speech = speech, clock = env.clock, content = content, dictionary = dictionary, players = players,
            initialPlan = plan, choice = GameChoice.QUIZ, demo = false, drowsy = DrowsinessMonitor(baseline),
            inputs = env.inputs, callbacks = rec, destinationName = "테스트", presetId = null, random = Random(3),
        )

    @Test
    fun quizRound_normalAnswers_scoresEveryoneAndFinishesOnArrival() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1300L, 1800L)) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver, kid), plan(GameKind.REGION_QUIZ, 4))
        val job = launch { s.run() }
        advanceTimeBy(120_000)
        assertEquals(SessionPhase.ALL_DONE, s.state.value.phase)
        assertEquals(listOf(true, false), rec.voiceRound)
        env.remain.value = 200 // 도착 5분 전
        advanceUntilIdle()
        val summary = assertNotNull(rec.summary).let { rec.summary!! }
        assertEquals(4, summary.totalCorrect)
        assertEquals(20, summary.scores[driver.id])
        assertEquals(20, summary.scores[kid.id])
        assertEquals(listOf("chungbuk"), summary.regionsVisited)
        assertTrue(speech.spoken.first().contains("1번째 판"))
        assertTrue("지역 안내", speech.spoken.any { it.contains("지금은 충청북도예요!") })
        assertTrue("턴 순서: 아빠 → 조수석", speech.spoken.indexOfFirst { it.contains("아빠 차례") } < speech.spoken.indexOfFirst { it.contains("조수석 차례") })
        job.cancel()
    }

    @Test
    fun sleepyScript_triggersStage1ExactlyOnce_andLowersDifficulty() = runTest {
        val env = Env(this)
        // answers_sleepy.json 과 같은 흐름: 2초 → 6초 → 무응답
        val delays = listOf(2000L, 2500L, 3800L, 5000L, 6000L, 6500L, 9000L, 6000L, 9000L, 6000L)
        val speech = FakeSpeech(env.clock, delays) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 10), DriverBaseline(1500.0, 0.9, 10))
        val job = launch { s.run() }
        advanceTimeBy(300_000)
        assertEquals("쿨다운 5분 안에 Stage1 은 정확히 1회", 1, rec.stage1)
        assertEquals(2, s.drowsy.difficultyCap)
        assertTrue(speech.spoken.any { it.contains("운전 오래 하셨죠?") })
        assertTrue(rec.logs.any { it.contains("샘플 제외") })
        job.cancel()
    }

    private val osan = RestStopSuggestion("오산 졸음쉼터", RestStopKind.SHELTER, 12_000, "경부선 부산 방향", RestStopSuggestion.Source.TABLE, null)

    @Test
    fun stage2_offersRest_andVoiceYesAccepts() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { if (it.yesNo) "네" else it.correctAnswer }
        val rec = Recorder().apply { restSuggestion = osan }
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 6), DriverBaseline(1500.0, 0.9, 10))
        s.requestStage2() // 디버그 "2단계 시험": 다음 운전자 답 뒤
        val job = launch { s.run() }
        advanceTimeBy(30_000)
        assertEquals(listOf<RestStopSuggestion?>(osan), rec.stage2Offers)
        assertEquals(listOf(true), rec.restDecisions)
        assertTrue(speech.spoken.any { it.contains("잠깐 쉬어 갈까요?") && it.contains("경부선 부산 방향 오산 졸음쉼터가 12킬로미터 앞에 있어요.") })
        assertTrue(speech.spoken.any { it.contains("좋아요. 오산 졸음쉼터에서 쉬어 가요") })
        assertEquals("2단계도 난이도 상한 -1", 2, s.drowsy.difficultyCap)
        job.cancel()
    }

    @Test
    fun stage2_screenButtonBeatsVoice_andDeclines() = runTest {
        val env = Env(this)
        // 두 번째 듣기(네/아니요)는 7초 뒤에야 답한다 → 그 전에 화면 "괜찮아요"
        val speech = FakeSpeech(env.clock, listOf(1500L, 7000L)) { if (it.yesNo) "네" else it.correctAnswer }
        val rec = Recorder().apply { restSuggestion = osan }
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 6), DriverBaseline(1500.0, 0.9, 10))
        s.requestStage2()
        val job = launch { s.run() }
        while (speech.spoken.none { it.contains("잠깐 쉬어 갈까요?") }) advanceTimeBy(100)
        advanceTimeBy(1_000) // 질문을 다 말하고 듣는 중
        assertTrue("듣는 중이면 버튼 답을 받는다", s.answerRestSuggestion(false))
        advanceTimeBy(5_000)
        assertEquals(listOf(false), rec.restDecisions)
        assertTrue(speech.spoken.any { it.contains("알겠어요. 대신 다음 문제는 쉬운 걸로") })
        assertTrue("질문이 끝나면 버튼은 세션이 아니라 컨트롤러가 받는다", !s.answerRestSuggestion(true))
        job.cancel()
    }

    @Test
    fun stage2_questionInterruptedByPause_isAskedAgainAfterResume() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { if (it.yesNo) "네" else it.correctAnswer }
        val rec = Recorder().apply { restSuggestion = osan }
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 6), DriverBaseline(1500.0, 0.9, 10))
        s.requestStage2()
        val job = launch { s.run() }
        while (speech.spoken.none { it.contains("잠깐 쉬어 갈까요?") }) advanceTimeBy(100)
        env.mode.value = GameMode.PAUSED // 질문하는 중 일시정지(TBT·버튼·Gleo)
        advanceTimeBy(10_000)
        assertTrue("멈춘 동안에는 답 없음으로 끝내지 않는다", rec.restDecisions.isEmpty())
        env.mode.value = GameMode.VOICE
        advanceTimeBy(10_000)
        assertEquals("재개하면 다시 묻는다", 2, speech.spoken.count { it.contains("잠깐 쉬어 갈까요?") })
        assertEquals(listOf(true), rec.restDecisions)
        job.cancel()
    }

    @Test
    fun stage2_screenButtonWhilePaused_answersRightAway() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { if (it.yesNo) "네" else it.correctAnswer }
        val rec = Recorder().apply { restSuggestion = osan }
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 6), DriverBaseline(1500.0, 0.9, 10))
        s.requestStage2()
        val job = launch { s.run() }
        while (speech.spoken.none { it.contains("잠깐 쉬어 갈까요?") }) advanceTimeBy(100)
        env.mode.value = GameMode.PAUSED
        advanceTimeBy(3_000)
        assertTrue("멈춘 동안에도 질문은 버튼 답을 기다린다", s.answerRestSuggestion(true))
        advanceTimeBy(1_000)
        assertEquals("재개를 기다리지 않고 바로 받아들인다", listOf(true), rec.restDecisions)
        job.cancel()
    }

    @Test
    fun secondListenInterruptedByPause_reasksSameQuestion() = runTest {
        val env = Env(this)
        var answers = 0
        // 첫 답은 사전에 없는 말 → "다시 한번 말해 줄래요?" → 두 번째 듣기
        val speech = FakeSpeech(env.clock, listOf(1500L)) { hint ->
            val word = hint.correctAnswer ?: return@FakeSpeech null
            if (answers++ == 0) "${word.first()}뭐뭐" else word
        }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.WORD_CHAIN, 2))
        val job = launch { s.run() }
        while (speech.spoken.none { it == "다시 한번 말해 줄래요?" }) advanceTimeBy(100)
        advanceTimeBy(600) // 재질문을 다 말하고 두 번째로 듣는 중
        env.mode.value = GameMode.PAUSED
        advanceTimeBy(5_000)
        env.mode.value = GameMode.VOICE
        advanceTimeBy(5_000)
        val retryAt = speech.spoken.indexOf("다시 한번 말해 줄래요?")
        assertTrue("재개하면 같은 문제를 다시 낸다", speech.spoken.drop(retryAt + 1).any { it.contains("아빠 차례예요") })
        assertTrue("무응답으로 넘기지 않는다", speech.spoken.none { it.startsWith("시간이 다 됐어요") })
        job.cancel()
    }

    @Test
    fun stage2_whileHeadingToRestStop_remindsInsteadOfAskingAgain() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { if (it.yesNo) "네" else it.correctAnswer }
        val rec = Recorder().apply { restSuggestion = osan; heading = osan }
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 6), DriverBaseline(1500.0, 0.9, 10))
        s.requestStage2()
        val job = launch { s.run() }
        advanceTimeBy(30_000)
        assertEquals("카드는 가던 곳", listOf<RestStopSuggestion?>(osan), rec.stage2Offers)
        assertTrue("다른 곳을 묻지 않는다", speech.spoken.none { it.contains("잠깐 쉬어 갈까요?") })
        assertTrue(rec.restDecisions.isEmpty())
        assertTrue(speech.spoken.any { it == "아빠, 반응이 계속 느려졌어요. 오산 졸음쉼터에서 꼭 쉬어 가요." })
        job.cancel()
    }

    @Test
    fun recognitionErrors_areExcludedFromDrowsinessSamples() = runTest {
        val env = Env(this)
        val rec = Recorder()
        val s = session(env, ErrorSpeech(), rec, listOf(driver), plan(GameKind.REGION_QUIZ, 8), DriverBaseline(1500.0, 0.9, 10))
        val job = launch { s.run() }
        advanceTimeBy(200_000)
        assertEquals("엔진 오류로 졸음 1단계가 걸리지 않는다", 0, rec.stage1)
        assertTrue(s.drowsy.snapshot().recentLatencies.isEmpty())
        assertTrue(rec.logs.any { it.contains("샘플 제외(인식 오류") })
        job.cancel()
    }

    @Test
    fun afterRest_greetsBeforeNextTurn_andResetsDifficulty() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { if (it.yesNo) "네" else it.correctAnswer }
        val rec = Recorder().apply { restSuggestion = osan }
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 8), DriverBaseline(1500.0, 0.9, 10))
        s.requestStage2()
        val job = launch { s.run() }
        while (rec.restDecisions.isEmpty()) advanceTimeBy(100)
        s.onRestFinished()
        advanceTimeBy(20_000)
        assertEquals(3, s.drowsy.difficultyCap)
        val greet = speech.spoken.indexOfFirst { it == "아빠, 잘 쉬셨나요? 다시 출발해요!" }
        assertTrue("다음 차례 앞에 인사", greet >= 0 && speech.spoken.drop(greet + 1).any { it.contains("아빠 차례") })
        job.cancel()
    }

    private val arrivedLine = "오산 졸음쉼터에 도착했어요. 차를 세우고 충분히 쉬었다 가요."

    @Test
    fun arrivalAnnouncement_holdsQuestions_untilRestFinished() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 8))
        val job = launch { s.run() }
        advanceTimeBy(3_000) // 첫 문제를 듣는 중
        s.announce(arrivedLine, holdAfter = true)
        while (speech.spoken.none { it == arrivedLine }) advanceTimeBy(100)
        val afterArrival = speech.spoken.size
        advanceTimeBy(60_000)
        assertEquals("도착 안내 뒤에는 다시 출발할 때까지 문제를 내지 않는다", afterArrival, speech.spoken.size)
        assertEquals(1, rec.restHolds)
        s.onRestFinished()
        advanceTimeBy(10_000)
        assertEquals("풀리면 인사부터", "아빠, 잘 쉬셨나요? 다시 출발해요!", speech.spoken[afterArrival])
        assertTrue(speech.spoken.drop(afterArrival + 1).any { it.contains("아빠 차례") })
        job.cancel()
    }

    @Test
    fun arrivalHold_continueButtonResumesWithoutGreeting_andParkingWrapsRound() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 8))
        val job = launch { s.run() }
        advanceTimeBy(3_000)
        s.announce(arrivedLine, holdAfter = true)
        while (speech.spoken.none { it == arrivedLine }) advanceTimeBy(100)
        advanceTimeBy(5_000)
        val held = speech.spoken.size
        s.releaseRestHold() // 화면 "계속"
        advanceTimeBy(3_000)
        assertTrue("계속이면 바로 다음 문제", speech.spoken.drop(held).any { it.contains("아빠 차례") })
        assertTrue(speech.spoken.none { it.contains("잘 쉬셨나요") })

        // 다시 도착 안내 → 기다리는 중 정차(화면 게임 가능)하면 판을 마무리한다
        s.announce(arrivedLine, holdAfter = true)
        while (speech.spoken.count { it == arrivedLine } < 2) advanceTimeBy(100)
        advanceTimeBy(1_000)
        env.screen.value = true
        advanceTimeBy(5_000)
        assertEquals(listOf(true, false), rec.voiceRound)
        job.cancel()
    }

    @Test
    fun announcementDuringRest_isSpokenRightAway_withoutStartingNextRound() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val rec = Recorder()
        val twoRounds = SessionPlan(
            listOf(RoundPlan(0, GameKind.REGION_QUIZ, 360, 2, 3 * 3600), RoundPlan(1, GameKind.REGION_QUIZ, 360, 2, 3600)),
            basisRemainSec = 3 * 3600,
        )
        val s = session(env, speech, rec, listOf(driver), twoRounds)
        val job = launch { s.run() }
        advanceTimeBy(60_000)
        assertEquals("1판 뒤 쉬는 시간", SessionPhase.RESTING, s.state.value.phase)
        s.announce("아빠, 잘 쉬셨나요? 다시 출발해요!")
        advanceTimeBy(5_000)
        assertTrue(speech.spoken.last() == "아빠, 잘 쉬셨나요? 다시 출발해요!")
        assertEquals("다음 판은 아직 시작하지 않는다", SessionPhase.RESTING, s.state.value.phase)
        assertEquals(1, s.state.value.roundIndex)
        job.cancel()
    }

    @Test
    fun pauseMidQuestion_stopsSpeech_andReasksSameQuestion() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(5000L)) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 2))
        val job = launch { s.run() }
        advanceTimeBy(1_500) // 인트로 + 첫 문제 출제 후 듣는 중
        runCurrent()
        val asked = speech.spoken.last()
        assertTrue(asked.contains("아빠 차례"))
        env.mode.value = GameMode.PAUSED
        runCurrent()
        assertTrue("일시정지 시 TTS/STT 즉시 중단", speech.stops >= 1)
        advanceTimeBy(10_000)
        env.mode.value = GameMode.VOICE
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals("같은 문제 다시", asked, speech.spoken.last { it.contains("차례") })
        job.cancel()
    }

    private val twoRounds = SessionPlan(
        listOf(RoundPlan(0, GameKind.REGION_QUIZ, 360, 2, 3 * 3600), RoundPlan(1, GameKind.REGION_QUIZ, 360, 2, 3600)),
        basisRemainSec = 3 * 3600,
    )

    @Test
    fun replanWhileResting_waitsForTheNewPlansRound() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val s = session(env, speech, Recorder(), listOf(driver), twoRounds)
        val job = launch { s.run() }
        advanceTimeBy(60_000)
        assertEquals("1판 뒤 쉬는 시간 (2판은 남은 1시간에 시작할 계획)", SessionPhase.RESTING, s.state.value.phase)
        env.remain.value = 2 * 3600 // 정체가 풀려 남은 시간이 크게 줄었다
        s.onTripUpdate(2 * 3600, 1f)
        advanceTimeBy(1_000)
        env.remain.value = 2 * 3600 - 300 // 새 계획의 2판 시작 시점(남은 시간 - 2분)을 지남
        advanceTimeBy(10_000)
        assertTrue("옛 계획(남은 1시간)이 아니라 새 계획으로 2판을 시작한다", speech.spoken.any { it.startsWith("2번째 판") })
        job.cancel()
    }

    @Test
    fun replanWhenAllDone_startsTheAddedRounds() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val s = session(env, speech, Recorder(), listOf(driver), plan(GameKind.REGION_QUIZ, 2))
        val job = launch { s.run() }
        advanceTimeBy(60_000)
        assertEquals(SessionPhase.ALL_DONE, s.state.value.phase)
        env.remain.value = 6 * 3600 // 휴게소에서 오래 쉬어 남은 시간이 늘었다
        s.onTripUpdate(6 * 3600, 1f)
        advanceTimeBy(1_000)
        env.remain.value = 6 * 3600 - 300
        advanceTimeBy(10_000)
        assertTrue("늘어난 시간에 더해진 판을 시작한다", speech.spoken.any { it.startsWith("2번째 판") })
        job.cancel()
    }

    @Test
    fun replanDuringRound_keepsRestAfterTheRoundEnds() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val s = session(env, speech, Recorder(), listOf(driver), plan(GameKind.REGION_QUIZ, 4))
        val job = launch { s.run() }
        advanceTimeBy(3_000) // 1판 진행 중
        env.remain.value = 6 * 3600
        s.onTripUpdate(6 * 3600, 1f) // 판 진행 중 다시 계획 → 새 첫 판 시작 = 지금 남은 시간 - 2분
        env.remain.value = 6 * 3600 - 200 // 판이 끝나기 전에 그 시점을 지남
        advanceTimeBy(30_000)
        assertEquals("판이 끝나자마자 다음 판을 잇지 않고 쉰다", SessionPhase.RESTING, s.state.value.phase)
        assertTrue(speech.spoken.none { it.startsWith("2번째 판") })
        env.remain.value = 6 * 3600 - 200 - 300 // 판이 끝난 때부터 2분 넘게 지남
        advanceTimeBy(10_000)
        assertTrue(speech.spoken.any { it.startsWith("2번째 판") })
        job.cancel()
    }

    @Test
    fun parkingMidRound_wrapsUpRound() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver), plan(GameKind.REGION_QUIZ, 10))
        val job = launch { s.run() }
        advanceTimeBy(6_000)
        env.screen.value = true // 정차 → 화면 게임 가능
        advanceTimeBy(30_000)
        assertTrue(speech.spoken.any { it.contains("차가 멈췄네요") })
        assertEquals(listOf(true, false), rec.voiceRound)
        assertTrue("10문제를 다 하지 않고 마무리", s.state.value.correctCounts[driver.id]!! < 10)
        job.cancel()
    }

    @Test
    fun wordChainRound_appAndPlayersAlternate() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val rec = Recorder()
        val s = session(env, speech, rec, listOf(driver, kid), plan(GameKind.WORD_CHAIN, 4))
        val job = launch { s.run() }
        advanceTimeBy(120_000)
        assertTrue(speech.spoken.any { it.startsWith("저는 '") })
        assertTrue(s.state.value.scores[driver.id]!! > 0)
        assertTrue(s.state.value.scores[kid.id]!! > 0)
        job.cancel()
    }

    @Test
    fun replan_whenRemainJumpsMoreThan20Percent() = runTest {
        val env = Env(this)
        val speech = FakeSpeech(env.clock, listOf(1500L)) { it.correctAnswer }
        val rec = Recorder()
        val longPlan = SessionPlanner.plan(4 * 3600, GameChoice.QUIZ)
        val s = GameSession(
            speech = speech, clock = env.clock, content = content, dictionary = dictionary, players = listOf(driver),
            initialPlan = longPlan, choice = GameChoice.QUIZ, demo = false, drowsy = DrowsinessMonitor(null),
            inputs = env.inputs, callbacks = rec, destinationName = "테스트", presetId = null, random = Random(3),
        )
        s.onTripUpdate(4 * 3600 + 600, 1f) // +4% → 유지
        assertEquals(longPlan.roundCount, s.state.value.plan.roundCount)
        s.onTripUpdate(6 * 3600, 1f) // +50% → 남은 판 재계산
        assertTrue(s.state.value.plan.roundCount > longPlan.roundCount)
    }
}
