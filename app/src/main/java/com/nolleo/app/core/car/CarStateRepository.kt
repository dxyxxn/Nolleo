package com.nolleo.app.core.car

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.core.SourceMode
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.domain.model.CarSignals
import com.nolleo.app.domain.model.UxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * 차량 입력 소스 선택: Real(AAOS + Pleos Vehicle 보조) → Simulated.
 * - auto: AAOS Car API 가 연결되면 실제 신호, 아니면 시뮬레이션
 * - 주행 규제(UX)는 실제 CarUxRestrictionsManager 가 살아 있으면 항상 그 값을 쓴다 (DO 는 실제, 속도는 시뮬 가능)
 */
class CarStateRepository(
    val aaos: AaosCarStateSource,
    val vehicleSdk: VehicleSdkClient,
    val sim: SimulatedCarStateSource,
    scope: CoroutineScope,
) {
    val mode = MutableStateFlow(SourceMode.AUTO)

    val activeKind: StateFlow<CarSourceKind> = combine(mode, aaos.status) { m, status ->
        when (m) {
            SourceMode.REAL -> CarSourceKind.REAL
            SourceMode.SIM -> CarSourceKind.SIMULATED
            SourceMode.AUTO -> if (status == SdkStatus.Available) CarSourceKind.REAL else CarSourceKind.SIMULATED
        }
    }.stateIn(scope, SharingStarted.Eagerly, CarSourceKind.SIMULATED)

    val signals: StateFlow<CarSignals> = combine(
        activeKind,
        aaos.signals,
        vehicleSdk.brakePedalPressed,
        sim.signals,
    ) { kind, real, brake, simulated ->
        if (kind == CarSourceKind.REAL) real.copy(brakePedalPressed = brake) else simulated
    }.stateIn(scope, SharingStarted.Eagerly, CarSignals())

    /** true = 실제 CarUxRestrictionsManager 값 사용 중 */
    val uxIsReal: StateFlow<Boolean> = aaos.ux.map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val ux: StateFlow<UxState> = combine(aaos.ux, sim.ux) { real, simulated ->
        real ?: simulated ?: UxState.UNRESTRICTED
    }.stateIn(scope, SharingStarted.Eagerly, UxState.UNRESTRICTED)

    init {
        activeKind
            .onEach { AppLog.i("CarSource", "active car source = ${it.label} (mode=${mode.value.label})") }
            .launchIn(scope)
    }

    fun start() {
        aaos.start()
        vehicleSdk.start()
    }

    fun stop() {
        aaos.stop()
        vehicleSdk.release()
    }
}
