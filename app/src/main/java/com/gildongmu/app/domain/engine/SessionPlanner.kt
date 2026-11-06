package com.gildongmu.app.domain.engine

import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameKind
import com.gildongmu.app.domain.model.RoundPlan
import com.gildongmu.app.domain.model.SessionPlan
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 남은 이동 시간 → 오늘의 게임 구성 (지시서 §3 세션 플랜).
 *  - 판 길이 6~10분, 판 사이 최소 2분 휴지(말 안 거는 시간), 마지막 판은 도착 5분 전에 끝난다
 *  - 약 45분마다 한 판 (4시간 → 5판). 판은 "남은 시간이 startAtRemainSec 이하가 되면" 시작한다
 *  - remainTime < 12분이면 "짧은 판" 1개만
 *  - remainTime 이 기대값 대비 ±20% 이상 바뀌면 남은 판만 다시 계산(진행 중인 판은 유지)
 * 실제 진행은 문제 수(questionCount) 기준이며, 문제당 약 30초(출제·응답·결과)로 환산한다.
 */
object SessionPlanner {
    const val END_BUFFER_SEC = 5 * 60
    const val SHORT_TRIP_SEC = 12 * 60
    const val SLOT_SEC = 45 * 60
    const val MIN_ROUND_SEC = 6 * 60
    const val MAX_ROUND_SEC = 10 * 60
    const val MIN_GAP_SEC = 2 * 60
    const val SEC_PER_QUESTION = 30
    const val DEMO_QUESTION_COUNT = 6
    const val REPLAN_THRESHOLD = 0.2

    fun plan(remainSec: Int, choice: GameChoice, demo: Boolean = false): SessionPlan {
        if (remainSec < SHORT_TRIP_SEC) {
            val duration = (remainSec - END_BUFFER_SEC).coerceIn(120, MIN_ROUND_SEC)
            val count = if (demo) DEMO_QUESTION_COUNT else (duration / SEC_PER_QUESTION).coerceIn(3, 6)
            val round = RoundPlan(
                index = 0,
                game = gameFor(0, choice),
                targetDurationSec = duration,
                questionCount = count,
                startAtRemainSec = remainSec,
                isShort = true,
            )
            return SessionPlan(listOf(round), basisRemainSec = remainSec)
        }
        val available = remainSec - END_BUFFER_SEC
        val n = (available.toDouble() / SLOT_SEC).roundToInt().coerceAtLeast(1)
        val slot = available / n
        var duration = (slot / 4).coerceIn(MIN_ROUND_SEC, MAX_ROUND_SEC)
        if (n == 1) duration = duration.coerceAtMost(available)
        if (n > 1 && slot - duration < MIN_GAP_SEC) duration = (slot - MIN_GAP_SEC).coerceAtLeast(MIN_ROUND_SEC)
        val count = if (demo) DEMO_QUESTION_COUNT else duration / SEC_PER_QUESTION
        val rounds = (0 until n).map { i ->
            RoundPlan(
                index = i,
                game = gameFor(i, choice),
                targetDurationSec = duration,
                questionCount = count,
                startAtRemainSec = remainSec - i * slot,
            )
        }
        return SessionPlan(rounds, basisRemainSec = remainSec)
    }

    /** 기대 남은 시간 대비 실제 값이 ±20% 이상 벗어났는가 */
    fun needsReplan(expectedRemainSec: Int, actualRemainSec: Int): Boolean {
        if (expectedRemainSec <= 0) return actualRemainSec > SHORT_TRIP_SEC
        return abs(actualRemainSec - expectedRemainSec).toDouble() / expectedRemainSec >= REPLAN_THRESHOLD
    }

    /**
     * 진행이 끝났거나 진행 중인 판([keepCount] 개)은 그대로 두고, 나머지만 [actualRemainSec] 기준으로 다시 만든다.
     * 새 첫 판도 최소 휴지(2분) 뒤에 시작한다.
     */
    fun replanRemaining(
        plan: SessionPlan,
        keepCount: Int,
        actualRemainSec: Int,
        choice: GameChoice,
        demo: Boolean = false,
    ): SessionPlan {
        val kept = plan.rounds.take(keepCount)
        val usable = actualRemainSec - END_BUFFER_SEC - MIN_GAP_SEC
        if (usable < MIN_ROUND_SEC) return SessionPlan(kept, basisRemainSec = actualRemainSec)
        val n = (usable.toDouble() / SLOT_SEC).roundToInt().coerceAtLeast(1)
        val slot = usable / n
        val duration = (slot / 4).coerceIn(MIN_ROUND_SEC, MAX_ROUND_SEC).coerceAtMost(slot)
        val count = if (demo) DEMO_QUESTION_COUNT else duration / SEC_PER_QUESTION
        val fresh = (0 until n).map { j ->
            val index = keepCount + j
            RoundPlan(
                index = index,
                game = gameFor(index, choice),
                targetDurationSec = duration,
                questionCount = count,
                startAtRemainSec = actualRemainSec - MIN_GAP_SEC - j * slot,
            )
        }
        return SessionPlan(kept + fresh, basisRemainSec = actualRemainSec)
    }

    /**
     * 다음 판까지 남은 여정 시간(초). 판은 "도착까지 남은 시간이 [startAtRemainSec] 이하"가 되면 시작하므로
     * 사용자에게는 그 차이를 "약 N분 뒤"로 보여 준다. 기준을 모르면 null, 이미 지났으면 0.
     */
    fun secondsUntilNextRound(remainSec: Int?, startAtRemainSec: Int?): Int? {
        if (remainSec == null || startAtRemainSec == null) return null
        return (remainSec.toLong() - startAtRemainSec.toLong()).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    fun gameFor(index: Int, choice: GameChoice): GameKind = when (choice) {
        GameChoice.QUIZ -> GameKind.REGION_QUIZ
        GameChoice.WORD_CHAIN -> GameKind.WORD_CHAIN
        GameChoice.ALTERNATE -> if (index % 2 == 0) GameKind.REGION_QUIZ else GameKind.WORD_CHAIN
    }

    /** 홈 화면 미리보기 문구: "4시간 10분 → 5판" */
    fun previewText(remainSec: Int, plan: SessionPlan, format: (Int) -> String): String {
        val rounds = plan.roundCount
        val minutes = plan.rounds.firstOrNull()?.targetDurationSec?.div(60) ?: 0
        val short = plan.rounds.firstOrNull()?.isShort == true
        return if (short) "${format(remainSec)} → 짧은 판 1개" else "${format(remainSec)} → ${rounds}판 (판당 약 ${minutes}분)"
    }
}
