package com.nolleo.app.data

import android.content.Context
import com.nolleo.app.core.log.AppLog
import com.nolleo.app.core.location.RegionResolver
import com.nolleo.app.core.speech.AnswerInput
import com.nolleo.app.core.speech.AnswerScript
import com.nolleo.app.core.speech.ScriptEntry
import com.nolleo.app.domain.games.ValidWords
import com.nolleo.app.domain.games.WordBlocklist
import com.nolleo.app.domain.games.WordDictionary
import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.domain.rest.RestArea
import com.nolleo.app.domain.rest.ServiceAreaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** assets/content·assets/demo 로딩. 네트워크 호출 없음 — 콘텐츠는 전부 앱에 포함된다. */
class ContentRepository(context: Context) {

    private val assets = context.applicationContext.assets

    private val _content = MutableStateFlow<GameContent?>(null)
    val content: StateFlow<GameContent?> = _content.asStateFlow()

    private val _dictionary = MutableStateFlow<WordDictionary?>(null)
    val dictionary: StateFlow<WordDictionary?> = _dictionary.asStateFlow()

    /** 끝말잇기 금칙어 — 출제용·검증용을 읽을 때 한 번 더 거른다 */
    private var blocklist: WordBlocklist = WordBlocklist.EMPTY
    private val validMutex = Mutex()

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
        val regions = readOrNull("content/regions.json")?.let { runCatching { ContentParser.parseRegions(it) }.onFailure { e -> AppLog.e(TAG, "regions parse", e) }.getOrNull() }.orEmpty()
        val regionQuiz = readOrNull("content/quiz_regions.json")
            ?.let { runCatching { ContentParser.parseRegionQuiz(it) }.onFailure { e -> AppLog.e(TAG, "quiz_regions parse", e) }.getOrNull() }
            .orEmpty()
        val general = readOrNull("content/quiz_general.json")
            ?.let { runCatching { ContentParser.parseGeneralQuiz(it) }.onFailure { e -> AppLog.e(TAG, "quiz_general parse", e) }.getOrNull() }
            .orEmpty()
        val idioms = readOrNull("content/quiz_idioms.json")
            ?.let { runCatching { ContentParser.parseIdioms(it) }.onFailure { e -> AppLog.e(TAG, "quiz_idioms parse", e) }.getOrNull() }
            .orEmpty()
        blocklist = readOrNull("content/words_blocklist.txt")?.let { WordBlocklist.parse(it) } ?: WordBlocklist.EMPTY
        // 끝말잇기 출제용 — 금칙어를 한 번 더 거른다(사전 파일은 고치지 않는다). 검증용은 첫 판 시작 때 [loadValidWords]
        val playAll = readOrNull("content/words_ko_play.json")
            ?.let { runCatching { ContentParser.parsePlayWords(it) }.onFailure { e -> AppLog.e(TAG, "words_ko_play parse", e) }.getOrNull() }
            .orEmpty()
        val play = playAll.filterNot { blocklist.blocks(it.first) }
        _restAreas.value = readOrNull("content/rest_areas.json")
            ?.let { runCatching { ContentParser.parseRestAreas(it) }.onFailure { e -> AppLog.e(TAG, "rest_areas parse", e) }.getOrNull() }
            .orEmpty()
        _serviceAreas.value = readOrNull("content/service_areas.json")
            ?.let { runCatching { ContentParser.parseServiceAreas(it) }.onFailure { e -> AppLog.e(TAG, "service_areas parse", e) }.getOrNull() }
            .orEmpty()

        _resolver.value = RegionResolver(regions)
        val dictionary = WordDictionary(play.map { it.first }, play.toMap())
        _dictionary.value = dictionary
        _content.value = GameContent(regions, regionQuiz, general, idioms)
        AppLog.i(
            TAG,
            "content loaded in ${System.currentTimeMillis() - started}ms: regions=${regions.size}, " +
                "regionQuiz=${regionQuiz.values.sumOf { it.size }}, general=${general.size}, idioms=${idioms.size}, " +
                "playWords=${dictionary.size} (blocked ${playAll.size - play.size}), deadEndSyllables=${dictionary.deadEndSyllableCount}, " +
                "restAreas=${_restAreas.value.size}, serviceAreas=${_serviceAreas.value.size}",
        )
    }

    /**
     * 끝말잇기 검증용 낱말(약 19만 어)을 백그라운드에서 읽어 사전에 붙인다. 첫 판을 시작할 때 부른다(앱 실행을 늦추지 않으려고).
     * 이미 붙었으면 아무것도 하지 않는다. 금칙어를 한 번 더 거른다.
     */
    suspend fun loadValidWords() = withContext(Dispatchers.IO) {
        val dictionary = _dictionary.value ?: return@withContext
        validMutex.withLock {
            if (dictionary.hasValidation) return@withLock
            val started = System.currentTimeMillis()
            val all = readOrNull("content/words_ko_valid.txt")?.let { ContentParser.parseWords(it) } ?: return@withLock
            val kept = all.filterNot { blocklist.blocks(it) }
            dictionary.attachValidation(ValidWords.of(kept))
            AppLog.i(TAG, "valid words loaded in ${System.currentTimeMillis() - started}ms: ${kept.size} (blocked ${all.size - kept.size})")
        }
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
                yesNoDelayMs = root.optLong("yesNoDelayMs", AnswerScript.DEFAULT_YES_NO_DELAY_MS),
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

    /** asset 을 UTF-8 로 읽는다. 맨 앞 BOM 은 여기서 한 번만 뗀다 (PowerShell 5.1 로 다시 저장하면 붙는다) */
    private fun readOrNull(path: String): String? = runCatching {
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }.removePrefix(BOM)
    }.onFailure { AppLog.w(TAG, "asset missing: $path") }.getOrNull()

    companion object {
        private const val TAG = "Content"
        private val BOM = Char(0xFEFF).toString()
    }
}
