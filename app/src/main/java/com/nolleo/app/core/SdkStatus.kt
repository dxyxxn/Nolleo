package com.nolleo.app.core

/**
 * SDK/소스 초기화 결과. 초기화 실패는 예외가 아니라 상태다 (SDK 공통).
 * 앱은 절대 죽지 않고 폴백 소스로 넘어간다.
 */
sealed interface SdkStatus {
    data object Idle : SdkStatus
    data object Connecting : SdkStatus
    data object Available : SdkStatus
    data class Unavailable(val reason: String) : SdkStatus

    val label: String
        get() = when (this) {
            Idle -> "대기"
            Connecting -> "연결 중"
            Available -> "사용 가능"
            is Unavailable -> "불가($reason)"
        }

    val isAvailable: Boolean get() = this == Available
}
