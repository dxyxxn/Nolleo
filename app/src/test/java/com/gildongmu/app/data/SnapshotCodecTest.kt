package com.gildongmu.app.data

import com.gildongmu.app.domain.drowsy.DriverBaseline
import com.gildongmu.app.domain.drowsy.ResponseSample
import com.gildongmu.app.domain.engine.SessionPlanner
import com.gildongmu.app.domain.engine.SessionSnapshot
import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameKind
import com.gildongmu.app.domain.model.Player
import com.gildongmu.app.domain.model.RoundPacing
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnapshotCodecTest {

    @Test
    fun sessionSnapshot_roundTrip() {
        val snap = SessionSnapshot(
            destinationName = "부산역",
            presetId = "busan",
            players = listOf(Player(1, "아빠", true), Player(2, "조수석", false)),
            choice = GameChoice.ALTERNATE,
            demo = true,
            plan = SessionPlanner.plan(4 * 3600, GameChoice.ALTERNATE, demo = true, pacing = RoundPacing.OFTEN),
            roundIndex = 1,
            turnInRound = 2,
            rotationIndex = 3,
            scores = mapOf(1 to 20, 2 to 10),
            correctCounts = mapOf(1 to 2, 2 to 1),
            regionsVisited = listOf("seoul", "gyeonggi"),
            roundsPlayed = listOf(GameKind.REGION_QUIZ),
            quizState = mapOf("asked" to "seoul-01,seoul-02", "lastRegion" to "gyeonggi"),
            chainState = mapOf("used" to "기차,차표", "lastWord" to "차표"),
            pendingQuestionId = "gyeonggi-03",
            drowsyWindow = listOf(ResponseSample(2500, true, 2, 1_000, DriveState.DRIVING), ResponseSample(8000, null, 1, 2_000, DriveState.STOPPED)),
            difficultyCap = 2,
            lastTriggerAtMs = 123_456,
            stage1Count = 1,
            savedAtWallMs = 999,
        )
        assertEquals(snap, SnapshotCodec.decode(SnapshotCodec.encode(snap)))
    }

    @Test
    fun oldSnapshotWithoutPacing_readsAsRelaxed() {
        val plan = SessionPlanner.plan(3600, GameChoice.QUIZ, pacing = RoundPacing.NONSTOP)
        val snap = SessionSnapshot(
            destinationName = "강릉역", presetId = null, players = listOf(Player(1, "엄마", true)),
            choice = GameChoice.QUIZ, demo = false, plan = plan, roundIndex = 0, turnInRound = 0, rotationIndex = 0,
            scores = emptyMap(), correctCounts = emptyMap(), regionsVisited = emptyList(), roundsPlayed = emptyList(),
            quizState = emptyMap(), chainState = emptyMap(), pendingQuestionId = null, drowsyWindow = emptyList(),
            difficultyCap = 3, lastTriggerAtMs = null, stage1Count = 0, savedAtWallMs = 1,
        )
        val json = JSONObject(SnapshotCodec.encode(snap))
        assertEquals("NONSTOP", json.getJSONObject("plan").getString("pacing"))
        json.getJSONObject("plan").remove("pacing") // pacing 필드가 생기기 전 저장본
        assertEquals(RoundPacing.RELAXED, SnapshotCodec.decode(json.toString())?.plan?.pacing)
    }

    @Test
    fun brokenJson_decodesToNull() {
        assertNull(SnapshotCodec.decode("{not json"))
        assertNull(SnapshotCodec.decodeBaseline("[]"))
    }

    @Test
    fun baseline_roundTrip() {
        val b = DriverBaseline(1834.5, 0.82, 17)
        assertEquals(b, SnapshotCodec.decodeBaseline(SnapshotCodec.encodeBaseline(b)))
    }
}
