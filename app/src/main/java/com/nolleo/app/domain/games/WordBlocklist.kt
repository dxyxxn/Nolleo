package com.nolleo.app.domain.games

/**
 * 끝말잇기 금칙어 (assets/content/words_blocklist.txt). 출제용·검증용 사전을 읽을 때 한 번 더 걸러 낸다.
 *  - [contains]: 이 말이 들어간 낱말은 모두 뺀다 (예: "살인" → 살인적)
 *  - [exact]: 낱말 전체가 같을 때만 뺀다 — 다른 말 속에 섞여 오탐이 나는 말 (예: "거지" 는 설거지를 빼지 않는다)
 */
class WordBlocklist(private val contains: List<String>, private val exact: Set<String>) {

    val size: Int get() = contains.size + exact.size

    fun blocks(word: String): Boolean = word in exact || contains.any { word.contains(it) }

    companion object {
        val EMPTY = WordBlocklist(emptyList(), emptySet())
        private val BOM = Char(0xFEFF).toString()

        /** 한 줄에 하나, '#' 은 설명, '=' 로 시작하면 완전 일치 */
        fun parse(text: String): WordBlocklist {
            val lines = text.lineSequence().map { it.trim().removePrefix(BOM) }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
            return WordBlocklist(
                contains = lines.filterNot { it.startsWith("=") },
                exact = lines.filter { it.startsWith("=") }.map { it.removePrefix("=").trim() }.filter { it.isNotEmpty() }.toSet(),
            )
        }
    }
}
