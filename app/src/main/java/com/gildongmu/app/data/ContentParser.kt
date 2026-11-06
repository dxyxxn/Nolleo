package com.gildongmu.app.data

import com.gildongmu.app.domain.model.ChosungItem
import com.gildongmu.app.domain.model.Question
import com.gildongmu.app.domain.model.RegionInfo
import org.json.JSONArray
import org.json.JSONObject

/**
  * assets/content 의 JSON 파서 (org.json 으로 통일 — kotlinx.serialization 플러그인 미사용).
 * 순수 함수라 JVM 단위 테스트에서 실제 org.json 구현으로 검증한다.
 */
object ContentParser {
    private val BOM = Char(0xFEFF).toString()

    fun parseRegions(json: String): List<RegionInfo> {
        val arr = JSONObject(json).getJSONArray("regions")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RegionInfo(
                code = o.getString("code"),
                name = o.getString("name"),
                shortName = o.getString("shortName"),
                minLat = o.getDouble("minLat"),
                maxLat = o.getDouble("maxLat"),
                minLng = o.getDouble("minLng"),
                maxLng = o.getDouble("maxLng"),
                centerLat = o.getDouble("centerLat"),
                centerLng = o.getDouble("centerLng"),
                radiusKm = if (o.has("radiusKm")) o.getDouble("radiusKm") else null,
                neighbors = o.optJSONArray("neighbors").toStringList(),
                keywords = o.optJSONArray("keywords").toStringList(),
            )
        }
    }

    /** quiz_regions.json → regionCode → 문제 */
    fun parseRegionQuiz(json: String): Map<String, List<Question>> {
        val arr = JSONObject(json).getJSONArray("regions")
        return (0 until arr.length()).associate { i ->
            val o = arr.getJSONObject(i)
            val code = o.getString("code")
            code to parseQuestions(o.getJSONArray("questions"), code)
        }
    }

    /** quiz_general.json */
    fun parseGeneralQuiz(json: String): List<Question> =
        parseQuestions(JSONObject(json).getJSONArray("questions"), regionCode = null)

    fun parseChosung(json: String): List<ChosungItem> {
        val arr = JSONObject(json).getJSONArray("items")
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val word = o.optString("word").trim()
            if (word.length < 2) return@mapNotNull null
            ChosungItem(
                id = o.optString("id", "cho-$i"),
                word = word,
                hint = o.optString("hint"),
                category = o.optString("category"),
                difficulty = o.optInt("difficulty", 1),
            )
        }
    }

    fun parseWords(text: String): List<String> =
        text.lineSequence().map { it.trim().removePrefix(BOM) }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()

    private fun parseQuestions(arr: JSONArray, regionCode: String?): List<Question> =
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val answers = o.optJSONArray("answers").toStringList().filter { it.isNotBlank() }
            val text = o.optString("question").trim()
            if (answers.isEmpty() || text.isEmpty()) return@mapNotNull null
            Question(
                id = o.optString("id", "${regionCode ?: "gen"}-$i"),
                regionCode = regionCode,
                difficulty = o.optInt("difficulty", 2).coerceIn(1, 3),
                category = o.optString("category"),
                text = text,
                answers = answers,
                hint = o.optString("hint"),
            )
        }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else (0 until length()).map { getString(it) }
}
