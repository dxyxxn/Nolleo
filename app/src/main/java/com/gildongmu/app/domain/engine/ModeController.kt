package com.gildongmu.app.domain.engine

import com.gildongmu.app.core.Clock
import com.gildongmu.app.domain.model.CarSignals
import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.domain.model.PauseReason
import com.gildongmu.app.domain.model.UxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 차량 신호·규제·세션 상태를 [DriveStateDeriver] → [ModeArbiter] 로 흘려 [GameMode] 를 만든다.
 * 이벤트 기반이며, 디바운스/STOPPED 3초 판정이 걸려 있을 때만 1회성 delay 로 재평가한다 (폴링 없음).
 */
class ModeController(
    private val signals: StateFlow<CarSignals>,
    private val ux: StateFlow<UxState>,
    private val clock: Clock,
    private val scope: CoroutineScope,
    log: (String) -> Unit,
) {
    private val deriver = DriveStateDeriver()
    private val arbiter = ModeArbiter(log = log)

    val sessionActive = MutableStateFlow(false)
    val voiceRoundActive = MutableStateFlow(false)

    private val _pauseReasons = MutableStateFlow<Set<PauseReason>>(emptySet())
    val pauseReasons: StateFlow<Set<PauseReason>> = _pauseReasons.asStateFlow()

    private val _rawDriveState = MutableStateFlow(DriveState.UNKNOWN)
    val rawDriveState: StateFlow<DriveState> = _rawDriveState.asStateFlow()

    private val _decision = MutableStateFlow(
        ModeArbiter.Decision(GameMode.IDLE, DriveState.UNKNOWN, false, "init", null),
    )
    val decision: StateFlow<ModeArbiter.Decision> = _decision.asStateFlow()

    private var recheckJob: Job? = null

    init {
        val inputs: List<Flow<Any>> = listOf(signals, ux, sessionActive, voiceRoundActive, _pauseReasons)
        merge(*inputs.toTypedArray()).onEach { reevaluate() }.launchIn(scope)
    }

    fun addPause(reason: PauseReason) = _pauseReasons.update { it + reason }

    fun removePause(reason: PauseReason) = _pauseReasons.update { it - reason }

    fun clearPauses() {
        _pauseReasons.value = emptySet()
    }

    fun reevaluate() {
        val now = clock.elapsedMs()
        val derived = deriver.derive(signals.value, now)
        _rawDriveState.value = derived.state
        val decision = arbiter.evaluate(
            ModeArbiter.Input(
                sessionActive = sessionActive.value,
                driveState = derived.state,
                ux = ux.value,
                pauseReasons = _pauseReasons.value,
                voiceRoundActive = voiceRoundActive.value,
            ),
            now,
        )
        _decision.value = decision
        val nextCheck = listOfNotNull(derived.recheckAtMs, decision.recheckAtMs).minOrNull()
        scheduleRecheck(nextCheck, now)
    }

    private fun scheduleRecheck(atMs: Long?, nowMs: Long) {
        recheckJob?.cancel()
        recheckJob = null
        if (atMs == null) return
        recheckJob = scope.launch {
            delay((atMs - nowMs).coerceAtLeast(0) + 20)
            reevaluate()
        }
    }
}
