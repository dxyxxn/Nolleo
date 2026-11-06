package com.gildongmu.app.domain.games

import com.gildongmu.app.domain.model.ChosungItem
import kotlin.random.Random

/**
 * 정차 중(SCREEN 모드)에만 열리는 화면 게임: 초성 보고 4지선다 터치 (P1).
 * 주행이 시작되면 화면이 즉시 닫힌다 (UI 가 모드를 보고 처리).
 */
class ChosungQuizGame(
    private val items: List<ChosungItem>,
    private val random: Random = Random.Default,
) {
    data class ChosungQuestion(
        val item: ChosungItem,
        val chosung: String,
        val choices: List<String>,
        val answerIndex: Int,
    )

    private val asked = mutableSetOf<String>()

    var score: Int = 0
        private set
    var answered: Int = 0
        private set
    var correct: Int = 0
        private set

    val isAvailable: Boolean get() = items.size >= 4

    fun next(): ChosungQuestion? {
        if (!isAvailable) return null
        var pool = items.filter { it.id !in asked }
        if (pool.isEmpty()) {
            asked.clear()
            pool = items
        }
        val item = pool.random(random)
        asked += item.id
        val sameLength = items.filter { it.id != item.id && it.word.length == item.word.length }
        val others = (sameLength.ifEmpty { items.filter { it.id != item.id } })
            .map { it.word }.distinct().shuffled(random).take(3)
        val choices = (others + item.word).shuffled(random)
        return ChosungQuestion(item, Hangul.chosungOf(item.word), choices, choices.indexOf(item.word))
    }

    fun answer(question: ChosungQuestion, choiceIndex: Int): Boolean {
        val ok = choiceIndex == question.answerIndex
        answered++
        if (ok) {
            correct++
            score += 10
        }
        return ok
    }
}
