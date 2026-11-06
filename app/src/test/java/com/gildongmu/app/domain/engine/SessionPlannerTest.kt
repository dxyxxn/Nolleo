package com.gildongmu.app.domain.engine

import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {

    @Test
    fun fourHours_givesFiveRounds() {
        val plan = SessionPlanner.plan(4 * 3600, GameChoice.QUIZ)
        assertEquals(5, plan.roundCount)
    }

    @Test
    fun fourHoursTenMinutes_givesFiveRounds() {
        assertEquals(5, SessionPlanner.plan(250 * 60, GameChoice.ALTERNATE).roundCount)
    }

    @Test
    fun longTrip_roundLengthsGapsAndEndBuffer() {
        val remain = 4 * 3600
        val plan = SessionPlanner.plan(remain, GameChoice.QUIZ)
        plan.rounds.forEach { r ->
            assertTrue("판 길이 6~10분: ${r.targetDurationSec}", r.targetDurationSec in 360..600)
        }
        assertEquals("첫 판은 바로 시작", remain, plan.rounds.first().startAtRemainSec)
        plan.rounds.zipWithNext { a, b ->
            val gap = (a.startAtRemainSec - a.targetDurationSec) - b.startAtRemainSec
            assertTrue("판 사이 휴지 ≥ 2분: $gap", gap >= SessionPlanner.MIN_GAP_SEC)
        }
        val last = plan.rounds.last()
        val lastEndsAtRemain = last.startAtRemainSec - last.targetDurationSec
        assertTrue("마지막 판은 도착 5분 전에 끝남: $lastEndsAtRemain", lastEndsAtRemain >= SessionPlanner.END_BUFFER_SEC)
    }

    @Test
    fun underTwelveMinutes_givesSingleShortRound() {
        val plan = SessionPlanner.plan(10 * 60, GameChoice.WORD_CHAIN)
        assertEquals(1, plan.roundCount)
        val r = plan.rounds.single()
        assertTrue(r.isShort)
        assertEquals(GameKind.WORD_CHAIN, r.game)
        assertTrue(r.questionCount in 3..6)
        assertTrue("짧은 판도 도착 5분 전에 끝남", 10 * 60 - r.targetDurationSec >= SessionPlanner.END_BUFFER_SEC)
    }

    @Test
    fun veryShortTrip_stillOneShortRound() {
        val plan = SessionPlanner.plan(4 * 60, GameChoice.QUIZ)
        assertEquals(1, plan.roundCount)
        assertTrue(plan.rounds.single().isShort)
    }

    @Test
    fun exactlyTwelveMinutes_isNormalSingleRound() {
        val plan = SessionPlanner.plan(12 * 60, GameChoice.QUIZ)
        assertEquals(1, plan.roundCount)
        assertFalse(plan.rounds.single().isShort)
        assertTrue(12 * 60 - plan.rounds.single().targetDurationSec >= SessionPlanner.END_BUFFER_SEC)
    }

    @Test
    fun alternate_switchesGames() {
        val plan = SessionPlanner.plan(3 * 3600, GameChoice.ALTERNATE)
        assertEquals(GameKind.REGION_QUIZ, plan.rounds[0].game)
        assertEquals(GameKind.WORD_CHAIN, plan.rounds[1].game)
        assertEquals(GameKind.REGION_QUIZ, plan.rounds[2].game)
    }

    @Test
    fun demoMode_usesFewQuestions() {
        val plan = SessionPlanner.plan(4 * 3600, GameChoice.QUIZ, demo = true)
        assertTrue(plan.rounds.all { it.questionCount == SessionPlanner.DEMO_QUESTION_COUNT })
    }

    @Test
    fun questionCount_followsDuration() {
        val plan = SessionPlanner.plan(4 * 3600, GameChoice.QUIZ)
        val r = plan.rounds.first()
        assertEquals(r.targetDurationSec / SessionPlanner.SEC_PER_QUESTION, r.questionCount)
    }

    @Test
    fun needsReplan_thresholdTwentyPercent() {
        assertFalse(SessionPlanner.needsReplan(expectedRemainSec = 6000, actualRemainSec = 6900)) // +15%
        assertTrue(SessionPlanner.needsReplan(expectedRemainSec = 6000, actualRemainSec = 7500)) // +25%
        assertTrue(SessionPlanner.needsReplan(expectedRemainSec = 6000, actualRemainSec = 4500)) // -25%
    }

    @Test
    fun replan_keepsPlayedRounds_andRecomputesRest() {
        val original = SessionPlanner.plan(4 * 3600, GameChoice.ALTERNATE)
        // 첫 판 진행 중 정체로 남은 시간이 크게 늘었다
        val replanned = SessionPlanner.replanRemaining(original, keepCount = 1, actualRemainSec = 5 * 3600, choice = GameChoice.ALTERNATE)
        assertEquals("진행 중인 판은 유지", original.rounds[0], replanned.rounds[0])
        assertTrue("남은 판 수가 늘어남", replanned.roundCount > original.roundCount)
        assertEquals(1, replanned.rounds[1].index)
        assertEquals(GameKind.WORD_CHAIN, replanned.rounds[1].game)
        assertTrue("새 첫 판은 최소 휴지 뒤", replanned.rounds[1].startAtRemainSec <= 5 * 3600 - SessionPlanner.MIN_GAP_SEC)
        val last = replanned.rounds.last()
        assertTrue(last.startAtRemainSec - last.targetDurationSec >= SessionPlanner.END_BUFFER_SEC)
    }

    @Test
    fun replan_whenAlmostArrived_dropsRemainingRounds() {
        val original = SessionPlanner.plan(4 * 3600, GameChoice.QUIZ)
        val replanned = SessionPlanner.replanRemaining(original, keepCount = 2, actualRemainSec = 8 * 60, choice = GameChoice.QUIZ)
        assertEquals(2, replanned.roundCount)
    }

    @Test
    fun secondsUntilNextRound_isRemainMinusThreshold() {
        // "남은 시간이 3시간 32분일 때 시작" 인 판을, 남은 4시간 10분 시점에서 보면 38분 뒤
        assertEquals(38 * 60, SessionPlanner.secondsUntilNextRound(250 * 60, 212 * 60))
        assertEquals("이미 지났으면 0", 0, SessionPlanner.secondsUntilNextRound(200 * 60, 212 * 60))
        assertEquals(null, SessionPlanner.secondsUntilNextRound(null, 212 * 60))
        assertEquals(null, SessionPlanner.secondsUntilNextRound(250 * 60, null))
        assertEquals("보너스 판(즉시 시작)", 0, SessionPlanner.secondsUntilNextRound(3600, Int.MAX_VALUE))
    }

    @Test
    fun previewText_formats() {
        val plan = SessionPlanner.plan(250 * 60, GameChoice.QUIZ)
        val text = SessionPlanner.previewText(250 * 60, plan) { "4시간 10분" }
        assertEquals("4시간 10분 → 5판 (판당 약 10분)", text)
    }
}
