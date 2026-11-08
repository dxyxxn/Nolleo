package com.nolleo.app.core.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.constants.TurnType
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.CautionPoint
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.TripInfo
import ai.pleos.playground.navi.data.TripRequest
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.domain.model.GeoPoint
import com.nolleo.app.domain.model.NaviStatus
import com.nolleo.app.domain.model.TripSourceKind
import com.nolleo.app.domain.rest.RestStopKind
import com.nolleo.app.domain.rest.RouteRestStop
import com.nolleo.app.domain.rest.RouteRestStops
import com.nolleo.app.domain.rest.TbtItem
import com.nolleo.app.domain.model.TurnHint
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
import kotlinx.coroutines.launch

/**
 * NaviHelper SDK — 남은 시간·경로·안내 시점 (DEV_GUIDE §8.2).
 * 패턴은 샘플 NaviHelperViewModel 과 같다: NaviHelper(appContext) → initialize(onConnected, onFailed)
 * → addListener → API 호출(결과는 전부 콜백) → removeListener(같은 인스턴스) → release().
 *
 * 조회 API(get*Info)는 값을 반환하지 않고 대응 콜백(onXxxInfo)으로 결과가 온다.
 * requestRoute 성공 호출 ≠ 탐색 완료 → onRouteStarted / onRoutingCompleted / onError 로 확인.
 *
 * 홈의 최근 목적지: getRecentDestinationInfo → onRecentDestinationInfo, 곳마다 requestTrip → onTripInfo 로 예상 시간
 * (길 안내는 시작하지 않는다 — 11/8 에뮬레이터 확인). 이 응답들은 우리 요청이 아니어도(다른 앱의 요청) 모든 연결 앱에 오고
 * 요청 번호가 없다 → 예상 시간은 한 번에 하나만 묻고, 응답의 목적지 좌표로 어느 곳의 답인지 맞춘다.
 *
 * @param scope 콜백(바인더 스레드)을 메인으로 옮기고 예상 시간 요청의 시간 제한을 잰다
 */
class NaviHelperTripSource(context: Context, private val scope: CoroutineScope) : TripSource {

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

    /** 마지막으로 받은 이벤트 이름 — 디버그 패널 표시용 */
    private val _lastEventLabel = MutableStateFlow<String?>(null)
    val lastEventLabel: StateFlow<String?> = _lastEventLabel.asStateFlow()

    private var helper: NaviHelper? = null

    /** 내비 상태 (ROUTE_DRIVE = 길 안내 중). 홈 카드 표시·남은 시간 주기 조회 판단에 쓴다 */
    private val _naviStatus = MutableStateFlow<NaviStatus?>(null)
    val naviStatus: StateFlow<NaviStatus?> = _naviStatus.asStateFlow()
    private val navStatus: NaviStatus? get() = _naviStatus.value

    /** 내비 최근 목적지 (내비 순서, 쓸 수 있는 곳만) — 홈 목적지 목록 */
    private val _recentDestinations = MutableStateFlow<List<NaviPlace>>(emptyList())
    val recentDestinations: StateFlow<List<NaviPlace>> = _recentDestinations.asStateFlow()

    /** 최근 목적지별 예상 시간(초) — [NaviPlace.key] → requestTrip 의 eta */
    private val _tripEstimates = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tripEstimates: StateFlow<Map<String, Int>> = _tripEstimates.asStateFlow()

    // 예상 시간 요청은 메인에서만 다룬다 (콜백은 scope 로 옮겨 온다)
    private val estimateQueue = ArrayDeque<NaviPlace>()
    private var estimating: NaviPlace? = null
    private var estimateTimeout: Job? = null

    /** 마지막 TBT 목록의 경로 위 휴게소·졸음쉼터(그때 기준 거리)와 그때의 남은 거리 — 이후 남은 거리가 준 만큼 앞당긴다 */
    private var tbtStops: List<RouteRestStop>? = null
    private var remainAtTbt: Int? = null

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(routeStartInfo: RouteStartInfo) {
            val distance = TripUnits.distanceM(routeStartInfo.distance)
            val sec = TripUnits.durationSec(routeStartInfo.duration, distance)
            AppLog.i(TAG, "onRouteStarted raw duration=${routeStartInfo.duration} distance=${routeStartInfo.distance} → ${sec}s")
            mark("onRouteStarted")
            update(name = routeStartInfo.name, remainSec = sec, distanceM = distance)
            // 사용자가 내비 앱에서 직접 길 안내를 시작해도 이 이벤트는 온다 (onRoutingCompleted·onDestinationInfo 는 오지 않음 — 에뮬레이터 관측)
            // → 홈의 "내비 목적지" 카드를 바로 띄우고, 정확한 남은 시간은 조회해서 다시 받는다
            if (sec > 0) _activeNaviDestination.value = _snapshot.value
            _events.tryEmit(TripEvent.RouteStarted(sec))
            runCall("getDestinationInfo") { helper?.getDestinationInfo() }
            refreshTbt() // 경로 위 휴게소·졸음쉼터 목록
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
            if (TripUnits.isNoRoute(destinationInfo.remainTime, destinationInfo.remainDistance)) {
                // 경로가 없을 때도 0/0 으로 응답한다 (에뮬레이터 관측) → 유효한 남은 시간으로 쓰지 않는다
                _activeNaviDestination.value = null
                return
            }
            update(name = destinationInfo.name, remainSec = sec, distanceM = distance)
            // 목적지 좌표: 진행 방향을 모를 때 "목적지 쪽"을 진행 방향으로 쓴다 (졸음쉼터 고르기)
            _snapshot.update { it?.copy(destination = GeoPoint(destinationInfo.latitude, destinationInfo.longitude)) }
            // 길 안내가 없다고 확인된 상태(FREE_DRIVE·INACTIVE)만 빼고 홈 카드를 최신 남은 시간으로 갱신
            if (navStatus != NaviStatus.FREE_DRIVE && navStatus != NaviStatus.INACTIVE) {
                _activeNaviDestination.value = _snapshot.value
            }
        }

        override fun onDrivingInfo(drivingInfo: DrivingInfo) {
            val dest = drivingInfo.destination
            val distance = TripUnits.distanceM(dest.distance)
            val sec = TripUnits.durationSec(dest.duration, distance)
            mark("onDrivingInfo")
            // 새 경로를 요청해 내비가 이전 경로를 취소한 직후(FREE_DRIVE)에는 0/0 이 온다 — 남은 시간 0 으로 쓰면
            // 막 시작한 게임이 곧바로 "도착"으로 끝난다 (11/7 에뮬레이터에서 재현) → onDestinationInfo 와 같은 규칙으로 무시
            if (TripUnits.isNoRoute(dest.duration, dest.distance)) return
            // onDestinationInfo 가 주 신호. 아직 값이 없을 때만 보조로 쓴다.
            if (_snapshot.value?.remainTimeSec == null) update(name = dest.name, remainSec = sec, distanceM = distance)
        }

        override fun onNavigationStatus(status: NavigationStatus) {
            AppLog.i(TAG, "onNavigationStatus $status")
            val next = status.toDomain()
            _naviStatus.value = next
            mark("onNavigationStatus")
            _snapshot.update { it?.copy(navStatus = next) }
            if (next == NaviStatus.ROUTE_DRIVE) {
                // 길 안내 시작·재개 → 목적지·남은 시간 조회 (결과는 onDestinationInfo)
                runCall("getDestinationInfo") { helper?.getDestinationInfo() }
            } else {
                _activeNaviDestination.value = null
                if (next == NaviStatus.FREE_DRIVE || next == NaviStatus.INACTIVE) clearRouteDetails()
            }
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
            // 경로 위 휴게소·졸음쉼터 — 졸음 케어 2단계는 이 목록에서 고른다 (distance 는 구간 거리라 누적, RouteRestStops)
            // 빈 목록은 "경로 없음"(경로를 다시 잡는 중 등)이다 — "경로 위에 쉼 장소 없음"과 구별해 null
            val stops = tbtInfoList.takeIf { it.isNotEmpty() }
                ?.let { list -> RouteRestStops.fromTbt(list.map { TbtItem(it.type.name, it.distance, it.description) }) }
            tbtStops = stops
            // 기준은 지금의 남은 거리여야 한다 — 마지막으로 받은 값은 최대 30초 전·재탐색 전 경로의 값일 수 있다.
            // 비워 두고 바로 다시 조회하면 다음 onDestinationInfo 가 그 값으로 기준을 잡는다 (currentRestStops)
            remainAtTbt = null
            refresh()
            AppLog.i(
                TAG,
                "onTBTInfo ${tbtInfoList.size}개 · 경로 위 휴게소 ${stops?.count { it.kind == RestStopKind.SERVICE_AREA } ?: 0}곳 · " +
                    "졸음쉼터 ${stops?.count { it.kind == RestStopKind.SHELTER } ?: 0}곳",
            )
            stops?.let { AppLog.sensitive(TAG, "TBT.restStops", it.joinToString { s -> "${s.name} ${s.distanceM}m" }) }
            _snapshot.update { it?.copy(nextTurn = hint, restStopsAhead = stops) }
        }

        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            AppLog.sensitive(TAG, "onCurrentLocationInfo.address", currentLocationInfo.address)
            _snapshot.update { (it ?: emptySnapshot()).copy(addressHint = currentLocationInfo.address) }
            _addressHint.value = currentLocationInfo.address
        }

        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            AppLog.i(TAG, "onDestinationArrived")
            mark("onDestinationArrived")
            _activeNaviDestination.value = null
            _events.tryEmit(TripEvent.Arrived(destinationArrivedInfo.name))
        }

        override fun onRouteCancelled() {
            AppLog.i(TAG, "onRouteCancelled")
            mark("onRouteCancelled")
            _activeNaviDestination.value = null
            clearRouteDetails()
            _events.tryEmit(TripEvent.Cancelled)
        }

        override fun onError(errorCode: NaviErrorCode) {
            AppLog.w(TAG, "onError $errorCode")
            mark("onError $errorCode")
            _events.tryEmit(TripEvent.Error(errorCode.name))
        }

        // 목적지·경유지·경로 옵션이 바뀌거나 재탐색이 끝나면 남은 시간이 달라진다 → 30초 주기를 기다리지 않고 바로 조회
        // (이벤트 목록은 샘플 NaviHelperViewModel 기준)
        override fun onDestinationChanged() = refreshOn("onDestinationChanged")

        override fun onReRouteEnded() = refreshOn("onReRouteEnded", tbt = true)

        override fun onRouteOptionChanged(routeOption: RouteOption) = refreshOn("onRouteOptionChanged $routeOption", tbt = true)

        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) = refreshOn("onWaypointChanged", tbt = true)

        override fun onWayPointArrived(waypointArrivedInfo: WaypointArrivedInfo) = refreshOn("onWayPointArrived", tbt = true)

        override fun onRouteStateInfo(routeStateInfo: RouteStateInfo) {
            AppLog.i(TAG, "onRouteStateInfo isRouting=${routeStateInfo.isRouting} mode=${routeStateInfo.naviMode}")
            // 앱보다 먼저 시작한 길 안내는 상태가 바뀔 때 오는 onNavigationStatus 를 받지 못해 상태가 비어 있다 →
            // 연결 때 조회한 경로 상태로 채운다 (그래야 30초 주기 남은 시간 조회가 돈다)
            if (_naviStatus.value == null && routeStateInfo.isRouting) {
                _naviStatus.value = NaviStatus.ROUTE_DRIVE
                _snapshot.update { it?.copy(navStatus = NaviStatus.ROUTE_DRIVE) }
            }
            // TODO(phase2): routeStateInfo.trafficStatus (TrafficCongestionType.HEAVY) → 정체 구간 자동 일시정지
        }

        override fun onRecentDestinationInfo(recentDestinationInfoList: List<RecentDestinationInfo>) {
            val places = NaviPlace.clean(
                recentDestinationInfoList.map { NaviPlace(it.name, it.address, it.latitude, it.longitude, it.poiId, it.poiSubId) },
            )
            AppLog.i(TAG, "onRecentDestinationInfo ${recentDestinationInfoList.size}곳 → 쓸 수 있는 곳 ${places.size}곳")
            AppLog.sensitive(TAG, "recent", places.joinToString { it.name })
            scope.launch {
                _recentDestinations.value = places
                requestEstimates(places.take(MAX_ESTIMATES))
            }
        }

        override fun onTripInfo(tripInfo: TripInfo) {
            val dest = runCatching { tripInfo.summary.routePoints.lastOrNull() }.getOrNull()
            val sec = TripUnits.durationSec(tripInfo.eta, TripUnits.distanceM(tripInfo.distance))
            scope.launch { onEstimate(dest?.latitude, dest?.longitude, sec) }
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
                    runCall("getRecentDestinationInfo") { h.getRecentDestinationInfo() }
                },
                onFailed = { t ->
                    AppLog.w(TAG, "NaviHelper connection failed", t)
                    // 샘플(NaviHelperViewModel)처럼 실패한 helper 를 버린다 — 남겨 두면 start() 로 다시 시도할 수 없다
                    runCatching { h.release() }
                    if (helper === h) helper = null
                    _status.value = SdkStatus.Unavailable(t.javaClass.simpleName)
                },
            )
        } catch (t: Throwable) {
            AppLog.w(TAG, "NaviHelper init threw", t)
            helper = null
            _status.value = SdkStatus.Unavailable(t.javaClass.simpleName)
        }
    }

    /** 홈에서 고른 최근 목적지로 길 안내를 요청한다 (출발을 눌렀을 때만 — 고르기만 해서는 내비를 바꾸지 않는다) */
    fun requestRoute(place: NaviPlace) {
        val h = helper
        if (h == null || _status.value != SdkStatus.Available) {
            _events.tryEmit(TripEvent.Error("NaviHelper 미연결"))
            return
        }
        tbtStops = null
        remainAtTbt = null
        _snapshot.value = TripSnapshot(
            destinationName = place.name,
            remainTimeSec = null,
            remainDistanceM = null,
            navStatus = navStatus,
            nextTurn = null,
            addressHint = _addressHint.value,
            destination = place.point,
        )
        AppLog.i(TAG, "requestRoute (poiId=${place.poiId.ifEmpty { "<none>" }})")
        AppLog.sensitive(TAG, "requestRoute.place", place.name)
        runCall("requestRoute") {
            // Kotlin named argument 필수 — Java 예제와 data class 선언의 인자 순서가 다르다 (DEV_GUIDE §8.2)
            h.requestRoute(
                RouteInfo(
                    latitude = place.latitude,
                    longitude = place.longitude,
                    poiName = place.name,
                    poiId = place.poiId,
                    // 최근 목적지는 poiSubId 가 null 로 온다(에뮬레이터) — RouteInfo 는 null 을 받지 않는다
                    poiSubId = place.poiSubId ?: "0",
                    address = place.address,
                ),
            )
        }
    }

    /** 최근 목적지 다시 조회 (결과는 onRecentDestinationInfo → 곳마다 예상 시간). 앱이 다시 보일 때·세션이 끝날 때 */
    fun refreshRecentDestinations() {
        if (_status.value != SdkStatus.Available) return
        runCall("getRecentDestinationInfo") { helper?.getRecentDestinationInfo() }
    }

    /** 예상 시간을 한 곳씩 묻는다 — 응답에 요청 번호가 없어서 겹쳐 물으면 어느 곳의 답인지 모른다 */
    private fun requestEstimates(places: List<NaviPlace>) {
        estimateQueue.clear()
        estimateQueue.addAll(places.filter { it.key != estimating?.key })
        if (estimating == null) sendNextEstimate()
    }

    private fun sendNextEstimate() {
        val next = estimateQueue.removeFirstOrNull() ?: return
        val h = helper ?: return
        estimating = next
        runCall("requestTrip") { h.requestTrip(TripRequest(next.latitude, next.longitude, next.poiId, next.name, next.poiSubId)) }
        estimateTimeout?.cancel()
        estimateTimeout = scope.launch {
            delay(ESTIMATE_TIMEOUT_MS)
            AppLog.w(TAG, "requestTrip: ${ESTIMATE_TIMEOUT_MS / 1000}초 안에 onTripInfo 없음 → 다음 곳")
            estimating = null
            sendNextEstimate()
        }
    }

    /** onTripInfo (메인). 응답의 목적지가 지금 묻는 곳이 아니면 다른 앱의 요청에 대한 답이다 → 무시 */
    private fun onEstimate(latitude: Double?, longitude: Double?, sec: Int) {
        val asked = estimating ?: return
        if (latitude != null && longitude != null && !NaviPlace.sameSpot(asked, latitude, longitude)) return
        estimateTimeout?.cancel()
        estimating = null
        if (sec > 0) {
            _tripEstimates.update { it + (asked.key to sec) }
            AppLog.i(TAG, "onTripInfo → 예상 ${TripUnits.formatDuration(sec)}")
        }
        sendNextEstimate()
    }

    /** 내비에 이미 설정된 경로를 그대로 여정으로 쓴다 (경로 요청 없이 정보만 갱신) */
    fun adoptActiveRoute() {
        _activeNaviDestination.value?.let { _snapshot.value = it }
        runCall("getDestinationInfo") { helper?.getDestinationInfo() }
        runCall("getTBTInfo") { helper?.getTBTInfo() }
    }

    /** 목적지·남은 시간 다시 조회 (결과는 onDestinationInfo). 연결 전에는 아무것도 하지 않는다 */
    fun refresh() {
        if (_status.value != SdkStatus.Available) return
        runCall("getDestinationInfo") { helper?.getDestinationInfo() }
    }

    /** TBT 목록 다시 조회 (결과는 onTBTInfo) — 경로가 바뀌었을 때만 (주기 조회는 하지 않는다) */
    private fun refreshTbt() {
        if (_status.value != SdkStatus.Available) return
        runCall("getTBTInfo") { helper?.getTBTInfo() }
    }

    /** 경로가 없어짐(취소·길 안내 끝): 경로 위 쉼 장소와 다음 분기점 안내를 함께 지운다 — 안내가 남으면 길 안내 일시정지가 풀리지 않는다 */
    private fun clearRouteDetails() {
        tbtStops = null
        remainAtTbt = null
        _snapshot.update { it?.copy(restStopsAhead = null, nextTurn = null) }
    }

    /** 마지막 TBT 뒤로 남은 거리가 준 만큼 경로 위 쉼 장소를 앞당긴다 (내비가 TBT 를 자주 보내지 않아도 거리가 맞게) */
    private fun currentRestStops(remainNow: Int?): List<RouteRestStop>? {
        val stops = tbtStops ?: return null
        if (remainAtTbt == null && remainNow != null) remainAtTbt = remainNow
        val base = remainAtTbt ?: return stops
        return if (remainNow == null) stops else RouteRestStops.advance(stops, base - remainNow)
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
            val remainDistance = distanceM ?: current?.remainDistanceM
            (current ?: emptySnapshot()).copy(
                destinationName = name?.takeIf { it.isNotBlank() } ?: current?.destinationName.orEmpty(),
                remainTimeSec = remainSec ?: current?.remainTimeSec,
                remainDistanceM = remainDistance,
                navStatus = navStatus,
                restStopsAhead = currentRestStops(remainDistance),
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

    private fun refreshOn(event: String, tbt: Boolean = false) {
        AppLog.i(TAG, "$event → 남은 시간 다시 조회")
        mark(event)
        refresh()
        if (tbt) refreshTbt()
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

        /** 홈에 보이는 최근 목적지 수 — 예상 시간도 이만큼만 묻는다 */
        const val MAX_ESTIMATES = 3
        private const val ESTIMATE_TIMEOUT_MS = 5_000L

        /** 분기/진출입/회전/교차로 — 운전에 집중해야 하는 안내 (P1 자동 일시정지 대상) */
        fun TurnType.needsAttention(): Boolean = when (this) {
            TurnType.TURN_UNKNOWN, TurnType.TURN_STRAIGHT, TurnType.TURN_TUNNEL,
            TurnType.TURN_WAY, TurnType.TURN_ARRIVE, TurnType.TURN_RESTAREA -> false
            else -> true
        }
    }
}
