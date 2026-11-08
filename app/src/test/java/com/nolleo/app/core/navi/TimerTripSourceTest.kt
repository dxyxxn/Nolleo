package com.nolleo.app.core.navi

import com.nolleo.app.core.log.AppLog
import com.nolleo.app.domain.model.TripSourceKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerTripSourceTest {

    @Before
    fun quietLog() {
        AppLog.logcatEnabled = false
    }

    @Test
    fun countsDownOnlyWhileMoving_andJumps() = runTest {
        val timer = TimerTripSource(backgroundScope)
        assertEquals(TripSourceKind.TIMER, timer.kind)
        timer.begin("목적지 없이 30분", 30 * 60)
        assertEquals(1800, timer.snapshot.value?.remainTimeSec)
        assertNull("경로가 없으니 경로 위 쉼 장소도 없다", timer.snapshot.value?.restStopsAhead)
        advanceTimeBy(60_001)
        assertEquals(1740, timer.snapshot.value?.remainTimeSec)
        timer.moving.value = false // 주차 중
        advanceTimeBy(60_000)
        assertEquals(1740, timer.snapshot.value?.remainTimeSec)
        timer.moving.value = true
        timer.jumpToRemain(5 * 60)
        assertEquals(300, timer.snapshot.value?.remainTimeSec)
        assertEquals(1800, timer.totalSec)
        timer.clear()
        assertNull(timer.snapshot.value)
        assertNull(timer.totalSec)
    }

    @Test
    fun timeScaleAndArrival() = runTest {
        val timer = TimerTripSource(backgroundScope)
        val arrived = backgroundScope.launch { timer.events.first { it is TripEvent.Arrived } }
        runCurrent()
        timer.begin("목적지 없이 30분", 30 * 60, timeScale = 30f) // 여정=시뮬: 실제 1분 = 30분
        advanceTimeBy(65_000)
        assertEquals(0, timer.snapshot.value?.remainTimeSec)
        assertTrue("정한 시간이 다 되면 도착 이벤트", arrived.isCompleted)
    }
}
