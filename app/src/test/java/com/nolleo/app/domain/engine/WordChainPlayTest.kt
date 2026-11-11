package com.nolleo.app.domain.engine

import com.nolleo.app.core.Clock
import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.domain.games.WordChainGame
import com.nolleo.app.domain.games.WordDictionary
import com.nolleo.app.domain.model.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 정차 중 끝말잇기(말로 잇기) — 가상 시간 + 가짜 음성 */
@OptIn(ExperimentalCoroutinesApi::class)
class WordChainPlayTest {

    private val driver = Player(1, "아빠", isDriver = true)
    private val kid = Player(2, "조수석", isDriver = false)

    private val dictionary = WordDictionary(
        listOf("기차", "차표", "표지판", "판다", "다리미", "미역", "역사", "사과", "과자", "자동차", "차도", "도토리", "리본", "본관",
            "관광", "광장", "장미", "미소", "소나무", "무지개", "개미", "미술", "술래", "내일", "일기", "기린", "인형", "형제", "제비", "비누",
            "누나", "나무", "무릎", "나비", "비행기", "기러기", "토끼", "끼니", "니트", "트럭", "럭비", "비둘기", "구름", "우산", "산책"),
    )

    /** 말하기 400ms. 듣기는 [answerDelayMs] 뒤 말을 시작해 hint 의 정답을 말한다 (null 이면 끝까지 말이 없음) */
    private class FakeSpeech(private val clock: Clock, private val answerDelayMs: Long?) : SpeechPort {
        val spoken = mutableListOf<String>()

        override suspend fun speak(text: String) {
            spoken += text
            delay(400)
        }

        override fun listen(hint: ListenHint): Flow<SttEvent> = flow {
            emit(SttEvent.Listening(clock.elapsedMs()))
            val d = answerDelayMs ?: awaitCancellation()
            delay(d)
            val text = hint.correctAnswer ?: awaitCancellation()
            emit(SttEvent.SpeechDetected(clock.elapsedMs()))
            delay(200)
            emit(SttEvent.Final(text, clock.elapsedMs()))
        }

        override fun stop() = Unit
        override fun markNewStream() = Unit
    }

    private fun TestScope.clock() = object : Clock {
        override fun elapsedMs() = testScheduler.currentTime
        override fun wallMs() = testScheduler.currentTime
    }

    @Test
    fun answeringWords_growsTheChainAndScore() = runTest {
        val speech = FakeSpeech(clock(), answerDelayMs = 1_000)
        val play = WordChainPlay(dictionary, listOf(driver, kid), speech, clock(), Random(3))
        val job = launch { play.run() }
        advanceTimeBy(30_000)
        val s = play.state.value
        job.cancel()
        assertTrue("사람이 몇 번 답했어야 함: $s", s.answered >= 3)
        assertEquals("정답만 말했으므로 모두 이음", s.answered, s.correct)
        assertEquals(10 * s.correct, s.score)
        assertTrue(s.words.size > s.answered)
        assertTrue(speech.spoken.first().startsWith("끝말잇기를 시작할게요"))
    }

    @Test
    fun nobodyAnswers_endsAfterTwoSilentTurns_forOnePlayer() = runTest {
        val speech = FakeSpeech(clock(), answerDelayMs = null)
        val play = WordChainPlay(dictionary, listOf(driver), speech, clock(), Random(4))
        play.run()
        val s = play.state.value
        assertEquals(WordChainPlay.End.NO_ANSWER, s.ended)
        assertEquals(WordChainPlay.Phase.ENDED, s.phase)
        assertEquals(WordChainPlay.SILENT_TURNS_MIN, s.answered)
        assertEquals(0, s.score)
        assertTrue(speech.spoken.last().startsWith("대답이 없어서 끝말잇기는 여기까지"))
    }

    @Test
    fun nobodyAnswers_waitsForEveryPlayer_whenThreePlay() = runTest {
        val third = Player(3, "뒷자리", isDriver = false)
        val play = WordChainPlay(dictionary, listOf(driver, kid, third), FakeSpeech(clock(), null), clock(), Random(5))
        play.run()
        assertEquals(WordChainPlay.End.NO_ANSWER, play.state.value.ended)
        assertEquals(3, play.state.value.answered)
    }

    @Test
    fun listenDeadline_shownUntilSpeechStarts() = runTest {
        val speech = FakeSpeech(clock(), answerDelayMs = 1_000)
        val play = WordChainPlay(dictionary, listOf(driver), speech, clock(), Random(6))
        val job = launch { play.run() }
        // 시작 안내 400ms + 놀레오 낱말 400ms + 쉬기 + 운전자 차례 안내 400ms 뒤 듣기
        val listenAt = 1_200L + WordChainGame.TURN_HOLD_MS
        advanceTimeBy(listenAt + 1)
        val listening = play.state.value
        assertEquals(WordChainPlay.Phase.LISTENING, listening.phase)
        assertEquals(driver, listening.player)
        assertEquals(listenAt + AnswerCollector.FIRST_SPEECH_TIMEOUT_MS, listening.listenDeadlineMs)
        // 놀레오는 제 낱말만, 사람 차례는 누구 차례를 먼저 (11/11)
        assertTrue(speech.spoken[1], speech.spoken[1].matches(Regex("저는 '.+' 할게요!")))
        assertTrue(speech.spoken[2], speech.spoken[2].startsWith("${driver.name} 차례예요! '") && speech.spoken[2].endsWith("시작하는 말!"))
        assertNotNull(listening.needed)
        advanceTimeBy(1_000)
        assertNull("말을 시작하면 남은 초를 거둔다", play.state.value.listenDeadlineMs)
        job.cancel()
    }

    @Test
    fun answerLine_staysThroughPraiseAndHold_thenClearsOnTheNextTurn() = runTest {
        val play = WordChainPlay(dictionary, listOf(driver, kid), FakeSpeech(clock(), answerDelayMs = 1_000), clock(), Random(8))
        val job = launch { play.run() }
        while (play.state.value.lastAnswer == null) advanceTimeBy(50)
        assertEquals(driver.name, play.state.value.lastAnswer?.playerName)
        // 칭찬(400ms) + 더 보여 주기가 끝나기 직전까지 운전자 답이 그대로, 아직 다음 차례가 아님
        advanceTimeBy(400 + WordChainGame.TURN_HOLD_MS - 100)
        assertEquals(driver.name, play.state.value.lastAnswer?.playerName)
        assertEquals(driver, play.state.value.player)
        assertEquals("더 보여 주는 동안은 말하지도 듣지도 않음", WordChainPlay.Phase.READY, play.state.value.phase)
        advanceTimeBy(200)
        assertEquals(kid, play.state.value.player)
        assertNull("다음 차례에는 앞사람 판정 줄이 없다", play.state.value.lastAnswer)
        job.cancel()
    }

    @Test
    fun cancelThenRunAgain_asksTheSameTurnAgain() = runTest {
        val speech = FakeSpeech(clock(), answerDelayMs = null)
        val play = WordChainPlay(dictionary, listOf(driver, kid), speech, clock(), Random(7))
        val first = launch { play.run() }
        advanceTimeBy(1_201 + WordChainGame.TURN_HOLD_MS)
        val before = play.state.value
        assertEquals(WordChainPlay.Phase.LISTENING, before.phase)
        first.cancel()
        runCurrent()
        assertNull("멈추면 남은 초도 거둔다", play.state.value.listenDeadlineMs)

        val spokenBefore = speech.spoken.size
        val second = launch { play.run() }
        runCurrent()
        // 시작 안내는 다시 하지 않고, 같은 사람에게 같은 음절을 다시 묻는다
        val again = speech.spoken.drop(spokenBefore)
        assertEquals(1, again.size)
        assertTrue(again.first(), again.first().startsWith("${driver.name} 차례예요!"))
        assertEquals(before.words, play.state.value.words)
        assertEquals(before.needed, play.state.value.needed)
        assertEquals(driver, play.state.value.player)
        second.cancel()
    }
}
