package com.nolleo.app.domain.games

import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.Player

/** 한 차례에 필요한 정보 */
data class TurnContext(
    val player: Player,
    /** 이 차례 다음 사람 (끝말잇기에서 앱이 다음 사람을 지목할 때) */
    val nextPlayer: Player,
    val regionCode: String?,
    /** 졸음 케어가 내린 난이도 상한 (1~3) */
    val difficultyCap: Int,
)

/** 한 차례 = 출제(TTS) → (필요하면) 듣기 */
data class Turn(
    val player: Player,
    /** TTS 문장. 비어 있으면 말하지 않고 바로 듣는다 */
    val prompt: String,
    /** 화면 표시 (≤ 3줄) */
    val display: String,
    /** LIMIT_STRING_LENGTH 규제 시 1줄 요약 */
    val shortDisplay: String,
    val hint: ListenHint = ListenHint(),
    val expectsAnswer: Boolean = true,
    /** 이 차례가 끝나면 판을 끝낸다 (예: 앱이 이을 단어가 없음) */
    val endsRoundAfter: Boolean = false,
    val questionId: String? = null,
    val difficulty: Int = 1,
    /** 일시정지 후 다시 낼 때 쓰는 문장 (prompt 가 비어 있는 차례 대비) */
    val reprompt: String = "",
)

enum class Outcome { CORRECT, WRONG, NO_ANSWER, RETRY, ACCEPTED_LENIENT }

data class Judgement(
    val outcome: Outcome,
    /** TTS 결과 안내 */
    val feedback: String,
    val scoreDelta: Int = 0,
    /** 졸음 지표용 정답 여부 (RETRY 는 null) */
    val correct: Boolean?,
    /** 화면에 남길 판정 한 줄 */
    val label: String,
    val endsRound: Boolean = false,
)

/** 음성 게임 공통 인터페이스 (지역 퀴즈 · 끝말잇기) */
interface VoiceGame {
    val kind: GameKind

    /** 이 게임에서 차례를 도는 순서 */
    fun rotation(players: List<Player>): List<Player>

    fun intro(players: List<Player>, questionCount: Int): String

    /** null = 더 낼 차례가 없음 (판 종료) */
    fun nextTurn(ctx: TurnContext): Turn?

    /** attempt 는 1부터. RETRY 를 돌려주면 세션이 한 번 더 듣는다. */
    fun judge(turn: Turn, answer: String?, attempt: Int): Judgement

    fun outro(): String

    /** 상태 저장용 (프로세스 재생성 복원) */
    fun saveState(): Map<String, String>
    fun restoreState(state: Map<String, String>)
}
