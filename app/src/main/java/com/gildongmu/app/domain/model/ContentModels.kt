package com.gildongmu.app.domain.model

data class RegionInfo(
    val code: String,
    val name: String,
    val shortName: String,
    val minLat: Double,
    val maxLat: Double,
    val minLng: Double,
    val maxLng: Double,
    val centerLat: Double,
    val centerLng: Double,
    /** 광역시·특별시처럼 넓은 도 안에 있는 지역은 중심 반경 안에 있어야 일치 */
    val radiusKm: Double?,
    val neighbors: List<String>,
    val keywords: List<String>,
)

/** 사자성어 한 글자: 한자와 훈(뜻)·음(소리) — 예: 家 / 집 / 가 */
data class IdiomChar(val hanja: String, val hun: String, val eum: String)

/** 정차 중 화면 게임 "사자성어" 문제 한 개 (assets/content/quiz_idioms.json, 힌트는 쓰지 않는다) */
data class IdiomItem(
    val id: String,
    /** 정답 사자성어 한글 표기 (answers 의 첫 값, 예: 가가호호) */
    val word: String,
    /** 한자 표기 (예: 家家戶戶) */
    val hanja: String,
    /** "다음 뜻을 가진 사자성어는? 한 집 한 집." */
    val question: String,
    /** 유래 — 없으면 null */
    val origin: String?,
    val chars: List<IdiomChar>,
    val difficulty: Int,
) {
    /** 화면에 크게 보여 줄 뜻 (질문 앞머리를 뗀 부분) */
    val meaning: String get() = question.removePrefix(QUESTION_LEAD).trim().ifEmpty { question }

    companion object {
        const val QUESTION_LEAD = "다음 뜻을 가진 사자성어는?"
    }
}

/** assets/content 에서 읽은 게임 콘텐츠 전체 */
data class GameContent(
    val regions: List<RegionInfo>,
    /** regionCode → 문제 */
    val regionQuestions: Map<String, List<Question>>,
    val generalQuestions: List<Question>,
    /** 정차 중 화면 게임 사자성어 */
    val idioms: List<IdiomItem>,
) {
    fun region(code: String?): RegionInfo? = regions.firstOrNull { it.code == code }

    companion object {
        val EMPTY = GameContent(emptyList(), emptyMap(), emptyList(), emptyList())
    }
}
