package com.nolleo.app.core.location

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.SourceMode
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.navi.SimPosition
import com.nolleo.app.domain.model.GeoPoint
import com.nolleo.app.domain.model.TripSourceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

enum class LocationSourceKind(val label: String) {
    FUSED("FusedLocation"),
    NAVI_ADDRESS("내비 주소"),
    SIMULATED("시뮬레이션"),
    MANUAL("수동 지정"),
    NONE("없음"),
}

data class RegionReading(val regionCode: String?, val source: LocationSourceKind)

/**
 * 지역 판정 폴백 순서 (지시서 §5.3): FusedLocation → NaviHelper onCurrentLocationInfo.address → 시뮬레이션 → 없음(일반 퀴즈).
 * 여정이 시뮬레이션이면 위치도 시뮬레이션 경로를 따른다 (정지한 에뮬레이터 위치와 압축된 여정이 어긋나지 않게).
 */
class LocationRepository(
    val fused: FusedLocationSource,
    scope: CoroutineScope,
    resolverFlow: StateFlow<RegionResolver?>,
    naviAddress: StateFlow<String?>,
    simPosition: StateFlow<SimPosition?>,
    tripKind: StateFlow<TripSourceKind>,
) {
    val mode = MutableStateFlow(SourceMode.AUTO)

    /** 디버그 패널: 지역 강제 지정 (null = 해제) */
    val manualRegion = MutableStateFlow<String?>(null)

    private data class Inputs(
        val mode: SourceMode,
        val manual: String?,
        val fusedPoint: GeoPoint?,
        val fusedStatus: SdkStatus,
        val address: String?,
        val sim: SimPosition?,
        val tripKind: TripSourceKind,
        val resolver: RegionResolver?,
    )

    private val inputs = combine(
        combine(mode, manualRegion, fused.location, fused.status) { m, manual, p, s -> listOf(m, manual, p, s) },
        combine(naviAddress, simPosition, tripKind, resolverFlow) { a, sim, k, r -> listOf(a, sim, k, r) },
    ) { a, b ->
        Inputs(
            mode = a[0] as SourceMode,
            manual = a[1] as String?,
            fusedPoint = a[2] as GeoPoint?,
            fusedStatus = a[3] as SdkStatus,
            address = b[0] as String?,
            sim = b[1] as SimPosition?,
            tripKind = b[2] as TripSourceKind,
            resolver = b[3] as RegionResolver?,
        )
    }

    val reading: StateFlow<RegionReading> = inputs.map { i ->
        val resolver = i.resolver
        fun fusedRegion() = i.fusedPoint?.let { resolver?.resolve(it) }
        fun addressRegion() = resolver?.fromAddress(i.address)
        when {
            i.manual != null -> RegionReading(i.manual, LocationSourceKind.MANUAL)
            i.mode == SourceMode.SIM -> RegionReading(i.sim?.regionCode, LocationSourceKind.SIMULATED)
            i.mode == SourceMode.REAL ->
                fusedRegion()?.let { RegionReading(it, LocationSourceKind.FUSED) }
                    ?: addressRegion()?.let { RegionReading(it, LocationSourceKind.NAVI_ADDRESS) }
                    ?: RegionReading(null, LocationSourceKind.NONE)
            i.tripKind == TripSourceKind.SIMULATED && i.sim != null ->
                RegionReading(i.sim.regionCode, LocationSourceKind.SIMULATED)
            else ->
                fusedRegion()?.let { RegionReading(it, LocationSourceKind.FUSED) }
                    ?: addressRegion()?.let { RegionReading(it, LocationSourceKind.NAVI_ADDRESS) }
                    ?: i.sim?.let { RegionReading(it.regionCode, LocationSourceKind.SIMULATED) }
                    ?: RegionReading(null, LocationSourceKind.NONE)
        }
    }.stateIn(scope, SharingStarted.Eagerly, RegionReading(null, LocationSourceKind.NONE))

    val regionCode: StateFlow<String?> = reading.map { it.regionCode }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        reading.onEach { AppLog.i("Location", "region=${it.regionCode ?: "-"} via ${it.source.label}") }.launchIn(scope)
    }

    fun start() = fused.start()
    fun pause() = fused.pause()
    fun release() = fused.release()
}
