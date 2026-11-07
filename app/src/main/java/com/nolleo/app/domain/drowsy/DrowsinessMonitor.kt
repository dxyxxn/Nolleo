package com.nolleo.app.domain.drowsy

import com.nolleo.app.domain.model.DriveState
import kotlin.math.max

/** 운전자 차례의 응답 1건. PAUSED 직후 첫 응답과 판 첫 문제(워밍업)는 호출자가 제외한다. */
data class ResponseSample(
    val latencyMs: Long,
    val correct: Boolean?,
    val difficulty: Int,
    val atEpochMs: Long,
    val driveState: DriveState,
)

/** 운전자 프로필별 기준값 (DataStore 저장, 과거 여정 누적) */
data class DriverBaseline(
    val latencyEmaMs: Double,
    val accuracyEma: Double,
    val sampleCount: Int,
)

/**
 * 참여형 졸음 케어 1단계. 게임 결과만으로 졸음을 단정하지 않는 **보조 기능**이다.
 *
 *  - 기준값: 응답 지연 EMA(α=0.1) + 정답률 EMA. 기준값이 없으면 첫 5개 응답(중앙값)으로 캘리브레이션 — 그동안 알림 없음.
 *  - 느림: latency > max(baseline × 1.5, baseline + 1500)
 *  - Stage1: 최근 5개 중 느림 3개 이상 → (1) 가볍게 말 걸기 (2) 난이도 상한 -1 (최소 1) (3) 쿨다운 5분 (4) 카드 표시
 *  - 기준값 EMA 는 느리지 않은 응답으로만 갱신한다 (졸린 동안 기준값이 함께 늘어나는 것을 막는다).
 *  - Stage2: 15분 안에 Stage1 이 두 번째로 걸리거나, (이번 응답이 느림 && 최근 5개 정답률 < 50%) → 휴식 권유 +
 *    경로 위 휴게소·졸음쉼터 안내 (GameSession → RestCareController: NaviHelper TBT, FusedLocation.addProximityAlert).
 *    쿨다운 20분, 난이도 상한도 -1. 쉼터에서 쉬고 다시 출발하면 [resetAfterRest].
 */
class DrowsinessMonitor(
    initialBaseline: DriverBaseline?,
    private val config: Config = Config(),
) {
    data class Config(
        val calibrationSamples: Int = 5,
        val windowSize: Int = 5,
        val slowTrigger: Int = 3,
        val slowRatio: Double = 1.5,
        val slowExtraMs: Long = 1_500,
        val cooldownMs: Long = 5 * 60_000,
        val emaAlpha: Double = 0.1,
        val maxDifficulty: Int = 3,
        /** AnswerCollector 의 무응답 타임아웃과 같다 */
        val timeoutMs: Long = 8_000,
        /** 이 시간 안에 Stage1 이 다시 걸리면 Stage2 */
        val stage2WindowMs: Long = 15 * 60_000,
        val stage2CooldownMs: Long = 20 * 60_000,
        /** 최근 창의 정답률이 이보다 낮고 이번 응답이 느리면 Stage2 */
        val lowAccuracy: Double = 0.5,
    ) {
        companion object {
            /** 데모 모드: 3분 시연 안에 1단계 → 2단계가 보이도록 쿨다운만 줄인다 (판단 규칙은 같다) */
            val DEMO = Config(cooldownMs = 20_000, stage2CooldownMs = 60_000)
        }
    }

    enum class Stage2Reason(val label: String) {
        REPEATED_STAGE1("15분 안에 1단계 두 번"),
        SLOW_AND_INACCURATE("느린 응답 + 정답률 50% 미만"),
        MANUAL("디버그 시험"),
    }

    sealed interface Action {
        data object None : Action
        data class Stage1(val newDifficultyCap: Int) : Action
        data class Stage2(val newDifficultyCap: Int, val reason: Stage2Reason) : Action
    }

    data class Snapshot(
        val baselineMs: Long?,
        val baselineAccuracy: Double?,
        val calibrating: Boolean,
        val calibrationCount: Int,
        val recentLatencies: List<Long>,
        val slowCount: Int,
        val difficultyCap: Int,
        val lastTriggerAtMs: Long?,
        val stage1Count: Int,
        val stage2Count: Int = 0,
        val lastStage2AtMs: Long? = null,
    )

    private var baseline: DriverBaseline? = initialBaseline
    private val calibration = mutableListOf<ResponseSample>()
    private val window = ArrayDeque<ResponseSample>()
    private var lastTriggerAtMs: Long? = null
    private var stage1Count = 0
    private val stage1Times = ArrayDeque<Long>()
    private var stage2Count = 0
    private var lastStage2AtMs: Long? = null

    var difficultyCap: Int = config.maxDifficulty
        private set

    val currentBaseline: DriverBaseline? get() = baseline

    fun isSlow(latencyMs: Long, baselineMs: Double): Boolean =
        latencyMs > max(baselineMs * config.slowRatio, baselineMs + config.slowExtraMs)

    /** @param nowMs 쿨다운 계산용 시각(벽시계 ms) */
    fun record(sample: ResponseSample, nowMs: Long): Action {
        val base = baseline
        if (base == null) {
            // 무응답(타임아웃)은 평소 반응을 대표하지 않으므로 캘리브레이션에 넣지 않는다 (마이크 미연결 등)
            if (sample.latencyMs >= config.timeoutMs) return Action.None
            calibration += sample
            if (calibration.size >= config.calibrationSamples) {
                val sorted = calibration.map { it.latencyMs }.sorted()
                val median = sorted[sorted.size / 2].toDouble()
                val answered = calibration.mapNotNull { it.correct }
                val accuracy = if (answered.isEmpty()) 1.0 else answered.count { it } / answered.size.toDouble()
                baseline = DriverBaseline(median, accuracy, calibration.size)
                calibration.clear()
            }
            return Action.None
        }

        window.addLast(sample)
        while (window.size > config.windowSize) window.removeFirst()

        val slow = isSlow(sample.latencyMs, base.latencyEmaMs)
        val alpha = config.emaAlpha
        baseline = base.copy(
            latencyEmaMs = if (slow) base.latencyEmaMs else base.latencyEmaMs * (1 - alpha) + sample.latencyMs * alpha,
            accuracyEma = sample.correct?.let { base.accuracyEma * (1 - alpha) + (if (it) 1.0 else 0.0) * alpha }
                ?: base.accuracyEma,
            sampleCount = base.sampleCount + 1,
        )

        val reference = baseline!!.latencyEmaMs
        val slowCount = window.count { isSlow(it.latencyMs, reference) }
        val cooling = lastTriggerAtMs?.let { nowMs - it < config.cooldownMs } == true
        val stage1Due = slowCount >= config.slowTrigger && !cooling

        // Stage2 판단 (Stage1 보다 먼저)
        val stage2Cooling = lastStage2AtMs?.let { nowMs - it < config.stage2CooldownMs } == true
        val repeated = stage1Due && stage1Times.any { nowMs - it <= config.stage2WindowMs }
        val answered = window.mapNotNull { it.correct }
        val inaccurate = window.size >= config.windowSize && answered.isNotEmpty() &&
            answered.count { it } / answered.size.toDouble() < config.lowAccuracy
        val slowAndInaccurate = slow && inaccurate
        if (!stage2Cooling && (repeated || slowAndInaccurate)) {
            return triggerStage2(nowMs, if (repeated) Stage2Reason.REPEATED_STAGE1 else Stage2Reason.SLOW_AND_INACCURATE)
        }

        if (stage1Due) {
            lastTriggerAtMs = nowMs
            stage1Count++
            stage1Times.addLast(nowMs)
            while (stage1Times.size > 4) stage1Times.removeFirst()
            difficultyCap = (difficultyCap - 1).coerceAtLeast(1)
            return Action.Stage1(difficultyCap)
        }
        return Action.None
    }

    /** 디버그 패널 "2단계 시험" — 규칙과 상관없이 Stage2 를 낸다 */
    fun forceStage2(nowMs: Long): Action.Stage2 = triggerStage2(nowMs, Stage2Reason.MANUAL)

    private fun triggerStage2(nowMs: Long, reason: Stage2Reason): Action.Stage2 {
        lastStage2AtMs = nowMs
        lastTriggerAtMs = nowMs // 바로 이어서 Stage1 이 또 걸리지 않게
        stage2Count++
        difficultyCap = (difficultyCap - 1).coerceAtLeast(1)
        return Action.Stage2(difficultyCap, reason)
    }

    /** 졸음쉼터에서 쉬고 다시 출발: 최근 응답 창과 난이도 상한을 처음으로 (기준값은 유지) */
    fun resetAfterRest() {
        window.clear()
        stage1Times.clear()
        difficultyCap = config.maxDifficulty
        lastTriggerAtMs = null
    }

    fun snapshot(): Snapshot {
        val base = baseline
        val reference = base?.latencyEmaMs
        return Snapshot(
            baselineMs = reference?.toLong(),
            baselineAccuracy = base?.accuracyEma,
            calibrating = base == null,
            calibrationCount = calibration.size,
            recentLatencies = window.map { it.latencyMs },
            slowCount = if (reference == null) 0 else window.count { isSlow(it.latencyMs, reference) },
            difficultyCap = difficultyCap,
            lastTriggerAtMs = lastTriggerAtMs,
            stage1Count = stage1Count,
            stage2Count = stage2Count,
            lastStage2AtMs = lastStage2AtMs,
        )
    }

    /** 세션 복원용 */
    fun restore(window: List<ResponseSample>, difficultyCap: Int, lastTriggerAtMs: Long?, stage1Count: Int) {
        this.window.clear()
        this.window.addAll(window.takeLast(config.windowSize))
        this.difficultyCap = difficultyCap.coerceIn(1, config.maxDifficulty)
        this.lastTriggerAtMs = lastTriggerAtMs
        this.stage1Count = stage1Count
    }

    val windowSamples: List<ResponseSample> get() = window.toList()
}
