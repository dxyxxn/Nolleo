package com.nolleo.app.domain.engine

import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.RoundPacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionPlannerTest {

    @Test
    fun randomChoice_sameSeedSameGames_bothKinds_neverThreeInARow() {
        val plan = SessionPlanner.plan(10 * 3600, GameChoice.RANDOM, pacing = RoundPacing.OFTEN, gameSeed = 42L)
        val games = plan.rounds.map { it.game }
        assertEquals(games, SessionPlanner.plan(10 * 3600, GameChoice.RANDOM, pacing = RoundPacing.OFTEN, gameSeed = 42L).rounds.map { it.game })
        assertTrue(GameKind.REGION_QUIZ in games && GameKind.WORD_CHAIN in games)
        assertTrue(games.windowed(3).none { w -> w.toSet().size == 1 })
        // 다시 짜도 판 번호별 게임은 같은 순서를 따른다
        val replanned = SessionPlanner.replanRemaining(plan, keepCount = 2, actualRemainSec = 12 * 3600, choice = GameChoice.RANDOM)
        assertEquals(SessionPlanner.randomGames(42L, replanned.roundCount), replanned.rounds.map { it.game })
        assertEquals(42L, replanned.gameSeed)
    }

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

    /** 판 간격별: 판 길이 6~10분, 판 사이 ≥ 2분, 마지막 판은 도착 5분 전에 끝난다 */
    private fun assertPlanInvariants(plan: com.nolleo.app.domain.model.SessionPlan) = assertRoundInvariants(plan.rounds)

    private fun assertRoundInvariants(rounds: List<com.nolleo.app.domain.model.RoundPlan>) {
        rounds.forEach { assertTrue(it.targetDurationSec in SessionPlanner.MIN_ROUND_SEC..SessionPlanner.MAX_ROUND_SEC) }
        rounds.zipWithNext { a, b ->
            assertTrue("판 사이 휴지 ≥ 2분", (a.startAtRemainSec - a.targetDurationSec) - b.startAtRemainSec >= SessionPlanner.MIN_GAP_SEC)
        }
        val last = rounds.last()
        assertTrue(last.startAtRemainSec - last.targetDurationSec >= SessionPlanner.END_BUFFER_SEC)
    }

    @Test
    fun pacing_relaxedIsDefault_andOftenNonstopAddRounds() {
        val remain = 250 * 60
        val relaxed = SessionPlanner.plan(remain, GameChoice.QUIZ)
        val often = SessionPlanner.plan(remain, GameChoice.QUIZ, pacing = RoundPacing.OFTEN)
        val nonstop = SessionPlanner.plan(remain, GameChoice.QUIZ, pacing = RoundPacing.NONSTOP)
        assertEquals(RoundPacing.RELAXED, relaxed.pacing)
        assertEquals(5, relaxed.roundCount)
        assertEquals(12, often.roundCount)
        assertEquals(20, nonstop.roundCount)
        listOf(relaxed, often, nonstop).forEach(::assertPlanInvariants)
        // 4시간 10분 기준 판 사이 쉬는 시간: 여유 약 39분 > 자주 약 10분 > 쉬지 않고 2분대
        assertEquals(39 * 60, SessionPlanner.restBetweenRoundsSec(relaxed))
        assertTrue(SessionPlanner.restBetweenRoundsSec(often)!! in 9 * 60..11 * 60)
        assertTrue(SessionPlanner.restBetweenRoundsSec(nonstop)!! in SessionPlanner.MIN_GAP_SEC..3 * 60)
    }

    @Test
    fun replan_keepsPacing() {
        val original = SessionPlanner.plan(4 * 3600, GameChoice.ALTERNATE, pacing = RoundPacing.NONSTOP)
        val replanned = SessionPlanner.replanRemaining(original, keepCount = 1, actualRemainSec = 5 * 3600, choice = GameChoice.ALTERNATE)
        assertEquals(RoundPacing.NONSTOP, replanned.pacing)
        assertRoundInvariants(replanned.rounds.drop(1)) // 새로 짠 판들 (진행한 판은 옛 기준 그대로)
        val restAfterReplan = (replanned.rounds[1].startAtRemainSec - replanned.rounds[1].targetDurationSec) - replanned.rounds[2].startAtRemainSec
        assertTrue("쉬지 않고: 다시 짜도 판 사이 2분대", restAfterReplan in SessionPlanner.MIN_GAP_SEC..3 * 60)
    }

    @Test
    fun restBetweenRounds_singleRoundIsNull() {
        assertEquals(null, SessionPlanner.restBetweenRoundsSec(SessionPlanner.plan(10 * 60, GameChoice.QUIZ)))
    }

    @Test
    fun previewText_formats() {
        val plan = SessionPlanner.plan(250 * 60, GameChoice.QUIZ)
        val text = SessionPlanner.previewText(250 * 60, plan) { "4시간 10분" }
        assertEquals("4시간 10분 → 5판 (판당 약 10분)", text)
    }

    @Test
    fun previewText_tooCloseToArrival_saysNoGame() {
        // 남은 5분 이하면 세션이 시작하자마자 "곧 도착"으로 끝나므로 출발을 막는다 (홈 미리보기도 같은 문구)
        val text = SessionPlanner.previewText(4 * 60, SessionPlanner.plan(4 * 60, GameChoice.QUIZ)) { "4분" }
        assertEquals("4분 → ${SessionPlanner.TOO_CLOSE_MESSAGE}", text)
        assertEquals("6분 → 짧은 판 1개", SessionPlanner.previewText(6 * 60, SessionPlanner.plan(6 * 60, GameChoice.QUIZ)) { "6분" })
    }
}
