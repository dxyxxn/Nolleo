package com.nolleo.app.core.car

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.domain.model.CarSignals
import com.nolleo.app.domain.model.UxState
import kotlinx.coroutines.flow.StateFlow

enum class CarSourceKind(val label: String) { REAL("AAOS Car API"), SIMULATED("시뮬레이션") }

/** 차량 상태 입력 소스. 구현: [AaosCarStateSource](실제) / [SimulatedCarStateSource](시연·테스트). */
interface CarStateSource {
    val kind: CarSourceKind
    val signals: StateFlow<CarSignals>

    /** null = 규제 정보를 모름 (CarUxRestrictionsManager 미연결) */
    val ux: StateFlow<UxState?>
    val status: StateFlow<SdkStatus>

    fun start()
    fun stop()
}
