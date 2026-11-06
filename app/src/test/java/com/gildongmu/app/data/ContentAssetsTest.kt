package com.gildongmu.app.data

import com.gildongmu.app.domain.engine.AnswerMatcher
import com.gildongmu.app.domain.games.Hangul
import com.gildongmu.app.domain.games.WordDictionary
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 실제 assets 콘텐츠가 지시서 §6 규칙을 지키는지 검증한다. */
class ContentAssetsTest {

    private fun asset(path: String) = File("src/main/assets/$path").readText(Charsets.UTF_8)

    private val regions = ContentParser.parseRegions(asset("content/regions.json"))
    private val regionQuiz = ContentParser.parseRegionQuiz(asset("content/quiz_regions.json"))
    private val general = ContentParser.parseGeneralQuiz(asset("content/quiz_general.json"))
    private val chosung = ContentParser.parseChosung(asset("content/chosung_quiz.json"))
    private val words = ContentParser.parseWords(asset("content/words_ko.txt"))

    private fun sentences(text: String) = text.split(Regex("(?<=[.?!])\\s*")).filter { it.isNotBlank() }

    @Test
    fun seventeenRegions_eachWithAtLeastEightQuestions() {
        assertEquals(17, regions.size)
        assertEquals(regions.map { it.code }.toSet(), regionQuiz.keys)
        regionQuiz.forEach { (code, qs) -> assertTrue("$code: ${qs.size}", qs.size >= 8) }
    }

    @Test
    fun generalQuiz_atLeastForty() {
        assertTrue(general.size >= 40)
    }

    @Test
    fun questions_followTtsRules() {
        (regionQuiz.values.flatten() + general).forEach { q ->
            val s = sentences(q.text)
            assertTrue("${q.id}: 2문장 이내 — ${q.text}", s.size <= 2)
            s.forEach { assertTrue("${q.id}: 한 문장 ≤ 80자 — $it", it.length <= 80) }
            assertTrue("${q.id}: 난이도", q.difficulty in 1..3)
            assertTrue("${q.id}: 정답", q.answers.isNotEmpty() && q.answers.first().all(Hangul::isSyllable))
        }
    }

    @Test
    fun difficultyOneQuestions_containTheirAnswer() {
        (regionQuiz.values.flatten() + general).filter { it.difficulty == 1 }.forEach { q ->
            assertTrue("${q.id}: 두 개 중 고르기", q.text.contains(q.answers.first()))
        }
    }

    @Test
    fun ids_areUnique() {
        val ids = (regionQuiz.values.flatten() + general).map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun hintsDoNotRevealAnswers() {
        (regionQuiz.values.flatten() + general).forEach { q ->
            assertTrue("${q.id}: 힌트에 정답", !AnswerMatcher.normalize(q.hint).contains(AnswerMatcher.normalize(q.answers.first())) || q.answers.first().length == 1)
        }
    }

    @Test
    fun dictionary_hasAtLeast3000CleanNouns() {
        assertTrue("단어 수 ${words.size}", words.size >= 3000)
        assertEquals("중복 없음", words.size, words.toSet().size)
        words.forEach { w -> assertTrue("2음절 이상 한글: $w", w.length >= 2 && w.all(Hangul::isSyllable)) }
        val dict = WordDictionary(words)
        assertEquals(words.size, dict.size)
    }

    @Test
    fun dictionary_coversCommonChainSyllables() {
        val dict = WordDictionary(words)
        listOf('기', '이', '리', '자', '지', '수', '사', '도', '구', '차').forEach { s ->
            assertTrue("'$s' 로 이을 말", dict.continuationCount(s) >= 5)
        }
    }

    @Test
    fun chosungItems_areUsable() {
        assertTrue(chosung.size >= 20)
        chosung.forEach { assertTrue(it.word.all(Hangul::isSyllable)) }
    }

    @Test
    fun demoScripts_parse() {
        listOf("demo/answers_normal.json", "demo/answers_sleepy.json").forEach { path ->
            val entries = JSONObject(asset(path)).getJSONArray("entries")
            assertTrue(entries.length() >= 5)
        }
    }
}
