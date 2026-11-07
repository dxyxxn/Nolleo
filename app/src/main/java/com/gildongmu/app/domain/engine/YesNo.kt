package com.gildongmu.app.domain.engine

/** 음성 응답의 네/아니요 판정 (쉬어 가기 수락 등). 모르면 null. */
object YesNo {
    private val NO = listOf("아니", "아뇨", "괜찮", "싫어", "됐어", "안할래", "안갈래", "그냥가", "노")
    private val YES = listOf("네", "예", "응", "어", "그래", "좋아", "좋아요", "알겠", "안내", "가자", "쉬자", "쉴래", "쉬어", "오케이", "ok", "yes")

    fun parse(text: String?): Boolean? {
        val t = AnswerMatcher.normalize(text ?: return null)
        if (t.isEmpty()) return null
        // "아니요, 괜찮아요" 같은 거절을 먼저 본다 ("안내"의 "안" 과 헷갈리지 않게 단어로만 비교)
        if (NO.any { t.startsWith(it) || t.contains(it) && it.length >= 2 }) return false
        if (YES.any { t.startsWith(it) || it.length >= 2 && t.contains(it) }) return true
        return null
    }
}
