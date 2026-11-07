package com.gildongmu.app.domain.rest

import com.gildongmu.app.domain.model.GeoMath
import com.gildongmu.app.domain.model.GeoPoint
import kotlin.math.roundToInt

enum class RestStopKind(val label: String) {
    SERVICE_AREA("휴게소"),
    SHELTER("졸음쉼터"),
}

/**
 * 고속도로 졸음쉼터 1곳 (assets/content/rest_areas.json — 공공데이터포털 전국졸음쉼터표준데이터, tools/convert_rest_areas.js).
 * [from] → [to] 방향 차로의 쉼터다(원자료 "from기점 + to종점"). [heading] 은 같은 노선 이웃 쉼터로 구한 그 지점의
 * 진행 방향으로, 진행 방향이 맞는 쉼터만 고르는 데 쓴다.
 */
data class RestArea(
    val name: String,
    val route: String,
    val from: String,
    val to: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Int,
    val parking: Int,
    val toilet: Boolean,
) {
    val point: GeoPoint get() = GeoPoint(latitude, longitude)
    val displayName: String get() = "$name 졸음쉼터"
}

/**
 * 고속도로 휴게소 편의시설 (assets/content/service_areas.json — 한국도로공사 LOD 휴게소별 편의시설 + 가족사랑 화장실,
 * tools/convert_service_areas.js). 좌표가 없어 내비 TBT 의 경로 위 휴게소 이름으로 맞춘다.
 * [direction] 은 그 도시 쪽 차로, [dirLat]/[dirLng] 는 그 도시의 대략 좌표("통합" = 양방향 공용이면 없음).
 */
data class ServiceAreaInfo(
    val name: String,
    val direction: String,
    val amenities: List<String>,
    val foods: String?,
    val familyRestroom: Boolean?,
    val dirLat: Double?,
    val dirLng: Double?,
) {
    val dirPoint: GeoPoint? get() = if (dirLat != null && dirLng != null) GeoPoint(dirLat, dirLng) else null

    /** 졸음 케어·가족 여행에 의미 있는 시설만, 중요한 순서로 (수면실 → 수유실 → 가족사랑 화장실) */
    val highlights: List<String>
        get() = listOfNotNull(
            "수면실".takeIf { it in amenities },
            "수유실".takeIf { it in amenities },
            "가족사랑 화장실".takeIf { familyRestroom == true },
        )
}

/** 경로 위 쉼 장소 — 내비 TBT 또는 시뮬 경로. [distanceM] 은 지금 위치부터 경로를 따라간 거리 */
data class RouteRestStop(val name: String, val kind: RestStopKind, val distanceM: Int)

/** SDK 비의존 TBT 항목 (`TBTInfo` 의 type 이름·distance·description) */
data class TbtItem(val type: String, val distanceM: Double, val description: String)

object RouteRestStops {
    const val TYPE_SHELTER = "TURN_RESTAREA"
    const val TYPE_SERVICE_AREA = "TURN_ENTER_SERVICEAREA"
    private const val TYPE_ARRIVE = "TURN_ARRIVE"

    /**
     * 내비 TBT 목록 → 경로 위 휴게소·졸음쉼터 (경로 순서).
     * `TBTInfo.distance` 는 "지금부터"가 아니라 **앞 항목부터의 구간 거리**다(에뮬레이터 관측: 서울 → 전주 경로에서
     * 기흥휴게소 14000 → 오산졸음쉼터 9300 → … 처럼 순서대로 늘지 않고, 더하면 실제 거리와 맞는다) → 누적한다.
     * `TURN_ENTER_SERVICEAREA` 에는 하이패스센터 같은 곳도 있어 이름에 "휴게소"가 있는 것만 휴게소로 본다.
     */
    fun fromTbt(items: List<TbtItem>): List<RouteRestStop> {
        var cumulative = 0.0
        val out = mutableListOf<RouteRestStop>()
        for (item in items) {
            cumulative += item.distanceM.coerceAtLeast(0.0)
            if (item.type == TYPE_ARRIVE) break
            val kind = when (item.type) {
                TYPE_SHELTER -> RestStopKind.SHELTER
                TYPE_SERVICE_AREA -> RestStopKind.SERVICE_AREA.takeIf { item.description.contains("휴게소") }
                else -> null
            } ?: continue
            val name = item.description.trim().ifEmpty { kind.label }
            out += RouteRestStop(name, kind, cumulative.roundToInt())
        }
        return out
    }

    /** 목록을 받은 뒤 [travelledM] 만큼 더 갔을 때 — 지나친 곳은 뺀다 */
    fun advance(stops: List<RouteRestStop>, travelledM: Int): List<RouteRestStop> =
        if (travelledM <= 0) stops
        else stops.mapNotNull { s -> (s.distanceM - travelledM).takeIf { it > 0 }?.let { s.copy(distanceM = it) } }

    /** "풍세(하)졸음쉼터" → "풍세", "기흥휴게소" → "기흥", "오산 졸음쉼터" → "오산" */
    fun baseName(name: String): String =
        name.replace(Regex("\\(.*?\\)"), "").replace("졸음쉼터", "").replace("휴게소", "").replace(" ", "").trim()
}

/** 졸음 케어 2단계에서 권할 쉼 장소 */
data class RestStopSuggestion(
    val name: String,
    val kind: RestStopKind,
    val distanceM: Int,
    /** "경부선 부산 방향" — 표에서 고른 경우 운전자가 도로·방향을 확인할 수 있게 함께 말한다 */
    val directionLabel: String?,
    val source: Source,
    /** 좌표 (표에서 고른 졸음쉼터, 또는 경로 위 졸음쉼터를 표와 맞춘 것) — 도착 감지(근접 알림)에 쓴다 */
    val area: RestArea?,
    /** 휴게소 편의시설 (경로 위 휴게소를 편의시설 자료와 맞춘 것) */
    val facilities: ServiceAreaInfo? = null,
) {
    enum class Source(val label: String) {
        ON_ROUTE("경로 위"),
        TABLE("졸음쉼터 자료"),
    }

    val distanceLabel: String
        get() = if (distanceM >= 1_000) "${(distanceM / 100.0).roundToInt() / 10.0}km" else "${distanceM}m"

    /** 화면 한 줄: "수면실 · 수유실 · 가족사랑 화장실 · 대표 음식 안성국밥" */
    val facilityLine: String?
        get() {
            val f = facilities ?: return null
            val parts = f.highlights + listOfNotNull(f.foods?.let { "대표 음식 $it" })
            return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        }

    /** TTS 용: "가는 길에 있는 안성휴게소가 17킬로미터 앞이에요. 수면실과 수유실이 있어요." */
    val spokenTarget: String
        get() {
            val km = if (distanceM >= 1_000) "${(distanceM / 1000.0).roundToInt()}킬로미터" else "${distanceM}미터"
            val main = when (source) {
                Source.ON_ROUTE -> "가는 길에 있는 ${name}${subject(name)} $km 앞이에요."
                Source.TABLE -> {
                    val where = listOfNotNull(directionLabel, name).joinToString(" ")
                    "${where}${subject(where)} $km 앞에 있어요."
                }
            }
            // 시설 이름은 모두 "실"로 끝난다 (수면실·수유실·가족사랑 화장실) → 조사 "과"·"이"
            val note = facilities?.highlights?.take(2)?.takeIf { it.isNotEmpty() }?.joinToString("과 ")?.let { " ${it}이 있어요." }
            return main + (note ?: "")
        }

    companion object {
        fun onRoute(stop: RouteRestStop, area: RestArea?, facilities: ServiceAreaInfo?) =
            RestStopSuggestion(stop.name, stop.kind, stop.distanceM, null, Source.ON_ROUTE, area, facilities)

        /** 노선 이름을 함께 말해, 지금 달리는 도로가 아니면 운전자가 거절할 수 있게 한다 */
        fun fromTable(area: RestArea, distanceM: Int) =
            RestStopSuggestion(area.displayName, RestStopKind.SHELTER, distanceM, "${area.route} ${area.to} 방향", Source.TABLE, area)

        private fun subject(word: String) = if (endsWithBatchim(word)) "이" else "가"

        fun endsWithBatchim(word: String): Boolean {
            val c = word.lastOrNull() ?: return false
            if (c !in '가'..'힣') return false
            return (c - '가') % 28 != 0
        }
    }
}

/**
 * 쉼 장소 고르기 (순수 함수).
 *
 * 1. 경로 정보가 있으면(내비 TBT 목록·시뮬 경로) **경로 위에서만** 고른다 — 경로를 벗어나게 하지 않는다.
 *    가장 가까운 곳([MIN_AHEAD_M] ~ [ON_ROUTE_MAX_M]), 단 휴게소가 가장 가까운 졸음쉼터보다 [PREFER_SERVICE_AREA_M]
 *    안쪽으로만 더 멀면 휴게소(화장실·먹거리·수유실 — 가족 여행). 경로 위에 없으면 고르지 않는다.
 * 2. 경로 정보가 없을 때(길 안내 없음)만 공공데이터 졸음쉼터에서 진행 방향 앞쪽([nearestAhead]) — 근사라 노선 이름을 함께 말한다.
 *
 * 공공데이터에는 POI ID 가 없어 좌표로 `addWaypoint` 를 하면 내비가 고속도로 옆 일반 도로에 붙여 IC 로 나갔다 들어오는
 * 우회 경로를 만든다(11/7 에뮬레이터 관측) → 경유지는 넣지 않는다. 경로 위 쉼 장소는 내비가 이미 지나가는 곳이다.
 */
object RestStopFinder {
    const val DIRECTION_TOLERANCE_DEG = 45.0
    const val AHEAD_TOLERANCE_DEG = 45.0
    const val MIN_DISTANCE_M = 300.0
    const val MAX_DISTANCE_M = 80_000.0
    /** 경로 위: 이보다 가까우면 차로를 바꿔 들어가기 늦다 */
    const val MIN_AHEAD_M = 1_000
    const val ON_ROUTE_MAX_M = 80_000
    const val PREFER_SERVICE_AREA_M = 5_000
    private const val LATERAL_MIN_M = 2_000.0
    private const val LATERAL_RATIO = 0.25
    private const val LATERAL_WEIGHT = 3.0
    private const val SAME_DIRECTION_DEG = 90.0

    private data class Candidate(val area: RestArea, val distance: Double, val lateral: Double)

    /**
     * 표에서 진행 방향 앞쪽 졸음쉼터:
     *  - 방향: 그 지점의 도로 방향([RestArea.heading])이 내 진행 방향과 [DIRECTION_TOLERANCE_DEG] 이내 — 반대편 차로·교차 노선 제외
     *  - 앞쪽: 나 → 쉼터 방위각이 [AHEAD_TOLERANCE_DEG] 이내 — 지나친 쉼터 제외
     *  - 옆으로 벗어난 거리 ≤ max(2 km, 앞쪽 거리의 25%) — 나란히 가는 다른 고속도로를 덜 고르게
     *  - 점수 = 거리 + 3 × 옆 거리 가 가장 작은 곳, 거리 [MIN_DISTANCE_M] ~ [MAX_DISTANCE_M]
     */
    fun nearestAhead(areas: List<RestArea>, position: GeoPoint, headingDeg: Double): Pair<RestArea, Int>? =
        areas.asSequence()
            .filter { GeoMath.angleDiff(it.heading.toDouble(), headingDeg) <= DIRECTION_TOLERANCE_DEG }
            .mapNotNull { area ->
                val d = GeoMath.distanceM(position, area.point)
                val off = Math.toRadians(GeoMath.angleDiff(GeoMath.bearingDeg(position, area.point), headingDeg))
                val forward = d * kotlin.math.cos(off)
                val lateral = d * kotlin.math.abs(kotlin.math.sin(off))
                val ok = d in MIN_DISTANCE_M..MAX_DISTANCE_M &&
                    Math.toDegrees(off) <= AHEAD_TOLERANCE_DEG &&
                    lateral <= maxOf(LATERAL_MIN_M, LATERAL_RATIO * forward)
                if (ok) Candidate(area, d, lateral) else null
            }
            .minByOrNull { it.distance + LATERAL_WEIGHT * it.lateral }
            ?.let { it.area to it.distance.roundToInt() }

    /** 경로 위 목록에서 고르기 (규칙은 [RestStopFinder] 설명 1) */
    fun pickOnRoute(stops: List<RouteRestStop>): RouteRestStop? {
        val inRange = stops.filter { it.distanceM in MIN_AHEAD_M..ON_ROUTE_MAX_M }.sortedBy { it.distanceM }
        val nearest = inRange.firstOrNull() ?: return null
        if (nearest.kind == RestStopKind.SERVICE_AREA) return nearest
        return inRange.firstOrNull {
            it.kind == RestStopKind.SERVICE_AREA && it.distanceM <= nearest.distanceM + PREFER_SERVICE_AREA_M
        } ?: nearest
    }

    /**
     * 경로 위 졸음쉼터를 표에서 찾아 좌표를 얻는다(도착 감지용). 같은 이름의 상·하행 중 도로 방향이 진행 방향과 맞는 것.
     * 진행 방향을 모르면 반대편 차로 좌표를 쓸 수 있어 좌표 없이 둔다.
     */
    fun matchShelter(areas: List<RestArea>, name: String, headingDeg: Double?, position: GeoPoint?): RestArea? {
        if (headingDeg == null) return null
        val base = RouteRestStops.baseName(name)
        return areas.filter { it.name == base && GeoMath.angleDiff(it.heading.toDouble(), headingDeg) <= SAME_DIRECTION_DEG }
            .minByOrNull { a -> position?.let { GeoMath.distanceM(it, a.point) } ?: 0.0 }
    }

    /**
     * 경로 위 휴게소의 편의시설: 이름이 정확히 같은 것 중 방향 도시가 진행 방향 쪽인 차로. 방향을 모르면 상·하행 공통 시설만.
     * (이름 앞부분만 같은 다른 휴게소 — 예: 천안 / 천안삼거리 — 를 잘못 붙이지 않도록 정확히 같은 이름만 쓴다)
     */
    fun matchServiceArea(infos: List<ServiceAreaInfo>, name: String, headingDeg: Double?, position: GeoPoint?): ServiceAreaInfo? {
        val base = RouteRestStops.baseName(name)
        val same = infos.filter { it.name == base }
        if (same.isEmpty()) return null
        if (same.size == 1) return same.first()
        same.firstOrNull { it.dirPoint == null }?.let { return it } // "통합" — 양방향 공용
        if (headingDeg != null && position != null) {
            val best = same.minBy { GeoMath.angleDiff(GeoMath.bearingDeg(position, it.dirPoint!!), headingDeg) }
            if (GeoMath.angleDiff(GeoMath.bearingDeg(position, best.dirPoint!!), headingDeg) <= SAME_DIRECTION_DEG) return best
        }
        val common = same.map { it.amenities.toSet() }.reduce { a, b -> a intersect b }
        val family = if (same.all { it.familyRestroom == true }) true else null
        return ServiceAreaInfo(base, "", common.toList(), null, family, null, null)
    }

    /**
     * 권할 곳 하나. [onRoute] 가 null 이면 경로 정보 없음 → 표에서 고른 [table] 을 쓴다.
     * @param headingDeg·[position] 상·하행을 가르는 데 쓴다 (모르면 null)
     */
    fun suggest(
        onRoute: List<RouteRestStop>?,
        table: Pair<RestArea, Int>?,
        areas: List<RestArea>,
        infos: List<ServiceAreaInfo>,
        headingDeg: Double?,
        position: GeoPoint?,
    ): RestStopSuggestion? {
        if (onRoute == null) return table?.let { (area, d) -> RestStopSuggestion.fromTable(area, d) }
        val stop = pickOnRoute(onRoute) ?: return null
        return when (stop.kind) {
            RestStopKind.SHELTER -> RestStopSuggestion.onRoute(stop, matchShelter(areas, stop.name, headingDeg, position), null)
            RestStopKind.SERVICE_AREA -> RestStopSuggestion.onRoute(stop, null, matchServiceArea(infos, stop.name, headingDeg, position))
        }
    }
}

/**
 * 진행 방향 추정 (순수 함수): 실제로 움직인 방향 → 위치 SDK 의 heading → 목적지 쪽 방위각 순.
 * heading 이 정확히 0 이면(에뮬레이터 등) 모르는 값으로 본다.
 */
object MotionHeading {
    fun resolve(movedHeading: Double?, sdkHeading: Float?, position: GeoPoint, destination: GeoPoint?): Double? =
        movedHeading
            ?: sdkHeading?.takeIf { it.isFinite() && it != 0f && it in 0f..360f }?.toDouble()
            ?: destination?.let { GeoMath.bearingDeg(position, it) }
}
