package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.Player
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
    fun appTurn_saysOnlyItsOwnWord() {
        val g = game()
        val t = g.nextTurn(TurnContext(Player.APP, driver, null, 3))!!
        val word = g.currentWord!!
        // 놀레오도 한 사람의 차례 — 다음 사람 이름·음절은 그 사람 차례에 (11/11)
        assertEquals("저는 '$word' 할게요!", t.prompt)
        assertFalse(t.prompt.contains("아빠"))
    }

    @Test
    fun humanTurn_afterAppTurn_namesThePlayerFirst_thenTheSyllable() {
        val g = game()
        startChain(g)
        val human = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        val phrase = g.startsWithPhrase(g.currentWord!!.last())
        assertEquals("아빠 차례예요! $phrase 시작하는 말!", human.prompt)
        assertEquals(human.prompt, human.reprompt)
        assertTrue(human.expectsAnswer)
        // 이어진 낱말은 화면 위 낱말 줄에 따로 보이므로 화면 글은 무엇으로 시작할지만
        assertEquals("${g.startsWithPhrase(g.currentWord!!.last())} 시작하는 말", human.display)
        assertEquals(listOf(g.currentWord), g.words)
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
    fun missOnLastTurnOfRound_doesNotPassToNextPerson() {
        val g = WordChainGame(WordDictionary(listOf("기차", "차표", "사과")), Random(1))
        g.restoreState(mapOf("used" to "기차", "lastWord" to "기차"))
        val middle = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        assertTrue(g.judge(middle, null, 1).feedback.endsWith(WordChainGame.PASS_LINE))
        assertTrue(g.judge(middle, "사과", 1).feedback.endsWith(WordChainGame.PASS_LINE))
        // 판의 마지막 차례: 다음 사람에게 넘기지 않는다 (11/13 시연 피드백 — 판이 여기서 끝난다)
        val last = g.nextTurn(TurnContext(kid, Player.APP, null, 3, lastInRound = true))!!
        val timeUp = g.judge(last, null, 1)
        assertEquals(Outcome.NO_ANSWER, timeUp.outcome)
        assertEquals("시간이 다 됐어요!", timeUp.feedback)
        val wrong = g.judge(last, "사과", 1)
        assertEquals(Outcome.WRONG, wrong.outcome)
        assertFalse(wrong.feedback.contains(WordChainGame.PASS_LINE))
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
    fun extractWord_keepsWordsThatEndWithYo() {
        // "요"까지가 낱말이면 어미로 떼지 않는다 (출제용 사전에 동요·필요·대중가요 등 13개)
        val g = WordChainGame(WordDictionary(listOf("동요", "필요", "대중가요", "사과", "요리")), Random(1))
        assertEquals("동요", g.extractWord("동요", '동'))
        assertEquals("필요", g.extractWord("필요요", '필'))
        assertEquals("대중가요", g.extractWord("대중가요", '대'))
        assertEquals("동요", g.extractWord("동요요", '동'))
        // 사전에 없는 꼴은 지금처럼 어미를 뗀다
        assertEquals("사과", g.extractWord("사과요", '사'))
        assertEquals("사과", g.extractWord("사과예요", '사'))
    }

    @Test
    fun yoEndingWord_isAcceptedInJudge() {
        val g = WordChainGame(WordDictionary(listOf("기차", "차표", "표지", "지도", "도요", "동요", "요리")), Random(1))
        g.restoreState(mapOf("used" to "기차", "lastWord" to "고동"))
        val turn = g.nextTurn(TurnContext(driver, kid, null, 3))!!
        val j = g.judge(turn, "동요", 1)
        assertEquals(Outcome.CORRECT, j.outcome)
        assertEquals("동요", g.currentWord)
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
