package com.nolleo.app.app

import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.navi.TripSnapshot
import com.nolleo.app.domain.model.DriveState
import com.nolleo.app.domain.model.GeoMath
import com.nolleo.app.domain.model.GeoPoint
import com.nolleo.app.domain.model.PauseReason
import com.nolleo.app.domain.model.TripSourceKind
import com.nolleo.app.domain.rest.MotionHeading
import com.nolleo.app.domain.rest.RestStopFinder
import com.nolleo.app.domain.rest.RestStopSuggestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 졸음 케어 2단계의 "쉬어 가기" (기획서 졸음 케어 2단계 + 쉼터 안내).
 *  1. 권할 곳 고르기 ([RestStopFinder]): 경로 정보가 있으면 경로 위 휴게소·졸음쉼터(내비 TBT `TURN_ENTER_SERVICEAREA`·
 *     `TURN_RESTAREA`, 시뮬 경로)에서만 — 가장 가까운 곳, 휴게소가 5 km 안쪽으로만 더 멀면 휴게소. 경로 정보가 없을 때만
 *     공공데이터 졸음쉼터 중 진행 방향 앞쪽. 휴게소는 편의시설(수면실·수유실·가족사랑 화장실·대표 음식)을 붙인다.
 *  2. 수락하면 게임은 [PauseReason.REST_STOP] 으로 쉰다. 경로 위 쉼 장소는 내비가 이미 지나가는 곳이라 경유지를 넣지 않는다
 *     (좌표 경유지는 고속도로 옆 일반 도로에 붙어 우회 경로가 된다 — 11/7 관측). 좌표를 아는 졸음쉼터는
 *     `FusedLocation.addProximityAlert`(반경 300 m, ENTER)로 근처에 온 것을 안다.
 *  3. 도착 = 근처(근접 알림·위치)에서 **서거나 주차** / 주차 / 시뮬 여정이 그 지점에 닿음 — 옆을 지나가기만 하면 도착이 아니다.
 *     도착하는 순간 [PauseReason.REST_STOP] 을 [PauseReason.REST_AREA] 로 바꿔 끼워 게임이 잠깐도 다시 시작하지 않게 하고
 *     (11/11 — 전에는 도착 안내를 말하는 몇 초 동안 끊겼던 문제가 화면에 다시 보였다), 도착 안내 말은 코디네이터가 한다.
 *  4. 주차했다가 다시 주행하면 "잘 쉬셨나요?" + 졸음 지표 초기화
 */
class RestCareController(
    private val c: AppContainer,
    private val scope: CoroutineScope,
    private val driveState: StateFlow<DriveState>,
) {
    sealed interface State {
        val suggestion: RestStopSuggestion?

        /**
         * 2단계가 걸려 쉬어 가기를 권하는 중 (음성 질문 + 화면 버튼).
         * [answered] = 음성 질문은 끝났다(거절·무응답) — 게임이 이어지므로 화면은 작은 띠로 잠시 남겨 버튼으로만 다시 수락할 수 있다
         */
        data class Offered(override val suggestion: RestStopSuggestion?, val answered: Boolean = false) : State

        /** 수락해서 쉼 장소로 가는 중 */
        data class Heading(override val suggestion: RestStopSuggestion, val alertRegistered: Boolean) : State

        /** 도착 — 다시 출발하면 끝 */
        data class Arrived(override val suggestion: RestStopSuggestion, val trigger: String) : State
    }

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    /** 쉼 장소까지 남은 거리 (가는 중) — 경로 위면 경로를 따라간 거리, 표에서 고른 곳이면 직선거리 */
    private val _distanceM = MutableStateFlow<Int?>(null)
    val distanceM: StateFlow<Int?> = _distanceM.asStateFlow()

    /** 세션에 도착 안내·휴식 후 초기화를 맡긴다 (GameCoordinator 가 연결) */
    var onArrived: ((RestStopSuggestion) -> Unit)? = null
    var onRested: (() -> Unit)? = null

    private var alertId: String? = null

    /** 지금 근접 알림 요청의 번호 — 지난 요청(취소·도착·다른 쉼 장소)의 늦은 등록·ENTER 를 거른다 */
    private var alertRequest = 0
    private var sawParkedAfterArrival = false
    private var hideJob: Job? = null

    /** 쉼터 반경 안에 들어왔다(근접 알림 ENTER 또는 위치). 이 상태에서 서거나 주차하면 도착 */
    private var nearTrigger: String? = null

    init {
        // 좌표를 아는 졸음쉼터: 위치가 반경 안이면 "근처" (근접 알림의 보조)
        c.fusedLocation.motion.onEach { m ->
            val heading = _state.value as? State.Heading ?: return@onEach
            val area = heading.suggestion.area ?: return@onEach
            if (m == null || !nearKorea(m.point)) return@onEach
            val d = GeoMath.distanceM(m.point, area.point).roundToInt()
            if (heading.suggestion.source == RestStopSuggestion.Source.TABLE &&
                c.tripRepository.activeKind.value != TripSourceKind.SIMULATED
            ) {
                _distanceM.value = d
            }
            when {
                d <= ARRIVE_RADIUS_M -> markNear("위치가 쉼터 ${d}m 안")
                d > ARRIVE_RADIUS_M * 3 -> nearTrigger = null // 옆을 지나쳐 멀어짐
            }
        }.launchIn(scope)
        // 표에서 고른 곳 + 시뮬 여정: 남은 거리는 시뮬 위치로
        c.simTrip.position.onEach { p ->
            val heading = _state.value as? State.Heading ?: return@onEach
            if (heading.suggestion.source != RestStopSuggestion.Source.TABLE) return@onEach
            val area = heading.suggestion.area ?: return@onEach
            if (p == null || c.tripRepository.activeKind.value != TripSourceKind.SIMULATED) return@onEach
            _distanceM.value = GeoMath.distanceM(p.point, area.point).roundToInt()
        }.launchIn(scope)
        // 경로 위 쉼 장소: 남은 거리는 경로 목록으로. 시뮬 여정은 그 지점을 지나면(목록에서 빠지면) 닿은 것으로 본다
        c.tripRepository.snapshot.onEach { snap ->
            val heading = _state.value as? State.Heading ?: return@onEach
            if (heading.suggestion.source != RestStopSuggestion.Source.ON_ROUTE) return@onEach
            val stops = snap?.restStopsAhead ?: return@onEach
            val stop = stops.firstOrNull { it.name == heading.suggestion.name }
            when {
                stop != null -> _distanceM.value = stop.distanceM
                c.tripRepository.activeKind.value == TripSourceKind.SIMULATED -> arrive("시뮬 여정이 쉼 장소에 닿음")
                // 실제 내비: 목록에서 빠져도(지나침·재탐색) 그대로 둔다 — 주차하면 도착, 지나쳤으면 "계속"·"안내 그만두기"
            }
        }.launchIn(scope)
        driveState.onEach { s ->
            when (val st = _state.value) {
                is State.Heading -> when {
                    s == DriveState.PARKED -> arrive(nearTrigger ?: "주차")
                    s == DriveState.STOPPED && nearTrigger != null -> arrive(nearTrigger!!)
                    else -> Unit
                }
                is State.Arrived -> when {
                    s == DriveState.PARKED -> sawParkedAfterArrival = true
                    s == DriveState.DRIVING && sawParkedAfterArrival -> finishRest()
                    else -> Unit
                }
                else -> Unit
            }
        }.launchIn(scope)
    }

    /** 2단계에서 권할 쉼 장소. 경로 위에 없거나(경로 정보가 있을 때) 위치·방향을 모르면 null */
    fun suggest(): RestStopSuggestion? {
        val trip = c.tripRepository.snapshot.value
        val motion = currentMotion(trip)
        val suggestion = compute(trip, motion)
        // 쉼 장소 이름·거리·진행 방향은 차의 위치를 짐작하게 하므로 release 에서는 가린다 (디버그 패널 화면에는 보인다)
        AppLog.i(
            TAG,
            "suggest: 위치=${if (motion != null) "있음" else "없음"} 경로 정보=${trip?.restStopsAhead?.let { "${it.size}곳" } ?: "없음"}" +
                " → ${suggestion?.let { "${it.kind.label} (${it.source.label})" } ?: "없음"}",
        )
        AppLog.sensitive(
            TAG,
            "suggest.detail",
            "방향=${motion?.second?.roundToInt() ?: "-"}° → " +
                (suggestion?.let { "${it.name} (${it.directionLabel ?: it.source.label}) ${it.distanceLabel} ${it.facilityLine ?: ""}" } ?: "없음"),
        )
        return suggestion
    }

    /** 디버그 패널 미리보기: 지금 2단계가 걸리면 권할 곳 */
    fun preview(): RestStopSuggestion? {
        val trip = c.tripRepository.snapshot.value
        return compute(trip, currentMotion(trip))
    }

    private fun compute(trip: TripSnapshot?, motion: Pair<GeoPoint, Double>?): RestStopSuggestion? {
        val onRoute = trip?.restStopsAhead
        val table = if (onRoute == null) motion?.let { (p, h) -> RestStopFinder.nearestAhead(c.content.restAreas.value, p, h) } else null
        return RestStopFinder.suggest(
            onRoute = onRoute,
            table = table,
            areas = c.content.restAreas.value,
            infos = c.content.serviceAreas.value,
            headingDeg = motion?.second,
            position = motion?.first,
        )
    }

    /** (현재 지점, 진행 방향°) — 시뮬 여정이면 시뮬 경로, 아니면 FusedLocation (움직인 방향 → SDK heading → 목적지 쪽) */
    fun currentMotion(trip: TripSnapshot? = c.tripRepository.snapshot.value): Pair<GeoPoint, Double>? {
        if (c.tripRepository.activeKind.value == TripSourceKind.SIMULATED) {
            c.simTrip.position.value?.let { sp -> sp.headingDeg?.let { return sp.point to it } }
        }
        val m = c.fusedLocation.motion.value ?: return null
        if (!nearKorea(m.point)) return null
        val heading = MotionHeading.resolve(m.movedHeading, m.sdkHeading, m.point, trip?.destination) ?: return null
        return m.point to heading
    }

    /** 이미 쉼 장소로 가는 중이면 그곳 (2단계가 다시 걸리면 새로 묻지 않고 가던 곳을 다시 알려 준다) */
    val headingTo: RestStopSuggestion? get() = (_state.value as? State.Heading)?.suggestion

    fun offer(suggestion: RestStopSuggestion?) {
        hideJob?.cancel()
        if (_state.value is State.Heading) return
        _state.value = State.Offered(suggestion)
    }

    /** 음성 질문의 결과 */
    fun decide(suggestion: RestStopSuggestion?, accepted: Boolean) {
        if (accepted && suggestion != null) {
            start(suggestion)
            return
        }
        // 거절·무응답: 카드를 잠시 작게 남겨 두고(버튼으로 다시 수락 가능) 닫는다
        (_state.value as? State.Offered)?.let { _state.value = it.copy(answered = true) }
        hideJob?.cancel()
        hideJob = scope.launch {
            delay(OFFER_CARD_MS)
            if (_state.value is State.Offered) _state.value = null
        }
    }

    /** 화면 "쉬어 가기" — 음성 질문이 끝난 뒤에 눌렀을 때 */
    fun acceptOffered() {
        (_state.value as? State.Offered)?.suggestion?.let(::start)
    }

    fun dismiss() {
        if (_state.value is State.Offered) _state.value = null
    }

    private fun start(s: RestStopSuggestion) {
        hideJob?.cancel()
        removeAlert() // 앞서 가던 쉼 장소의 알림이 남아 있으면 지운다
        nearTrigger = null
        val area = s.area
        val request = alertRequest
        val alert = area != null && c.fusedLocation.addProximityAlert(
            center = area.point,
            radiusMeters = ARRIVE_RADIUS_M.toFloat(),
            registered = { id -> scope.launch { onAlertRegistered(request, id) } },
            entered = { scope.launch { onAlertEntered(request) } },
            failed = { e -> AppLog.w(TAG, "proximity alert error: ${e.message}") },
        )
        c.modeController.addPause(PauseReason.REST_STOP)
        _distanceM.value = s.distanceM
        _state.value = State.Heading(s, alert)
        AppLog.i(TAG, "rest stop accepted: ${s.kind.label} · ${s.source.label} · 근접 알림 ${if (alert) "요청" else "없음(좌표 없음 — 주차하면 도착)"}")
        // 에뮬레이터에서 도착을 시험할 때 붙여 쓰는 명령 (debug 빌드 logcat, 그리고 디버그 패널 "쉬어 가기 상태" 줄)
        AppLog.sensitive(TAG, "rest stop", "${s.name} ${s.distanceLabel}${area?.let { " · 도착 시험: adb emu geo fix ${it.longitude} ${it.latitude}" } ?: ""}")
    }

    private fun onAlertRegistered(request: Int, id: String) {
        if (request == alertRequest) {
            alertId = id
            AppLog.i(TAG, "proximity alert registered ($id)")
        } else {
            // 등록이 끝나기 전에 취소·도착했거나 다른 쉼 장소로 바뀌었다 → 늦게 온 알림은 바로 지운다
            c.fusedLocation.removeProximityAlert(id)
            AppLog.i(TAG, "proximity alert registered late ($id) → removed")
        }
    }

    private fun onAlertEntered(request: Int) {
        if (request == alertRequest) markNear("근접 알림 ENTER") else AppLog.i(TAG, "ENTER from an old proximity alert → ignored")
    }

    /** 근처에 왔다 — 이미 서 있거나 주차 중이면 바로 도착, 아니면 서는 순간 도착 (옆을 지나가는 것과 구별) */
    private fun markNear(trigger: String) {
        if (_state.value !is State.Heading) return
        nearTrigger = trigger
        val s = driveState.value
        if (s == DriveState.STOPPED || s == DriveState.PARKED) arrive(trigger)
    }

    fun arrive(trigger: String) {
        val st = _state.value as? State.Heading ?: return
        removeAlert()
        nearTrigger = null
        // 쉬는 사유를 먼저 바꿔 끼운다 — REST_STOP 을 먼저 풀면 잠깐 음성 모드로 돌아가 끊겼던 문제가 보인다
        c.modeController.addPause(PauseReason.REST_AREA)
        c.modeController.removePause(PauseReason.REST_STOP)
        sawParkedAfterArrival = driveState.value == DriveState.PARKED
        _distanceM.value = null
        _state.value = State.Arrived(st.suggestion, trigger)
        AppLog.i(TAG, "rest stop arrived ($trigger)")
        onArrived?.invoke(st.suggestion)
    }

    /** "안내 그만두기" — 게임 쉬기·도착 감지를 끈다 */
    fun cancel() {
        if (_state.value !is State.Heading) return
        removeAlert()
        nearTrigger = null
        c.modeController.removePause(PauseReason.REST_STOP)
        _distanceM.value = null
        _state.value = null
        AppLog.i(TAG, "rest guidance cancelled by user")
    }

    private fun finishRest() {
        _state.value = null
        c.modeController.removePause(PauseReason.REST_AREA)
        AppLog.i(TAG, "rested → 다시 출발")
        onRested?.invoke()
    }

    fun clear() {
        hideJob?.cancel()
        removeAlert()
        nearTrigger = null
        c.modeController.removePause(PauseReason.REST_STOP)
        c.modeController.removePause(PauseReason.REST_AREA)
        _distanceM.value = null
        _state.value = null
    }

    /** 지금 알림을 지우고 요청 번호를 넘긴다 — 아직 오지 않은 등록·ENTER 콜백은 지난 요청이 된다 */
    private fun removeAlert() {
        alertRequest++
        alertId?.let(c.fusedLocation::removeProximityAlert)
        alertId = null
    }

    /** 졸음쉼터 자료는 국내만 — 에뮬레이터 기본 좌표(해외)로는 고르지 않는다 */
    private fun nearKorea(p: GeoPoint) = p.latitude in 33.0..39.0 && p.longitude in 124.0..132.0

    companion object {
        private const val TAG = "RestCare"
        const val ARRIVE_RADIUS_M = 300
        private const val OFFER_CARD_MS = 15_000L
    }
}
