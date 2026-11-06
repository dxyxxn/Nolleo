package com.gildongmu.app.core.navi

import com.gildongmu.app.core.SdkStatus
import com.gildongmu.app.domain.model.NaviStatus
import com.gildongmu.app.domain.model.TripSourceKind
import com.gildongmu.app.domain.model.TurnHint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** 여정 소스가 내보내는 현재 값. 단위는 [TripUnits] 에서 이미 초/미터로 변환된 상태다. */
data class TripSnapshot(
    val destinationName: String,
    val remainTimeSec: Int?,
    val remainDistanceM: Int?,
    val navStatus: NaviStatus?,
    val nextTurn: TurnHint?,
    /** NaviHelper onCurrentLocationInfo.address — FusedLocation 폴백용 지역 추정 */
    val addressHint: String? = null,
    /** 여정 시간 / 실제 시간 배율. 실제 내비는 1, 시뮬레이션은 압축 배율 */
    val timeScale: Float = 1f,
)

sealed interface TripEvent {
    data class RouteStarted(val durationSec: Int?) : TripEvent
    data class Arrived(val name: String) : TripEvent
    data object Cancelled : TripEvent
    data class Error(val reason: String) : TripEvent
}

/** 구현: [NaviHelperTripSource](실제, 심사 대상) / [SimulatedTripSource](빈 곳을 메우는 시뮬레이션). */
interface TripSource {
    val kind: TripSourceKind
    val status: StateFlow<SdkStatus>
    val snapshot: StateFlow<TripSnapshot?>
    val events: SharedFlow<TripEvent>

    fun start()
    fun requestRoute(preset: DestinationPreset)
    fun stop()
}
