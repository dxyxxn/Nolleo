package com.gildongmu.app.domain.games

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
        assertEquals(setOf('력', '역'), Hangul.allowedStarts('력'))
        assertEquals(setOf('과'), Hangul.allowedStarts('과'))
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
        assertEquals("'과'로", "'과'" + Hangul.euro("과").drop(1))
        assertEquals("역으로", Hangul.euro("역"))
        assertEquals("길로", Hangul.euro("길")) // ㄹ 받침은 "로"
        assertTrue(Hangul.hasBatchim('력'))
        assertFalse(Hangul.hasBatchim('과'))
    }
}
