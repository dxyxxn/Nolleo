package com.nolleo.app.data

import com.nolleo.app.domain.engine.AnswerMatcher
import com.nolleo.app.domain.games.Hangul
import com.nolleo.app.domain.games.IdiomQuizGame
import com.nolleo.app.domain.games.IdiomSpeech
import com.nolleo.app.domain.games.Outcome
import com.nolleo.app.domain.games.Turn
import com.nolleo.app.domain.games.TurnContext
import com.nolleo.app.domain.games.ValidWords
import com.nolleo.app.domain.games.WordBlocklist
import com.nolleo.app.domain.games.WordChainGame
import com.nolleo.app.domain.games.WordDictionary
import com.nolleo.app.domain.games.WordLevel
import com.nolleo.app.domain.model.Player
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** 실제 assets 콘텐츠가 게임 콘텐츠 규칙(지역 17곳·지역마다 8문항 이상·일반 40문항 이상 등)을 지키는지 검증한다. */
class ContentAssetsTest {

    private fun asset(path: String) = File("src/main/assets/$path").readText(Charsets.UTF_8)

    private val regions = ContentParser.parseRegions(asset("content/regions.json"))
    private val regionQuiz = ContentParser.parseRegionQuiz(asset("content/quiz_regions.json"))
    private val general = ContentParser.parseGeneralQuiz(asset("content/quiz_general.json"))
    private val idiomsJson = asset("content/quiz_idioms.json")
    private val idioms = ContentParser.parseIdioms(idiomsJson)
    private fun sentences(text: String) = text.split(Regex("(?<=[.?!])\\s*")).filter { it.isNotBlank() }

    /** 끝말잇기 사전은 크니까(검증용 약 19만 줄) 클래스에서 한 번만 읽는다 */
    companion object {
        private fun read(path: String) = File("src/main/assets/$path").readText(Charsets.UTF_8)
        private val validRaw by lazy { read("content/words_ko_valid.txt") }
        private val validWords by lazy { ContentParser.parseWords(validRaw) }
        private val playJson by lazy { read("content/words_ko_play.json") }
        private val playWords by lazy { ContentParser.parsePlayWords(playJson) }
        private val blocklist by lazy { WordBlocklist.parse(read("content/words_blocklist.txt")) }

        /** 앱과 같은 방식: 금칙어를 거른 출제용 + 검증용을 붙인 사전 */
        private val wordDictionary by lazy {
            val play = playWords.filterNot { blocklist.blocks(it.first) }
            WordDictionary(play.map { it.first }, play.toMap())
                .also { d -> d.attachValidation(ValidWords.of(validWords.filterNot { blocklist.blocks(it) })) }
        }
    }

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
    fun wordChainFiles_lineCounts() {
        assertEquals("검증용 줄 수", 194_871, validRaw.lineSequence().count { it.isNotBlank() })
        assertEquals("출제용 항목 수", 7_882, JSONArray(playJson).length())
        assertEquals("출제용 파싱", 7_882, playWords.size)
    }

    @Test
    fun wordChainFiles_cleanNouns_playInsideValid() {
        assertEquals("검증용 중복 없음", validWords.size, validWords.toSet().size)
        validWords.forEach { w -> assertTrue("검증용 2음절 이상 한글: $w", WordDictionary.isWord(w)) }
        val playSet = playWords.map { it.first }
        assertEquals("출제용 중복 없음", playSet.size, playSet.toSet().size)
        playSet.forEach { w -> assertTrue("출제용 2음절 이상 한글: $w", WordDictionary.isWord(w)) }
        val valid = validWords.toSet()
        assertTrue("출제용은 모두 검증용 안에", playSet.all { it in valid })
        // 등급 표기는 초급·중급·고급·팀목록 네 가지뿐
        val arr = JSONArray(playJson)
        val labels = (0 until arr.length()).map { arr.getJSONObject(it).getString("level") }.toSet()
        assertEquals(WordLevel.entries.map { it.label }.toSet(), labels)
    }

    @Test
    fun wordChain_blocklist_filtersBothSets_withoutEverydayFalsePositives() {
        listOf("살인", "맥주", "담배", "바보", "거지", "이혼녀").forEach { assertTrue("금칙어: $it", blocklist.blocks(it)) }
        listOf("술래", "술래잡기", "미술", "입술", "설거지", "사장님", "성교육", "시신경", "무기력", "얼음주머니", "고속도로", "주행거리", "물총")
            .forEach { assertFalse("오탐: $it", blocklist.blocks(it)) }
        assertTrue("출제용에서 빠진 말이 있다", playWords.count { blocklist.blocks(it.first) } > 0)
        assertFalse("출제용에 금칙어가 남지 않는다", "살인" in wordDictionary.continuations('살'))
        assertFalse("검증용에서도 빠진다", "살인" in wordDictionary)
    }

    @Test
    fun wordChain_validAcceptsEverydayCompounds_rejectsNonStandard() {
        assertTrue(wordDictionary.hasValidation)
        assertTrue("고속도로" in wordDictionary)
        assertTrue("주행거리" in wordDictionary)
        assertTrue("가자미" in wordDictionary)
        assertFalse("가재미(비표준어)", "가재미" in wordDictionary)
    }

    @Test
    fun wordChain_nonStandardWord_isAskedAgainThenLenientlyAccepted() {
        val lenient = mutableListOf<String>()
        val driver = Player(1, "아빠", isDriver = true)
        fun turnAfter(g: WordChainGame, last: String): Turn {
            g.restoreState(mapOf("used" to last, "lastWord" to last))
            return g.nextTurn(TurnContext(driver, Player.APP, null, 3))!!
        }
        val g = WordChainGame(wordDictionary, Random(1)) { lenient += it }
        val t = turnAfter(g, "만화가")
        assertEquals(Outcome.RETRY, g.judge(t, "가재미", attempt = 1).outcome)
        assertEquals(Outcome.ACCEPTED_LENIENT, g.judge(t, "가재미", attempt = 2).outcome)
        assertEquals(listOf("가재미"), lenient)
        val g2 = WordChainGame(wordDictionary, Random(1))
        assertEquals(Outcome.CORRECT, g2.judge(turnAfter(g2, "만화가"), "가자미", attempt = 1).outcome)
        val g3 = WordChainGame(wordDictionary, Random(1))
        assertEquals("사전 미등재 일상 복합어", Outcome.CORRECT, g3.judge(turnAfter(g3, "아주"), "주행거리", attempt = 1).outcome)
    }

    @Test
    fun dictionary_coversCommonChainSyllables_andDeadEndsAreComputed() {
        listOf('기', '이', '리', '자', '지', '수', '사', '도', '구', '차').forEach { s ->
            assertTrue("'$s' 로 이을 말", wordDictionary.continuationCount(s) >= 5)
            assertFalse("'$s' 는 막다른 음절이 아님", wordDictionary.isDeadEnd(s))
        }
        assertTrue("막다른 음절이 계산돼 있다", wordDictionary.deadEndSyllableCount > 0)
        assertTrue("'름'(이을 말 거의 없음)", wordDictionary.isDeadEnd('름'))
    }

    @Test
    fun idioms_everyItemParsesWithFourCharsAndReadings() {
        assertTrue("사자성어 100개 이상: ${idioms.size}", idioms.size >= 100)
        assertEquals("건너뛴 항목 없음", JSONArray(idiomsJson).length(), idioms.size)
        assertEquals("id 중복 없음", idioms.size, idioms.map { it.id }.toSet().size)
        assertEquals("정답 중복 없음", idioms.size, idioms.map { it.word }.toSet().size)
        idioms.forEach { i ->
            assertTrue(i.word, i.word.all(Hangul::isSyllable))
            assertEquals("${i.word} 글자 수 = 한자 풀이 수", i.word.length, i.chars.size)
            i.chars.forEach { ch -> assertTrue("${i.word} 훈·음", ch.hanja.isNotBlank() && ch.hun.isNotBlank() && ch.eum.isNotBlank()) }
            assertTrue(i.word, i.meaning.isNotBlank())
            // 뜻풀이 뒷부분("또는 그런 행동.")이 유래 칸에 들어가지 않게 (11/7 정리)
            i.origin?.let { o -> assertFalse("${i.word} 유래: $o", Regex("^(또는|곧|흔히|주로) ").containsMatchIn(o)) }
        }
    }

    @Test
    fun idioms_ttsLinesHaveNoHanjaOrBookMarks() {
        val hanja = Regex("[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uF900-\\uFAFF]")
        idioms.forEach { i ->
            (listOf(IdiomSpeech.question(i)) + IdiomSpeech.afterAnswer(i, correct = true)).forEach { line ->
                assertFalse("${i.word}: $line", hanja.containsMatchIn(line))
                assertFalse("${i.word}: $line", line.any { it in "≪≫<>“”‘’" })
                assertTrue("${i.word}: 빈 문장", line.isNotBlank())
            }
        }
    }

    @Test
    fun idioms_everyQuestionGetsFourDistinctChoicesWithoutSameMeaning() {
        val game = IdiomQuizGame(idioms, Random(7))
        repeat(idioms.size) {
            val q = game.next()!!
            assertEquals(4, q.choices.map { it.word }.toSet().size)
            assertEquals(q.item, q.answer)
            q.choices.filter { it != q.item }.forEach { d -> assertTrue("${q.item.word} vs ${d.word}", d.question != q.item.question) }
        }
    }

    @Test
    fun restAreas_areDomesticHighwayRestStopsWithBothDirections() {
        val areas = ContentParser.parseRestAreas(asset("content/rest_areas.json"))
        assertTrue("전국 졸음쉼터 200곳 이상: ${areas.size}", areas.size >= 200)
        areas.forEach { a ->
            assertTrue("${a.name} 위도", a.latitude in 33.0..39.0)
            assertTrue("${a.name} 경도", a.longitude in 124.0..132.0)
            assertTrue("${a.name} 방위각", a.heading in 0..359)
            assertTrue("${a.name} 기점≠종점", a.from != a.to)
        }
        // 시연 경로(경부선)에 양방향 쉼터가 있다
        val gyeongbu = areas.filter { it.route == "경부선" }
        assertTrue(gyeongbu.any { it.to == "부산" } && gyeongbu.any { it.to == "서울" })
    }

    @Test
    fun serviceAreas_haveDirectionsAndFacilities() {
        val infos = ContentParser.parseServiceAreas(asset("content/service_areas.json"))
        assertTrue("휴게소 150곳 이상: ${infos.size}", infos.size >= 150)
        assertTrue("수유실 정보", infos.count { "수유실" in it.amenities } >= 100)
        assertTrue("수면실 정보(졸음 케어)", infos.any { "수면실" in it.amenities })
        assertTrue("가족사랑 화장실 정보", infos.any { it.familyRestroom == true })
        infos.forEach { i ->
            assertTrue("${i.name} 이름에 '휴게소'·괄호가 남지 않는다", !i.name.contains("휴게소") && !i.name.contains("("))
            assertTrue("${i.name} 방향 도시 좌표(통합 제외)", i.direction == "통합" || ((i.dirLat ?: 0.0) in 33.0..39.0 && (i.dirLng ?: 0.0) in 124.0..132.0))
        }
        // 시뮬 여정(서울 → 부산)의 경로 위 휴게소가 자료와 맞는다
        listOf("기흥", "안성", "망향").forEach { n -> assertTrue("$n(부산)", infos.any { it.name == n && it.direction == "부산" }) }
    }

    @Test
    fun demoScripts_parse() {
        listOf("demo/answers_normal.json", "demo/answers_sleepy.json").forEach { path ->
            val entries = JSONObject(asset(path)).getJSONArray("entries")
            assertTrue(entries.length() >= 5)
        }
    }
}
