package com.nolleo.app.domain.games

import com.nolleo.app.domain.engine.AnswerMatcher
import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.domain.model.IdiomItem
import com.nolleo.app.domain.model.Question
import kotlin.random.Random

/**
 * 정차 중 보기 고르기 게임(사자성어·지역 퀴즈) 한 문제 — "읽어 주고 보기 중에서 고르기".
 * 주행 중 음성 판과는 따로 논다(점수·진행이 음성 판에 이어지지 않음). 정차 중 끝말잇기는 말로 잇는다(WordChainPlay).
 */
data class ScreenQuestion(
    val id: String,
    /** 문제 위 작은 글 (예: "다음 뜻을 가진 사자성어는?", "지금 지나는 곳 · 경기도") */
    val lead: String,
    /** 크게 보여 줄 문제 */
    val prompt: String,
    val choices: List<String>,
    val answerIndex: Int,
    /** 문제를 읽어 줄 문장 */
    val speech: String,
    /** 고른 뒤 결과 아래 한 줄 (지역 퀴즈 힌트) */
    val note: String? = null,
    /** 사자성어면 한자 훈·음·유래 풀이 */
    val idiom: IdiomItem? = null,
) {
    val answer: String get() = choices[answerIndex]
}

/** 정차 중 화면 게임 하나의 규칙 */
interface ScreenGame {
    val score: Int
    val answered: Int
    val correct: Int

    /** 고른 때부터 결과를 보여 주는 최소 시간 (그 뒤 저절로 다음 문제) */
    val revealMinMs: Long

    fun next(): ScreenQuestion?

    /** @return 맞혔으면 true */
    fun answer(question: ScreenQuestion, index: Int): Boolean

    /** 고른 뒤 읽어 줄 말 */
    fun afterAnswer(question: ScreenQuestion, correct: Boolean): List<String>

    /** 상태 줄에 덧붙일 진행 */
    fun progress(): String? = null

    companion object {
        const val POINTS = 10
    }
}

/** 점수 셈은 게임마다 같다 */
abstract class ScoredScreenGame : ScreenGame {
    final override var score: Int = 0
        private set
    final override var answered: Int = 0
        private set
    final override var correct: Int = 0
        private set

    protected fun record(ok: Boolean): Boolean {
        answered++
        if (ok) {
            correct++
            score += ScreenGame.POINTS
        }
        return ok
    }
}

/** 사자성어 — [IdiomQuizGame] 을 화면 게임 틀에 맞춘다 */
class IdiomScreenGame(items: List<IdiomItem>, random: Random = Random.Default) : ScreenGame {
    private val game = IdiomQuizGame(items, random)
    private var last: IdiomQuizGame.IdiomQuestion? = null

    override val score: Int get() = game.score
    override val answered: Int get() = game.answered
    override val correct: Int get() = game.correct
    override val revealMinMs: Long = 10_000L

    val isAvailable: Boolean get() = game.isAvailable

    override fun next(): ScreenQuestion? {
        val q = game.next() ?: return null
        last = q
        return ScreenQuestion(
            id = q.item.id,
            lead = IdiomItem.QUESTION_LEAD,
            prompt = q.item.meaning,
            choices = q.choices.map { it.word },
            answerIndex = q.answerIndex,
            speech = IdiomSpeech.question(q.item),
            idiom = q.item,
        )
    }

    override fun answer(question: ScreenQuestion, index: Int): Boolean {
        val q = last?.takeIf { it.item.id == question.id } ?: return false
        return game.answer(q, index)
    }

    override fun afterAnswer(question: ScreenQuestion, correct: Boolean): List<String> =
        question.idiom?.let { IdiomSpeech.afterAnswer(it, correct) }.orEmpty()
}

/**
 * 지역 퀴즈(정차 중) — 지금 있는 시/도의 문제를 화면에서 고른다. 위치를 모르면 아무 지역에서.
 *  - "부산일까요, 서울일까요?" 처럼 문제에 두 보기가 들어 있으면 그 두 개만 보기로
 *  - 아니면 정답 + 같은 갈래(음식·지명·문화·특산물) 다른 문제의 정답 3개 = 4지선다
 *  - 고른 뒤 힌트 문장을 읽어 준다(재미있는 설명으로)
 */
class RegionScreenQuiz(
    private val content: GameContent,
    private val currentRegion: () -> String?,
    private val random: Random = Random.Default,
) : ScoredScreenGame() {

    override val revealMinMs: Long = 5_000L
    private val asked = mutableSetOf<String>()
    private val hints = mutableMapOf<String, String>()

    val isAvailable: Boolean get() = content.regionQuestions.values.any { it.isNotEmpty() }

    override fun next(): ScreenQuestion? {
        val here = currentRegion()?.takeIf { content.regionQuestions[it].orEmpty().isNotEmpty() }
        val pool = if (here != null) content.regionQuestions[here].orEmpty() else content.regionQuestions.values.flatten()
        if (pool.isEmpty()) return null
        val fresh = pool.filter { it.id !in asked }.ifEmpty {
            asked.removeAll(pool.map { it.id }.toSet())
            pool
        }
        val q = fresh.random(random)
        asked += q.id
        val answer = q.answers.first()
        val pair = twoChoices(q)
        val choices = pair ?: (distractors(q, CHOICES - 1) + answer).shuffled(random)
        val answerIndex = choices.indexOfFirst { matches(q, it) }.takeIf { it >= 0 } ?: return null
        val regionName = content.region(q.regionCode)?.name
        q.hint.takeIf { it.isNotBlank() }?.let { hints[q.id] = it }
        return ScreenQuestion(
            id = q.id,
            lead = if (here != null && regionName != null) "지금 지나는 곳 · $regionName" else "${regionName ?: "우리나라"} 퀴즈",
            prompt = q.text,
            choices = choices,
            answerIndex = answerIndex,
            speech = q.text,
            note = q.hint.takeIf { it.isNotBlank() },
        )
    }

    override fun answer(question: ScreenQuestion, index: Int): Boolean = record(index == question.answerIndex)

    override fun afterAnswer(question: ScreenQuestion, correct: Boolean): List<String> = listOfNotNull(
        if (correct) "딩동댕! ${Hangul.ieyo(question.answer)}." else "아쉬워요. 정답은 ${Hangul.ieyo(question.answer)}.",
        hints[question.id],
    )

    private fun matches(q: Question, choice: String): Boolean =
        q.answers.any { AnswerMatcher.normalize(it) == AnswerMatcher.normalize(choice) }

    /** "…부산일까요, 서울일까요?" → [부산, 서울] (둘 중 하나가 정답일 때만) */
    private fun twoChoices(q: Question): List<String>? {
        val m = TWO_CHOICES.find(q.text) ?: return null
        val options = listOf(m.groupValues[1], m.groupValues[2])
        return options.takeIf { o -> o.count { matches(q, it) } == 1 }
    }

    /** 같은 갈래의 다른 문제 정답 — 문제 글에 이미 나온 말·정답과 같은 말은 뺀다 */
    private fun distractors(q: Question, count: Int): List<String> {
        val all = content.regionQuestions.values.flatten().filter { it.id != q.id }
        fun usable(o: Question) = o.answers.first().let { a -> !matches(q, a) && a !in q.text && a.length <= 8 }
        val sameCategory = all.filter { it.category == q.category && usable(it) }.shuffled(random)
        val others = all.filter { it.category != q.category && usable(it) }.shuffled(random)
        return (sameCategory + others).map { it.answers.first() }.distinct().take(count)
    }

    companion object {
        const val CHOICES = 4
        private val TWO_CHOICES = Regex("(\\S+)일까요,\\s*(\\S+)일까요\\?\\s*$")
    }
}
