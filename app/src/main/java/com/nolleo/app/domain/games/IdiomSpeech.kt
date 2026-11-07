package com.nolleo.app.domain.games

import com.nolleo.app.domain.model.IdiomItem

/**
 * 사자성어 게임이 TTS 로 읽을 문장. 화면에는 원문(한자·괄호 포함)을 그대로 보여 주고, 소리로는 읽기 좋게 다듬는다.
 *
 *  - 한자가 든 괄호 "(姻戚)" 는 통째로 뺀다 → "인척(姻戚)" 을 "인척 인척" 으로 두 번 읽지 않게
 *  - 책·편 이름 표시 ≪ ≫ < > 와 따옴표는 빼고, 가운뎃점 · 은 쉼표로 읽는다
 *  - 그 밖에 남은 한자는 TTS 가 엉뚱하게 읽거나 건너뛰므로 뺀다
 */
object IdiomSpeech {

    private const val HANJA = "[\\u3400-\\u4DBF\\u4E00-\\u9FFF\\uF900-\\uFAFF]"
    private val PAREN_WITH_HANJA = Regex("\\([^()]*$HANJA[^()]*\\)")
    private val STRAY_HANJA = Regex("$HANJA+")
    private val MARKS = Regex("[≪≫《》〈〉<>‘’“”\"]")
    private val EMPTY_PAREN = Regex("\\(\\s*\\)")
    private val SPACE_BEFORE_PUNCT = Regex("\\s+([,.?!])")
    private val DOUBLE_COMMA = Regex(",\\s*,")
    private val SPACES = Regex("\\s{2,}")

    fun clean(text: String): String = text
        .replace(PAREN_WITH_HANJA, "")
        .replace(MARKS, "")
        .replace("·", ", ")
        .replace(STRAY_HANJA, "")
        .replace(EMPTY_PAREN, "")
        .replace(SPACE_BEFORE_PUNCT, "$1")
        .replace(DOUBLE_COMMA, ",")
        .replace(SPACES, " ")
        .trim()

    /** 문제: 질문 원문 그대로 ("다음 뜻을 가진 사자성어는? …") */
    fun question(item: IdiomItem): String = clean(item.question)

    /** 고른 뒤: 정답 사자성어를 읽어 준다 */
    fun reveal(item: IdiomItem, correct: Boolean): String =
        if (correct) "정답! ${item.word}." else "아쉬워요. 정답은 ${item.word}."

    /** 유래가 있으면 이어서 읽는다 */
    fun origin(item: IdiomItem): String? = item.origin?.let(::clean)?.takeIf { it.isNotBlank() }

    /** 고른 뒤 읽을 문장 전체 (정답 → 유래) */
    fun afterAnswer(item: IdiomItem, correct: Boolean): List<String> = listOfNotNull(reveal(item, correct), origin(item))
}
