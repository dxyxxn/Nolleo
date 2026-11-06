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

data class ChosungItem(
    val id: String,
    val word: String,
    val hint: String,
    val category: String,
    val difficulty: Int,
)

/** assets/content 에서 읽은 게임 콘텐츠 전체 */
data class GameContent(
    val regions: List<RegionInfo>,
    /** regionCode → 문제 */
    val regionQuestions: Map<String, List<Question>>,
    val generalQuestions: List<Question>,
    val chosungItems: List<ChosungItem>,
) {
    fun region(code: String?): RegionInfo? = regions.firstOrNull { it.code == code }

    companion object {
        val EMPTY = GameContent(emptyList(), emptyMap(), emptyList(), emptyList())
    }
}
