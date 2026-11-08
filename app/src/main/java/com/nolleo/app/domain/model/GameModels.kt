package com.nolleo.app.domain.model

enum class GameKind(val label: String) {
    REGION_QUIZ("지역 퀴즈"),
    WORD_CHAIN("끝말잇기"),
}

/** 홈에서 고르는 게임 구성. */
enum class GameChoice(val label: String) {
    QUIZ("지역 퀴즈"),
    WORD_CHAIN("끝말잇기"),
    ALTERNATE("번갈아"),

    /** 판마다 지역 퀴즈·끝말잇기 중 무작위 — 같은 게임이 세 판 넘게 이어지지 않는다 ([SessionPlan.gameSeed]) */
    RANDOM("랜덤"),
}

/**
 * 홈에서 고르는 판 간격 — 여정에 판을 얼마나 촘촘히 놓을지. [slotSec] 마다 한 판(판 시작 간격의 목표값).
 * 판 사이에는 말을 걸지 않는다. 쉬지 않고 이어 해도 최소 휴지(2분)는 남긴다.
 */
enum class RoundPacing(val label: String, val slotSec: Int) {
    /** 약 45분마다 한 판 */
    RELAXED("여유 있게", 45 * 60),
    /** 약 20분마다 한 판 */
    OFTEN("자주", 20 * 60),
    /** 판 사이 최소 휴지(2분)만 */
    NONSTOP("쉬지 않고", 12 * 60),
}

/** 운전자 1 + 동승자 0~3. 턴은 라운드 로빈. */
data class Player(val id: Int, val name: String, val isDriver: Boolean) {
    companion object {
        /** 끝말잇기에서 앱(놀레오)도 한 차례를 맡는다. */
        val APP = Player(id = 0, name = "놀레오", isDriver = false)
    }
}

data class Question(
    val id: String,
    val regionCode: String?,
    val difficulty: Int,
    val category: String,
    val text: String,
    val answers: List<String>,
    val hint: String,
)

data class RoundPlan(
    val index: Int,
    val game: GameKind,
    val targetDurationSec: Int,
    val questionCount: Int,
    /** 남은 시간이 이 값 이하가 되면 이 판을 시작한다 (첫 판은 즉시). */
    val startAtRemainSec: Int,
    val isShort: Boolean = false,
)

data class SessionPlan(
    val rounds: List<RoundPlan>,
    /** 플랜을 만들 때 기준으로 삼은 남은 시간 */
    val basisRemainSec: Int,
    /** 판 간격 — 남은 시간이 바뀌어 다시 짤 때도 같은 간격을 쓴다 */
    val pacing: RoundPacing = RoundPacing.RELAXED,
    /** "랜덤" 게임 순서의 씨앗 — 다시 짜거나 이어서 해도 판 번호별 게임이 그대로다 */
    val gameSeed: Long = 0L,
) {
    val roundCount: Int get() = rounds.size
}
