package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 끝말잇기 사전: 난이도 상한별 출제 범위, 막다른 음절, 두음법칙 양방향, 검증용, 금칙어 */
class WordDictionaryTest {

    private val driver = Player(1, "아빠", isDriver = true)

    /** '가' 다음 후보 넷(등급마다 하나) — 모두 끝 음절에 이을 말이 3개 이상이라 막다른 말이 아니다 */
    private val leveled = WordDictionary(
        listOf(
            "만화가", "가방", "가수", "가격", "가위",
            "방석", "방울", "방패", "수박", "수건", "수영", "격식", "격투", "격파", "위성", "위로", "위치",
        ),
        mapOf("가방" to WordLevel.BASIC, "가수" to WordLevel.INTERMEDIATE, "가격" to WordLevel.ADVANCED, "가위" to WordLevel.TEAM),
    )

    private fun picks(cap: Int): Set<String> =
        (0 until 200).mapNotNull { seed -> (leveled.pick('가', setOf("만화가"), Random(seed), cap) as? WordDictionary.Pick.Word)?.word }.toSet()

    @Test
    fun difficultyCap_limitsPlayLevels() {
        assertEquals(setOf("가방"), picks(1))
        assertEquals(setOf("가방", "가수", "가위"), picks(2))
        assertEquals(setOf("가방", "가수", "가격", "가위"), picks(3))
        assertTrue(WordLevel.TEAM.allowedAt(2))
        assertFalse(WordLevel.ADVANCED.allowedAt(2))
        assertFalse(WordLevel.INTERMEDIATE.allowedAt(1))
    }

    @Test
    fun difficultyCap_widensWhenNothingFitsInsteadOfGivingUp() {
        // 상한 1 인데 초급 후보가 없으면 등급을 넓혀서라도 잇는다
        val used = setOf("만화가", "가방")
        val got = (0 until 50).mapNotNull { (leveled.pick('가', used, Random(it), 1) as? WordDictionary.Pick.Word)?.word }.toSet()
        assertTrue(got.isNotEmpty())
        assertFalse("가방" in got)
    }

    @Test
    fun deadEnds_areComputedOnce_usingBidirectionalDueum() {
        val d = WordDictionary(listOf("과자", "자름", "자두", "노래", "래커", "래프팅", "래미안", "나비"))
        assertTrue("'름' 으로 이을 말 없음", d.isDeadEnd('름'))
        assertTrue("'자' 로 이을 말 2개뿐", d.isDeadEnd('자'))
        // '내' 로 끝나면 '래' 로 시작하는 말 3개로 이을 수 있다(양방향) → 막다른 음절 아님
        assertEquals(3, d.continuationCount('내'))
        assertFalse(d.isDeadEnd('내'))
        assertEquals(d.continuationCount('래'), d.continuationCount('내'))
        assertTrue(d.deadEndSyllableCount > 0)
    }

    @Test
    fun onlyDeadEnds_restartsWithNewWordInsteadOfEndingRound() {
        // '자' 다음은 자름(→름)·자몽(→몽) 뿐 — 둘 다 막다른 말. 기차(→차: 차표·차도·차례)로 다시 시작한다
        val d = WordDictionary(listOf("과자", "자름", "자몽", "기차", "차표", "차도", "차례", "표지", "도로", "예물"))
        assertEquals(WordDictionary.Pick.OnlyDeadEnds, d.pick('자', setOf("과자"), Random(1)))
        val g = WordChainGame(d, Random(1))
        g.restoreState(mapOf("used" to "과자", "lastWord" to "과자"))
        val t = g.nextTurn(TurnContext(Player.APP, driver, null, 3))!!
        assertTrue(t.prompt, t.prompt.startsWith(WordChainGame.RESTART_LINE))
        assertEquals("기차", g.currentWord)
        assertFalse(t.endsRoundAfter)
    }

    @Test
    fun appPrefersStandardDueumStart_butPlayerMayUseEitherDirection() {
        // '내' 로 끝남: 앱은 '내'로 시작하는 말을 먼저, 사람은 '래'로 시작해도 인정
        val d = WordDictionary(listOf("안내", "내일", "래커", "일기", "일출", "일몰", "커피", "커튼", "커브", "기차", "출구", "몰래"))
        repeat(20) { seed ->
            assertEquals("내일", (d.pick('내', setOf("안내"), Random(seed)) as WordDictionary.Pick.Word).word)
        }
        val g = WordChainGame(d, Random(1))
        g.restoreState(mapOf("used" to "안내", "lastWord" to "안내"))
        val turn = g.nextTurn(TurnContext(driver, Player.APP, null, 3))!!
        assertEquals(Outcome.CORRECT, g.judge(turn, "래커", attempt = 1).outcome)
    }

    @Test
    fun validation_isUsedOnceAttached_playSetBefore() {
        val d = WordDictionary(listOf("기차", "차표"))
        assertFalse("고속도로" in d)
        d.attachValidation(ValidWords.of(listOf("주행거리", "고속도로", "가자미")))  // 정렬 안 된 입력도 정렬해서 찾는다
        assertTrue(d.hasValidation)
        assertTrue("고속도로" in d)
        assertTrue("주행거리" in d)
        assertTrue("기차" in d)
        assertFalse("가재미" in d)
        assertEquals(3, d.validSize)
    }

    @Test
    fun blocklist_containsAndExactRules() {
        val b = WordBlocklist.parse("# 설명\n살인\n=거지\n\n=술집\n")
        assertEquals(3, b.size)
        assertTrue(b.blocks("살인"))
        assertTrue(b.blocks("살인적"))
        assertTrue(b.blocks("거지"))
        assertFalse(b.blocks("설거지"))
        assertTrue(b.blocks("술집"))
        assertFalse(b.blocks("기술집약"))
    }
}
