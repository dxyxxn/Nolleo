package com.gildongmu.app.core.navi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripUnitsTest {

    @Test
    fun noRoute_zeroTimeAndDistance_isIgnored() {
        // 새 경로 요청 직후 이전 경로 취소(FREE_DRIVE)에서 오는 onDrivingInfo 0/0 — 남은 시간 0(=도착)으로 쓰면 안 된다
        assertTrue(TripUnits.isNoRoute(0, 0))
        assertFalse("도착 직전(시간 0, 거리 남음)은 유효", TripUnits.isNoRoute(0, 150))
        assertFalse(TripUnits.isNoRoute(8100, 195_000))
    }

    @Test
    fun seconds_andFormatting() {
        assertEquals(14_820, TripUnits.durationSec(14_820, 378_000))
        assertEquals(0, TripUnits.remainTimeSec(0, 0))
        assertEquals("4시간 7분", TripUnits.formatDuration(14_820))
        assertEquals("1분 미만", TripUnits.formatDuration(30))
    }
}
