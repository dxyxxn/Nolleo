package com.nolleo.app.domain.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HangulTest {

    @Test
    fun dueum_nieun() {
        assertEquals('여', Hangul.dueum('녀'))
        assertEquals('요', Hangul.dueum('뇨'))
        assertEquals('유', Hangul.dueum('뉴'))
        assertEquals('이', Hangul.dueum('니'))
    }

    @Test
    fun dueum_rieulToIeung() {
        assertEquals('야', Hangul.dueum('랴'))
        assertEquals('여', Hangul.dueum('려'))
        assertEquals('예', Hangul.dueum('례'))
        assertEquals('요', Hangul.dueum('료'))
        assertEquals('유', Hangul.dueum('류'))
        assertEquals('이', Hangul.dueum('리'))
    }

    @Test
    fun dueum_rieulToNieun() {
        assertEquals('나', Hangul.dueum('라'))
        assertEquals('내', Hangul.dueum('래'))
        assertEquals('노', Hangul.dueum('로'))
        assertEquals('뇌', Hangul.dueum('뢰'))
        assertEquals('누', Hangul.dueum('루'))
        assertEquals('느', Hangul.dueum('르'))
    }

    @Test
    fun dueum_keepsFinalConsonant() {
        assertEquals('역', Hangul.dueum('력'))
        assertEquals('연', Hangul.dueum('년'))
        assertEquals('낙', Hangul.dueum('락'))
        assertEquals('임', Hangul.dueum('림'))
        assertEquals('양', Hangul.dueum('량'))
    }

    @Test
    fun dueum_noChangeForOthers() {
        assertNull(Hangul.dueum('과'))
        assertNull(Hangul.dueum('나'))
        assertNull(Hangul.dueum('노')) // ㄴ+ㅗ 는 두음법칙 대상 아님
        assertNull(Hangul.dueum('A'))
    }

    @Test
    fun allowedStarts_includesOriginalAndDueum() {
        assertEquals(setOf('력', '녁', '역'), Hangul.allowedStarts('력'))
        assertEquals(setOf('과'), Hangul.allowedStarts('과'))
    }

    @Test
    fun allowedStarts_isBidirectionalForRieulAndNieun() {
        // ㄹ↔ㄴ (ㅏ·ㅐ·ㅗ·ㅚ·ㅜ·ㅡ)
        assertEquals(setOf('래', '내'), Hangul.allowedStarts('래'))
        assertEquals(setOf('내', '래'), Hangul.allowedStarts('내'))
        assertEquals(setOf('노', '로'), Hangul.allowedStarts('노'))
        // ㄹ·ㄴ·ㅇ (ㅕ·ㅛ·ㅠ·ㅣ) — 려↔여, 녀↔여, 리↔니↔이
        assertEquals(setOf('려', '녀', '여'), Hangul.allowedStarts('려'))
        assertEquals(setOf('녀', '려', '여'), Hangul.allowedStarts('녀'))
        assertEquals(setOf('여', '려', '녀'), Hangul.allowedStarts('여'))
        assertEquals(setOf('이', '리', '니'), Hangul.allowedStarts('이'))
        // ㄹ↔ㅇ 만 (ㅑ·ㅖ — ㄴ 은 두음법칙 대상 아님)
        assertEquals(setOf('야', '랴'), Hangul.allowedStarts('야'))
        assertEquals(setOf('예', '례'), Hangul.allowedStarts('예'))
        // 받침 유지
        assertEquals(setOf('량', '양'), Hangul.allowedStarts('량'))
        assertEquals(setOf('녁', '력', '역'), Hangul.allowedStarts('녁'))
        assertEquals(setOf('낙', '락'), Hangul.allowedStarts('낙'))
        // 대상이 아닌 음절·ㅇ 초성이지만 짝이 없는 음절
        assertEquals(setOf('아'), Hangul.allowedStarts('아'))
        assertEquals(setOf('기'), Hangul.allowedStarts('기'))
    }

    @Test
    fun chosung() {
        assertEquals("ㅅㄱ", Hangul.chosungOf("사과"))
        assertEquals("ㅎㅂㄹㄱ", Hangul.chosungOf("해바라기"))
    }

    @Test
    fun josa() {
        assertEquals("사과예요", Hangul.ieyo("사과"))
        assertEquals("쌀이에요", Hangul.ieyo("쌀"))
        assertEquals("아빠가", Hangul.iGa("아빠"))
        assertEquals("엄마가", Hangul.iGa("엄마"))
        assertEquals("조수석이", Hangul.iGa("조수석"))
        // 따옴표로 감싼 낱말은 따옴표 앞 음절로 고른다 (끝말잇기 "'사람'은 이미 나왔어요")
        assertEquals("'사람'은", Hangul.eunNeun("'사람'"))
        assertEquals("'기차'는", Hangul.eunNeun("'기차'"))
        assertEquals("'사람'이에요", Hangul.ieyo("'사람'"))
        assertEquals("'기차'예요", Hangul.ieyo("'기차'"))
        assertEquals("'과'로", "'과'" + Hangul.euro("과").drop(1))
        assertEquals("역으로", Hangul.euro("역"))
        assertEquals("길로", Hangul.euro("길")) // ㄹ 받침은 "로"
        assertTrue(Hangul.hasBatchim('력'))
        assertFalse(Hangul.hasBatchim('과'))
    }
}
