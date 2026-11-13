package com.nolleo.app.core.navi

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.SourceMode
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.domain.model.NaviStatus
import com.nolleo.app.domain.model.TripSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 여정 소스 고르기 — 홈에서 고른 [TripTarget] 에 따라 내비(실제)·타이머·시뮬레이션 중 하나를 쓴다.
 *  - 현재 경로: 내비에 잡힌 길 안내를 그대로 (경로 요청 없음)
 *  - 최근 목적지: 출발할 때 내비에 길 안내를 요청한다. 자동 모드에서 내비 미연결·onError·이벤트 없음(60초)이면
 *    출발 전에 받은 예상 시간으로 타이머 여정을 대신 돌리고 UI 에 사유를 띄운다
 *  - 목적지 없이: 타이머 여정 (실제 1초 = 1초, 디버그 패널 여정=시뮬이면 시뮬 배율)
 *  - 시뮬 경로: 디버그 패널 여정=시뮬일 때만 (시연용)
 * 게임이 끝나도 사용자의 길 안내는 취소하지 않는다.
 */
class TripRepository(
    val navi: NaviHelperTripSource,
    val sim: SimulatedTripSource,
    val timer: TimerTripSource,
    private val scope: CoroutineScope,
) {
    val mode = MutableStateFlow(SourceMode.AUTO)

    private val _activeKind = MutableStateFlow(TripSourceKind.SIMULATED)
    val activeKind: StateFlow<TripSourceKind> = _activeKind.asStateFlow()

    private val _fallbackReason = MutableStateFlow<String?>(null)
    val fallbackReason: StateFlow<String?> = _fallbackReason.asStateFlow()

    private val _events = MutableSharedFlow<TripEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<TripEvent> = _events.asSharedFlow()

    val snapshot: StateFlow<TripSnapshot?> = combine(_activeKind, navi.snapshot, sim.snapshot, timer.snapshot) { kind, real, simulated, timed ->
        when (kind) {
            TripSourceKind.REAL_NAVI -> real
            TripSourceKind.SIMULATED -> simulated
            TripSourceKind.TIMER -> timed
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /** 길 안내를 요청한 최근 목적지 — 내비가 시작하지 못하면 이 예상 시간으로 타이머를 돌린다 */
    private var pendingPlace: TripTarget.Place? = null
    private var watchdog: Job? = null
    private val _tripActive = MutableStateFlow(false)
    private var tripActive: Boolean
        get() = _tripActive.value
        set(value) {
            _tripActive.value = value
        }
    private val foreground = MutableStateFlow(false)
    private var awaitingRouteStart = false

    init {
        navi.events.onEach { event -> onNaviEvent(event) }.launchIn(scope)
        sim.events.onEach { event -> if (_activeKind.value == TripSourceKind.SIMULATED && tripActive) _events.emit(event) }
            .launchIn(scope)
        timer.events.onEach { event -> if (_activeKind.value == TripSourceKind.TIMER && tripActive) _events.emit(event) }
            .launchIn(scope)
        // 길 안내 중이면 남은 시간을 주기적으로 다시 조회한다 (onDestinationInfo 는 조회해야만 온다).
        // 앱이 화면에 보일 때(홈의 "현재 경로" 줄) 또는 실제 내비 여정으로 게임 중일 때만 — 그 밖에는 폴링하지 않는다.
        scope.launch {
            combine(navi.naviStatus, foreground, _tripActive, _activeKind) { status, visible, active, kind ->
                shouldPollNavi(status, visible, active, kind)
            }.distinctUntilChanged().collectLatest { poll ->
                if (poll) {
                    while (true) {
                        delay(NAVI_REFRESH_MS)
                        navi.refresh()
                    }
                }
            }
        }
    }

    fun start() {
        navi.start()
    }

    /** 앱이 다시 보이면 내비 목적지·남은 시간·최근 목적지를 바로 다시 조회한다 (내비 앱에서 바꾼 것을 홈에 반영) */
    fun setForeground(visible: Boolean) {
        foreground.value = visible
        if (!visible) return
        // 연결에 실패했던 NaviHelper 는 앱이 다시 보일 때 한 번 더 연결해 본다 (연결되면 최근 목적지도 묻는다)
        if (navi.status.value is SdkStatus.Unavailable) {
            navi.start()
        } else {
            navi.refresh()
            navi.refreshRecentDestinations()
        }
    }

    /** 주차(휴게소 등)·쉬어 가기를 묻는 동안에는 시뮬·타이머 여정의 시간이 흐르지 않는다 */
    fun setMoving(moving: Boolean) {
        sim.moving.value = moving
        timer.moving.value = moving
    }

    /** 홈에서 고른 여정을 시작한다 */
    fun begin(target: TripTarget) {
        tripActive = true
        watchdog?.cancel()
        pendingPlace = null
        awaitingRouteStart = false
        when (target) {
            TripTarget.NaviRoute -> adoptNaviRoute()
            is TripTarget.Place -> beginPlace(target)
            is TripTarget.Timer -> useTimer(target.name, target.totalSec, reason = null)
            is TripTarget.Simulated -> useSim(target.preset)
        }
    }

    /** 내비에 이미 설정된 경로를 여정으로 채택 */
    private fun adoptNaviRoute() {
        clearLocal()
        _activeKind.value = TripSourceKind.REAL_NAVI
        _fallbackReason.value = null
        navi.adoptActiveRoute()
        AppLog.i(TAG, "adopted active navi route")
    }

    private fun beginPlace(target: TripTarget.Place) {
        if (mode.value == SourceMode.AUTO && navi.status.value != SdkStatus.Available) {
            useTimer(target.place.name, target.estimatedSec, reason = "내비 미연결(${navi.status.value.label})", target)
            return
        }
        clearLocal()
        _activeKind.value = TripSourceKind.REAL_NAVI
        _fallbackReason.value = null
        pendingPlace = target
        awaitingRouteStart = true
        navi.requestRoute(target.place)
        if (mode.value == SourceMode.AUTO) {
            watchdog = scope.launch {
                delay(NAVI_EVENT_TIMEOUT_MS)
                if (tripActive && _activeKind.value == TripSourceKind.REAL_NAVI && navi.snapshot.value?.remainTimeSec == null) {
                    useTimer(target.place.name, target.estimatedSec, reason = "내비 이벤트 없음(60초)", target)
                }
            }
        }
    }

    /**
     * @param reason null 이면 홈에서 고른 "목적지 없이", 아니면 최근 목적지로 길 안내를 시작하지 못해 대신 세는 것
     * @param place 대신 세는 경우의 목적지 (진행 방향을 모를 때 그쪽을 진행 방향으로 쓴다)
     */
    private fun useTimer(name: String, totalSec: Int, reason: String?, place: TripTarget.Place? = null) {
        watchdog?.cancel()
        awaitingRouteStart = false
        pendingPlace = null
        if (reason != null) AppLog.w(TAG, "route not started → timer trip with the estimate: $reason")
        _fallbackReason.value = reason
        sim.clear()
        _activeKind.value = TripSourceKind.TIMER
        // 여정=시뮬이면 시간도 시뮬 배율로 흐른다 (에뮬레이터에서 끝까지 빨리 보려고)
        val scale = if (mode.value == SourceMode.SIM) sim.timeScale.value else 1f
        timer.begin(name, totalSec, scale, place?.place?.point)
    }

    private fun useSim(preset: DestinationPreset) {
        watchdog?.cancel()
        timer.clear()
        _fallbackReason.value = null
        _activeKind.value = TripSourceKind.SIMULATED
        sim.requestRoute(preset)
    }

    /** 디버그 패널 "남은 5분으로" · 이어서 하기: 시뮬·타이머 여정의 남은 시간을 바로 지정 (실제 내비는 해당 없음) */
    fun jumpToRemain(remainSec: Int) {
        when (_activeKind.value) {
            TripSourceKind.SIMULATED -> sim.jumpToRemain(remainSec)
            TripSourceKind.TIMER -> timer.jumpToRemain(remainSec)
            TripSourceKind.REAL_NAVI -> Unit
        }
    }

    /** 디버그 패널 "내비 남은 시간으로": 시뮬은 위치를 두고 빠르기를 맞추고([SimulatedTripSource.matchRemain]), 시간 여정은 남은 시간을 바로 지정 */
    fun matchRemain(remainSec: Int) {
        when (_activeKind.value) {
            TripSourceKind.SIMULATED -> sim.matchRemain(remainSec)
            TripSourceKind.TIMER -> timer.jumpToRemain(remainSec)
            TripSourceKind.REAL_NAVI -> Unit
        }
    }

    private suspend fun onNaviEvent(event: TripEvent) {
        if (!tripActive || _activeKind.value != TripSourceKind.REAL_NAVI) return
        when (event) {
            is TripEvent.Error -> {
                val place = pendingPlace
                if (awaitingRouteStart && mode.value == SourceMode.AUTO && place != null) {
                    useTimer(place.place.name, place.estimatedSec, reason = "내비 오류(${event.reason})", place)
                } else {
                    _events.emit(event)
                }
            }
            is TripEvent.RouteStarted -> {
                // 남은 시간 재조회는 NaviHelperTripSource.onRouteStarted 가 이미 한다
                awaitingRouteStart = false
                pendingPlace = null
                watchdog?.cancel()
                _events.emit(event)
            }
            // 새 경로를 요청하면 내비가 이전 경로를 먼저 취소한다(에뮬레이터 관측) → 그 취소는 무시
            TripEvent.Cancelled -> if (!awaitingRouteStart) _events.emit(event)
            else -> _events.emit(event)
        }
    }

    private fun clearLocal() {
        sim.clear()
        timer.clear()
    }

    /** 게임 세션이 끝나도 사용자의 길 안내는 취소하지 않는다. 시뮬·타이머만 정리하고 최근 목적지를 다시 묻는다 */
    fun endTrip() {
        tripActive = false
        watchdog?.cancel()
        pendingPlace = null
        awaitingRouteStart = false
        clearLocal()
        navi.refreshRecentDestinations()
    }

    fun stop() {
        endTrip()
        navi.stop()
    }

    companion object {
        private const val TAG = "TripRepo"
        const val NAVI_EVENT_TIMEOUT_MS = 60_000L
        const val NAVI_REFRESH_MS = 30_000L

        /** 길 안내 중이고, 앱이 보이거나 실제 내비 여정으로 게임 중일 때만 남은 시간을 주기적으로 조회한다 */
        fun shouldPollNavi(status: NaviStatus?, visible: Boolean, tripActive: Boolean, kind: TripSourceKind): Boolean =
            status == NaviStatus.ROUTE_DRIVE && (visible || (tripActive && kind == TripSourceKind.REAL_NAVI))
    }
}
