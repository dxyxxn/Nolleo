package com.nolleo.app.domain.model

enum class TripSourceKind(val label: String) { REAL_NAVI("내비 연동"), SIMULATED("시뮬레이션 여정") }

/** NaviHelper NavigationStatus 의 도메인 사본 (SDK 비의존). */
enum class NaviStatus { INACTIVE, INITIALIZING, FREE_DRIVE, ROUTE_DRIVE }

/** TBTInfo 첫 항목 요약. */
data class TurnHint(
    val typeName: String,
    val distanceM: Int,
    val description: String,
    /** 분기/진출입/회전 등 운전 집중이 필요한 종류인지 */
    val needsAttention: Boolean,
)

/** 트립 컨텍스트 (지시서 §3). */
data class TripContext(
    val destinationName: String,
    val remainTimeSec: Int?,
    val remainDistanceM: Int?,
    val navigationStatus: NaviStatus?,
    val nextTurn: TurnHint?,
    val regionCode: String?,
    val source: TripSourceKind,
)

data class GeoPoint(val latitude: Double, val longitude: Double)
