package com.nolleo.app.domain.engine

/**
 * 음성 답 판정 (지시서 §6).
 *  1. 정규화: 공백·문장부호 제거, 끝의 "요/이요/입니다/예요/이에요" 제거
 *  2. 허용 표기 중 하나와 일치 또는 포함 (1글자 정답은 포함 판정을 하지 않는다 — "배" ⊂ "배추" 오판 방지)
 *  3. 3글자 이상이면 편집거리 ≤ 1 허용
 */
object AnswerMatcher {
    private val SUFFIXES = listOf("입니다", "이에요", "예요", "이요", "요")

    fun normalize(text: String): String = text.trim().lowercase().filter { it.isLetterOrDigit() }

    fun stripPoliteSuffix(normalized: String): String {
        for (suffix in SUFFIXES) {
            if (normalized.length > suffix.length && normalized.endsWith(suffix)) {
                return normalized.dropLast(suffix.length)
            }
        }
        return normalized
    }

    /** 원문 정규화 + 존댓말 어미 제거 형태 둘 다 비교한다 ("요요" 같은 정답 보호) */
    fun variants(spoken: String): Set<String> {
        val n = normalize(spoken)
        return setOf(n, stripPoliteSuffix(n)).filter { it.isNotEmpty() }.toSet()
    }

    fun matches(spoken: String?, accepted: List<String>): Boolean {
        if (spoken.isNullOrBlank()) return false
        val heard = variants(spoken)
        val targets = accepted.map(::normalize).filter { it.isNotEmpty() }
        return targets.any { target ->
            heard.any { h ->
                h == target ||
                    (target.length >= 2 && h.contains(target)) ||
                    (target.length >= 3 && editDistance(h, target) <= 1)
            }
        }
    }

    fun editDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }
}
