package com.nolleo.app.core.navi

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.domain.model.GeoPoint
import com.nolleo.app.domain.model.TripSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 경로 없이 시간만 세는 여정.
 *  - 홈의 "목적지 없이"(30분~2시간): 정한 시간만큼 논다.
 *  - 최근 목적지로 출발했는데 내비가 길 안내를 시작하지 못함: 출발 전에 받은 예상 시간으로 대신 센다.
 * 차를 세워 둔 동안([moving] = false)에는 흐르지 않는다 — 실제 내비의 남은 시간도 정차 중엔 그대로다.
 * 경로가 없으므로 분기점 안내·경로 위 쉼 장소가 없고, 지역은 위치(FusedLocation)로, 쉬어 갈 곳은 졸음쉼터 표에서 고른다.
 */
class TimerTripSource(private val scope: CoroutineScope) : TripSource {

    override val kind = TripSourceKind.TIMER

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Available)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    private val _snapshot = MutableStateFlow<TripSnapshot?>(null)
    override val snapshot: StateFlow<TripSnapshot?> = _snapshot.asStateFlow()

    private val _events = MutableSharedFlow<TripEvent>(extraBufferCapacity = 16)
    override val events: SharedFlow<TripEvent> = _events.asSharedFlow()

    val moving = MutableStateFlow(true)

    /** 지금 세는 전체 시간(초) — 이어서 하기 저장본에 남긴다. 세는 중이 아니면 null */
    var totalSec: Int? = null
        private set

    private var name = ""
    private var destination: GeoPoint? = null
    private var timeScale = 1f
    private var elapsedSec = 0.0
    private var tickJob: Job? = null

    override fun start() = Unit

    /**
     * @param timeScale 실제 1초 = 여정 N초. 실제 사용은 1, 디버그 패널 여정=시뮬이면 시뮬 배율
     * @param destination 최근 목적지의 좌표(대신 세는 경우) — 진행 방향을 모를 때 쉼 장소 방향으로 쓴다
     */
    fun begin(name: String, totalSec: Int, timeScale: Float = 1f, destination: GeoPoint? = null) {
        tickJob?.cancel()
        this.name = name
        this.totalSec = totalSec
        this.timeScale = timeScale
        this.destination = destination
        elapsedSec = 0.0
        AppLog.i(TAG, "timer trip ${totalSec / 60}분 (x$timeScale)")
        publish()
        tickJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                if (moving.value) elapsedSec += TICK_MS / 1000.0 * this@TimerTripSource.timeScale
                publish()
                if (remainSec() <= 0) {
                    AppLog.i(TAG, "timer trip ended")
                    _events.tryEmit(TripEvent.Arrived(this@TimerTripSource.name))
                    break
                }
            }
        }
    }

    /** 디버그 패널 "남은 5분으로" · 이어서 하기(저장할 때의 남은 시간부터) */
    fun jumpToRemain(remainSec: Int) {
        val total = totalSec ?: return
        elapsedSec = (total - remainSec).coerceIn(0, total).toDouble()
        publish()
    }

    private fun remainSec(): Int = ((totalSec ?: 0) - elapsedSec).toInt().coerceAtLeast(0)

    private fun publish() {
        if (totalSec == null) return
        _snapshot.value = TripSnapshot(
            destinationName = name,
            remainTimeSec = remainSec(),
            remainDistanceM = null,
            navStatus = null,
            nextTurn = null,
            timeScale = timeScale,
            restStopsAhead = null,
            destination = destination,
        )
    }

    fun clear() {
        tickJob?.cancel()
        tickJob = null
        totalSec = null
        destination = null
        _snapshot.value = null
    }

    override fun stop() = clear()

    companion object {
        private const val TAG = "TimerTrip"
        private const val TICK_MS = 5_000L
    }
}
