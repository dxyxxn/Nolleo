package com.nolleo.app.domain.engine

import com.nolleo.app.domain.model.CarSignals
import com.nolleo.app.domain.model.DriveState
import com.nolleo.app.domain.model.GearPosition

/**
 * 원시 차량 신호 → [DriveState]. 순수 로직, 시간은 인자로 받는다.
 *
 * - PARKED  : gear == PARK. 기어를 모를 때만 parkingBrake == true && speed ≈ 0 을 보조로 쓴다.
 *             (기어가 D 인데 주차 브레이크 텔테일이 켜져 있는 에뮬레이터 상태에서 PARKED 로 오판하지 않기 위함)
 * - STOPPED : gear != PARK && speed < 0.5 m/s 가 [stopHoldMs] 이상 지속, 또는 속도 0 + 브레이크 페달(Pleos Vehicle 보조 신호)
 * - DRIVING : speed ≥ 0.5 m/s, 또는 속도를 모를 때(CAR_SPEED 거부) gear != PARK
 * - UNKNOWN : Car API 미연결 / 신호 전무
 */
class DriveStateDeriver(
    private val stopHoldMs: Long = 3_000,
    private val movingThresholdMps: Float = 0.5f,
) {
    data class Result(val state: DriveState, val recheckAtMs: Long?)

    private var lowSpeedSinceMs: Long? = null

    fun derive(signals: CarSignals, nowMs: Long): Result {
        if (!signals.connected) {
            lowSpeedSinceMs = null
            return Result(DriveState.UNKNOWN, null)
        }
        val gear = signals.gear
        val speed = signals.speedMps?.let { kotlin.math.abs(it) }

        val parked = when {
            gear != null -> gear == GearPosition.PARK
            signals.parkingBrakeOn == true && speed != null && speed < movingThresholdMps -> true
            else -> false
        }
        if (parked) {
            lowSpeedSinceMs = null
            return Result(DriveState.PARKED, null)
        }
        if (gear == null && speed == null) {
            lowSpeedSinceMs = null
            return Result(DriveState.UNKNOWN, null)
        }
        if (speed == null) {
            // 속도 퍼미션 거부: STOPPED 판정 불가 → DRIVING 으로 간주
            lowSpeedSinceMs = null
            return Result(DriveState.DRIVING, null)
        }
        if (speed >= movingThresholdMps) {
            lowSpeedSinceMs = null
            return Result(DriveState.DRIVING, null)
        }
        // 저속 + 비주차
        val since = lowSpeedSinceMs ?: nowMs.also { lowSpeedSinceMs = it }
        if (signals.brakePedalPressed == true) {
            return Result(DriveState.STOPPED, null)
        }
        val heldMs = nowMs - since
        return if (heldMs >= stopHoldMs) {
            Result(DriveState.STOPPED, null)
        } else {
            Result(DriveState.DRIVING, since + stopHoldMs)
        }
    }
}
