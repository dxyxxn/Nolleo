package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.IdiomItem
import kotlin.random.Random

/**
 * 정차 중 화면 게임 "사자성어" — 뜻을 보고(듣고) 네 개 중에서 사자성어를 고른다. 힌트는 쓰지 않는다.
 *
 *  - 한 바퀴를 다 돌 때까지 같은 문제를 다시 내지 않는다.
 *  - 오답 보기 3개는 정답과 뜻(질문)이 같은 사자성어를 빼고 고른다 (같은 뜻을 가진 동의어가 정답처럼 보이지 않게).
 *    난이도가 같은 것에서 먼저 고르고, 모자라면 전체에서 채운다.
 *  - 맞히면 10점.
 */
class IdiomQuizGame(private val items: List<IdiomItem>, private val random: Random = Random.Default) {

    data class IdiomQuestion(val item: IdiomItem, val choices: List<IdiomItem>, val answerIndex: Int) {
        val answer: IdiomItem get() = choices[answerIndex]
    }

    private val asked = mutableSetOf<String>()

    var score: Int = 0
        private set
    var answered: Int = 0
        private set
    var correct: Int = 0
        private set

    val isAvailable: Boolean get() = items.size >= CHOICES

    fun next(): IdiomQuestion? {
        if (!isAvailable) return null
        val pool = items.filter { it.id !in asked }.ifEmpty {
            asked.clear()
            items
        }
        val item = pool.random(random)
        asked += item.id
        val candidates = items.filter { it.id != item.id && it.word != item.word && it.question != item.question }
        val sameLevel = candidates.filter { it.difficulty == item.difficulty }.shuffled(random)
        val distractors = (sameLevel + candidates.shuffled(random)).distinctBy { it.word }.take(CHOICES - 1)
        if (distractors.size < CHOICES - 1) return null
        val choices = (distractors + item).shuffled(random)
        return IdiomQuestion(item, choices, choices.indexOf(item))
    }

    /** @return 맞혔으면 true */
    fun answer(question: IdiomQuestion, choiceIndex: Int): Boolean {
        answered++
        val ok = choiceIndex == question.answerIndex
        if (ok) {
            correct++
            score += POINTS
        }
        return ok
    }

    companion object {
        const val CHOICES = 4
        const val POINTS = 10
    }
}
