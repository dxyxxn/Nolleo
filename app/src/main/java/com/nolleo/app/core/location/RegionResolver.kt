package com.nolleo.app.core.location

import com.nolleo.app.domain.model.GeoPoint
import com.nolleo.app.domain.model.RegionInfo
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 위경도 → 시/도 코드. 정밀 폴리곤 대신 17개 시/도 바운딩 박스 + 중심점 거리로 판정하는
 * 오프라인 테이블(assets/content/regions.json)을 쓴다.
 *  - 박스 안에 있는 지역들이 후보. 광역시·특별시는 radiusKm 안이어야 후보가 된다.
 *  - 후보가 여럿이면(경계 겹침) 중심점이 가장 가까운 지역
 *  - 후보가 없으면 가장 가까운 중심점이 [FALLBACK_MAX_KM] 안일 때만 그 지역, 아니면 null(일반 퀴즈)
 */
class RegionResolver(private val regions: List<RegionInfo>) {

    fun resolve(point: GeoPoint): String? {
        val candidates = regions.filter { r ->
            point.latitude in r.minLat..r.maxLat && point.longitude in r.minLng..r.maxLng &&
                (r.radiusKm == null || distanceKm(point, r) <= r.radiusKm)
        }
        if (candidates.isNotEmpty()) return candidates.minBy { distanceKm(point, it) }.code
        val nearest = regions.minByOrNull { distanceKm(point, it) } ?: return null
        return if (distanceKm(point, nearest) <= FALLBACK_MAX_KM) nearest.code else null
    }

    /**
     * NaviHelper onCurrentLocationInfo.address 문자열에서 지역 추정 (FusedLocation 폴백).
     * 가장 긴(구체적인) 키워드가 일치하는 지역을 고른다. 예: "수정구 시흥동" → gyeonggi
     */
    fun fromAddress(address: String?): String? {
        if (address.isNullOrBlank()) return null
        var best: Pair<String, Int>? = null
        for (r in regions) {
            val names = r.keywords + r.name + r.shortName
            for (k in names) {
                if (k.length >= 2 && address.contains(k)) {
                    if (best == null || k.length > best.second) best = r.code to k.length
                }
            }
        }
        return best?.first
    }

    fun distanceKm(p: GeoPoint, r: RegionInfo): Double = haversineKm(p.latitude, p.longitude, r.centerLat, r.centerLng)

    companion object {
        const val FALLBACK_MAX_KM = 40.0

        fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
            return 2 * 6371.0 * asin(sqrt(a))
        }
    }
}
