package com.nolleo.app.data

import com.nolleo.app.domain.games.WordLevel
import com.nolleo.app.domain.model.IdiomChar
import com.nolleo.app.domain.model.IdiomItem
import com.nolleo.app.domain.model.Question
import com.nolleo.app.domain.model.RegionInfo
import com.nolleo.app.domain.rest.RestArea
import com.nolleo.app.domain.rest.ServiceAreaInfo
import org.json.JSONArray
import org.json.JSONObject

/**
  * assets/content 의 JSON 파서 (org.json 으로 통일 — kotlinx.serialization 플러그인 미사용).
 * 순수 함수라 JVM 단위 테스트에서 실제 org.json 구현으로 검증한다.
 */
object ContentParser {
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

    /**
     * assets/content/quiz_idioms.json (최상위 배열). 정답·질문이 비었거나 정답이 한글 음절이 아니면 건너뛴다.
     * hint 는 읽지 않는다 (사지선다라 힌트를 쓰지 않음).
     */
    fun parseIdioms(json: String): List<IdiomItem> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val word = o.optJSONArray("answers").toStringList().firstOrNull()?.trim().orEmpty()
            val question = o.optString("question").trim()
            if (word.length < 2 || !word.all { it in '가'..'힣' } || question.isEmpty()) return@mapNotNull null
            val chars = o.optJSONArray("chars")?.let { ca ->
                (0 until ca.length()).map { j ->
                    val ch = ca.getJSONObject(j)
                    IdiomChar(hanja = ch.optString("hanja"), hun = ch.optString("hun"), eum = ch.optString("eum"))
                }
            }.orEmpty()
            IdiomItem(
                id = o.optString("id", "idiom-$i"),
                word = word,
                hanja = o.optString("hanja"),
                question = question,
                origin = if (o.isNull("origin")) null else o.optString("origin").trim().ifEmpty { null },
                chars = chars,
                difficulty = o.optInt("difficulty", 2).coerceIn(1, 3),
            )
        }
    }

    /** assets/content/rest_areas.json (전국졸음쉼터표준데이터에서 변환) */
    fun parseRestAreas(json: String): List<RestArea> {
        val arr = JSONObject(json).getJSONArray("restAreas")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RestArea(
                name = o.getString("name"),
                route = o.getString("route"),
                from = o.getString("from"),
                to = o.getString("to"),
                latitude = o.getDouble("lat"),
                longitude = o.getDouble("lng"),
                heading = o.getInt("heading"),
                parking = o.optInt("parking"),
                toilet = o.optBoolean("toilet"),
            )
        }
    }

    /** assets/content/service_areas.json (도로공사 휴게소 편의시설 + 가족사랑 화장실에서 변환, 좌표 없음) */
    fun parseServiceAreas(json: String): List<ServiceAreaInfo> {
        val arr = JSONObject(json).getJSONArray("serviceAreas")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ServiceAreaInfo(
                name = o.getString("name"),
                direction = o.getString("direction"),
                amenities = o.optJSONArray("amenities").toStringList(),
                foods = o.optString("foods").takeIf { !o.isNull("foods") && it.isNotBlank() },
                familyRestroom = if (o.isNull("familyRestroom")) null else o.optBoolean("familyRestroom"),
                dirLat = if (o.isNull("dirLat")) null else o.getDouble("dirLat"),
                dirLng = if (o.isNull("dirLng")) null else o.getDouble("dirLng"),
            )
        }
    }

    /** 한 줄에 한 낱말 (끝말잇기 검증용 words_ko_valid.txt) */
    fun parseWords(text: String): List<String> =
        text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()

    /**
     * 끝말잇기 출제용 (words_ko_play.json): [{word, level, category, definition, from_team_list}] → 낱말·어휘 등급.
     * 앱은 `word`·`level` 만 쓴다. 등급 표기를 모르면 중급으로 본다.
     */
    fun parsePlayWords(json: String): List<Pair<String, WordLevel>> {
        val arr = JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val word = o.optString("word").trim()
            if (word.isEmpty()) null else word to (WordLevel.fromLabel(o.optString("level")) ?: WordLevel.INTERMEDIATE)
        }
    }

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
