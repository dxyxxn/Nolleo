package com.gildongmu.app.domain.engine

import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.domain.model.PauseReason
import com.gildongmu.app.domain.model.UxState

/**
 * 주행 상태 + 플랫폼 규제 + 일시정지 사유 → [GameMode] (지시서 §3 규칙 1~5).
 *
 * 1. requiresDO == true 면 어떤 경우에도 SCREEN 금지 (플랫폼 규제가 기어 신호보다 우선, 디바운스 없이 즉시 반영)
 * 2. DriveState 전환은 [debounceMs] 디바운스 → 흔들리는 신호로 모드가 깜빡이지 않는다
 * 3. PAUSED 진입/해제 시 TTS·STT 처리는 GameSession 이 한다 (여기서는 모드만 결정)
 * 4. SCREEN→VOICE(출발)는 즉시, VOICE→SCREEN(정차)은 진행 중인 음성 판이 끝난 뒤
 * 5. 모든 전환을 로그로 남긴다: "ModeArbiter: DRIVING+DO → VOICE (reason=...)"
 */
class ModeArbiter(
    private val debounceMs: Long = 1_500,
    private val log: (String) -> Unit = {},
) {
    data class Input(
        val sessionActive: Boolean,
        /** DriveStateDeriver 결과(디바운스 전) */
        val driveState: DriveState,
        val ux: UxState,
        val pauseReasons: Set<PauseReason>,
        /** 음성 판이 진행 중인지 (정차해도 판을 마무리할 때까지 VOICE 유지) */
        val voiceRoundActive: Boolean,
    )

    data class Decision(
        val mode: GameMode,
        /** 디바운스된 주행 상태 */
        val driveState: DriveState,
        /** 정차 + 규제 없음 → 화면 게임을 열 수 있는 상태 */
        val screenAvailable: Boolean,
        val reason: String,
        /** 디바운스 대기 중이면 다시 평가해야 할 시각 */
        val recheckAtMs: Long?,
    )

    private var stableDrive: DriveState = DriveState.UNKNOWN
    private var candidate: DriveState? = null
    private var candidateSinceMs: Long = 0
    private var lastMode: GameMode? = null
    private var lastDriveLogged: DriveState? = null
    private var lastDoLogged: Boolean? = null

    val currentDriveState: DriveState get() = stableDrive

    fun evaluate(input: Input, nowMs: Long): Decision {
        var recheckAt: Long? = null
        if (input.driveState == stableDrive) {
            candidate = null
        } else {
            if (candidate != input.driveState) {
                candidate = input.driveState
                candidateSinceMs = nowMs
            }
            if (nowMs - candidateSinceMs >= debounceMs) {
                stableDrive = input.driveState
                candidate = null
            } else {
                recheckAt = candidateSinceMs + debounceMs
            }
        }

        val requiresDO = input.ux.requiresDO
        val screenAvailable = stableDrive == DriveState.PARKED && !requiresDO
        val (mode, reason) = when {
            !input.sessionActive -> GameMode.IDLE to "no-session"
            screenAvailable && !input.voiceRoundActive -> GameMode.SCREEN to "parked"
            input.pauseReasons.isNotEmpty() ->
                GameMode.PAUSED to input.pauseReasons.joinToString("+") { it.name.lowercase() }
            screenAvailable && input.voiceRoundActive -> GameMode.VOICE to "finishing-voice-round"
            requiresDO && stableDrive == DriveState.PARKED -> GameMode.VOICE to "distraction-optimization"
            stableDrive == DriveState.UNKNOWN -> GameMode.VOICE to "car-state-unknown(safe)"
            stableDrive == DriveState.STOPPED -> GameMode.VOICE to "stopped"
            else -> GameMode.VOICE to "driving"
        }

        if (mode != lastMode || stableDrive != lastDriveLogged || requiresDO != lastDoLogged) {
            val from = lastMode?.name ?: "-"
            val doTag = if (requiresDO) "+DO" else ""
            log("ModeArbiter: ${stableDrive.name}$doTag → ${mode.name} (from=$from, reason=$reason)")
            lastMode = mode
            lastDriveLogged = stableDrive
            lastDoLogged = requiresDO
        }
        return Decision(mode, stableDrive, screenAvailable, reason, recheckAt)
    }
}
