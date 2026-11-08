package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.domain.model.IdiomChar
import com.nolleo.app.domain.model.IdiomItem
import com.nolleo.app.domain.model.Question
import com.nolleo.app.domain.model.RegionInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 정차 중 보기 고르기 게임(지역 퀴즈·사자성어 틀) 규칙 */
class ScreenGamesTest {

    private fun region(code: String, name: String) =
        RegionInfo(code, name, name.take(2), 0.0, 1.0, 0.0, 1.0, 0.5, 0.5, null, emptyList(), emptyList())

    private fun q(id: String, region: String, category: String, text: String, vararg answers: String) =
        Question(id, region, 1, category, text, answers.toList(), "힌트 $id")

    private val content = GameContent(
        regions = listOf(region("seoul", "서울특별시"), region("busan", "부산광역시")),
        regionQuestions = mapOf(
            "seoul" to listOf(
                q("s1", "seoul", "지명", "우리나라의 수도는 부산일까요, 서울일까요?", "서울"),
                q("s2", "seoul", "음식", "뽀얀 소뼈 국물에 밥을 말아 먹는 서울의 향토 음식은 무엇일까요?", "설렁탕", "설농탕"),
            ),
            "busan" to listOf(
                q("b1", "busan", "음식", "부산의 돼지 국물 음식은 무엇일까요?", "돼지국밥"),
                q("b2", "busan", "음식", "부산의 차가운 면 음식은 무엇일까요?", "밀면"),
                q("b3", "busan", "음식", "부산 시장의 달콤한 간식은 무엇일까요?", "씨앗호떡"),
                q("b4", "busan", "지명", "부산의 유명한 해수욕장은 어디일까요?", "해운대"),
            ),
        ),
        generalQuestions = emptyList(),
        idioms = emptyList(),
    )

    private fun regionQuestions(quiz: RegionScreenQuiz, n: Int) = (1..n).mapNotNull { quiz.next() }

    @Test
    fun region_twoOptionQuestion_usesTheTwoOptionsFromTheText() {
        val quiz = RegionScreenQuiz(content, { "seoul" }, Random(1))
        val s1 = regionQuestions(quiz, 2).first { it.id == "s1" }
        assertEquals(listOf("부산", "서울"), s1.choices)
        assertEquals("서울", s1.answer)
        assertEquals("지금 지나는 곳 · 서울특별시", s1.lead)
    }

    @Test
    fun region_openQuestion_fourChoicesFromSameCategoryFirst() {
        val quiz = RegionScreenQuiz(content, { "seoul" }, Random(2))
        val s2 = regionQuestions(quiz, 2).first { it.id == "s2" }
        assertEquals(4, s2.choices.size)
        assertEquals("설렁탕", s2.answer)
        // 같은 갈래(음식)의 다른 문제 정답 3개 — 다른 갈래(해운대)는 모자랄 때만
        assertEquals(setOf("설렁탕", "돼지국밥", "밀면", "씨앗호떡"), s2.choices.toSet())
    }

    @Test
    fun region_prefersCurrentRegion_andNoRepeatUntilAllAsked() {
        val quiz = RegionScreenQuiz(content, { "busan" }, Random(3))
        val ids = regionQuestions(quiz, 4).map { it.id }
        assertEquals(setOf("b1", "b2", "b3", "b4"), ids.toSet())
        val anywhere = RegionScreenQuiz(content, { null }, Random(3))
        assertTrue(anywhere.next()!!.lead.endsWith("퀴즈"))
    }

    @Test
    fun region_scoreAndAfterAnswerSpeech() {
        val quiz = RegionScreenQuiz(content, { "busan" }, Random(4))
        val q = quiz.next()!!
        assertTrue(quiz.answer(q, q.answerIndex))
        assertEquals(10, quiz.score)
        val wrong = q.choices.indices.first { it != q.answerIndex }
        assertFalse(quiz.answer(q, wrong))
        assertEquals(10, quiz.score)
        assertEquals(2, quiz.answered)
        assertEquals(1, quiz.correct)
        assertTrue(quiz.afterAnswer(q, true).first().startsWith("딩동댕!"))
        assertTrue(quiz.afterAnswer(q, false).first().startsWith("아쉬워요. 정답은"))
        assertEquals("힌트 ${q.id}", quiz.afterAnswer(q, true).last())
    }

    @Test
    fun idiomScreenGame_mapsWordsAndChecksAnswer() {
        val items = (1..6).map { i ->
            IdiomItem("i$i", "가나다$i", "家家戶$i", "다음 뜻을 가진 사자성어는? 뜻 $i", null, listOf(IdiomChar("家", "집", "가")), 2)
        }
        val game = IdiomScreenGame(items, Random(7))
        val q = game.next()!!
        assertEquals(IdiomItem.QUESTION_LEAD, q.lead)
        assertEquals(4, q.choices.size)
        assertEquals(q.idiom!!.word, q.answer)
        assertTrue(game.answer(q, q.answerIndex))
        assertEquals(10, game.score)
        assertEquals(10_000L, game.revealMinMs)
    }
}
