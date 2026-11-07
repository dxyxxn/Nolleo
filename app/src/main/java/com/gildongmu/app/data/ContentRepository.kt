package com.gildongmu.app.data

import android.content.Context
import com.gildongmu.app.core.log.AppLog
import com.gildongmu.app.core.location.RegionResolver
import com.gildongmu.app.core.speech.AnswerInput
import com.gildongmu.app.core.speech.AnswerScript
import com.gildongmu.app.core.speech.ScriptEntry
import com.gildongmu.app.domain.games.WordDictionary
import com.gildongmu.app.domain.model.GameContent
import com.gildongmu.app.domain.rest.RestArea
import com.gildongmu.app.domain.rest.ServiceAreaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** assets/content·assets/demo 로딩. 네트워크 호출 없음 — 콘텐츠는 전부 앱에 포함된다. */
class ContentRepository(context: Context) {

    private val assets = context.applicationContext.assets

    private val _content = MutableStateFlow<GameContent?>(null)
    val content: StateFlow<GameContent?> = _content.asStateFlow()

    private val _dictionary = MutableStateFlow<WordDictionary?>(null)
    val dictionary: StateFlow<WordDictionary?> = _dictionary.asStateFlow()

    private val _resolver = MutableStateFlow<RegionResolver?>(null)
    val resolverFlow: StateFlow<RegionResolver?> = _resolver.asStateFlow()
    val resolver: RegionResolver? get() = _resolver.value

    /** 고속도로 졸음쉼터 (졸음 케어 2단계) */
    private val _restAreas = MutableStateFlow<List<RestArea>>(emptyList())
    val restAreas: StateFlow<List<RestArea>> = _restAreas.asStateFlow()

    private val _serviceAreas = MutableStateFlow<List<ServiceAreaInfo>>(emptyList())
    val serviceAreas: StateFlow<List<ServiceAreaInfo>> = _serviceAreas.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        if (_content.value != null) return@withContext
        val started = System.currentTimeMillis()
        val regions = readOrNull("content/regions.json")?.let { runCatching { ContentParser.parseRegions(it) }.getOrNull() }.orEmpty()
        val regionQuiz = readOrNull("content/quiz_regions.json")
            ?.let { runCatching { ContentParser.parseRegionQuiz(it) }.onFailure { e -> AppLog.e(TAG, "quiz_regions parse", e) }.getOrNull() }
            .orEmpty()
        val general = readOrNull("content/quiz_general.json")
            ?.let { runCatching { ContentParser.parseGeneralQuiz(it) }.onFailure { e -> AppLog.e(TAG, "quiz_general parse", e) }.getOrNull() }
            .orEmpty()
        val idioms = readOrNull("content/quiz_idioms.json")
            ?.let { runCatching { ContentParser.parseIdioms(it) }.onFailure { e -> AppLog.e(TAG, "quiz_idioms parse", e) }.getOrNull() }
            .orEmpty()
        val words = readOrNull("content/words_ko.txt")?.let { ContentParser.parseWords(it) }.orEmpty()
        _restAreas.value = readOrNull("content/rest_areas.json")
            ?.let { runCatching { ContentParser.parseRestAreas(it) }.onFailure { e -> AppLog.e(TAG, "rest_areas parse", e) }.getOrNull() }
            .orEmpty()
        _serviceAreas.value = readOrNull("content/service_areas.json")
            ?.let { runCatching { ContentParser.parseServiceAreas(it) }.onFailure { e -> AppLog.e(TAG, "service_areas parse", e) }.getOrNull() }
            .orEmpty()

        _resolver.value = RegionResolver(regions)
        _dictionary.value = WordDictionary(words)
        _content.value = GameContent(regions, regionQuiz, general, idioms)
        AppLog.i(
            TAG,
            "content loaded in ${System.currentTimeMillis() - started}ms: regions=${regions.size}, " +
                "regionQuiz=${regionQuiz.values.sumOf { it.size }}, general=${general.size}, idioms=${idioms.size}, words=${_dictionary.value?.size}, restAreas=${_restAreas.value.size}, serviceAreas=${_serviceAreas.value.size}",
        )
    }

    fun loadScript(input: AnswerInput): AnswerScript? {
        val path = input.scriptAsset ?: return null
        val json = readOrNull(path) ?: return null
        return runCatching {
            val root = JSONObject(json)
            val arr = root.getJSONArray("entries")
            AnswerScript(
                name = root.optString("name", input.label),
                driverOnly = root.optBoolean("driverOnly", false),
                entries = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    ScriptEntry(
                        delayMs = o.optLong("delayMs", 1500),
                        answer = if (o.isNull("answer")) null else o.optString("answer"),
                    )
                },
            )
        }.onFailure { AppLog.e(TAG, "script parse $path", it) }.getOrNull()
    }

    private fun readOrNull(path: String): String? = runCatching {
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.onFailure { AppLog.w(TAG, "asset missing: $path") }.getOrNull()

    companion object {
        private const val TAG = "Content"
    }
}
