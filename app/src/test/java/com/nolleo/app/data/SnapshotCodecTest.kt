package com.nolleo.app.data

import com.nolleo.app.domain.drowsy.DriverBaseline
import com.nolleo.app.domain.drowsy.DrowsinessMonitor
import com.nolleo.app.domain.drowsy.ResponseSample
import com.nolleo.app.domain.engine.SessionPlanner
import com.nolleo.app.domain.engine.SessionSnapshot
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.RoundPacing
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
            drowsy = DrowsinessMonitor.SavedState(
                window = listOf(ResponseSample(2500, true), ResponseSample(8000, null)),
                difficultyCap = 2,
                lastTriggerAtMs = 123_456,
                stage1Count = 2,
                stage1Times = listOf(100_000, 123_456),
                stage2Count = 1,
                lastStage2AtMs = 200_000,
            ),
            longestChain = 7,
            driverProfileId = 2,
            simRemainSec = 5_400,
            savedAtWallMs = 999,
        )
        assertEquals(snap, SnapshotCodec.decode(SnapshotCodec.encode(snap)))
    }

    @Test
    fun timerSnapshot_roundTrip_andV2ReadsAsNotTimer() {
        val timer = minimal().copy(destinationName = "목적지 없이 1시간", noDestination = true, timerTotalSec = 3600, simRemainSec = 2400)
        assertEquals(timer, SnapshotCodec.decode(SnapshotCodec.encode(timer)))
        val v2 = JSONObject(SnapshotCodec.encode(timer)).put("v", 2)
        listOf("noDest", "timerTotal").forEach(v2::remove)
        val decoded = SnapshotCodec.decode(v2.toString())!!
        assertEquals(false, decoded.noDestination)
        assertNull(decoded.timerTotalSec)
    }

    private fun minimal(plan: com.nolleo.app.domain.model.SessionPlan = SessionPlanner.plan(3600, GameChoice.QUIZ)) = SessionSnapshot(
        destinationName = "강릉역", presetId = null, players = listOf(Player(1, "엄마", true)),
        choice = GameChoice.QUIZ, demo = false, plan = plan, roundIndex = 0, turnInRound = 0, rotationIndex = 0,
        scores = emptyMap(), correctCounts = emptyMap(), regionsVisited = emptyList(), roundsPlayed = emptyList(),
        quizState = emptyMap(), chainState = emptyMap(), pendingQuestionId = null,
        drowsy = DrowsinessMonitor.SavedState(emptyList(), difficultyCap = 3, lastTriggerAtMs = null, stage1Count = 0),
        savedAtWallMs = 1,
    )

    @Test
    fun v1Snapshot_readsWithDefaultsForNewFields() {
        val json = JSONObject(SnapshotCodec.encode(minimal().copy(longestChain = 5, driverProfileId = 2, simRemainSec = 600)))
        json.put("v", 1)
        listOf("stage1Times", "stage2", "lastStage2", "longestChain", "driverId", "simRemain").forEach(json::remove)
        // v1 의 응답 샘플에는 판단에 쓰지 않는 필드가 더 있었다
        json.put("window", org.json.JSONArray().put(JSONObject().put("latency", 2000).put("correct", true).put("difficulty", 2).put("at", 5).put("drive", "DRIVING")))
        val decoded = SnapshotCodec.decode(json.toString())!!
        assertEquals(0, decoded.longestChain)
        assertNull(decoded.driverProfileId)
        assertNull(decoded.simRemainSec)
        assertEquals(0, decoded.drowsy.stage2Count)
        assertEquals(listOf(ResponseSample(2000, true)), decoded.drowsy.window)
    }

    @Test
    fun unknownVersionOrInconsistentSnapshot_decodesToNull() {
        val future = JSONObject(SnapshotCodec.encode(minimal())).put("v", SnapshotCodec.VERSION + 1)
        assertNull(SnapshotCodec.decode(future.toString()))
        assertNull("운전자 없음", SnapshotCodec.decode(SnapshotCodec.encode(minimal().copy(players = listOf(Player(2, "조수석", false))))))
        assertNull("계획 밖 판", SnapshotCodec.decode(SnapshotCodec.encode(minimal().copy(roundIndex = 99))))
        assertNull("음수 위치", SnapshotCodec.decode(SnapshotCodec.encode(minimal().copy(turnInRound = -1))))
    }

    @Test
    fun oldSnapshotWithoutPacing_readsAsRelaxed() {
        val snap = minimal(SessionPlanner.plan(3600, GameChoice.QUIZ, pacing = RoundPacing.NONSTOP))
        val json = JSONObject(SnapshotCodec.encode(snap))
        assertEquals("NONSTOP", json.getJSONObject("plan").getString("pacing"))
        json.getJSONObject("plan").remove("pacing") // pacing 필드가 생기기 전 저장본
        assertEquals(RoundPacing.RELAXED, SnapshotCodec.decode(json.toString())?.plan?.pacing)
    }

    @Test
    fun randomPlanSeed_roundTrips_andOldPlanReadsAsZero() {
        val snap = minimal(SessionPlanner.plan(3 * 3600, GameChoice.RANDOM, gameSeed = 7L)).copy(choice = GameChoice.RANDOM)
        assertEquals(snap, SnapshotCodec.decode(SnapshotCodec.encode(snap)))
        val json = JSONObject(SnapshotCodec.encode(snap))
        json.getJSONObject("plan").remove("gameSeed")
        assertEquals(0L, SnapshotCodec.decode(json.toString())?.plan?.gameSeed)
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
