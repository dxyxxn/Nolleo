package com.nolleo.app.app

import com.nolleo.app.core.speech.FocusChange

/**
 * 음성 세션 중 오디오 포커스 변화에 어떻게 대응할지 (DEV_GUIDE §7.2). 판단만 하고 적용은 [GameCoordinator] 가 한다.
 *
 *  - 우리 Pleos TTS 가 말하는 중의 손실: Speech Service 가 따로 포커스를 잡아 생긴 것 → 무시(요청은 유지)
 *  - 일시 손실(통화·Gleo): 바로 멈추고 GAIN 을 기다린다 → GAIN 1초 뒤 같은 문제
 *  - 영구 손실(다른 미디어 앱): 바로 멈추고 요청을 거둔다. GAIN 이 오지 않으므로 사용자가 "계속"을 눌러야 이어진다
 *  - CAN_DUCK: 게임은 발화자라 계속
 */
object SessionFocusPolicy {

    enum class Action {
        /** 우리 발화로 생긴 손실 — 상태만 정리 */
        IGNORE_SELF_INDUCED,

        /** 일시정지(AUDIO_FOCUS), 요청 유지 */
        PAUSE_UNTIL_GAIN,

        /** 일시정지(AUDIO_UNAVAILABLE), 요청 반납, 상태 저장 */
        PAUSE_UNTIL_USER,

        /** AUDIO_FOCUS 해제 → 같은 문제 다시 */
        RESUME,

        /** 아무것도 하지 않음 */
        CONTINUE,
    }

    fun decide(change: FocusChange, ownTts: Boolean, pausedForFocus: Boolean): Action = when (change) {
        FocusChange.LOSS_TRANSIENT -> if (ownTts) Action.IGNORE_SELF_INDUCED else Action.PAUSE_UNTIL_GAIN
        FocusChange.LOSS -> if (ownTts) Action.IGNORE_SELF_INDUCED else Action.PAUSE_UNTIL_USER
        FocusChange.GAIN -> if (pausedForFocus) Action.RESUME else Action.CONTINUE
        FocusChange.LOSS_TRANSIENT_CAN_DUCK -> Action.CONTINUE
    }
}
