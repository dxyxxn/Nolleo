package com.gildongmu.app.domain.engine

import com.gildongmu.app.core.Clock
import com.gildongmu.app.core.speech.ListenHint
import com.gildongmu.app.core.speech.SttEvent
import com.gildongmu.app.domain.drowsy.DriverBaseline
import com.gildongmu.app.domain.drowsy.DrowsinessMonitor
import com.gildongmu.app.domain.games.WordDictionary
import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameContent
import com.gildongmu.app.domain.model.GameKind
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.domain.model.Player
import com.gildongmu.app.domain.model.Question
import com.gildongmu.app.domain.model.RegionInfo
import com.gildongmu.app.domain.model.RoundPlan
import com.gildongmu.app.domain.model.SessionPlan
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
        chosungItems = emptyList(),
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

        override fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent> = flow {
            val d = delays[cursor++ % delays.size]
            emit(SttEvent.Listening(clock.elapsedMs()))
            if (d >= timeoutMs) awaitCancellation()
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

    private class Recorder : SessionCallbacks {
        val voiceRound = mutableListOf<Boolean>()
        var stage1 = 0
        var summary: SessionSummary? = null
        val logs = mutableListOf<String>()
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
        val drive = MutableStateFlow(DriveState.DRIVING)
        val inputs = SessionInputs(mode, screen, remain, region, drive)
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
