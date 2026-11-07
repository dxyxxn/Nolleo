package com.nolleo.app.domain.engine

import com.nolleo.app.domain.model.DriveState
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.domain.model.PauseReason
import com.nolleo.app.domain.model.UxState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeArbiterTest {

    private val doOn = UxState(requiresDO = true, activeRestrictions = UxState.UX_RESTRICTIONS_NO_VIDEO)
    private val doOff = UxState.UNRESTRICTED

    private fun input(
        drive: DriveState,
        ux: UxState = doOff,
        session: Boolean = true,
        pauses: Set<PauseReason> = emptySet(),
        voiceRound: Boolean = false,
    ) = ModeArbiter.Input(session, drive, ux, pauses, voiceRound)

    /** 같은 상태를 디바운스 시간 넘게 유지시켜 확정한다. */
    private fun ModeArbiter.settle(input: ModeArbiter.Input, startMs: Long): ModeArbiter.Decision {
        evaluate(input, startMs)
        return evaluate(input, startMs + 1_600)
    }

    @Test
    fun noSession_isIdle() {
        val arbiter = ModeArbiter()
        val d = arbiter.settle(input(DriveState.PARKED, session = false), 0)
        assertEquals(GameMode.IDLE, d.mode)
    }

    @Test
    fun parkedWithoutDo_isScreen() {
        val arbiter = ModeArbiter()
        val d = arbiter.settle(input(DriveState.PARKED), 0)
        assertEquals(GameMode.SCREEN, d.mode)
        assertTrue(d.screenAvailable)
    }

    @Test
    fun driving_isVoice() {
        val arbiter = ModeArbiter()
        assertEquals(GameMode.VOICE, arbiter.settle(input(DriveState.DRIVING), 0).mode)
    }

    @Test
    fun stopped_isVoice() {
        val arbiter = ModeArbiter()
        assertEquals(GameMode.VOICE, arbiter.settle(input(DriveState.STOPPED), 0).mode)
    }

    @Test
    fun unknownCarState_isVoiceForSafety() {
        val arbiter = ModeArbiter()
        val d = arbiter.settle(input(DriveState.UNKNOWN), 0)
        assertEquals(GameMode.VOICE, d.mode)
        assertFalse(d.screenAvailable)
    }

    @Test
    fun requiresDo_neverAllowsScreen_evenWhenParked() {
        val arbiter = ModeArbiter()
        val d = arbiter.settle(input(DriveState.PARKED, ux = doOn), 0)
        assertEquals(GameMode.VOICE, d.mode)
        assertFalse(d.screenAvailable)
    }

    @Test
    fun requiresDo_appliesImmediately_withoutDebounce() {
        val arbiter = ModeArbiter()
        assertEquals(GameMode.SCREEN, arbiter.settle(input(DriveState.PARKED), 0).mode)
        // DO 가 켜진 바로 그 평가에서 SCREEN 이 금지되어야 한다
        val d = arbiter.evaluate(input(DriveState.PARKED, ux = doOn), 10_000)
        assertEquals(GameMode.VOICE, d.mode)
    }

    @Test
    fun driveStateChange_isDebounced() {
        val arbiter = ModeArbiter(debounceMs = 1_500)
        assertEquals(GameMode.SCREEN, arbiter.settle(input(DriveState.PARKED), 0).mode)

        val t0 = 10_000L
        val first = arbiter.evaluate(input(DriveState.DRIVING), t0)
        assertEquals("1.5초 전에는 아직 이전 상태", GameMode.SCREEN, first.mode)
        assertEquals(t0 + 1_500, first.recheckAtMs)

        val mid = arbiter.evaluate(input(DriveState.DRIVING), t0 + 1_000)
        assertEquals(GameMode.SCREEN, mid.mode)

        val done = arbiter.evaluate(input(DriveState.DRIVING), t0 + 1_500)
        assertEquals(GameMode.VOICE, done.mode)
        assertEquals(DriveState.DRIVING, done.driveState)
        assertNull(done.recheckAtMs)
    }

    @Test
    fun flickeringSignal_doesNotFlipMode() {
        val arbiter = ModeArbiter(debounceMs = 1_500)
        assertEquals(GameMode.VOICE, arbiter.settle(input(DriveState.DRIVING), 0).mode)
        var t = 10_000L
        // STOPPED ↔ DRIVING 이 0.5초 간격으로 흔들린다
        repeat(6) { i ->
            val drive = if (i % 2 == 0) DriveState.PARKED else DriveState.DRIVING
            val d = arbiter.evaluate(input(drive), t)
            assertEquals(GameMode.VOICE, d.mode)
            t += 500
        }
    }

    @Test
    fun pauseReason_turnsVoiceIntoPaused() {
        val arbiter = ModeArbiter()
        arbiter.settle(input(DriveState.DRIVING), 0)
        val d = arbiter.evaluate(input(DriveState.DRIVING, pauses = setOf(PauseReason.NAV_GUIDANCE)), 5_000)
        assertEquals(GameMode.PAUSED, d.mode)
        val resumed = arbiter.evaluate(input(DriveState.DRIVING), 6_000)
        assertEquals(GameMode.VOICE, resumed.mode)
    }

    @Test
    fun pauseReason_doesNotAffectScreenMode() {
        val arbiter = ModeArbiter()
        val d = arbiter.settle(input(DriveState.PARKED, pauses = setOf(PauseReason.USER)), 0)
        assertEquals(GameMode.SCREEN, d.mode)
    }

    @Test
    fun parking_duringVoiceRound_staysVoiceUntilRoundEnds() {
        val arbiter = ModeArbiter()
        arbiter.settle(input(DriveState.DRIVING, voiceRound = true), 0)
        val parkedDuringRound = arbiter.settle(input(DriveState.PARKED, voiceRound = true), 10_000)
        assertEquals(GameMode.VOICE, parkedDuringRound.mode)
        assertTrue("세션이 판을 마무리할 수 있게 알린다", parkedDuringRound.screenAvailable)

        val roundFinished = arbiter.evaluate(input(DriveState.PARKED, voiceRound = false), 20_000)
        assertEquals(GameMode.SCREEN, roundFinished.mode)
    }

    @Test
    fun departing_fromScreen_switchesToVoiceAfterDebounce() {
        val arbiter = ModeArbiter()
        assertEquals(GameMode.SCREEN, arbiter.settle(input(DriveState.PARKED), 0).mode)
        val d = arbiter.settle(input(DriveState.DRIVING), 10_000)
        assertEquals(GameMode.VOICE, d.mode)
    }

    @Test
    fun transitions_areLogged() {
        val logs = mutableListOf<String>()
        val arbiter = ModeArbiter(log = { logs += it })
        arbiter.settle(input(DriveState.PARKED), 0)
        arbiter.evaluate(input(DriveState.PARKED, ux = doOn), 5_000)
        assertNotNull(logs.find { it.startsWith("ModeArbiter: PARKED+DO → VOICE") })
    }
}
