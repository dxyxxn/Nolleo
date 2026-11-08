package com.nolleo.app.domain.engine

import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.core.speech.SttEvent
import com.nolleo.app.domain.drowsy.DrowsinessMonitor
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.SessionPlan
import com.nolleo.app.domain.rest.RestStopSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** GameSession 이 보는 음성 입출력 (SpeechRouter 를 감싼다; 테스트는 가짜를 넣는다) */
interface SpeechPort {
    suspend fun speak(text: String)
    fun listen(timeoutMs: Long, hint: ListenHint): Flow<SttEvent>
    fun stop()
    fun markNewStream()
}

/** GameSession 의 입력 — 모두 앱 계층에서 차량·내비·위치 SDK 로부터 만들어진다 */
class SessionInputs(
    val mode: StateFlow<GameMode>,
    val screenAvailable: StateFlow<Boolean>,
    val remainSec: StateFlow<Int?>,
    val regionCode: StateFlow<String?>,
)

interface SessionCallbacks {
    fun onVoiceRoundActive(active: Boolean)
    fun onStage1(driver: Player, newDifficultyCap: Int)

    /** 졸음 케어 2단계 — 권할 쉼 장소 (진행 방향 앞쪽 졸음쉼터 / 내비 경로 위 쉼터). 없으면 null */
    fun suggestRestStop(): RestStopSuggestion? = null

    /** 앞서 받아들인 쉼 장소로 가는 중이면 그곳 — 2단계가 다시 걸려도 새로 묻지 않고 그곳을 다시 알린다 */
    fun headingRestStop(): RestStopSuggestion? = null

    /** 2단계가 걸려 휴식을 권하기 시작함 (화면 카드 표시) */
    fun onStage2(driver: Player, suggestion: RestStopSuggestion?) = Unit

    /** 휴식 권유에 대한 답: 수락이면 게임을 쉬고 도착 감지를 시작한다 */
    fun onRestDecision(suggestion: RestStopSuggestion?, accepted: Boolean) = Unit

    /** 졸음쉼터 도착 안내를 말했다 — 다시 출발할 때까지 게임을 쉬게 한다 (화면은 일시정지로) */
    fun onRestHold() = Unit
    fun onSnapshot(snapshot: SessionSnapshot)
    fun onFinished(summary: SessionSummary)
    fun log(message: String)
    fun logSensitive(label: String, value: String)
}

enum class SessionPhase(val label: String) {
    WAITING("출발을 기다리는 중"),
    RESTING("쉬는 시간"),
    INTRO("판 시작"),
    ASKING("문제 내는 중"),
    LISTENING("듣는 중"),
    FEEDBACK("결과 안내"),
    ROUND_END("판 마무리"),
    ALL_DONE("오늘 준비한 판 완료"),
    FINISHING("결과 정리"),
    FINISHED("끝"),
}

enum class MicState { IDLE, LISTENING, SPEAKING }

data class AnswerLine(val playerName: String, val heard: String?, val verdict: String)

data class SessionState(
    val phase: SessionPhase = SessionPhase.WAITING,
    val plan: SessionPlan,
    val roundIndex: Int = 0,
    val roundGame: GameKind? = null,
    val turnInRound: Int = 0,
    val turnsInRound: Int = 0,
    val currentPlayer: Player? = null,
    val display: String? = null,
    val shortDisplay: String? = null,
    val caption: String? = null,
    val mic: MicState = MicState.IDLE,
    val partial: String? = null,
    val lastAnswer: AnswerLine? = null,
    val scores: Map<Int, Int> = emptyMap(),
    val correctCounts: Map<Int, Int> = emptyMap(),
    val nextRoundAtRemainSec: Int? = null,
    val regionsVisited: List<String> = emptyList(),
    val roundsPlayed: List<GameKind> = emptyList(),
    val difficultyCap: Int = 3,
)

data class SessionSummary(
    val destinationName: String,
    val players: List<Player>,
    val scores: Map<Int, Int>,
    val correctCounts: Map<Int, Int>,
    val regionsVisited: List<String>,
    val roundsPlayed: List<GameKind>,
    val stage1Count: Int,
    val longestChain: Int,
    /** 졸음 케어 2단계로 쉬어 가기를 권한 횟수 */
    val stage2Count: Int = 0,
    /** 도착 전에 사용자가 "게임 끝내기"로 끝냄 */
    val endedByUser: Boolean = false,
) {
    val totalCorrect: Int get() = correctCounts.values.sum()
    val winner: Player? get() = players.maxByOrNull { scores[it.id] ?: 0 }?.takeIf { (scores[it.id] ?: 0) > 0 }
}

/**
 * 프로세스 재생성·백그라운드 복귀 대비 저장본 (상태 복원).
 * 쉬어 가기 진행(가는 중·쉬는 중)은 넣지 않는다 — 이어서 하기는 쉬어 가기를 처음 상태로 시작한다(11/8 결정).
 */
data class SessionSnapshot(
    val destinationName: String,
    val presetId: String?,
    val players: List<Player>,
    val choice: GameChoice,
    val demo: Boolean,
    val plan: SessionPlan,
    val roundIndex: Int,
    val turnInRound: Int,
    val rotationIndex: Int,
    val scores: Map<Int, Int>,
    val correctCounts: Map<Int, Int>,
    val regionsVisited: List<String>,
    val roundsPlayed: List<GameKind>,
    val quizState: Map<String, String>,
    val chainState: Map<String, String>,
    val pendingQuestionId: String?,
    val drowsy: DrowsinessMonitor.SavedState,
    /** 끝말잇기 최장(다른 말로 다시 시작한 전후 합산) — 요약에 쓴다 */
    val longestChain: Int = 0,
    /** 운전자 프로필 id. 이 필드가 없는 v1 저장본은 null → 운전자 이름으로 찾는다 */
    val driverProfileId: Int? = null,
    /** 저장할 때 시뮬 여정의 남은 시간(초) — 이어서 하기 때 그 지점부터. 실제 내비 여정이면 null */
    val simRemainSec: Int? = null,
    val savedAtWallMs: Long,
)
