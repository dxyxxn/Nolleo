package com.gildongmu.app.core.navi

import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.core.SourceMode
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.domain.model.TripSourceKind
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 여정 소스 선택: NaviHelper(실제) → Simulated.
 * auto 모드에서 내비 미연결·onError·이벤트 없음(60초)이면 시뮬레이션 여정으로 넘어가고 UI 에 배지를 띄운다.
 * NaviHelperTripSource 코드는 그대로 남아 계속 동작한다 (심사 대상).
 */
class TripRepository(
    val navi: NaviHelperTripSource,
    val sim: SimulatedTripSource,
    private val scope: CoroutineScope,
) {
    val mode = MutableStateFlow(SourceMode.AUTO)

    private val _activeKind = MutableStateFlow(TripSourceKind.SIMULATED)
    val activeKind: StateFlow<TripSourceKind> = _activeKind.asStateFlow()

    private val _fallbackReason = MutableStateFlow<String?>(null)
    val fallbackReason: StateFlow<String?> = _fallbackReason.asStateFlow()

    private val _events = MutableSharedFlow<TripEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<TripEvent> = _events.asSharedFlow()

    val snapshot: StateFlow<TripSnapshot?> = combine(_activeKind, navi.snapshot, sim.snapshot) { kind, real, simulated ->
        if (kind == TripSourceKind.REAL_NAVI) real else simulated
    }.stateIn(scope, SharingStarted.Eagerly, null)

    private var currentPreset: DestinationPreset? = null
    private var watchdog: Job? = null
    private var tripActive = false

    init {
        navi.events.onEach { event -> onNaviEvent(event) }.launchIn(scope)
        sim.events.onEach { event -> if (_activeKind.value == TripSourceKind.SIMULATED && tripActive) _events.emit(event) }
            .launchIn(scope)
    }

    fun start() {
        navi.start()
    }

    /** 프리셋 목적지로 여정을 시작한다. */
    fun beginTrip(preset: DestinationPreset) {
        currentPreset = preset
        tripActive = true
        watchdog?.cancel()
        when (mode.value) {
            SourceMode.SIM -> useSim(preset, "시뮬레이션 강제")
            SourceMode.REAL -> useReal(preset, allowFallback = false)
            SourceMode.AUTO ->
                if (navi.status.value == SdkStatus.Available) useReal(preset, allowFallback = true)
                else useSim(preset, "내비 미연결(${navi.status.value.label})")
        }
    }

    /** 내비에 이미 설정된 경로를 여정으로 채택 */
    fun adoptNaviRoute() {
        tripActive = true
        currentPreset = null
        watchdog?.cancel()
        sim.clear()
        _activeKind.value = TripSourceKind.REAL_NAVI
        _fallbackReason.value = null
        navi.adoptActiveRoute()
        AppLog.i(TAG, "adopted active navi route")
    }

    private fun useReal(preset: DestinationPreset, allowFallback: Boolean) {
        sim.clear()
        _activeKind.value = TripSourceKind.REAL_NAVI
        _fallbackReason.value = null
        awaitingRouteStart = true
        navi.requestRoute(preset)
        if (allowFallback) {
            watchdog = scope.launch {
                delay(NAVI_EVENT_TIMEOUT_MS)
                if (tripActive && _activeKind.value == TripSourceKind.REAL_NAVI && navi.snapshot.value?.remainTimeSec == null) {
                    useSim(preset, "내비 이벤트 없음(60초)")
                }
            }
        }
    }

    private fun useSim(preset: DestinationPreset, reason: String) {
        watchdog?.cancel()
        AppLog.w(TAG, "fallback to simulated trip: $reason")
        _fallbackReason.value = reason
        _activeKind.value = TripSourceKind.SIMULATED
        sim.requestRoute(preset)
    }

    private suspend fun onNaviEvent(event: TripEvent) {
        if (!tripActive || _activeKind.value != TripSourceKind.REAL_NAVI) return
        when (event) {
            is TripEvent.Error -> {
                val preset = currentPreset
                if (awaitingRouteStart && mode.value == SourceMode.AUTO && preset != null) {
                    useSim(preset, "내비 오류(${event.reason})")
                } else {
                    _events.emit(event)
                }
            }
            is TripEvent.RouteStarted -> {
                awaitingRouteStart = false
                watchdog?.cancel()
                navi.refresh()
                _events.emit(event)
            }
            // 새 경로를 요청하면 내비가 이전 경로를 먼저 취소한다(에뮬레이터 관측) → 그 취소는 무시
            TripEvent.Cancelled -> if (!awaitingRouteStart) _events.emit(event)
            else -> _events.emit(event)
        }
    }

    private var awaitingRouteStart = false

    /** 게임 세션이 끝나도 사용자의 길 안내는 취소하지 않는다. 시뮬레이션만 정리한다. */
    fun endTrip() {
        tripActive = false
        watchdog?.cancel()
        sim.clear()
        currentPreset = null
    }

    fun stop() {
        endTrip()
        navi.stop()
    }

    companion object {
        private const val TAG = "TripRepo"
        const val NAVI_EVENT_TIMEOUT_MS = 60_000L
    }
}
