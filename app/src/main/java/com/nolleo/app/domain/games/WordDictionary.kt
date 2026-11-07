package com.nolleo.app.domain.games

import kotlin.random.Random

/** 끝말잇기 사전 (assets/content/words_ko.txt). 첫 음절 색인 + 두음법칙을 고려한 이어 말하기 후보. */
class WordDictionary(words: Collection<String>) {

    private val set: Set<String> = words.asSequence()
        .map { it.trim() }
        .filter { it.length >= 2 && it.all(Hangul::isSyllable) }
        .toSet()

    private val byFirst: Map<Char, List<String>> = set.groupBy { it.first() }

    val size: Int get() = set.size

    operator fun contains(word: String): Boolean = word in set

    /** [lastSyllable] 로 끝난 말 다음에 올 수 있는 단어 (두음법칙 포함) */
    fun continuations(lastSyllable: Char): List<String> =
        Hangul.allowedStarts(lastSyllable).flatMap { byFirst[it].orEmpty() }

    fun continuationCount(lastSyllable: Char): Int = continuations(lastSyllable).size

    /** 이을 말이 거의 없는 음절이면 막다른 길 */
    fun isDeadEnd(lastSyllable: Char, minContinuations: Int = 3): Boolean =
        continuationCount(lastSyllable) < minContinuations

    /**
     * 앱 차례 단어 선택: 아직 안 쓴 단어 중, 다음 사람이 이을 수 있는(막다른 음절이 아닌) 단어를 고른다.
     * 그런 단어가 없으면 막다른 단어라도 고르고, 그것도 없으면 null (= "제가 졌어요!").
     */
    fun pickNext(lastSyllable: Char, used: Set<String>, random: Random, avoidDeadEnd: Boolean = true): String? {
        val candidates = continuations(lastSyllable).filter { it !in used }
        if (candidates.isEmpty()) return null
        if (!avoidDeadEnd) return candidates.random(random)
        val friendly = candidates.filter { w -> !isDeadEnd(w.last()) && continuations(w.last()).any { it !in used && it != w } }
        return (friendly.ifEmpty { candidates }).random(random)
    }

    fun randomStarter(random: Random, preferred: List<String>): String? {
        val good = preferred.filter { it in set && !isDeadEnd(it.last()) }
        if (good.isNotEmpty()) return good.random(random)
        return set.filter { !isDeadEnd(it.last()) }.randomOrNull(random)
    }

    /**
     * 화면 끝말잇기의 오답 보기: [starts] 로 시작하지 않는 말. [syllable] 이 끝이나 가운데에 든 말(예: '과' → 결과·효과)을
     * 두 개까지 먼저 넣어 헷갈리게 하고, 나머지는 아무 말로 채운다.
     */
    fun lookAlikes(syllable: Char, starts: Set<Char>, exclude: Set<String>, count: Int, random: Random): List<String> {
        val ok = { w: String -> w.first() !in starts && w !in exclude }
        val tricky = set.filter { ok(it) && syllable in it }.shuffled(random).take(2)
        val rest = set.filter(ok).shuffled(random)
        return (tricky + rest).distinct().take(count)
    }

    /** 대본 엔진의 "{wrong}" 답: 요구 음절로 시작하지 않는 아무 단어 */
    fun randomNotStartingWith(starts: Set<Char>, random: Random): String? =
        set.asSequence().filter { it.first() !in starts }.take(200).toList().randomOrNull(random)
}
