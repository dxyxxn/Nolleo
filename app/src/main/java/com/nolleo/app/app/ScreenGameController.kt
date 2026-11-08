package com.nolleo.app.app

import com.nolleo.app.core.speech.FocusChange

/** 정차 중 화면 게임 종류 (열 수 있는 것만 — 틀린 그림 찾기·노래방은 아직 카드만) */
enum class ScreenGameKind(val title: String) {
    IDIOM("사자성어"),
    REGION("지역 퀴즈"),
    WORD("끝말잇기"),
}

/**
 * 정차 중 화면 게임 하나의 진행 — 사자성어·지역 퀴즈는 [ScreenQuizController](보기 고르기),
 * 끝말잇기는 [ScreenWordChainController](말로 잇기). 코디네이터는 이 공통 동작으로 멈추고 정리한다.
 *
 * 모두 주행 중 음성 판과 따로 논다. 출발(SCREEN 이탈)·닫기·백그라운드·세션 종료·음성 게임의 발화·포커스 손실이면
 * 즉시 멈추고, 저절로 다시 말하지 않는다(자동 재생 금지, DEV_GUIDE §7.2–7.3).
 */
interface ScreenGameController {
    val kind: ScreenGameKind

    /** 이 게임이 오디오 포커스를 요청해 들고 있다 (포커스 변경은 [onFocusChange] 로 받는다) */
    val holdsFocus: Boolean

    /** 콘텐츠가 있어 게임을 열 수 있는지 */
    val isAvailable: Boolean

    /** 카드를 눌러 열기 (사용자 동작) */
    fun open()

    /** "← 목록"·뒤로: 소리를 멈추고 게임 목록으로 (점수·진행은 남긴다) */
    fun close()

    /** 소리·진행을 멈추고 포커스를 반납한다. 아무것도 안 하고 있으면 그냥 돌아간다. */
    fun stop(reason: String)

    /** 새 세션: 점수와 진행을 처음부터 */
    fun reset()

    /** 이 게임이 포커스를 들고 있을 때의 포커스 변경. LOSS 류는 즉시 멈추고, GAIN 이 와도 저절로 이어 하지 않는다. */
    fun onFocusChange(change: FocusChange, ownTts: Boolean)
}
