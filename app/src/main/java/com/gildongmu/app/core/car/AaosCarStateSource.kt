package com.gildongmu.app.core.car

import android.car.Car
import android.car.VehicleGear
import android.car.VehiclePropertyIds
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.domain.model.CarSignals
import com.gildongmu.app.domain.model.GearPosition
import com.gildongmu.app.domain.model.UxState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 표준 차량 데이터(기어·속도·주차 브레이크)는 AAOS CarPropertyManager 로 읽는다 (DEV_GUIDE §5.6/§8.1 주석).
 * 주행 규제는 CarUxRestrictionsManager (DEV_GUIDE §7.1). 모두 콜백 기반 — 폴링하지 않는다.
 *
 * 퍼미션 (에뮬레이터 `dumpsys package permission` 으로 확인):
 *  - android.car.permission.CAR_POWERTRAIN (normal)  : GEAR_SELECTION, PARKING_BRAKE_ON
 *  - android.car.permission.CAR_SPEED      (dangerous): PERF_VEHICLE_SPEED → 런타임 요청, 거부 시 기어만으로 동작
 */
class AaosCarStateSource(context: Context) : CarStateSource {

    private val appContext = context.applicationContext

    override val kind = CarSourceKind.REAL

    private val _signals = MutableStateFlow(CarSignals())
    override val signals: StateFlow<CarSignals> = _signals.asStateFlow()

    private val _ux = MutableStateFlow<UxState?>(null)
    override val ux: StateFlow<UxState?> = _ux.asStateFlow()

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
    private var uxManager: CarUxRestrictionsManager? = null
    private var speedRegistered = false
    private var foreground = true

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            @Suppress("DEPRECATION")
            if (value.status != CarPropertyValue.STATUS_AVAILABLE) return
            when (value.propertyId) {
                VehiclePropertyIds.GEAR_SELECTION -> {
                    val gear = (value.value as? Int)?.toGearPosition()
                    _signals.update { it.copy(gear = gear) }
                }
                VehiclePropertyIds.PERF_VEHICLE_SPEED -> {
                    val speed = (value.value as? Float)?.let { kotlin.math.abs(it) }
                    _signals.update { it.copy(speedMps = speed) }
                }
                VehiclePropertyIds.PARKING_BRAKE_ON -> {
                    _signals.update { it.copy(parkingBrakeOn = value.value as? Boolean) }
                }
            }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            AppLog.w(TAG, "property error id=0x${Integer.toHexString(propertyId)} area=$areaId")
        }
    }

    private val uxListener = CarUxRestrictionsManager.OnUxRestrictionsChangedListener { restrictions ->
        val state = restrictions.toUxState()
        AppLog.i(TAG, "UX restrictions: requiresDO=${state.requiresDO} active=${state.activeRestrictions}")
        _ux.value = state
    }

    override fun start() {
        if (car != null) return
        _status.value = SdkStatus.Connecting
        try {
            car = Car.createCar(appContext, null, Car.CAR_WAIT_TIMEOUT_DO_NOT_WAIT) { connectedCar, ready ->
                if (ready) onCarReady(connectedCar) else onCarLost()
            }
        } catch (t: Throwable) {
            // 일반(비-AAOS) 기기: android.car 클래스 자체가 없을 수 있다 → 상태로 바꾸고 폴백
            AppLog.w(TAG, "Car.createCar failed", t)
            car = null
            _status.value = SdkStatus.Unavailable("Car API 없음")
        }
    }

    private fun onCarReady(connectedCar: Car) {
        try {
            val pm = connectedCar.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
            propertyManager = pm
            val gearOk = register(pm, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            register(pm, VehiclePropertyIds.PARKING_BRAKE_ON, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            registerSpeedIfPermitted()

            val um = connectedCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as CarUxRestrictionsManager
            uxManager = um
            um.registerListener(uxListener)
            um.currentCarUxRestrictions?.let { _ux.value = it.toUxState() }

            _signals.update { it.copy(connected = true) }
            _status.value = if (gearOk) SdkStatus.Available else SdkStatus.Unavailable("GEAR_SELECTION 구독 실패")
            AppLog.i(TAG, "Car connected (gear=$gearOk, speed=$speedRegistered)")
        } catch (t: Throwable) {
            AppLog.e(TAG, "Car manager setup failed", t)
            _status.value = SdkStatus.Unavailable("CarPropertyManager 실패")
        }
    }

    private fun onCarLost() {
        AppLog.w(TAG, "Car service disconnected")
        propertyManager = null
        uxManager = null
        speedRegistered = false
        _signals.value = CarSignals(connected = false)
        _ux.value = null
        _status.value = SdkStatus.Connecting
    }

    private fun register(pm: CarPropertyManager, propertyId: Int, rate: Float): Boolean = try {
        @Suppress("DEPRECATION")
        pm.registerCallback(propertyCallback, propertyId, rate).also {
            if (!it) AppLog.w(TAG, "registerCallback false for 0x${Integer.toHexString(propertyId)}")
        }
    } catch (e: SecurityException) {
        AppLog.w(TAG, "permission denied for 0x${Integer.toHexString(propertyId)}", e)
        false
    } catch (e: IllegalArgumentException) {
        AppLog.w(TAG, "property not supported 0x${Integer.toHexString(propertyId)}", e)
        false
    }

    private fun hasSpeedPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Car.PERMISSION_SPEED) == PackageManager.PERMISSION_GRANTED

    /** 런타임 퍼미션 결과가 바뀌었을 때 MainActivity 가 호출한다. */
    fun onPermissionsChanged() {
        registerSpeedIfPermitted()
    }

    /** 백그라운드에서는 1 Hz 속도 구독을 끊는다 (DEV_GUIDE §5.4 불필요 센서 수집 중단). 기어·규제(ONCHANGE)는 유지. */
    fun setForeground(isForeground: Boolean) {
        foreground = isForeground
        if (isForeground) {
            registerSpeedIfPermitted()
        } else {
            val pm = propertyManager ?: return
            if (speedRegistered) {
                runCatching {
                    @Suppress("DEPRECATION")
                    pm.unregisterCallback(propertyCallback, VehiclePropertyIds.PERF_VEHICLE_SPEED)
                }
                speedRegistered = false
            }
        }
    }

    private fun registerSpeedIfPermitted() {
        val pm = propertyManager ?: return
        if (speedRegistered || !foreground) return
        if (!hasSpeedPermission()) {
            AppLog.i(TAG, "CAR_SPEED not granted → gear-only mode (STOPPED 판정 불가)")
            _signals.update { it.copy(speedMps = null) }
            return
        }
        // 1 Hz 이하 (지시서 §5.1). 폴링 금지 — 콜백만.
        speedRegistered = register(pm, VehiclePropertyIds.PERF_VEHICLE_SPEED, 1f)
    }

    override fun stop() {
        runCatching {
            @Suppress("DEPRECATION")
            propertyManager?.unregisterCallback(propertyCallback)
        }
        runCatching { uxManager?.unregisterListener() }
        runCatching { car?.disconnect() }
        car = null
        propertyManager = null
        uxManager = null
        speedRegistered = false
        _status.value = SdkStatus.Idle
    }

    private fun CarUxRestrictions.toUxState() =
        UxState(requiresDO = isRequiresDistractionOptimization, activeRestrictions = activeRestrictions)

    private fun Int.toGearPosition(): GearPosition = when (this) {
        VehicleGear.GEAR_PARK -> GearPosition.PARK
        VehicleGear.GEAR_REVERSE -> GearPosition.REVERSE
        VehicleGear.GEAR_NEUTRAL -> GearPosition.NEUTRAL
        VehicleGear.GEAR_UNKNOWN -> GearPosition.OTHER
        else -> if (this >= VehicleGear.GEAR_DRIVE) GearPosition.DRIVE else GearPosition.OTHER
    }

    companion object {
        private const val TAG = "AaosCar"

        /** MainActivity 가 런타임으로 요청할 퍼미션 (= Car.PERMISSION_SPEED) */
        const val SPEED_PERMISSION: String = "android.car.permission.CAR_SPEED"
    }
}
