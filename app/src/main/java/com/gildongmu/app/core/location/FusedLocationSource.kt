package com.gildongmu.app.core.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.core.log.AppLog
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

    private var fused: FusedLocation? = null
    private var initialized = false
    private var subscribed = false

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            location ?: return // null 을 유효 위치로 취급하지 않는다
            val point = GeoPoint(location.latitude, location.longitude)
            if (_location.value == null) AppLog.sensitive(TAG, "first fix", "${location.latitude},${location.longitude}")
            _location.value = point
        }

        override fun onError(exception: Exception) {
            AppLog.w(TAG, "FusedLocation error", exception)
        }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun start() {
        if (subscribed) return
        if (!hasPermission()) {
            _status.value = SdkStatus.Unavailable("퍼미션 거부")
            return
        }
        try {
            val f = fused ?: FusedLocation(appContext).also { fused = it }
            if (!f.isSupported()) {
                AppLog.i(TAG, "FusedLocation not supported → 위치 기능 비활성")
                _status.value = SdkStatus.Unavailable("미지원")
                return
            }
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
    }
}
