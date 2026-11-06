package com.gildongmu.app.app

import com.gildongmu.app.app.WindowPlacement.Box
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 좌표는 Pleos Connect Emulator 2.1.0 (2560×1440) 에서 측정한 창 경계 */
class WindowPlacementTest {

    private val screenRight = 2560
    private val full = Box(853, 2535) // 앱 영역 전체 (landscape)
    private val driverSide = Box(853, 1694) // 3분할 왼쪽 창 (Driving view 옆)
    private val passengerSide = Box(1718, 2559) // 3분할 오른쪽 창

    @Test
    fun slots_areRecognized() {
        assertTrue(WindowPlacement.isDriverSideSlot(driverSide, screenRight))
        assertFalse(WindowPlacement.isDriverSideSlot(passengerSide, screenRight))
        assertFalse(WindowPlacement.isDriverSideSlot(full, screenRight))
    }

    @Test
    fun splitFromFull_intoDriverSide_moves() {
        assertTrue(WindowPlacement.shouldMoveToPassengerSide(full, driverSide, screenRight, cameToFront = false))
    }

    @Test
    fun firstShownOrBackInFront_onDriverSide_moves() {
        assertTrue(WindowPlacement.shouldMoveToPassengerSide(null, driverSide, screenRight, cameToFront = false))
        assertTrue(WindowPlacement.shouldMoveToPassengerSide(driverSide, driverSide, screenRight, cameToFront = true))
    }

    @Test
    fun userSwappedWithSameSize_isRespected() {
        assertFalse(WindowPlacement.shouldMoveToPassengerSide(passengerSide, driverSide, screenRight, cameToFront = false))
    }

    @Test
    fun alreadyOnPassengerSideOrFull_staysPut() {
        assertFalse(WindowPlacement.shouldMoveToPassengerSide(driverSide, passengerSide, screenRight, cameToFront = false))
        assertFalse(WindowPlacement.shouldMoveToPassengerSide(null, passengerSide, screenRight, cameToFront = true))
        assertFalse(WindowPlacement.shouldMoveToPassengerSide(passengerSide, full, screenRight, cameToFront = true))
    }

    @Test
    fun unrelatedConfigChange_onDriverSide_doesNothing() {
        assertFalse(WindowPlacement.shouldMoveToPassengerSide(driverSide, driverSide, screenRight, cameToFront = false))
    }
}
