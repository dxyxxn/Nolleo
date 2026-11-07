package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.IdiomItem
import org.junit.Assert.assertEquals
import org.junit.Test

class IdiomSpeechTest {

    private fun item(word: String, question: String, origin: String?) =
        IdiomItem("1", word, "", question, origin, emptyList(), 2)

    @Test
    fun clean_dropsHanjaInParentheses() {
        assertEquals(
            "촌수가 먼 인척을 이르는 말.",
            IdiomSpeech.clean("촌수가 먼 인척(姻戚)을 이르는 말."),
        )
        assertEquals("금과 슬이 합주하여", IdiomSpeech.clean("금(琴)과 슬(瑟)이 합주하여"))
    }

    @Test
    fun clean_dropsBookMarksAndReadsMiddleDotAsComma() {
        assertEquals(
            "여씨춘추의 찰금편에 나오는 말이다.",
            IdiomSpeech.clean("≪여씨춘추≫의 <찰금편(察今篇)>에 나오는 말이다."),
        )
        assertEquals("코, 혀, 몸으로 냄새를", IdiomSpeech.clean("코·혀·몸으로 냄새를"))
        assertEquals("이른바 출세를 뜻한다", IdiomSpeech.clean("이른바 ‘출세’를 뜻한다"))
    }

    @Test
    fun clean_dropsStrayHanjaAndExtraSpaces() {
        assertEquals("한자 섞인 문장.", IdiomSpeech.clean("한자 家 섞인  문장 ."))
    }

    @Test
    fun afterAnswer_readsAnswerThenOrigin() {
        val withOrigin = item("각주구검", "다음 뜻을 가진 사자성어는? 융통성 없이.", "≪여씨춘추≫에 나오는 말이다.")
        assertEquals(listOf("정답! 각주구검.", "여씨춘추에 나오는 말이다."), IdiomSpeech.afterAnswer(withOrigin, correct = true))
        val noOrigin = item("가가호호", "다음 뜻을 가진 사자성어는? 한 집 한 집.", null)
        assertEquals(listOf("아쉬워요. 정답은 가가호호."), IdiomSpeech.afterAnswer(noOrigin, correct = false))
    }

    @Test
    fun question_isReadInFull() {
        val q = item("가부지친", "다음 뜻을 가진 사자성어는? 촌수가 먼 인척(姻戚)을 이르는 말.", null)
        assertEquals("다음 뜻을 가진 사자성어는? 촌수가 먼 인척을 이르는 말.", IdiomSpeech.question(q))
    }
}
