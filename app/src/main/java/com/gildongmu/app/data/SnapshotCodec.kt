package com.gildongmu.app.data

import com.gildongmu.app.domain.drowsy.DriverBaseline
import com.gildongmu.app.domain.drowsy.ResponseSample
import com.gildongmu.app.domain.engine.SessionSnapshot
import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameKind
import com.gildongmu.app.domain.model.Player
import com.gildongmu.app.domain.model.RoundPacing
import com.gildongmu.app.domain.model.RoundPlan
import com.gildongmu.app.domain.model.SessionPlan
import org.json.JSONArray
import org.json.JSONObject

/** 진행 중 세션·운전자 기준값의 JSON 직렬화 (org.json). 순수 함수 — 단위 테스트 대상. */
object SnapshotCodec {

    fun encode(s: SessionSnapshot): String = JSONObject().apply {
        put("v", 1)
        put("destinationName", s.destinationName)
        put("presetId", s.presetId ?: JSONObject.NULL)
        put("players", JSONArray(s.players.map { JSONObject().put("id", it.id).put("name", it.name).put("driver", it.isDriver) }))
        put("choice", s.choice.name)
        put("demo", s.demo)
        put("plan", encodePlan(s.plan))
        put("roundIndex", s.roundIndex)
        put("turnInRound", s.turnInRound)
        put("rotationIndex", s.rotationIndex)
        put("scores", encodeIntMap(s.scores))
        put("correct", encodeIntMap(s.correctCounts))
        put("regions", JSONArray(s.regionsVisited))
        put("roundsPlayed", JSONArray(s.roundsPlayed.map { it.name }))
        put("quiz", JSONObject(s.quizState))
        put("chain", JSONObject(s.chainState))
        put("pendingQuestionId", s.pendingQuestionId ?: JSONObject.NULL)
        put("window", JSONArray(s.drowsyWindow.map(::encodeSample)))
        put("cap", s.difficultyCap)
        put("lastTrigger", s.lastTriggerAtMs ?: JSONObject.NULL)
        put("stage1", s.stage1Count)
        put("savedAt", s.savedAtWallMs)
    }.toString()

    fun decode(json: String): SessionSnapshot? = runCatching {
        val o = JSONObject(json)
        val players = o.getJSONArray("players").let { arr ->
            (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                Player(p.getInt("id"), p.getString("name"), p.getBoolean("driver"))
            }
        }
        SessionSnapshot(
            destinationName = o.getString("destinationName"),
            presetId = o.optStringOrNull("presetId"),
            players = players,
            choice = GameChoice.valueOf(o.getString("choice")),
            demo = o.optBoolean("demo"),
            plan = decodePlan(o.getJSONObject("plan")),
            roundIndex = o.getInt("roundIndex"),
            turnInRound = o.optInt("turnInRound"),
            rotationIndex = o.optInt("rotationIndex"),
            scores = decodeIntMap(o.getJSONObject("scores")),
            correctCounts = decodeIntMap(o.getJSONObject("correct")),
            regionsVisited = o.getJSONArray("regions").toStrings(),
            roundsPlayed = o.getJSONArray("roundsPlayed").toStrings().map { GameKind.valueOf(it) },
            quizState = decodeStringMap(o.optJSONObject("quiz")),
            chainState = decodeStringMap(o.optJSONObject("chain")),
            pendingQuestionId = o.optStringOrNull("pendingQuestionId"),
            drowsyWindow = o.getJSONArray("window").let { arr -> (0 until arr.length()).map { decodeSample(arr.getJSONObject(it)) } },
            difficultyCap = o.optInt("cap", 3),
            lastTriggerAtMs = if (o.isNull("lastTrigger")) null else o.getLong("lastTrigger"),
            stage1Count = o.optInt("stage1"),
            savedAtWallMs = o.optLong("savedAt"),
        )
    }.getOrNull()

    fun encodeBaseline(b: DriverBaseline): String =
        JSONObject().put("latency", b.latencyEmaMs).put("accuracy", b.accuracyEma).put("count", b.sampleCount).toString()

    fun decodeBaseline(json: String?): DriverBaseline? = json?.let {
        runCatching {
            val o = JSONObject(it)
            DriverBaseline(o.getDouble("latency"), o.getDouble("accuracy"), o.getInt("count"))
        }.getOrNull()
    }

    private fun encodePlan(p: SessionPlan) = JSONObject().apply {
        put("basis", p.basisRemainSec)
        put("pacing", p.pacing.name)
        put("rounds", JSONArray(p.rounds.map { r ->
            JSONObject().put("index", r.index).put("game", r.game.name).put("duration", r.targetDurationSec)
                .put("count", r.questionCount).put("startAt", r.startAtRemainSec).put("short", r.isShort)
        }))
    }

    private fun decodePlan(o: JSONObject): SessionPlan {
        val arr = o.getJSONArray("rounds")
        val rounds = (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            RoundPlan(
                index = r.getInt("index"),
                game = GameKind.valueOf(r.getString("game")),
                targetDurationSec = r.getInt("duration"),
                questionCount = r.getInt("count"),
                startAtRemainSec = r.getInt("startAt"),
                isShort = r.optBoolean("short"),
            )
        }
        // pacing 이 없는 이전 저장본은 기본 간격(여유 있게)으로 읽는다
        val pacing = runCatching { RoundPacing.valueOf(o.getString("pacing")) }.getOrDefault(RoundPacing.RELAXED)
        return SessionPlan(rounds, o.getInt("basis"), pacing)
    }

    private fun encodeSample(s: ResponseSample) = JSONObject()
        .put("latency", s.latencyMs).put("correct", s.correct ?: JSONObject.NULL).put("difficulty", s.difficulty)
        .put("at", s.atEpochMs).put("drive", s.driveState.name)

    private fun decodeSample(o: JSONObject) = ResponseSample(
        latencyMs = o.getLong("latency"),
        correct = if (o.isNull("correct")) null else o.getBoolean("correct"),
        difficulty = o.optInt("difficulty", 1),
        atEpochMs = o.optLong("at"),
        driveState = runCatching { DriveState.valueOf(o.getString("drive")) }.getOrDefault(DriveState.UNKNOWN),
    )

    private fun encodeIntMap(m: Map<Int, Int>) = JSONObject().apply { m.forEach { (k, v) -> put(k.toString(), v) } }

    private fun decodeIntMap(o: JSONObject): Map<Int, Int> = o.keys().asSequence().associate { it.toInt() to o.getInt(it) }

    private fun decodeStringMap(o: JSONObject?): Map<String, String> =
        o?.keys()?.asSequence()?.associateWith { o.getString(it) }.orEmpty()

    private fun JSONArray.toStrings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
}
