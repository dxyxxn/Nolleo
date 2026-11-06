package com.gildongmu.app.core.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.domain.model.NaviStatus
import com.gildongmu.app.domain.model.TripSourceKind
import com.gildongmu.app.domain.model.TurnHint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * NaviHelper SDK — 남은 시간·경로·안내 시점 (지시서 §5.2, DEV_GUIDE §8.2).
 * 패턴은 샘플 NaviHelperViewModel 과 같다: NaviHelper(appContext) → initialize(onConnected, onFailed)
 * → addListener → API 호출(결과는 전부 콜백) → removeListener(같은 인스턴스) → release().
 *
 * 조회 API(get*Info)는 값을 반환하지 않고 대응 콜백(onXxxInfo)으로 결과가 온다.
 * requestRoute 성공 호출 ≠ 탐색 완료 → onRouteStarted / onRoutingCompleted / onError 로 확인.
 */
class NaviHelperTripSource(context: Context) : TripSource {

    private val appContext = context.applicationContext

    override val kind = TripSourceKind.REAL_NAVI

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Idle)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    private val _snapshot = MutableStateFlow<TripSnapshot?>(null)
    override val snapshot: StateFlow<TripSnapshot?> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<TripEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<TripEvent> = _events.asSharedFlow()

    /** 앱이 경로를 요청하기 전부터 내비에 설정돼 있던 목적지 (홈의 "내비 목적지로 출발" 카드) */
    private val _activeNaviDestination = MutableStateFlow<TripSnapshot?>(null)
    val activeNaviDestination: StateFlow<TripSnapshot?> = _activeNaviDestination.asStateFlow()

    /** 마지막으로 받은 이벤트 시각(ms, elapsedRealtime 기준이 아니라 단순 카운터 용도) — 디버그 표시용 */
    private val _lastEventLabel = MutableStateFlow<String?>(null)
    val lastEventLabel: StateFlow<String?> = _lastEventLabel.asStateFlow()

    private var helper: NaviHelper? = null
    private var navStatus: NaviStatus? = null

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            val distance = TripUnits.distanceM(routeStartInfo.distance)
            val sec = TripUnits.durationSec(routeStartInfo.duration, distance)
            AppLog.i(TAG, "onRouteStarted raw duration=${routeStartInfo.duration} distance=${routeStartInfo.distance} → ${sec}s")
            mark("onRouteStarted")
            update(name = routeStartInfo.name, remainSec = sec, distanceM = distance)
            _events.tryEmit(TripEvent.RouteStarted(sec))
        }

        override fun onRoutingCompleted() {
            AppLog.i(TAG, "onRoutingCompleted")
            mark("onRoutingCompleted")
            runCall("getDestinationInfo") { helper?.getDestinationInfo() }
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            val distance = TripUnits.distanceM(destinationInfo.remainDistance)
            val sec = TripUnits.remainTimeSec(destinationInfo.remainTime, distance)
            AppLog.i(TAG, "onDestinationInfo raw remainTime=${destinationInfo.remainTime} remainDistance=${destinationInfo.remainDistance} → ${sec}s")
            mark("onDestinationInfo")
            if (destinationInfo.remainTime <= 0 && destinationInfo.remainDistance <= 0) {
                // 경로가 없을 때도 0/0 으로 응답한다 (에뮬레이터 관측) → 유효한 남은 시간으로 쓰지 않는다
                _activeNaviDestination.value = null
                return
            }
            update(name = destinationInfo.name, remainSec = sec, distanceM = distance)
            if (navStatus == NaviStatus.ROUTE_DRIVE || _activeNaviDestination.value == null) {
                _activeNaviDestination.value = _snapshot.value
            }
        }

        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            val dest = drivingInfo.destination
            val distance = TripUnits.distanceM(dest.distance)
            val sec = TripUnits.durationSec(dest.duration, distance)
            mark("onDrivingInfo")
            // onDestinationInfo 가 주 신호. 아직 값이 없을 때만 보조로 쓴다.
            if (_snapshot.value?.remainTimeSec == null) update(name = dest.name, remainSec = sec, distanceM = distance)
        }

        override fun onNavigationStatus(status: NavigationStatus) {
            AppLog.i(TAG, "onNavigationStatus $status")
            navStatus = status.toDomain()
            mark("onNavigationStatus")
            _snapshot.update { it?.copy(navStatus = navStatus) }
            if (navStatus != NaviStatus.ROUTE_DRIVE) _activeNaviDestination.value = null
        }

        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            val first = tbtInfoList.firstOrNull()
            val hint = first?.let {
                TurnHint(
                    typeName = it.type.name,
                    distanceM = it.distance.toInt(),
                    description = it.description,
                    needsAttention = it.type.needsAttention(),
                )
            }
            _snapshot.update { it?.copy(nextTurn = hint) }
        }

        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            AppLog.sensitive(TAG, "onCurrentLocationInfo.address", currentLocationInfo.address)
            _snapshot.update { (it ?: emptySnapshot()).copy(addressHint = currentLocationInfo.address) }
            _addressHint.value = currentLocationInfo.address
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            AppLog.i(TAG, "onDestinationArrived")
            mark("onDestinationArrived")
            _events.tryEmit(TripEvent.Arrived(destinationArrivedInfo.name))
        }

        override fun onRouteCancelled() {
            AppLog.i(TAG, "onRouteCancelled")
            mark("onRouteCancelled")
            _events.tryEmit(TripEvent.Cancelled)
        }

        override fun onError(errorCode: NaviErrorCode) {
            AppLog.w(TAG, "onError $errorCode")
            mark("onError $errorCode")
            _events.tryEmit(TripEvent.Error(errorCode.name))
        }

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            AppLog.i(TAG, "onRouteStateInfo isRouting=${routeStateInfo.isRouting} mode=${routeStateInfo.naviMode}")
            // TODO(phase2): routeStateInfo.trafficStatus (TrafficCongestionType.HEAVY) → 정체 구간 자동 일시정지
        }

        override fun onCautionPointInfo(cautionPointList: List<CautionPoint>) {
            // TODO(phase2): 주의 구간(급커브·사고다발) 접근 시 자동 일시정지
        }

        override fun onCameraAlert(cameraAlertList: List<CameraAlert>) {
            // TODO(phase2): 단속 카메라 안내 직전 자동 일시정지
        }
    }

    private val _addressHint = MutableStateFlow<String?>(null)
    val addressHint: StateFlow<String?> = _addressHint.asStateFlow()

    override fun start() {
        if (helper != null) return
        _status.value = SdkStatus.Connecting
        try {
            val h = NaviHelper(appContext)
            helper = h
            h.initialize(
                onConnected = {
                    AppLog.i(TAG, "NaviHelper connected")
                    runCall("addListener") { h.addListener(listener) }
                    _status.value = SdkStatus.Available
                    runCall("getRouteStateInfo") { h.getRouteStateInfo() }
                    runCall("getDestinationInfo") { h.getDestinationInfo() }
                    runCall("getCurrentLocationInfo") { h.getCurrentLocationInfo() }
                },
                onFailed = { t ->
                    AppLog.w(TAG, "NaviHelper connection failed", t)
                    _status.value = SdkStatus.Unavailable(t.javaClass.simpleName)
                },
            )
        } catch (t: Throwable) {
            AppLog.w(TAG, "NaviHelper init threw", t)
            helper = null
            _status.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    override fun requestRoute(preset: DestinationPreset) {
        val h = helper
        if (h == null || _status.value != SdkStatus.Available) {
            _events.tryEmit(TripEvent.Error("NaviHelper 미연결"))
            return
        }
        _snapshot.value = TripSnapshot(
            destinationName = preset.poiName,
            remainTimeSec = null,
            remainDistanceM = null,
            navStatus = navStatus,
            nextTurn = null,
            addressHint = _addressHint.value,
        )
        AppLog.i(TAG, "requestRoute ${preset.poiName} (poiId=${preset.poiId.ifEmpty { "<none>" }})")
        runCall("requestRoute") {
            // Kotlin named argument 필수 — Java 예제와 data class 선언의 인자 순서가 다르다 (DEV_GUIDE §8.2)
            h.requestRoute(
                RouteInfo(
                    latitude = preset.latitude,
                    longitude = preset.longitude,
                    poiName = preset.poiName,
                    poiId = preset.poiId,
                    poiSubId = preset.poiSubId,
                    address = preset.address,
                ),
            )
        }
    }

    /** 내비에 이미 설정된 경로를 그대로 여정으로 쓴다 (경로 요청 없이 정보만 갱신) */
    fun adoptActiveRoute() {
        _activeNaviDestination.value?.let { _snapshot.value = it }
        runCall("getDestinationInfo") { helper?.getDestinationInfo() }
        runCall("getTBTInfo") { helper?.getTBTInfo() }
    }

    fun refresh() {
        runCall("getDestinationInfo") { helper?.getDestinationInfo() }
    }

    override fun stop() {
        val h = helper ?: return
        runCall("removeListener") { h.removeListener(listener) }
        runCall("release") { h.release() }
        helper = null
        _status.value = SdkStatus.Idle
    }

    private fun update(name: String?, remainSec: Int?, distanceM: Int?) {
        _snapshot.update { current ->
            (current ?: emptySnapshot()).copy(
                destinationName = name?.takeIf { it.isNotBlank() } ?: current?.destinationName.orEmpty(),
                remainTimeSec = remainSec ?: current?.remainTimeSec,
                remainDistanceM = distanceM ?: current?.remainDistanceM,
                navStatus = navStatus,
            )
        }
    }

    private fun emptySnapshot() = TripSnapshot(
        destinationName = "",
        remainTimeSec = null,
        remainDistanceM = null,
        navStatus = navStatus,
        nextTurn = null,
    )

    private fun mark(label: String) {
        _lastEventLabel.value = label
    }

    private inline fun runCall(label: String, action: () -> Unit) {
        try {
            action()
        } catch (t: Throwable) {
            AppLog.w(TAG, "$label failed", t)
            if (t is SecurityException) _status.value = SdkStatus.Unavailable("퍼미션")
        }
    }

    private fun NavigationStatus.toDomain(): NaviStatus = when (this) {
        NavigationStatus.INACTIVE -> NaviStatus.INACTIVE
        NavigationStatus.INITIALIZING -> NaviStatus.INITIALIZING
        NavigationStatus.FREE_DRIVE -> NaviStatus.FREE_DRIVE
        NavigationStatus.ROUTE_DRIVE -> NaviStatus.ROUTE_DRIVE
    }

    companion object {
        private const val TAG = "NaviHelper"

        /** 분기/진출입/회전/교차로 — 운전에 집중해야 하는 안내 (P1 자동 일시정지 대상) */
        fun TurnType.needsAttention(): Boolean = when (this) {
            TurnType.TURN_UNKNOWN, TurnType.TURN_STRAIGHT, TurnType.TURN_TUNNEL,
            TurnType.TURN_WAY, TurnType.TURN_ARRIVE, TurnType.TURN_RESTAREA -> false
            else -> true
        }
    }
}
