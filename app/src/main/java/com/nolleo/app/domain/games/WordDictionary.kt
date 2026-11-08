package com.nolleo.app.domain.games

import kotlin.random.Random

/** 출제용 낱말의 어휘 등급 (words_ko_play.json 의 level) */
enum class WordLevel(val label: String) {
    BASIC("초급"),
    INTERMEDIATE("중급"),
    ADVANCED("고급"),
    TEAM("팀목록"),
    ;

    /** 졸음 케어 난이도 상한과 연동: 1 → 초급만, 2 → 초급·중급·팀목록, 3 → 전부 */
    fun allowedAt(difficultyCap: Int): Boolean = when {
        difficultyCap <= 1 -> this == BASIC
        difficultyCap == 2 -> this != ADVANCED
        else -> true
    }

    companion object {
        fun fromLabel(label: String?): WordLevel? = entries.firstOrNull { it.label == label }
    }
}

/**
 * 검증용 낱말 목록 (assets/content/words_ko_valid.txt, 약 19만 어). 메모리를 아끼려고 정렬된 배열 + 이진 탐색으로 찾는다.
 */
class ValidWords private constructor(private val sorted: Array<String>) {
    val size: Int get() = sorted.size

    operator fun contains(word: String): Boolean = sorted.binarySearch(word) >= 0

    companion object {
        /** 이미 가나다(UTF-16) 순이면 그대로, 아니면 정렬한다. 중복은 판정에 영향이 없어 그대로 둔다. */
        fun of(words: List<String>): ValidWords {
            val arr = words.toTypedArray()
            var ordered = true
            for (i in 1 until arr.size) if (arr[i - 1] > arr[i]) { ordered = false; break }
            if (!ordered) arr.sort()
            return ValidWords(arr)
        }
    }
}

/**
 * 끝말잇기 사전.
 *  - **출제용**([playWords], words_ko_play.json): 앱이 자기 차례에 말하는 낱말. 어휘 등급([levels])이 있다.
 *  - **검증용**([attachValidation], words_ko_valid.txt): 플레이어가 말한 낱말을 인정할지 판정. 크니까 첫 판을 시작할 때
 *    백그라운드에서 붙인다. 붙기 전에는 출제용으로 판정한다(없으면 한 번 되묻고 관대하게 인정 — [WordChainGame]).
 *  - 두음법칙은 [Hangul.allowedStarts] 하나로 판정과 막다른 음절 계산을 함께 한다.
 *  - 막다른 음절(이을 출제용 낱말이 [MIN_CONTINUATIONS] 개 미만)은 만들 때 한 번 계산해 둔다.
 */
class WordDictionary(playWords: Collection<String>, levels: Map<String, WordLevel> = emptyMap()) {

    private val play: Set<String> = playWords.asSequence()
        .map { it.trim() }
        .filter { isWord(it) }
        .toCollection(LinkedHashSet())

    private val levelOf: Map<String, WordLevel> = levels

    private val byFirst: Map<Char, List<String>> = play.groupBy { it.first() }

    /** 출제용 낱말의 끝 음절마다 막다른지 (만들 때 한 번 계산) */
    private val deadEndByLast: Map<Char, Boolean> =
        play.asSequence().map { it.last() }.distinct().associateWith { countContinuations(it) < MIN_CONTINUATIONS }

    @Volatile
    private var valid: ValidWords? = null

    /** 출제용 낱말 수 */
    val size: Int get() = play.size

    /** 붙은 검증용 낱말 수 (아직 안 붙었으면 null) */
    val validSize: Int? get() = valid?.size

    val hasValidation: Boolean get() = valid != null

    /** 막다른 음절 수 (출제용 끝 음절 기준) — 로그·디버그용 */
    val deadEndSyllableCount: Int get() = deadEndByLast.count { it.value }

    fun attachValidation(words: ValidWords) {
        valid = words
    }

    /** 플레이어 낱말 인정 여부: 검증용(붙었으면) 또는 출제용에 있으면 인정 */
    operator fun contains(word: String): Boolean = word in play || valid?.contains(word) == true

    fun level(word: String): WordLevel = levelOf[word] ?: WordLevel.BASIC

    /** [lastSyllable] 로 끝난 말 다음에 올 수 있는 출제용 낱말 (두음법칙 양방향 포함) */
    fun continuations(lastSyllable: Char): List<String> =
        Hangul.allowedStarts(lastSyllable).flatMap { byFirst[it].orEmpty() }

    fun continuationCount(lastSyllable: Char): Int = countContinuations(lastSyllable)

    private fun countContinuations(lastSyllable: Char): Int =
        Hangul.allowedStarts(lastSyllable).sumOf { byFirst[it]?.size ?: 0 }

    /** 이을 출제용 낱말이 거의 없는 음절이면 막다른 길 */
    fun isDeadEnd(lastSyllable: Char): Boolean =
        deadEndByLast[lastSyllable] ?: (countContinuations(lastSyllable) < MIN_CONTINUATIONS)

    /** 앱 차례 단어 고르기 결과 */
    sealed interface Pick {
        data class Word(val word: String) : Pick
        /** 이을 말은 있지만 모두 다음 사람이 잇기 어려운(막다른) 말 */
        data object OnlyDeadEnds : Pick
        /** 이을 말이 아예 없음 */
        data object Nothing : Pick
    }

    /**
     * 앱 차례 단어: 아직 안 쓴 출제용 낱말 중, 다음 사람이 이을 수 있는(막다른 음절이 아닌) 말.
     * 고르는 순서 — 표준 두음법칙 첫 음절(원래 음절·[Hangul.dueum]) + 난이도 상한 안 → 상한 안 → 표준 첫 음절 → 아무거나.
     */
    fun pick(lastSyllable: Char, used: Set<String>, random: Random, difficultyCap: Int = 3): Pick {
        val all = continuations(lastSyllable).filter { it !in used }
        if (all.isEmpty()) return Pick.Nothing
        val friendly = all.filter { canContinueAfter(it, used) }
        if (friendly.isEmpty()) return Pick.OnlyDeadEnds
        return Pick.Word(preferred(friendly, lastSyllable, difficultyCap).random(random))
    }

    /** [word] 를 말한 뒤 다음 사람이 이을 수 있는가: 막다른 음절이 아니고, 안 쓴 이을 말이 남아 있다 */
    private fun canContinueAfter(word: String, used: Set<String>): Boolean =
        !isDeadEnd(word.last()) &&
            Hangul.allowedStarts(word.last()).any { s -> byFirst[s].orEmpty().any { it !in used && it != word } }

    /**
     * 이어 말할 낱말 하나 (대본 응답 힌트). [avoidDeadEnd] 면 막다른 말은 빼고,
     * 그런 말밖에 없으면 null.
     */
    fun pickNext(lastSyllable: Char, used: Set<String>, random: Random, avoidDeadEnd: Boolean = true, difficultyCap: Int = 3): String? {
        if (avoidDeadEnd) return (pick(lastSyllable, used, random, difficultyCap) as? Pick.Word)?.word
        val candidates = continuations(lastSyllable).filter { it !in used }
        if (candidates.isEmpty()) return null
        return preferred(candidates, lastSyllable, difficultyCap).random(random)
    }

    private fun preferred(candidates: List<String>, lastSyllable: Char, difficultyCap: Int): List<String> {
        val standard = setOfNotNull(lastSyllable, Hangul.dueum(lastSyllable))
        val easy = candidates.filter { level(it).allowedAt(difficultyCap) }
        return easy.filter { it.first() in standard }.ifEmpty { easy }
            .ifEmpty { candidates.filter { it.first() in standard } }
            .ifEmpty { candidates }
    }

    /**
     * 첫 낱말(판 시작·막다른 말 뒤 다시 시작): 안 쓴 출제용 낱말 중 다음 사람이 이을 수 있는 말.
     * 고르는 순서는 [pick] 과 같이 난이도 상한이 먼저다 — 상한 안의 [preferred] → 상한 안의 아무 말 → [preferred] → 아무 말.
     */
    fun randomStarter(random: Random, preferred: List<String>, difficultyCap: Int = 3, exclude: Set<String> = emptySet()): String? {
        val usable = { w: String -> w !in exclude && canContinueAfter(w, exclude) }
        val easy = { w: String -> level(w).allowedAt(difficultyCap) }
        val starters = preferred.filter { it in play && usable(it) }
        starters.filter(easy).randomOrNull(random)?.let { return it }
        val pool = play.filter(usable)
        return pool.filter(easy).ifEmpty { starters }.ifEmpty { pool }.randomOrNull(random)
    }

    /** 대본 엔진의 "{wrong}" 답: 요구 음절로 시작하지 않는 아무 출제용 낱말 */
    fun randomNotStartingWith(starts: Set<Char>, random: Random): String? =
        play.asSequence().filter { it.first() !in starts }.take(200).toList().randomOrNull(random)

    companion object {
        /** 이을 출제용 낱말이 이보다 적으면 막다른 음절 */
        const val MIN_CONTINUATIONS = 3

        /** 한글 2음절 이상 */
        fun isWord(w: String): Boolean = w.length >= 2 && w.all(Hangul::isSyllable)
    }
}
