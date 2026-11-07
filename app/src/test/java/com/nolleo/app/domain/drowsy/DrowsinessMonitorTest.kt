package com.nolleo.app.domain.drowsy

import com.nolleo.app.domain.model.DriveState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrowsinessMonitorTest {

    private fun sample(latency: Long, correct: Boolean? = true) =
        ResponseSample(latency, correct, difficulty = 2, atEpochMs = 0, driveState = DriveState.DRIVING)

    @Test
    fun calibration_firstFiveSamples_noAlerts() {
        val m = DrowsinessMonitor(initialBaseline = null)
        listOf(1200L, 1500L, 9000L, 1800L, 1400L).forEachIndexed { i, l ->
            assertEquals(DrowsinessMonitor.Action.None, m.record(sample(l), i * 1000L))
        }
        assertTrue("타임아웃은 캘리브레이션에서 제외 → 아직 4개", m.snapshot().calibrating)
        m.record(sample(1600), 6_000)
        val snap = m.snapshot()
        assertTrue(!snap.calibrating)
        assertEquals("중앙값", 1500L, snap.baselineMs)
    }

    @Test
    fun slowRule_isMaxOfRatioAndExtra() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        // max(1500*1.5=2250, 1500+1500=3000) = 3000
        assertTrue(!m.isSlow(3000, 1500.0))
        assertTrue(m.isSlow(3001, 1500.0))
        // 기준이 4000 이면 max(6000, 5500) = 6000
        assertTrue(!m.isSlow(5900, 4000.0))
        assertTrue(m.isSlow(6100, 4000.0))
    }

    @Test
    fun stage1_triggersOnThreeSlowInWindow_thenCooldown() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        var t = 0L
        fun rec(l: Long) = m.record(sample(l), t).also { t += 20_000 }

        assertEquals(DrowsinessMonitor.Action.None, rec(1400))
        assertEquals(DrowsinessMonitor.Action.None, rec(4000))
        assertEquals(DrowsinessMonitor.Action.None, rec(5000))
        val action = rec(8000)
        assertEquals(DrowsinessMonitor.Action.Stage1(2), action)
        assertEquals(2, m.difficultyCap)
        // 쿨다운 5분 동안은 계속 느려도 다시 걸리지 않는다
        repeat(5) { assertEquals(DrowsinessMonitor.Action.None, rec(8000)) }
        assertEquals(1, m.snapshot().stage1Count)
    }

    @Test
    fun secondStage1Within15Min_becomesStage2_thenStage1WhileStage2CoolsDown() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        repeat(3) { m.record(sample(8000), 0) }
        assertEquals(2, m.difficultyCap)
        // 5분 쿨다운 뒤 다시 느림 3개 → 15분 안의 두 번째 → Stage2
        assertEquals(
            DrowsinessMonitor.Action.Stage2(1, DrowsinessMonitor.Stage2Reason.REPEATED_STAGE1),
            m.record(sample(8000), 5 * 60_000 + 1),
        )
        // Stage2 쿨다운(20분) 중에는 Stage1 로만 (난이도 최소 1)
        assertEquals(DrowsinessMonitor.Action.Stage1(1), m.record(sample(8000), 11 * 60_000))
        assertEquals("최소 1", 1, m.difficultyCap)
        assertEquals(2, m.snapshot().stage1Count)
        assertEquals(1, m.snapshot().stage2Count)
    }

    @Test
    fun stage2_slowAndInaccurate_whenWindowFull() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        var t = 0L
        fun rec(l: Long, correct: Boolean?) = m.record(sample(l, correct), t).also { t += 10_000 }
        assertEquals(DrowsinessMonitor.Action.None, rec(8000, false))
        assertEquals(DrowsinessMonitor.Action.None, rec(8000, false))
        assertEquals("창이 덜 찼으면 1단계", DrowsinessMonitor.Action.Stage1(2), rec(8000, false))
        assertEquals(DrowsinessMonitor.Action.None, rec(8000, true))
        // 5개 중 정답 1개(20%) + 이번 응답도 느림 → 2단계
        assertEquals(
            DrowsinessMonitor.Action.Stage2(1, DrowsinessMonitor.Stage2Reason.SLOW_AND_INACCURATE),
            rec(8000, false),
        )
        // 2단계 쿨다운 동안은 다시 걸리지 않는다
        repeat(3) { assertEquals(DrowsinessMonitor.Action.None, rec(8000, false)) }
    }

    @Test
    fun slowButAccurate_isNotStage2() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        var t = 0L
        val actions = (1..6).map { m.record(sample(8000, true), t).also { t += 10_000 } }
        assertTrue(actions.none { it is DrowsinessMonitor.Action.Stage2 })
        assertEquals(1, m.snapshot().stage1Count)
    }

    @Test
    fun forceStage2_andResetAfterRest() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        repeat(3) { m.record(sample(8000), 0) }
        val forced = m.forceStage2(1_000)
        assertEquals(DrowsinessMonitor.Stage2Reason.MANUAL, forced.reason)
        assertEquals(1, m.difficultyCap)
        m.resetAfterRest()
        assertEquals("쉬고 나면 난이도 처음으로", 3, m.difficultyCap)
        assertTrue("응답 창 비움", m.snapshot().recentLatencies.isEmpty())
        assertEquals(1, m.snapshot().stage2Count)
    }

    @Test
    fun demoConfig_onlyShortensCooldowns() {
        val demo = DrowsinessMonitor.Config.DEMO
        val normal = DrowsinessMonitor.Config()
        assertTrue(demo.cooldownMs < normal.cooldownMs && demo.stage2CooldownMs < normal.stage2CooldownMs)
        assertEquals(normal.copy(cooldownMs = demo.cooldownMs, stage2CooldownMs = demo.stage2CooldownMs), demo)
    }

    @Test
    fun baselineEma_updatesOnlyWithNormalSamples() {
        val m = DrowsinessMonitor(DriverBaseline(2000.0, 0.9, 10))
        m.record(sample(1000), 0)
        assertEquals(1900L, m.snapshot().baselineMs) // 2000*0.9 + 1000*0.1
        m.record(sample(8000), 1_000) // 느림 → 기준값 유지
        assertEquals(1900L, m.snapshot().baselineMs)
    }

    @Test
    fun noBaseline_noTriggerEvenIfSlow() {
        val m = DrowsinessMonitor(initialBaseline = null)
        repeat(4) { assertEquals(DrowsinessMonitor.Action.None, m.record(sample(7000), it * 1000L)) }
        assertNull(m.snapshot().baselineMs)
    }

    @Test
    fun restore_keepsWindowAndCooldown() {
        val m = DrowsinessMonitor(DriverBaseline(1500.0, 0.9, 10))
        m.restore(listOf(sample(8000), sample(8000)), difficultyCap = 2, lastTriggerAtMs = 1_000, stage1Count = 1)
        assertEquals(2, m.difficultyCap)
        // 쿨다운 안: 세 번째 느린 응답이 와도 트리거 없음
        assertEquals(DrowsinessMonitor.Action.None, m.record(sample(8000), 2_000))
    }
}
