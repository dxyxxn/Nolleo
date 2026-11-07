package com.nolleo.app.core.car

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Pleos Vehicle SDK — OEM 특화 보조 신호 (지시서 §5.1).
 *  - Brake.BrakePedalStatus (pleos.car.permission.CAR_DRIVE): STOPPED 판정 보조 (속도 0 + 브레이크 on)
 *  - Seat.SeatOccupancy     (pleos.car.permission.CAR_SEATS): 동승자 수 자동 제안
 *
 * 생명주기는 샘플 VehicleClient 와 같다: Vehicle(appContext) → initialize() → checkXxxCapabilities →
 * register/get → unregister(같은 인스턴스) → release(). 미지원이면 조용히 수동 선택만.
 */
class VehicleSdkClient(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private var vehicle: Vehicle? = null
    private var isInitialized = false

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    val status: StateFlow<SdkStatus> = _status.asStateFlow()

    private val _brakePedalPressed = MutableStateFlow<Boolean?>(null)
    val brakePedalPressed: StateFlow<Boolean?> = _brakePedalPressed.asStateFlow()

    private val _brakeStatus = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    val brakeStatus: StateFlow<SdkStatus> = _brakeStatus.asStateFlow()

    /** 운전석을 제외한 좌석별 착좌 여부. 비어 있으면 미지원/미확인. */
    private val _seatOccupancy = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val seatOccupancy: StateFlow<Map<String, Boolean>> = _seatOccupancy.asStateFlow()

    private val _seatStatus = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    val seatStatus: StateFlow<SdkStatus> = _seatStatus.asStateFlow()

    private var brakeRegistered = false
    private val seatListeners = mutableMapOf<SeatArea, SeatOccupancyListener>()

    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(status: Boolean?) {
            _brakePedalPressed.value = status
        }

        override fun onFailed(e: Exception) {
            AppLog.w(TAG, "brake listener failed: ${e.describe()}")
        }
    }

    fun start() {
        if (isInitialized) return
        _status.value = SdkStatus.Connecting
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            isInitialized = true
            _status.value = SdkStatus.Available
            AppLog.i(TAG, "Vehicle SDK initialized")
            inspectBrake(attempt = 0)
            inspectSeats(attempt = 0)
        } catch (t: Throwable) {
            AppLog.w(TAG, "Vehicle SDK init failed", t)
            vehicle = null
            _status.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    private fun inspectBrake(attempt: Int) {
        val brake = vehicle?.getBrake() ?: return
        _brakeStatus.value = SdkStatus.Connecting
        runSdk("checkBrakePedalStatusCapabilities") {
            brake.checkBrakePedalStatusCapabilities(
                onComplete = { config ->
                    when (config.accessType) {
                        AccessType.READ, AccessType.READ_WRITE -> mainHandler.post { registerBrake() }
                        else -> {
                            AppLog.i(TAG, "BrakePedalStatus not readable (${config.accessType})")
                            _brakeStatus.value = SdkStatus.Unavailable("미지원")
                        }
                    }
                },
                onFailed = { e ->
                    if (e.isRetryable() && attempt < MAX_RETRY) {
                        mainHandler.postDelayed({ inspectBrake(attempt + 1) }, RETRY_DELAY_MS)
                    } else {
                        AppLog.w(TAG, "brake capability failed: ${e.describe()}")
                        _brakeStatus.value = SdkStatus.Unavailable(e.describe())
                    }
                },
            )
        }
    }

    private fun registerBrake() {
        val brake = vehicle?.getBrake() ?: return
        if (brakeRegistered) return
        runSdk("registerBrakePedalStatus") {
            brake.registerBrakePedalStatus(brakeListener)
            brakeRegistered = true
            _brakeStatus.value = SdkStatus.Available
            brake.getCurrentBrakePedalStatus(
                onComplete = { pressed -> _brakePedalPressed.value = pressed },
                onFailed = { e -> AppLog.w(TAG, "getCurrentBrakePedalStatus failed: ${e.describe()}") },
            )
        }
    }

    private fun inspectSeats(attempt: Int) {
        val seat = vehicle?.getSeat() ?: return
        _seatStatus.value = SdkStatus.Connecting
        runSdk("checkSeatOccupancyCapabilities") {
            seat.checkSeatOccupancyCapabilities(
                onComplete = { config, areas ->
                    when (config.accessType) {
                        AccessType.READ, AccessType.READ_WRITE -> mainHandler.post { registerSeats(areas) }
                        else -> {
                            AppLog.i(TAG, "SeatOccupancy not readable (${config.accessType})")
                            _seatStatus.value = SdkStatus.Unavailable("미지원")
                        }
                    }
                },
                onFailed = { e ->
                    if (e.isRetryable() && attempt < MAX_RETRY) {
                        mainHandler.postDelayed({ inspectSeats(attempt + 1) }, RETRY_DELAY_MS)
                    } else {
                        AppLog.w(TAG, "seat capability failed: ${e.describe()}")
                        _seatStatus.value = SdkStatus.Unavailable(e.describe())
                    }
                },
            )
        }
    }

    private fun registerSeats(areas: List<SeatArea>) {
        val seat = vehicle?.getSeat() ?: return
        // 국내(좌핸들) 기준 운전석 = ROW_1_LEFT. 차량 모델명 분기 없이 영역 값만 본다.
        val passengerAreas = areas.filterNot { area -> area.values.contains(SeatValue.ROW_1_LEFT) }
        AppLog.i(TAG, "SeatOccupancy areas=${areas.map { it.key() }} passengers=${passengerAreas.map { it.key() }}")
        if (passengerAreas.isEmpty()) {
            _seatStatus.value = SdkStatus.Unavailable("동승석 영역 없음")
            return
        }
        passengerAreas.forEach { area ->
            if (seatListeners.containsKey(area)) return@forEach
            val key = area.key()
            val listener = object : SeatOccupancyListener {
                override fun onSeatOccupancyUpdated(occupancy: Boolean?) {
                    updateSeat(key, occupancy)
                }

                override fun onFailed(e: Exception) {
                    AppLog.w(TAG, "seat listener $key failed: ${e.describe()}")
                }
            }
            runSdk("registerSeatOccupancy $key") {
                seat.registerSeatOccupancy(area, listener)
                seatListeners[area] = listener
                seat.getCurrentSeatOccupancy(
                    area = area,
                    onComplete = { occupied -> updateSeat(key, occupied) },
                    onFailed = { e -> AppLog.w(TAG, "getCurrentSeatOccupancy $key failed: ${e.describe()}") },
                )
            }
        }
        _seatStatus.value = SdkStatus.Available
    }

    private fun updateSeat(key: String, occupied: Boolean?) {
        _seatOccupancy.update { current ->
            if (occupied == null) current - key else current + (key to occupied)
        }
    }

    /** 운전석 제외 착좌 수. 착좌 센서를 못 읽으면 null. */
    fun occupiedPassengerCount(): Int? {
        val map = _seatOccupancy.value
        if (map.isEmpty()) return null
        return map.values.count { it }
    }

    fun release() {
        if (!isInitialized) return
        val v = vehicle
        runSdk("release") {
            if (brakeRegistered) v?.getBrake()?.unregisterBrakePedalStatus(brakeListener)
            seatListeners.forEach { (area, listener) -> v?.getSeat()?.unregisterSeatOccupancy(area, listener) }
            v?.release()
        }
        brakeRegistered = false
        seatListeners.clear()
        vehicle = null
        isInitialized = false
        _status.value = SdkStatus.Idle
    }

    private inline fun runSdk(label: String, action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
            AppLog.w(TAG, "$label threw", t)
        }
    }

    /**
     * TODO(verify): API 레퍼런스는 `VehicleException.errorCode` 를 안내하지만 배포된 Vehicle 2.3.2 AAR 에서는
     * 예외 클래스가 난독화(`callback.exception.a`)되어 있고 ErrorCode 는 runtime 스코프(vehicle-data)라 컴파일 시
     * 참조할 수 없다. 그래서 메시지로 "미지원/퍼미션" 만 걸러 내고, 나머지(서비스 연결 지연 등)는 제한 횟수 안에서 재시도한다.
     */
    private fun Exception.isRetryable(): Boolean {
        val text = (message ?: "") + " " + toString()
        val permanent = text.contains("NOT_SUPPORTED", ignoreCase = true) ||
            text.contains("SECURITY", ignoreCase = true) || text.contains("PERMISSION", ignoreCase = true)
        return !permanent
    }

    private fun Exception.describe(): String = message?.takeIf { it.isNotBlank() } ?: javaClass.simpleName

    private fun SeatArea.key(): String = values.joinToString("+") { it.name }

    companion object {
        private const val TAG = "VehicleSdk"
        private const val MAX_RETRY = 5
        private const val RETRY_DELAY_MS = 1_000L
    }
}
