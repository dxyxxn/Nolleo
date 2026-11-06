package com.gildongmu.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerMatcherTest {

    @Test
    fun normalize_removesSpacesAndPunctuation() {
        assertEquals("불국사", AnswerMatcher.normalize(" 불 국 사! "))
        assertEquals("한라봉", AnswerMatcher.normalize("한라봉."))
    }

    @Test
    fun politeSuffixes_areStripped() {
        assertTrue(AnswerMatcher.matches("사과요", listOf("사과")))
        assertTrue(AnswerMatcher.matches("한라봉이요", listOf("한라봉")))
        assertTrue(AnswerMatcher.matches("불국사입니다", listOf("불국사")))
        assertTrue(AnswerMatcher.matches("녹차예요", listOf("녹차")))
        assertTrue(AnswerMatcher.matches("쌀이에요", listOf("쌀")))
    }

    @Test
    fun answerThatEndsWithYo_isNotDamaged() {
        assertTrue(AnswerMatcher.matches("요요", listOf("요요")))
    }

    @Test
    fun anyAcceptedSpelling_matches() {
        assertTrue(AnswerMatcher.matches("영덕대게", listOf("대게", "영덕대게")))
    }

    @Test
    fun containment_matchesLongerUtterance() {
        assertTrue(AnswerMatcher.matches("음 정답은 한우", listOf("한우")))
    }

    @Test
    fun singleSyllableAnswer_requiresExactMatch() {
        assertTrue(AnswerMatcher.matches("배", listOf("배")))
        assertFalse("'배' 가 '배추' 에 포함되어도 정답이 아니다", AnswerMatcher.matches("배추", listOf("배")))
    }

    @Test
    fun editDistanceOne_allowedForThreeOrMoreSyllables() {
        assertTrue(AnswerMatcher.matches("불국자", listOf("불국사")))
        assertFalse(AnswerMatcher.matches("한우", listOf("한라")))
        assertFalse(AnswerMatcher.matches("불고기", listOf("불국사")))
    }

    @Test
    fun blankOrNull_neverMatches() {
        assertFalse(AnswerMatcher.matches(null, listOf("사과")))
        assertFalse(AnswerMatcher.matches("   ", listOf("사과")))
    }

    @Test
    fun wrongAnswer_doesNotMatch() {
        assertFalse(AnswerMatcher.matches("바나나", listOf("사과")))
    }

    @Test
    fun editDistance_basic() {
        assertEquals(0, AnswerMatcher.editDistance("사과", "사과"))
        assertEquals(1, AnswerMatcher.editDistance("사과", "사고"))
        assertEquals(2, AnswerMatcher.editDistance("abc", "a"))
    }
}
