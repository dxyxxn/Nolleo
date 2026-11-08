package com.nolleo.app.core.navi

import com.nolleo.app.domain.model.GeoMath
import com.nolleo.app.domain.model.GeoPoint
import java.util.Locale
import kotlin.math.abs

/**
 * 내비가 알려 준 장소 — 최근 목적지(`NaviHelper.getRecentDestinationInfo` → `onRecentDestinationInfo`).
 * 홈에서 고르고 출발하면 이 정보로 길 안내를 요청한다(`requestRoute`). 이름·주소·좌표는 개인정보라 로그는 AppLog.sensitive 로만.
 */
data class NaviPlace(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val poiId: String,
    val poiSubId: String?,
) {
    /** 목록 선택·예상 시간을 맞추는 열쇠 (좌표 소수 5자리 ≈ 1 m) */
    val key: String get() = keyOf(latitude, longitude)

    val point: GeoPoint get() = GeoPoint(latitude, longitude)

    companion object {
        /** 같은 곳으로 보는 거리 — 예상 시간 응답의 목적지 좌표와 요청 좌표 비교 */
        private const val SAME_SPOT_M = 300.0

        fun keyOf(latitude: Double, longitude: Double): String = String.format(Locale.US, "%.5f,%.5f", latitude, longitude)

        /** 이름이 비었거나 좌표가 0·범위 밖이면 쓰지 않는다 (등록 안 된 집·회사 즐겨찾기가 이렇게 온다 — 에뮬레이터 관측) */
        fun isUsable(p: NaviPlace): Boolean =
            p.name.isNotBlank() &&
                p.latitude in -90.0..90.0 && p.longitude in -180.0..180.0 &&
                !(abs(p.latitude) < 1e-6 && abs(p.longitude) < 1e-6)

        /** 내비 순서 그대로, 쓸 수 있는 곳만, 같은 곳은 한 번 */
        fun clean(places: List<NaviPlace>): List<NaviPlace> = places.filter(::isUsable).distinctBy { it.key }

        /** 예상 시간 응답이 이 장소에 대한 것인가 (응답에 요청 번호가 없어 목적지 좌표로 맞춘다) */
        fun sameSpot(p: NaviPlace, latitude: Double, longitude: Double): Boolean =
            GeoMath.distanceM(p.point, GeoPoint(latitude, longitude)) <= SAME_SPOT_M
    }
}
