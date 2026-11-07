package com.nolleo.app.core.car

import com.nolleo.app.core.SdkStatus
import com.nolleo.app.domain.model.CarSignals
import com.nolleo.app.domain.model.GearPosition
import com.nolleo.app.domain.model.UxState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 시연·테스트용 차량 상태. 디버그 패널의 기어·속도·DO 토글로 조작한다.
 * 실제 CarUxRestrictionsManager 가 살아 있으면 CarStateRepository 가 그 값을 우선한다 (지시서 §10).
 */
class SimulatedCarStateSource : CarStateSource {
    override val kind = CarSourceKind.SIMULATED

    private val _signals = MutableStateFlow(
        CarSignals(connected = true, gear = GearPosition.PARK, speedMps = 0f, parkingBrakeOn = true),
    )
    override val signals: StateFlow<CarSignals> = _signals.asStateFlow()

    private val _ux = MutableStateFlow<UxState?>(UxState.UNRESTRICTED)
    override val ux: StateFlow<UxState?> = _ux.asStateFlow()

    private val _status = MutableStateFlow<SdkStatus>(SdkStatus.Available)
    override val status: StateFlow<SdkStatus> = _status.asStateFlow()

    override fun start() = Unit
    override fun stop() = Unit

    fun setGear(gear: GearPosition) {
        _signals.update {
            it.copy(
                gear = gear,
                parkingBrakeOn = gear == GearPosition.PARK,
                speedMps = if (gear == GearPosition.PARK) 0f else it.speedMps,
            )
        }
        // 주행 기어 + 속도 → 플랫폼이라면 DO 를 켠다. 시뮬레이션도 같은 규칙으로 맞춘다.
        syncDo()
    }

    fun setSpeed(mps: Float) {
        _signals.update { it.copy(speedMps = mps.coerceAtLeast(0f)) }
        syncDo()
    }

    private var manualDo: Boolean? = null

    /** null = 기어/속도로부터 자동 */
    fun setDistractionOptimization(required: Boolean?) {
        manualDo = required
        syncDo()
    }

    private fun syncDo() {
        val s = _signals.value
        val auto = s.gear != GearPosition.PARK && (s.speedMps ?: 0f) > 0f
        val required = manualDo ?: auto
        _ux.value = if (required) {
            UxState(
                requiresDO = true,
                activeRestrictions = UxState.UX_RESTRICTIONS_NO_KEYBOARD or
                    UxState.UX_RESTRICTIONS_NO_VIDEO or UxState.UX_RESTRICTIONS_NO_SETUP,
            )
        } else {
            UxState.UNRESTRICTED
        }
    }
}
