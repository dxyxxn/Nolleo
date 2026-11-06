package com.gildongmu.app.domain.games

import com.gildongmu.app.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordChainGameTest {

    private val driver = Player(1, "아빠", isDriver = true)
    private val kid = Player(2, "조수석", isDriver = false)

    private val dictionary = WordDictionary(
        listOf("기차", "차표", "표지판", "판다", "다리미", "미역", "역사", "사과", "과자", "자동차", "이불", "불꽃", "꽃게", "게임",
            "역도", "도토리", "리본", "본관", "관광", "광장", "장미", "미소", "소나무", "무지개", "개미", "미술", "술래", "내일",
            "일기", "기린", "인형", "형제", "제주", "주스", "스키", "키위", "위성", "성장", "노래", "래미안", "나무", "무릎"),
    )

    private fun game(random: Random = Random(7)) = WordChainGame(dictionary, random)

    /** 앱 차례를 진행해 chain 을 시작시키고 마지막 낱말을 돌려준다 */
    private fun startChain(g: WordChainGame): String {
        val t = g.nextTurn(TurnContext(Player.APP, driver, null, 3))!!
        assertFalse(t.expectsAnswer)
        return g.currentWord!!
    }

    @Test
    fun rotation_isAppThenDriverThenPassengers() {
        assertEquals(listOf(Player.APP, driver, kid), game().rotation(listOf(driver, kid)))
    }

    @Test
    fun appTurn_namesNextPlayerAndSyllable() {
        val g = game()
        val t = g.nextTurn(TurnContext(Player.APP, driver, null, 3))!!
        val word = g.currentWord!!
        assertTrue(t.prompt.contains(word))
        assertTrue(t.prompt.contains("아빠"))
        assertTrue(t.prompt.contains("'${word.last()}'"))
    }

    @Test
    fun humanTurn_afterAppTurn_hasNoRedundantPrompt_butHasReprompt() {
        val g = game()
        startChain(g)
        val human = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertEquals("", human.prompt)
        assertTrue(human.reprompt.contains("아빠"))
        assertTrue(human.expectsAnswer)
    }

    @Test
    fun validWord_isAccepted() {
        val g = game()
        val last = startChain(g).last()
        val answer = dictionary.continuations(last).first()
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        val j = g.judge(t, "${answer}요", 1)
        assertEquals(Outcome.CORRECT, j.outcome)
        assertEquals(10, j.scoreDelta)
        assertEquals(answer, g.currentWord)
    }

    @Test
    fun dueumWord_isAccepted() {
        val g = WordChainGame(WordDictionary(listOf("경력", "역사", "사람")), Random(1))
        g.restoreState(mapOf("used" to "경력", "lastWord" to "경력"))
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertTrue(t.prompt.contains("'력'이나 '역'으로"))
        assertEquals(Outcome.CORRECT, g.judge(t, "역사", 1).outcome)
    }

    @Test
    fun wrongStartSyllable_isMiss() {
        val g = WordChainGame(WordDictionary(listOf("기차", "차표", "사과")), Random(1))
        g.restoreState(mapOf("used" to "기차", "lastWord" to "기차"))
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        val j = g.judge(t, "사과", 1)
        assertEquals(Outcome.WRONG, j.outcome)
        assertEquals("기차", g.currentWord)
    }

    @Test
    fun duplicateWord_isMiss() {
        val g = WordChainGame(WordDictionary(listOf("기차", "차기", "기차표")), Random(1))
        g.restoreState(mapOf("used" to "기차,차기", "lastWord" to "차기"))
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertEquals(Outcome.WRONG, g.judge(t, "기차", 1).outcome)
    }

    @Test
    fun unknownWord_retryOnceThenLenientAccept() {
        val lenient = mutableListOf<String>()
        val g = WordChainGame(WordDictionary(listOf("기차", "차표")), Random(1)) { lenient += it }
        g.restoreState(mapOf("used" to "기차", "lastWord" to "기차"))
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertEquals(Outcome.RETRY, g.judge(t, "차돌바위", 1).outcome)
        val second = g.judge(t, "차돌바위", 2)
        assertEquals(Outcome.ACCEPTED_LENIENT, second.outcome)
        assertEquals(listOf("차돌바위"), lenient)
        assertEquals("차돌바위", g.currentWord)
    }

    @Test
    fun singleSyllable_isNotAWord() {
        val g = WordChainGame(WordDictionary(listOf("기차", "차표")), Random(1))
        g.restoreState(mapOf("used" to "기차", "lastWord" to "기차"))
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertEquals(Outcome.RETRY, g.judge(t, "차", 1).outcome)
        assertEquals(Outcome.WRONG, g.judge(t, "차", 2).outcome)
    }

    @Test
    fun noAnswer_passesSameSyllable() {
        val g = game()
        startChain(g)
        val before = g.currentWord
        val t = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertEquals(Outcome.NO_ANSWER, g.judge(t, null, 1).outcome)
        assertEquals(before, g.currentWord)
    }

    @Test
    fun appLoses_whenNoContinuation() {
        val g = WordChainGame(WordDictionary(listOf("기차", "차표", "가름")), Random(1))
        g.restoreState(mapOf("used" to "가름", "lastWord" to "가름"))
        val t = g.nextTurn(TurnContext(Player.APP, driver, null, 3))!!
        assertTrue(t.prompt.contains("제가 졌어요"))
        assertTrue(t.endsRoundAfter)
    }

    @Test
    fun appAvoidsDeadEndWords() {
        // '자' 다음 후보: 자동차(→차: 이을 말 있음) / 자름(→름: 막다른 길)
        val dict = WordDictionary(listOf("과자", "자동차", "자름", "차표", "차도", "차례", "표지", "도로"))
        repeat(20) { seed ->
            val g = WordChainGame(dict, Random(seed))
            g.restoreState(mapOf("used" to "과자", "lastWord" to "과자"))
            g.nextTurn(TurnContext(Player.APP, driver, null, 3))
            assertEquals("자동차", g.currentWord)
        }
    }

    @Test
    fun extractWord_picksTokenStartingWithRequiredSyllable() {
        val g = game()
        assertEquals("차표", g.extractWord("음 차표요", '차'))
        assertEquals("역사", g.extractWord("역사입니다", '력'))
    }

    @Test
    fun stateRoundTrip() {
        val g = game()
        startChain(g)
        val saved = g.saveState()
        val restored = game()
        restored.restoreState(saved)
        assertEquals(g.currentWord, restored.currentWord)
        assertNotNull(restored.currentWord)
    }
}
