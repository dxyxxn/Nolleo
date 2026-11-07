package com.nolleo.app.domain.games

import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.domain.engine.AnswerMatcher
import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.Player
import com.nolleo.app.domain.model.Question
import kotlin.random.Random

/**
 * 지역 퀴즈: 현재 regionCode 의 미출제 문제 → 없으면 인접 지역 → 없으면 일반 퀴즈.
 * 난이도는 DrowsinessMonitor 가 내린 상한 이하에서 고른다. 지역이 바뀌면 "이제 충청북도예요!" 한 줄 안내.
 * 위치를 모르면 일반 가족 퀴즈.
 */
class RegionQuizGame(
    private val content: GameContent,
    private val random: Random = Random.Default,
) : VoiceGame {

    override val kind = GameKind.REGION_QUIZ

    private val asked = linkedSetOf<String>()
    private var lastRegion: String? = null
    private val allQuestions: Map<String, Question> =
        (content.regionQuestions.values.flatten() + content.generalQuestions).associateBy { it.id }

    override fun rotation(players: List<Player>): List<Player> = players

    override fun intro(players: List<Player>, questionCount: Int): String =
        "지역 퀴즈를 시작할게요! 지나가는 곳에 대한 문제예요."

    override fun nextTurn(ctx: TurnContext): Turn? {
        val region = ctx.regionCode?.takeIf { content.regionQuestions.containsKey(it) }
        val question = pick(region, ctx.difficultyCap) ?: return null
        asked += question.id

        val announcement = when {
            region == null -> null
            region != lastRegion -> {
                val name = content.region(region)?.name ?: region
                if (lastRegion == null) "지금은 ${Hangul.ieyo(name)}!" else "이제 ${Hangul.ieyo(name)}!"
            }
            else -> null
        }
        if (region != null) lastRegion = region

        val prompt = listOfNotNull(announcement, "${ctx.player.name} 차례예요!", question.text).joinToString(" ")
        val regionShort = question.regionCode?.let { content.region(it)?.shortName }
        val shortDisplay = if (regionShort != null) "$regionShort ${question.category} 문제" else "가족 퀴즈"
        return Turn(
            player = ctx.player,
            prompt = prompt,
            display = question.text,
            shortDisplay = shortDisplay,
            hint = ListenHint(correctAnswer = question.answers.first(), wrongAnswer = wrongAnswerFor(question), forDriver = ctx.player.isDriver),
            questionId = question.id,
            difficulty = question.difficulty,
        )
    }

    /** 복원된 세션에서 같은 문제를 다시 내기 위한 차례 */
    fun turnFor(questionId: String, player: Player): Turn? {
        val q = allQuestions[questionId] ?: return null
        return Turn(
            player = player,
            prompt = "${player.name} 차례예요! ${q.text}",
            display = q.text,
            shortDisplay = "퀴즈",
            hint = ListenHint(q.answers.first(), wrongAnswerFor(q), forDriver = player.isDriver),
            questionId = q.id,
            difficulty = q.difficulty,
        )
    }

    private fun pick(region: String?, cap: Int): Question? {
        fun pool(list: List<Question>, respectCap: Boolean) =
            list.filter { it.id !in asked && (!respectCap || it.difficulty <= cap) }

        val regional = region?.let { content.regionQuestions[it] }.orEmpty()
        val neighbors = region?.let { content.region(it)?.neighbors }.orEmpty()
            .flatMap { content.regionQuestions[it].orEmpty() }
        val candidates = sequenceOf(
            pool(regional, true),
            pool(neighbors, true),
            pool(content.generalQuestions, true),
            pool(regional + neighbors + content.generalQuestions, false),
        ).firstOrNull { it.isNotEmpty() }
        if (candidates != null) return candidates.random(random)
        // 모두 출제했으면 다시 처음부터 (긴 여정)
        if (asked.isNotEmpty()) {
            asked.clear()
            return pick(region, cap)
        }
        return null
    }

    private fun wrongAnswerFor(q: Question): String {
        val others = allQuestions.values.filter { it.id != q.id && it.answers.none { a -> a in q.answers } }
        return others.randomOrNull(random)?.answers?.first() ?: "모르겠어요"
    }

    override fun judge(turn: Turn, answer: String?, attempt: Int): Judgement {
        val q = turn.questionId?.let { allQuestions[it] }
            ?: return Judgement(Outcome.NO_ANSWER, "다음 문제로 갈게요.", correct = null, label = "건너뜀")
        val canonical = q.answers.first()
        return when {
            answer.isNullOrBlank() -> Judgement(
                Outcome.NO_ANSWER,
                "시간이 다 됐어요. 정답은 ${Hangul.ieyo(canonical)}.",
                correct = false,
                label = "무응답 · 정답 $canonical",
            )
            AnswerMatcher.matches(answer, q.answers) -> Judgement(
                Outcome.CORRECT,
                "딩동댕! ${canonical}, 정답이에요!",
                scoreDelta = 10,
                correct = true,
                label = "정답!",
            )
            else -> Judgement(
                Outcome.WRONG,
                "아쉬워요. 정답은 ${Hangul.ieosseoyo(canonical)}.",
                correct = false,
                label = "오답 · 정답 $canonical",
            )
        }
    }

    override fun outro(): String = "지역 퀴즈 한 판이 끝났어요!"

    override fun saveState(): Map<String, String> = mapOf(
        "asked" to asked.joinToString(","),
        "lastRegion" to (lastRegion ?: ""),
    )

    override fun restoreState(state: Map<String, String>) {
        asked.clear()
        state["asked"]?.split(",")?.filter { it.isNotBlank() }?.let { asked += it }
        lastRegion = state["lastRegion"]?.takeIf { it.isNotBlank() }
    }
}
