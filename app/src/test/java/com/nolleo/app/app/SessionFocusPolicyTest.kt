package com.nolleo.app.app

import com.nolleo.app.app.SessionFocusPolicy.Action
import com.nolleo.app.core.speech.FocusChange
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFocusPolicyTest {

    private fun decide(change: FocusChange, ownTts: Boolean = false, pausedForFocus: Boolean = false) =
        SessionFocusPolicy.decide(change, ownTts, pausedForFocus)

    @Test
    fun transientLoss_pausesUntilGain() {
        assertEquals(Action.PAUSE_UNTIL_GAIN, decide(FocusChange.LOSS_TRANSIENT))
    }

    @Test
    fun permanentLoss_pausesUntilUser_insteadOfEndingSession() {
        assertEquals(Action.PAUSE_UNTIL_USER, decide(FocusChange.LOSS))
    }

    @Test
    fun lossDuringOwnPleosTts_isIgnored() {
        assertEquals(Action.IGNORE_SELF_INDUCED, decide(FocusChange.LOSS_TRANSIENT, ownTts = true))
        assertEquals(Action.IGNORE_SELF_INDUCED, decide(FocusChange.LOSS, ownTts = true))
    }

    @Test
    fun gain_resumesOnlyWhenPausedForFocus() {
        assertEquals(Action.RESUME, decide(FocusChange.GAIN, pausedForFocus = true))
        assertEquals(Action.CONTINUE, decide(FocusChange.GAIN, pausedForFocus = false))
    }

    @Test
    fun canDuck_continues() {
        assertEquals(Action.CONTINUE, decide(FocusChange.LOSS_TRANSIENT_CAN_DUCK))
    }
}
