package com.gildongmu.app.core.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.domain.model.GeoMath
import com.gildongmu.app.domain.model.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pleos FusedLocation SDK — 지역 퀴즈 선택 (지시서 §5.3, DEV_GUIDE §8.5, 샘플 FusedLocationClient 패턴).
 * isSupported() 가 true 일 때만 initialize() → registerFusedLocationCallback. 종료 시 unregister → release
 * (둘은 서로 대체 불가). 퍼미션 pleos.car.permission.FUSED_LOCATION 은 dangerous → 런타임 요청, 거부되면 위치 기능만 끈다.
 */
class FusedLocationSource(context: Context) {

    private val appContext = context.applicationContext

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    val status: StateFlow<SdkStatus> = _status.asStateFlow()

    private val _location = MutableStateFlow<GeoPoint?>(null)
    val location: StateFlow<GeoPoint?> = _location.asStateFlow()

    /** 현재 위치 + 진행 방향 단서 (졸음쉼터 고르기). */
    data class Motion(val point: GeoPoint, val sdkHeading: Float?, val movedHeading: Double?)

    private val _motion = MutableStateFlow<Motion?>(null)
    val motion: StateFlow<Motion?> = _motion.asStateFlow()

    private var fused: FusedLocation? = null
    private var initialized = false
    private var subscribed = false
    private var anchor: GeoPoint? = null
    private var movedHeading: Double? = null

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            location ?: return // null 을 유효 위치로 취급하지 않는다
            val point = GeoPoint(location.latitude, location.longitude)
            if (_location.value == null) AppLog.sensitive(TAG, "first fix", "${location.latitude},${location.longitude}")
            _location.value = point
            // 30 m 이상 움직였을 때의 방향을 진행 방향으로 쓴다. 5 km 넘게 한 번에 바뀌면(에뮬레이터 geo fix 등) 방향은 버린다
            val from = anchor
            val moved = from?.let { GeoMath.distanceM(it, point) }
            when {
                from == null || moved == null -> anchor = point
                moved > TELEPORT_M -> { anchor = point; movedHeading = null }
                moved >= MOVE_MIN_M -> { movedHeading = GeoMath.bearingDeg(from, point); anchor = point }
            }
            _motion.value = Motion(point, location.heading, movedHeading)
        }

        override fun onError(exception: Exception) {
            AppLog.w(TAG, "FusedLocation error", exception)
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, PERMISSION) == PackageManager.PERMISSION_GRANTED

    /** 위치 서비스 지원 여부. 샘플과 같이 퍼미션 요청·initialize() 전에 확인한다 (미지원 차량에서 권한 대화상자를 띄우지 않기 위해) */
    fun isSupported(): Boolean = try {
        (fused ?: FusedLocation(appContext).also { fused = it }).isSupported()
    } catch (t: Throwable) {
        AppLog.w(TAG, "FusedLocation isSupported failed", t)
        false
    }

    fun start() {
        if (subscribed) return
        if (!isSupported()) {
            AppLog.i(TAG, "FusedLocation not supported → 위치 기능 비활성")
            _status.value = SdkStatus.Unavailable("미지원")
            return
        }
        if (!hasPermission()) {
            _status.value = SdkStatus.Unavailable("퍼미션 거부")
            return
        }
        try {
            val f = fused ?: FusedLocation(appContext).also { fused = it }
            if (!initialized) {
                f.initialize()
                initialized = true
            }
            f.registerFusedLocationCallback(listener)
            subscribed = true
            f.getLastKnownLocation(listener)
            _status.value = SdkStatus.Available
            AppLog.i(TAG, "FusedLocation subscribed")
        } catch (t: Throwable) {
            AppLog.w(TAG, "FusedLocation start failed", t)
            _status.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    /**
     * 졸음쉼터 도착 감지 (`addProximityAlert`, ENTER). 결과는 콜백으로 온다(SDK 스레드).
     * 퍼미션 FUSED_LOCATION(이미 선언·런타임 요청). 초기화 전이면 false.
     */
    fun addProximityAlert(
        center: GeoPoint,
        radiusMeters: Float,
        registered: (String) -> Unit,
        entered: (String) -> Unit,
        failed: (Exception) -> Unit,
    ): Boolean {
        val f = fused ?: return false
        if (!initialized) return false
        return runCatching {
            f.addProximityAlert(
                ProximityAlertRequest(center.latitude, center.longitude, radiusMeters, setOf(ProximityTransition.ENTER)),
                object : ProximityAlertListener {
                    override fun onRegistered(alertId: String) = registered(alertId)

                    override fun onTransition(event: ProximityAlertEvent) {
                        if (event.transition == ProximityTransition.ENTER) entered(event.alertId)
                    }

                    override fun onError(exception: Exception) = failed(exception)
                },
            )
        }.onFailure { AppLog.w(TAG, "addProximityAlert failed", it) }.isSuccess
    }

    fun removeProximityAlert(alertId: String) {
        runCatching { fused?.removeProximityAlert(alertId) }.onFailure { AppLog.w(TAG, "removeProximityAlert failed", it) }
    }

    /** 백그라운드에서는 위치 구독을 끊는다 (DEV_GUIDE §5.4) */
    fun pause() {
        val f = fused ?: return
        if (!subscribed) return
        runCatching { f.unregisterFusedLocationCallback(listener) }
        subscribed = false
    }

    fun release() {
        pause()
        if (initialized) runCatching { fused?.release() }
        initialized = false
        fused = null
        _status.value = SdkStatus.Idle
    }

    companion object {
        private const val TAG = "FusedLocation"
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        private const val MOVE_MIN_M = 30.0
        private const val TELEPORT_M = 5_000.0
    }
}
