package com.gildongmu.app.domain.games

/** 한글 음절 분해·조합, 초성, 두음법칙, 조사 선택. */
object Hangul {
    private const val BASE = 0xAC00
    private const val LAST = 0xD7A3
    private const val MEDIAL_COUNT = 21
    private const val FINAL_COUNT = 28

    private val CHOSUNG = charArrayOf(
        'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
    )

    // 초성 인덱스
    private const val I_NIEUN = 2
    private const val I_RIEUL = 5
    private const val I_IEUNG = 11

    // 중성 인덱스
    private const val M_A = 0
    private const val M_AE = 1
    private const val M_YA = 2
    private const val M_YEO = 6
    private const val M_YE = 7
    private const val M_O = 8
    private const val M_OE = 11
    private const val M_YO = 12
    private const val M_U = 13
    private const val M_YU = 17
    private const val M_EU = 18
    private const val M_I = 20

    private const val F_RIEUL = 8

    fun isSyllable(c: Char): Boolean = c.code in BASE..LAST

    data class Jamo(val initial: Int, val medial: Int, val final: Int)

    fun decompose(c: Char): Jamo? {
        if (!isSyllable(c)) return null
        val offset = c.code - BASE
        return Jamo(offset / (MEDIAL_COUNT * FINAL_COUNT), (offset / FINAL_COUNT) % MEDIAL_COUNT, offset % FINAL_COUNT)
    }

    fun compose(j: Jamo): Char = (BASE + (j.initial * MEDIAL_COUNT + j.medial) * FINAL_COUNT + j.final).toChar()

    fun chosungOf(word: String): String = buildString {
        word.forEach { c -> decompose(c)?.let { append(CHOSUNG[it.initial]) } ?: append(c) }
    }

    fun hasBatchim(c: Char): Boolean = decompose(c)?.let { it.final != 0 } ?: false

    /**
     * 두음법칙 (한글 맞춤법 제10~12항). 받침은 유지한다.
     *  - ㄴ + ㅕ,ㅛ,ㅠ,ㅣ          → ㅇ  (녀→여, 뇨→요, 뉴→유, 니→이, 녁→역)
     *  - ㄹ + ㅑ,ㅕ,ㅖ,ㅛ,ㅠ,ㅣ     → ㅇ  (랴→야, 려→여, 례→예, 료→요, 류→유, 리→이, 력→역)
     *  - ㄹ + ㅏ,ㅐ,ㅗ,ㅚ,ㅜ,ㅡ     → ㄴ  (라→나, 래→내, 로→노, 뢰→뇌, 루→누, 르→느, 락→낙)
     * 변환이 없으면 null.
     */
    fun dueum(c: Char): Char? {
        val j = decompose(c) ?: return null
        val newInitial = when (j.initial) {
            I_NIEUN -> if (j.medial in setOf(M_YEO, M_YO, M_YU, M_I)) I_IEUNG else null
            I_RIEUL -> when (j.medial) {
                in setOf(M_YA, M_YEO, M_YE, M_YO, M_YU, M_I) -> I_IEUNG
                in setOf(M_A, M_AE, M_O, M_OE, M_U, M_EU) -> I_NIEUN
                else -> null
            }
            else -> null
        } ?: return null
        return compose(j.copy(initial = newInitial))
    }

    /** 끝말잇기에서 [lastSyllable] 다음에 올 수 있는 첫 음절들 = 원래 음절 + 두음법칙 변환 */
    fun allowedStarts(lastSyllable: Char): Set<Char> = setOfNotNull(lastSyllable, dueum(lastSyllable))

    /** 받침 유무에 따라 조사를 고른다. 한글이 아니면 받침 없음으로 본다. */
    private fun batchimOf(word: String): Jamo? = word.lastOrNull()?.let { decompose(it) }

    fun ieyo(word: String): String = word + if ((batchimOf(word)?.final ?: 0) != 0) "이에요" else "예요"
    fun ieosseoyo(word: String): String = word + if ((batchimOf(word)?.final ?: 0) != 0) "이었어요" else "였어요"
    fun eunNeun(word: String): String = word + if ((batchimOf(word)?.final ?: 0) != 0) "은" else "는"
    fun iGa(word: String): String = word + if ((batchimOf(word)?.final ?: 0) != 0) "이" else "가"
    fun eulReul(word: String): String = word + if ((batchimOf(word)?.final ?: 0) != 0) "을" else "를"

    /** (으)로: 받침이 없거나 ㄹ 받침이면 "로" */
    fun euro(word: String): String {
        val f = batchimOf(word)?.final ?: 0
        return word + if (f == 0 || f == F_RIEUL) "로" else "으로"
    }
}
