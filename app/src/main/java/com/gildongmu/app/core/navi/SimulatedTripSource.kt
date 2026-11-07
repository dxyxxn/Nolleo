package com.gildongmu.app.core.navi

import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.domain.model.GeoMath
import com.gildongmu.app.domain.model.GeoPoint
import com.gildongmu.app.domain.model.NaviStatus
import com.gildongmu.app.domain.model.TripSourceKind
import com.gildongmu.app.domain.model.TurnHint
import com.gildongmu.app.domain.rest.RouteRestStop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 시뮬레이션 여정이 지나는 현재 지점. 지역·이름은 마지막으로 지난 경유 지점 기준이고,
 * [point] 는 다음 경유 지점까지 시간 비율로 옮겨 간 위치, [headingDeg] 는 그 구간의 진행 방향이다(졸음쉼터 고르기).
 */
data class SimPosition(val point: GeoPoint, val regionCode: String, val label: String, val headingDeg: Double? = null)

/**
 * 내비 미탑재·이벤트 없음 환경에서 같은 UI 흐름을 끝까지 시연하기 위한 여정 (지시서 §10).
 * 프리셋 타임라인을 [timeScale] 배로 압축한다 (기본 실제 1분 = 여정 30분). remainTime 은 5초마다 갱신.
 * 중간에 "분기점 안내 300 m" TBT 를 내고 실제 시간 8초 동안 유지한다 (P1 자동 일시정지 시연용).
 */
class SimulatedTripSource(private val scope: CoroutineScope) : TripSource {

    override val kind = TripSourceKind.SIMULATED

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Available)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    private val _snapshot = MutableStateFlow<TripSnapshot?>(null)
    override val snapshot: StateFlow<TripSnapshot?> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<TripEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<TripEvent> = _events.asSharedFlow()

    private val _position = MutableStateFlow<SimPosition?>(null)
    val position: StateFlow<SimPosition?> = _position.asStateFlow()

    /** 실제 1초 = 여정 [timeScale] 초 */
    val timeScale = MutableStateFlow(30f)

    /** 주차(휴게소 등) 중에는 여정 시간이 흐르지 않는다 — 실제 내비의 남은 시간도 정차 중엔 그대로다 */
    val moving = MutableStateFlow(true)

    private var preset: DestinationPreset? = null
    private var elapsedTripSec: Double = 0.0
    private var tickJob: Job? = null
    private var tbtJob: Job? = null
    private var nextTbtIndex = 0

    override fun start() = Unit

    override fun requestRoute(preset: DestinationPreset) {
        stopTimers()
        this.preset = preset
        elapsedTripSec = 0.0
        nextTbtIndex = 0
        timeScale.value = preset.defaultTimeScale
        AppLog.i(TAG, "simulated route ${preset.title} (${preset.estimatedMinutes}분, x${preset.defaultTimeScale})")
        publish()
        _events.tryEmit(TripEvent.RouteStarted(preset.estimatedSec))
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (moving.value) elapsedTripSec += TICK_MS / 1000.0 * timeScale.value
                publish()
                maybeFireTbt()
                val remain = remainSec()
                if (remain <= 0) {
                    AppLog.i(TAG, "simulated arrival")
                    _events.tryEmit(TripEvent.Arrived(preset.poiName))
                    break
                }
            }
        }
    }

    /** 디버그 패널: 남은 시간을 바로 지정 (예: "5분 남김") */
    fun jumpToRemain(remainSec: Int) {
        val p = preset ?: return
        elapsedTripSec = (p.estimatedSec - remainSec).coerceIn(0, p.estimatedSec).toDouble()
        nextTbtIndex = p.tbtAtMin.count { it * 60 <= elapsedTripSec }
        publish()
    }

    /** 디버그 패널: TBT 이벤트 즉시 발생 */
    fun triggerTbt() {
        fireTbt("분기점 안내 (수동)")
    }

    private fun maybeFireTbt() {
        val p = preset ?: return
        val due = p.tbtAtMin.getOrNull(nextTbtIndex) ?: return
        if (elapsedTripSec >= due * 60) {
            nextTbtIndex++
            fireTbt("분기점 안내")
        }
    }

    private fun fireTbt(label: String) {
        tbtJob?.cancel()
        tbtJob = scope.launch {
            AppLog.i(TAG, "TBT: $label 300m")
            setTurn(TurnHint("TURN_KEEP_R", 300, "$label · 300m 앞 오른쪽 방향", needsAttention = true))
            delay(TBT_HOLD_MS / 2)
            setTurn(TurnHint("TURN_KEEP_R", 120, "$label · 120m 앞 오른쪽 방향", needsAttention = true))
            delay(TBT_HOLD_MS / 2)
            setTurn(null)
            AppLog.i(TAG, "TBT passed")
        }
    }

    private fun setTurn(hint: TurnHint?) {
        _snapshot.update { it?.copy(nextTurn = hint) }
    }

    private fun remainSec(): Int {
        val p = preset ?: return 0
        return (p.estimatedSec - elapsedTripSec).toInt().coerceAtLeast(0)
    }

    private fun publish() {
        val p = preset ?: return
        val remain = remainSec()
        val totalDistance = haversineRouteMeters(p)
        val ratio = if (p.estimatedSec == 0) 0.0 else remain.toDouble() / p.estimatedSec
        _snapshot.update {
            TripSnapshot(
                destinationName = p.poiName,
                remainTimeSec = remain,
                remainDistanceM = (totalDistance * ratio).toInt(),
                navStatus = NaviStatus.ROUTE_DRIVE,
                nextTurn = it?.nextTurn,
                addressHint = null,
                timeScale = timeScale.value,
                destination = GeoPoint(p.latitude, p.longitude),
                restStopsAhead = restStopsAhead(p),
            )
        }
        val index = p.timeline.indexOfLast { it.atMin * 60 <= elapsedTripSec }.coerceAtLeast(0)
        val from = p.timeline[index]
        val to = p.timeline.getOrNull(index + 1)
        val fromPoint = GeoPoint(from.latitude, from.longitude)
        val point = if (to == null) fromPoint else {
            val span = ((to.atMin - from.atMin) * 60).coerceAtLeast(1)
            GeoMath.lerp(fromPoint, GeoPoint(to.latitude, to.longitude), (elapsedTripSec - from.atMin * 60) / span)
        }
        val heading = to?.let { GeoMath.bearingDeg(fromPoint, GeoPoint(it.latitude, it.longitude)) } ?: _position.value?.headingDeg
        _position.value = SimPosition(point, from.regionCode, from.label, heading)
    }

    /** 경로 위 휴게소·졸음쉼터 중 아직 지나지 않은 곳 (지금까지 간 거리 = 간 시간 비율 × 내비 경로 거리) */
    private fun restStopsAhead(p: DestinationPreset): List<RouteRestStop>? {
        val stops = p.restStops ?: return null
        val travelledM = if (p.estimatedSec == 0) 0.0 else elapsedTripSec / p.estimatedSec * p.routeKm * 1000
        return stops.mapNotNull { s ->
            val d = (s.km * 1000 - travelledM).toInt()
            if (d > 0) RouteRestStop(s.name, s.kind, d) else null
        }
    }

    private fun haversineRouteMeters(p: DestinationPreset): Double {
        var sum = 0.0
        p.timeline.zipWithNext { a, b -> sum += haversine(a.latitude, a.longitude, b.latitude, b.longitude) }
        return sum * 1.25 // 도로 굴곡 보정
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
            kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
            kotlin.math.sin(dLon / 2).let { it * it }
        return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
    }

    fun clear() {
        stopTimers()
        preset = null
        _snapshot.value = null
        _position.value = null
    }

    private fun stopTimers() {
        tickJob?.cancel()
        tbtJob?.cancel()
        tickJob = null
        tbtJob = null
    }

    override fun stop() = clear()

    companion object {
        private const val TAG = "SimTrip"
        private const val TICK_MS = 5_000L
        private const val TBT_HOLD_MS = 8_000L
    }
}
