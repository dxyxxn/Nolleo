package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.IdiomChar
import com.nolleo.app.domain.model.IdiomItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class IdiomQuizGameTest {

    private fun item(id: Int, word: String, meaning: String = "뜻 $id.", difficulty: Int = 2, origin: String? = null) = IdiomItem(
        id = "$id",
        word = word,
        hanja = "",
        question = "${IdiomItem.QUESTION_LEAD} $meaning",
        origin = origin,
        chars = word.map { IdiomChar(hanja = "字", hun = "글자", eum = it.toString()) },
        difficulty = difficulty,
    )

    private val items = listOf(
        item(1, "가가호호"), item(2, "각주구검"), item(3, "결초보은"),
        item(4, "과유불급"), item(5, "금상첨화"), item(6, "다다익선"),
    )

    @Test
    fun next_givesFourDistinctChoicesWithAnswerAtIndex() {
        val game = IdiomQuizGame(items, Random(1))
        repeat(20) {
            val q = game.next()!!
            assertEquals(IdiomQuizGame.CHOICES, q.choices.size)
            assertEquals(4, q.choices.map { it.word }.toSet().size)
            assertEquals(q.item, q.choices[q.answerIndex])
        }
    }

    @Test
    fun next_doesNotRepeatUntilEveryItemWasAsked() {
        val game = IdiomQuizGame(items, Random(3))
        val firstLap = (1..items.size).map { game.next()!!.item.id }
        assertEquals(items.size, firstLap.toSet().size)
        // 한 바퀴를 돌면 다시 처음부터
        assertTrue(game.next()!!.item.id in firstLap)
    }

    @Test
    fun distractors_neverShareTheSameMeaning() {
        // 군맹무상·군맹평상처럼 뜻(질문)이 같은 둘은 서로의 오답 보기로 나오지 않는다
        val twins = items + item(7, "군맹무상", meaning = "같은 뜻.") + item(8, "군맹평상", meaning = "같은 뜻.")
        val game = IdiomQuizGame(twins, Random(5))
        repeat(200) {
            val q = game.next()!!
            q.choices.filter { it != q.item }.forEach { d -> assertFalse(d.question == q.item.question) }
        }
    }

    @Test
    fun distractors_preferSameDifficulty() {
        val mixed = listOf(
            item(1, "가가호호", difficulty = 1), item(2, "각주구검", difficulty = 1),
            item(3, "결초보은", difficulty = 1), item(4, "과유불급", difficulty = 1),
            item(5, "금상첨화", difficulty = 3), item(6, "다다익선", difficulty = 3),
        )
        val game = IdiomQuizGame(mixed, Random(9))
        repeat(30) {
            val q = game.next()!!
            if (q.item.difficulty == 1) assertTrue(q.choices.all { it.difficulty == 1 })
        }
    }

    @Test
    fun answer_scoresTenPointsOnlyWhenCorrect() {
        val game = IdiomQuizGame(items, Random(2))
        val q1 = game.next()!!
        assertTrue(game.answer(q1, q1.answerIndex))
        val q2 = game.next()!!
        assertFalse(game.answer(q2, (q2.answerIndex + 1) % 4))
        assertEquals(10, game.score)
        assertEquals(2, game.answered)
        assertEquals(1, game.correct)
    }

    @Test
    fun fewerThanFourItems_isNotAvailable() {
        val game = IdiomQuizGame(items.take(3))
        assertFalse(game.isAvailable)
        assertNull(game.next())
    }

    @Test
    fun meaning_dropsTheQuestionLead() {
        assertEquals("한 집 한 집.", item(1, "가가호호", meaning = "한 집 한 집.").meaning)
    }
}
