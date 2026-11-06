package com.gildongmu.app.domain.engine

import com.gildongmu.app.core.speech.ListenHint
import com.gildongmu.app.core.speech.SttEvent
import com.gildongmu.app.domain.drowsy.ResponseSample
import com.gildongmu.app.domain.model.DriveState
import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.GameKind
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.domain.model.Player
import com.gildongmu.app.domain.model.SessionPlan
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
    val driveState: StateFlow<DriveState>,
)

interface SessionCallbacks {
    fun onVoiceRoundActive(active: Boolean)
    fun onStage1(driver: Player, newDifficultyCap: Int)
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
    /** 도착 전에 사용자가 "게임 끝내기"로 끝냄 */
    val endedByUser: Boolean = false,
) {
    val totalCorrect: Int get() = correctCounts.values.sum()
    val winner: Player? get() = players.maxByOrNull { scores[it.id] ?: 0 }?.takeIf { (scores[it.id] ?: 0) > 0 }
}

/** 프로세스 재생성·백그라운드 복귀 대비 저장본 (지시서 §9 상태 복원) */
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
    val drowsyWindow: List<ResponseSample>,
    val difficultyCap: Int,
    val lastTriggerAtMs: Long?,
    val stage1Count: Int,
    val savedAtWallMs: Long,
)
