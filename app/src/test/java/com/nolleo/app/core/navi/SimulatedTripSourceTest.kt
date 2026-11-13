package com.nolleo.app.core.navi

import com.nolleo.app.core.log.AppLog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatedTripSourceTest {

    @Before
    fun quietLog() {
        AppLog.logcatEnabled = false
    }

    @Test
    fun matchRemain_keepsThePlace_andChangesOnlyThePace() = runTest {
        val sim = SimulatedTripSource(backgroundScope)
        sim.requestRoute(DestinationPreset.SEOUL_BUSAN)
        sim.timeScale.value = 1f
        assertEquals(250 * 60, sim.snapshot.value?.remainTimeSec)
        val shelterKm = sim.snapshot.value!!.restStopsAhead!!.first()

        // 내비가 4시간 0분이라고 하면: 건너뛰지 않고(첫 쉼터 거리 그대로) 남은 시간만 4시간 0분
        sim.matchRemain(240 * 60)
        assertEquals(240 * 60, sim.snapshot.value?.remainTimeSec)
        assertEquals(shelterKm, sim.snapshot.value!!.restStopsAhead!!.first())
        assertEquals("seoul", sim.position.value?.regionCode)

        // 실제 1분(배율 1) 뒤 남은 시간도 1분 준다
        advanceTimeBy(60_001)
        assertEquals(239 * 60, sim.snapshot.value?.remainTimeSec)
    }

    @Test
    fun jumpToRemain_stillJumpsAhead() = runTest {
        val sim = SimulatedTripSource(backgroundScope)
        sim.requestRoute(DestinationPreset.SEOUL_BUSAN)
        sim.jumpToRemain(5 * 60)
        assertEquals(300, sim.snapshot.value?.remainTimeSec)
        assertEquals("busan", sim.position.value?.regionCode)
    }
}
