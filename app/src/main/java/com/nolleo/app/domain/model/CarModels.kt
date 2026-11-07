package com.nolleo.app.domain.model

/** 차량 입력에서 파생되는 주행 상태. */
enum class DriveState(val label: String) {
    /** gear == PARK (기어 미상일 때만: parkingBrake == true && speed ≈ 0) */
    PARKED("주차"),

    /** gear != PARK && speed < 0.5 m/s 가 3초 이상 (또는 속도 0 + 브레이크 페달) — 신호대기·정체 */
    STOPPED("정차(신호대기)"),

    /** speed ≥ 0.5 m/s, 또는 속도를 모를 때 gear != PARK */
    DRIVING("주행"),

    /** Car API 연결 실패 → 안전 쪽(VOICE, 화면 잠금)으로 처리 */
    UNKNOWN("알 수 없음"),
}

/** AAOS VehicleGear 를 도메인으로 옮긴 값. 도메인 계층은 android.car 에 의존하지 않는다. */
enum class GearPosition { PARK, REVERSE, NEUTRAL, DRIVE, OTHER }

/** 차량에서 읽은 원시 신호. null = 모름(미지원·퍼미션 거부·미연결). */
data class CarSignals(
    val connected: Boolean = false,
    val gear: GearPosition? = null,
    val speedMps: Float? = null,
    val parkingBrakeOn: Boolean? = null,
    val brakePedalPressed: Boolean? = null,
)

/**
 * CarUxRestrictionsManager 에서 받은 규제 상태.
 * 비트 값은 android.car.drivingstate.CarUxRestrictions 상수와 같다 (DEV_GUIDE §7.1).
 * 지역별 비트는 플랫폼이 내려주므로 앱은 받은 값만 반영한다.
 */
data class UxState(val requiresDO: Boolean, val activeRestrictions: Int) {

    private fun has(bit: Int) = requiresDO && (activeRestrictions and bit) != 0

    val noKeyboard: Boolean get() = has(UX_RESTRICTIONS_NO_KEYBOARD)
    val limitStringLength: Boolean get() = has(UX_RESTRICTIONS_LIMIT_STRING_LENGTH)
    val noSetup: Boolean get() = has(UX_RESTRICTIONS_NO_SETUP)
    val noVideo: Boolean get() = has(UX_RESTRICTIONS_NO_VIDEO)
    val noVoiceTranscription: Boolean get() = has(UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION)

    companion object {
        const val UX_RESTRICTIONS_NO_DIALPAD = 1
        const val UX_RESTRICTIONS_NO_FILTERING = 2
        const val UX_RESTRICTIONS_LIMIT_STRING_LENGTH = 4
        const val UX_RESTRICTIONS_NO_KEYBOARD = 8
        const val UX_RESTRICTIONS_NO_VIDEO = 16
        const val UX_RESTRICTIONS_LIMIT_CONTENT = 32
        const val UX_RESTRICTIONS_NO_SETUP = 64
        const val UX_RESTRICTIONS_NO_TEXT_MESSAGE = 128
        const val UX_RESTRICTIONS_NO_VOICE_TRANSCRIPTION = 256

        val UNRESTRICTED = UxState(requiresDO = false, activeRestrictions = 0)
    }
}

/** 앱이 결정하는 모드 (domain/engine/ModeArbiter). */
enum class GameMode(val label: String) {
    IDLE("대기"),
    VOICE("음성 모드"),
    SCREEN("화면 모드"),
    PAUSED("일시정지"),
}

/** VOICE 를 멈추게 하는 사유. 하나라도 있으면 PAUSED. */
enum class PauseReason(val title: String, val detail: String) {
    NAV_GUIDANCE("길 안내 후 이어서 할게요", "분기점·회전 안내 구간이에요"),
    AUDIO_FOCUS("통화가 끝나면 이어서 할게요", "다른 소리가 먼저 나오고 있어요"),
    AUDIO_UNAVAILABLE("소리를 낼 수 없어요", "잠시 후 계속을 눌러 주세요"),
    USER("잠시 멈췄어요", "계속을 누르면 같은 문제부터 다시 들려 드려요"),
    BACKGROUND("잠시 멈췄어요", "계속을 누르면 같은 문제부터 다시 들려 드려요"),
    /** 졸음 케어 2단계: 휴게소·졸음쉼터로 가는 동안 게임을 쉰다 (도착하면 풀림, 계속을 누르면 바로 이어 감) */
    REST_STOP("쉬어 갈 곳으로 가는 중이에요", "충분히 쉬고 이어서 해요 · 계속을 누르면 바로 이어져요"),
    /** 졸음쉼터 도착 안내를 한 뒤 — 차를 세웠다가 다시 출발하면 풀린다 (계속을 누르면 바로 이어 감) */
    REST_AREA("쉼터에서 쉬는 중이에요", "다시 출발하면 이어서 해요 · 바로 하려면 계속"),
}
