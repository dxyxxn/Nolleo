package com.nolleo.app.domain.games

import com.nolleo.app.core.speech.ListenHint
import com.nolleo.app.domain.engine.AnswerMatcher
import com.nolleo.app.domain.model.GameKind
import com.nolleo.app.domain.model.Player
import kotlin.random.Random

/**
 * 끝말잇기: 앱 ↔ 운전자 ↔ 동승자 순서.
 *  - 마지막 음절로 시작 + 두음법칙 양방향(력↔역, 래↔내, 리↔니↔이 …, [Hangul.allowedStarts]) + 사용 단어 중복 금지 + 2음절 이상
 *  - 사람 낱말은 검증용 사전으로 판정. 없으면 한 번 "다시 한번 말해 줄래요?" 후 관대하게 인정 (가족 게임; 로그에는 남긴다)
 *  - 앱 차례에는 출제용 사전에서만, 졸음 케어 난이도 상한 안의 등급([WordLevel])으로, 다음 사람이 이을 수 있는 단어를 고른다
 *    (막다른 음절 회피). 막다른 말밖에 없으면 [RESTART_LINE] 으로 새 낱말부터 다시, 이을 말이 아예 없으면 "제가 졌어요!"
 *  - 사람이 못 이으면 같은 음절이 다음 사람에게 넘어간다
 */
class WordChainGame(
    private val dictionary: WordDictionary,
    private val random: Random = Random.Default,
    private val onLenientAccept: (String) -> Unit = {},
) : VoiceGame {

    override val kind = GameKind.WORD_CHAIN

    private val used = linkedSetOf<String>()
    private var lastWord: String? = null
    private var appJustNamedNext = false

    val chainLength: Int get() = used.size
    val currentWord: String? get() = lastWord

    override fun rotation(players: List<Player>): List<Player> = listOf(Player.APP) + players

    override fun intro(players: List<Player>, questionCount: Int): String =
        "끝말잇기를 시작할게요! 제가 먼저 할게요."

    private fun requiredSyllable(): Char? = lastWord?.last()

    /** "'과'" / "'력'이나 '역'" */
    private fun syllableLabel(s: Char): String = Hangul.syllableLabel(s)

    /** "'과'로" / "'력'이나 '역'으로" */
    fun startsWithPhrase(s: Char): String = Hangul.startsWithPhrase(s)

    override fun nextTurn(ctx: TurnContext): Turn? {
        return if (ctx.player.id == Player.APP.id) appTurn(ctx) else humanTurn(ctx)
    }

    private fun appTurn(ctx: TurnContext): Turn {
        val required = requiredSyllable()
        var restarted = false
        val word = if (required == null) {
            dictionary.randomStarter(random, STARTERS, ctx.difficultyCap, exclude = used)
        } else {
            when (val pick = dictionary.pick(required, used, random, ctx.difficultyCap)) {
                is WordDictionary.Pick.Word -> pick.word
                // 이을 말이 다음 사람을 막다른 길로 모는 말뿐이면, 판을 끝내지 않고 새 낱말로 다시 시작한다
                WordDictionary.Pick.OnlyDeadEnds -> dictionary.randomStarter(random, STARTERS, ctx.difficultyCap, exclude = used)
                    ?.also { restarted = true }
                WordDictionary.Pick.Nothing -> null
            }
        }
        if (word == null) {
            appJustNamedNext = false
            val s = required ?: '가'
            return Turn(
                player = Player.APP,
                prompt = "음… ${startsWithPhrase(s)} 시작하는 말이 생각이 안 나요. 제가 졌어요!",
                display = "놀레오: 제가 졌어요!",
                shortDisplay = "놀레오가 졌어요",
                expectsAnswer = false,
                endsRoundAfter = true,
            )
        }
        used += word
        lastWord = word
        appJustNamedNext = true
        val next = word.last()
        return Turn(
            player = Player.APP,
            prompt = (if (restarted) "$RESTART_LINE " else "") + "저는 '$word'! ${ctx.nextPlayer.name}, ${startsWithPhrase(next)} 시작하는 말!",
            display = (if (restarted) "놀레오: 다른 말로 다시 시작 — $word" else "놀레오: $word") + "\n${startsWithPhrase(next)} 시작하는 말",
            shortDisplay = "${syllableLabel(next)} 차례",
            expectsAnswer = false,
        )
    }

    private fun humanTurn(ctx: TurnContext): Turn {
        val required = requiredSyllable() ?: '가'
        val starts = Hangul.allowedStarts(required)
        val fullPrompt = "${startsWithPhrase(required)} 시작하는 말, ${ctx.player.name} 차례예요!"
        val prompt = if (appJustNamedNext) "" else fullPrompt
        appJustNamedNext = false
        val sample = dictionary.pickNext(required, used, random, avoidDeadEnd = false, difficultyCap = ctx.difficultyCap)
        return Turn(
            player = ctx.player,
            prompt = prompt,
            reprompt = fullPrompt,
            display = "${lastWord ?: ""} → ${startsWithPhrase(required)} 시작하는 말",
            shortDisplay = "${syllableLabel(required)} 시작",
            hint = ListenHint(
                correctAnswer = sample,
                wrongAnswer = dictionary.randomNotStartingWith(starts, random),
                forDriver = ctx.player.isDriver,
            ),
        )
    }

    /** STT 문장에서 단어 하나를 뽑는다: 요구 음절로 시작하는 토큰 우선, 존댓말 어미 제거 */
    fun extractWord(answer: String, required: Char?): String {
        val tokens = answer.split(Regex("\\s+")).map { AnswerMatcher.stripPoliteSuffix(AnswerMatcher.normalize(it)) }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return ""
        val starts = required?.let { Hangul.allowedStarts(it) }.orEmpty()
        return tokens.firstOrNull { it.first() in starts }
            ?: AnswerMatcher.stripPoliteSuffix(AnswerMatcher.normalize(answer)).ifEmpty { tokens.last() }
    }

    override fun judge(turn: Turn, answer: String?, attempt: Int): Judgement {
        val required = requiredSyllable() ?: '가'
        if (answer.isNullOrBlank()) {
            return Judgement(Outcome.NO_ANSWER, "시간이 다 됐어요! 다음 사람이 이어 볼까요?", correct = false, label = "무응답")
        }
        val word = extractWord(answer, required)
        val starts = Hangul.allowedStarts(required)
        return when {
            word.length < 2 || !word.all(Hangul::isSyllable) ->
                if (attempt == 1) retry() else miss("두 글자 이상 낱말로 말해 주세요.", "짧은 말")
            word.first() !in starts ->
                miss("아쉬워요. ${startsWithPhrase(required)} 시작해야 해요.", "'$word' 시작 음절 틀림")
            word in used -> miss("'$word'${if (Hangul.hasBatchim(word.last())) "은" else "는"} 이미 나왔어요!", "'$word' 중복")
            word !in dictionary ->
                if (attempt == 1) {
                    retry()
                } else {
                    onLenientAccept(word)
                    accept(word, lenient = true)
                }
            else -> accept(word, lenient = false)
        }
    }

    private fun retry() = Judgement(Outcome.RETRY, "다시 한번 말해 줄래요?", correct = null, label = "다시 듣기")

    private fun miss(feedback: String, label: String) =
        Judgement(Outcome.WRONG, "$feedback 다음 사람이 이어 볼까요?", correct = false, label = label)

    private fun accept(word: String, lenient: Boolean): Judgement {
        used += word
        lastWord = word
        return Judgement(
            outcome = if (lenient) Outcome.ACCEPTED_LENIENT else Outcome.CORRECT,
            feedback = "좋아요, '$word'!",
            scoreDelta = 10,
            correct = true,
            label = if (lenient) "'$word' 인정!" else "'$word' 성공!",
        )
    }

    override fun outro(): String = "끝말잇기 한 판 끝! 모두 ${used.size}개 낱말을 이었어요."

    override fun saveState(): Map<String, String> = mapOf(
        "used" to used.joinToString(","),
        "lastWord" to (lastWord ?: ""),
    )

    override fun restoreState(state: Map<String, String>) {
        used.clear()
        state["used"]?.split(",")?.filter { it.isNotBlank() }?.let { used += it }
        lastWord = state["lastWord"]?.takeIf { it.isNotBlank() }
        appJustNamedNext = false
    }

    companion object {
        val STARTERS = listOf("기차", "나무", "사과", "바다", "토끼", "구름", "우산", "고래", "하늘", "자동차", "바나나", "도토리")

        /** 앱이 이을 말이 막다른 말뿐일 때 — 판을 끝내지 않고 새 낱말로 다시 시작한다 */
        const val RESTART_LINE = "이 말은 잇기 어렵네요, 다른 말로 다시 시작할까요?"
    }
}
