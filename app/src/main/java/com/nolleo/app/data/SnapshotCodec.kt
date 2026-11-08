package com.nolleo.app.data

import com.nolleo.app.domain.drowsy.DriverBaseline
import com.nolleo.app.domain.drowsy.DrowsinessMonitor
import com.nolleo.app.domain.drowsy.ResponseSample
import com.nolleo.app.domain.engine.SessionSnapshot
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.RoundPacing
import com.nolleo.app.domain.model.RoundPlan
import com.nolleo.app.domain.model.SessionPlan
import org.json.JSONArray
import org.json.JSONObject

/** 진행 중 세션·운전자 기준값의 JSON 직렬화 (org.json). 순수 함수 — 단위 테스트 대상. */
object SnapshotCodec {

    /**
     * 저장본 형식 버전.
     *  - 1: 첫 형식
     *  - 2: 졸음 1단계 시각·2단계 횟수·마지막 2단계 시각, 끝말잇기 최장, 운전자 id, 시뮬 남은 시간 (11/8)
     *  - 3: 목적지 없이 노는 세션 여부, 타이머 여정의 전체 시간 (11/8) — simRemain 은 타이머 여정의 남은 시간에도 쓴다
     */
    const val VERSION = 3

    fun encode(s: SessionSnapshot): String = JSONObject().apply {
        val d = s.drowsy
        put("v", VERSION)
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
        put("window", JSONArray(d.window.map(::encodeSample)))
        put("cap", d.difficultyCap)
        put("lastTrigger", d.lastTriggerAtMs ?: JSONObject.NULL)
        put("stage1", d.stage1Count)
        put("stage1Times", JSONArray(d.stage1Times))
        put("stage2", d.stage2Count)
        put("lastStage2", d.lastStage2AtMs ?: JSONObject.NULL)
        put("longestChain", s.longestChain)
        put("driverId", s.driverProfileId ?: JSONObject.NULL)
        put("simRemain", s.simRemainSec ?: JSONObject.NULL)
        put("noDest", s.noDestination)
        put("timerTotal", s.timerTotalSec ?: JSONObject.NULL)
        put("savedAt", s.savedAtWallMs)
    }.toString()

    /** v1~v3 을 읽는다 (없는 필드는 기본값). 모르는 버전·손상된 JSON·말이 안 되는 값이면 null — 홈에서 새로 시작한다 */
    fun decode(json: String): SessionSnapshot? = runCatching {
        val o = JSONObject(json)
        if (o.optInt("v", 1) !in 1..VERSION) return null
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
            drowsy = DrowsinessMonitor.SavedState(
                window = o.getJSONArray("window").let { arr -> (0 until arr.length()).map { decodeSample(arr.getJSONObject(it)) } },
                difficultyCap = o.optInt("cap", 3),
                lastTriggerAtMs = o.optLongOrNull("lastTrigger"),
                stage1Count = o.optInt("stage1"),
                stage1Times = o.optJSONArray("stage1Times")?.let { arr -> (0 until arr.length()).map { arr.getLong(it) } }.orEmpty(),
                stage2Count = o.optInt("stage2"),
                lastStage2AtMs = o.optLongOrNull("lastStage2"),
            ),
            longestChain = o.optInt("longestChain"),
            driverProfileId = if (o.isNull("driverId")) null else o.getInt("driverId"),
            simRemainSec = if (o.isNull("simRemain")) null else o.getInt("simRemain"),
            noDestination = o.optBoolean("noDest"),
            timerTotalSec = if (o.isNull("timerTotal")) null else o.getInt("timerTotal"),
            savedAtWallMs = o.optLong("savedAt"),
        )
    }.getOrNull()?.takeIf(::isConsistent)

    /** 이어서 할 수 없는 저장본(운전자 없음·진행 위치가 계획 밖)을 거른다 */
    private fun isConsistent(s: SessionSnapshot): Boolean =
        s.players.count { it.isDriver } == 1 &&
            s.roundIndex in 0..s.plan.rounds.size &&
            s.turnInRound >= 0 &&
            s.rotationIndex >= 0

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
        .put("latency", s.latencyMs).put("correct", s.correct ?: JSONObject.NULL)

    /** v1 의 difficulty·at·drive 는 판단에 쓰지 않아 읽지 않는다 */
    private fun decodeSample(o: JSONObject) = ResponseSample(
        latencyMs = o.getLong("latency"),
        correct = if (o.isNull("correct")) null else o.getBoolean("correct"),
    )

    private fun encodeIntMap(m: Map<Int, Int>) = JSONObject().apply { m.forEach { (k, v) -> put(k.toString(), v) } }

    private fun decodeIntMap(o: JSONObject): Map<Int, Int> = o.keys().asSequence().associate { it.toInt() to o.getInt(it) }

    private fun decodeStringMap(o: JSONObject?): Map<String, String> =
        o?.keys()?.asSequence()?.associateWith { o.getString(it) }.orEmpty()

    private fun JSONArray.toStrings(): List<String> = (0 until length()).map { getString(it) }

    private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
}
