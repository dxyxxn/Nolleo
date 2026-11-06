package com.gildongmu.app.domain.engine

import com.gildongmu.app.domain.model.CarSignals
import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GearPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveStateDeriverTest {

    private val deriver = DriveStateDeriver(stopHoldMs = 3_000)

    @Test
    fun disconnected_isUnknown() {
        assertEquals(DriveState.UNKNOWN, deriver.derive(CarSignals(connected = false), 0).state)
    }

    @Test
    fun gearPark_isParked() {
        val r = deriver.derive(CarSignals(connected = true, gear = GearPosition.PARK, speedMps = 0f), 0)
        assertEquals(DriveState.PARKED, r.state)
    }

    @Test
    fun gearDrive_withParkingBrakeTelltale_isNotParked() {
        // 기어를 알면 기어가 우선 (에뮬레이터는 주차 브레이크 텔테일이 켜진 채로 D 가 될 수 있다)
        val r = deriver.derive(
            CarSignals(connected = true, gear = GearPosition.DRIVE, speedMps = 0f, parkingBrakeOn = true),
            0,
        )
        assertEquals(DriveState.DRIVING, r.state)
    }

    @Test
    fun unknownGear_parkingBrakeAndZeroSpeed_isParked() {
        val r = deriver.derive(CarSignals(connected = true, gear = null, speedMps = 0f, parkingBrakeOn = true), 0)
        assertEquals(DriveState.PARKED, r.state)
    }

    @Test
    fun moving_isDriving() {
        val r = deriver.derive(CarSignals(connected = true, gear = GearPosition.DRIVE, speedMps = 12f), 0)
        assertEquals(DriveState.DRIVING, r.state)
        assertNull(r.recheckAtMs)
    }

    @Test
    fun reverseSpeed_isAbsoluteValue() {
        val r = deriver.derive(CarSignals(connected = true, gear = GearPosition.REVERSE, speedMps = -2f), 0)
        assertEquals(DriveState.DRIVING, r.state)
    }

    @Test
    fun lowSpeed_becomesStoppedAfterThreeSeconds() {
        val signals = CarSignals(connected = true, gear = GearPosition.DRIVE, speedMps = 0.1f)
        val first = deriver.derive(signals, 1_000)
        assertEquals(DriveState.DRIVING, first.state)
        assertEquals(4_000L, first.recheckAtMs)
        assertEquals(DriveState.DRIVING, deriver.derive(signals, 3_999).state)
        assertEquals(DriveState.STOPPED, deriver.derive(signals, 4_000).state)
    }

    @Test
    fun movingAgain_resetsStopTimer() {
        val slow = CarSignals(connected = true, gear = GearPosition.DRIVE, speedMps = 0f)
        val fast = slow.copy(speedMps = 5f)
        deriver.derive(slow, 0)
        deriver.derive(fast, 2_000)
        assertEquals(DriveState.DRIVING, deriver.derive(slow, 4_000).state)
        assertEquals(DriveState.STOPPED, deriver.derive(slow, 7_000).state)
    }

    @Test
    fun brakePedal_makesStoppedImmediately() {
        val r = deriver.derive(
            CarSignals(connected = true, gear = GearPosition.DRIVE, speedMps = 0f, brakePedalPressed = true),
            0,
        )
        assertEquals(DriveState.STOPPED, r.state)
    }

    @Test
    fun speedPermissionDenied_gearDrive_isDriving() {
        val r = deriver.derive(CarSignals(connected = true, gear = GearPosition.DRIVE, speedMps = null), 0)
        assertEquals(DriveState.DRIVING, r.state)
    }

    @Test
    fun noGearNoSpeed_isUnknown() {
        assertEquals(DriveState.UNKNOWN, deriver.derive(CarSignals(connected = true), 0).state)
    }
}
