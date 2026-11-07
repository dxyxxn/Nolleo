package com.gildongmu.app.domain.rest

import com.gildongmu.app.domain.model.GeoMath
import com.gildongmu.app.domain.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RestStopFinderTest {

    // 경부선 오산 졸음쉼터 상·하행 (자료 좌표), 서울→부산 방위각 ≈ 135°
    private val osanSouthbound = RestArea("오산", "경부선", "서울", "부산", 37.15670802, 127.0871649, 135, 14, true)
    private val osanNorthbound = RestArea("오산", "경부선", "부산", "서울", 37.1542659, 127.0861027, 315, 14, true)
    private val ipjangSouthbound = RestArea("입장", "경부선", "서울", "부산", 36.91443246, 127.1884647, 135, 10, true)
    private val all = listOf(osanSouthbound, osanNorthbound, ipjangSouthbound)

    @Test
    fun geoMath_basics() {
        val seoul = GeoPoint(37.5665, 126.9780)
        val busan = GeoPoint(35.1796, 129.0756)
        val d = GeoMath.distanceM(seoul, busan)
        assertTrue("서울-부산 직선 약 325km: $d", d in 315_000.0..335_000.0)
        val b = GeoMath.bearingDeg(seoul, busan)
        assertTrue("남동쪽: $b", b in 130.0..150.0)
        assertEquals(20.0, GeoMath.angleDiff(350.0, 10.0), 1e-9)
        assertEquals(180.0, GeoMath.angleDiff(0.0, 180.0), 1e-9)
    }

    @Test
    fun southbound_picksNearestAheadOnSameDirection() {
        val suwon = GeoPoint(37.27, 127.03) // 오산보다 북쪽, 부산 방향으로 가는 중
        val (area, distance) = RestStopFinder.nearestAhead(all, suwon, headingDeg = 160.0)!!
        assertEquals(osanSouthbound, area)
        assertTrue(distance in 10_000..16_000)
    }

    @Test
    fun northbound_skipsOppositeCarriageway() {
        val cheonan = GeoPoint(36.80, 127.15)
        val picked = RestStopFinder.nearestAhead(all, cheonan, headingDeg = 340.0)
        assertEquals("반대편(부산 방향) 쉼터는 고르지 않는다", osanNorthbound, picked?.first)
    }

    @Test
    fun passedRestAreasAreIgnored() {
        val southOfIpjang = GeoPoint(36.80, 127.20)
        assertNull("이미 지나친 쉼터만 있으면 없음", RestStopFinder.nearestAhead(all, southOfIpjang, headingDeg = 160.0))
    }

    @Test
    fun crossingRouteIsNotPicked_evenIfCloser() {
        // 서울 남쪽에서 남행 중: 동쪽으로 가는 영동선 쉼터(가까움)보다 같은 방향 경부선 쉼터
        val yonginEastbound = RestArea("용인", "영동선", "인천", "강릉", 37.2468, 127.2153, 95, 20, true)
        val pangyo = GeoPoint(37.40, 127.10)
        assertEquals(osanSouthbound, RestStopFinder.nearestAhead(all + yonginEastbound, pangyo, headingDeg = 180.0)?.first)
    }

    @Test
    fun parallelHighwayFarToTheSide_isPenalized() {
        // 바로 앞 경부선(옆 0.5km, 30km)과 옆으로 12km 떨어진 다른 고속도로 쉼터(25km)
        val p = GeoPoint(37.40, 127.10)
        val ahead = RestArea("앞", "경부선", "서울", "부산", 37.13, 127.10, 180, 10, true)
        val side = RestArea("옆", "중부선", "하남", "통영", 37.20, 127.235, 180, 10, true)
        assertEquals(ahead, RestStopFinder.nearestAhead(listOf(ahead, side), p, headingDeg = 180.0)?.first)
    }

    // 에뮬레이터 순정 내비가 판교 → 전주 경로로 준 TBT 앞부분 (11/7 logcat) — distance 는 앞 항목부터의 구간 거리
    private val jeonjuTbt = listOf(
        TbtItem("TURN_LEFT", 80.0, "기업지원허브 방면"),
        TbtItem("TURN_RIGHT", 380.0, "판교IC 방면"),
        TbtItem("TURN_AVOID_UNDER_R", 730.0, "판교IC 방면"),
        TbtItem("TURN_ENTER_IC_R", 1700.0, "판교IC - 부산 방면"),
        TbtItem("TURN_ENTER_SERVICEAREA", 2600.0, "하이패스센터"),
        TbtItem("TURN_TOLL", 1000.0, "서울톨게이트"),
        TbtItem("TURN_ENTER_SERVICEAREA", 14000.0, "기흥휴게소"),
        TbtItem("TURN_RESTAREA", 9300.0, "오산졸음쉼터"),
        TbtItem("TURN_KEEP_R", 100.0, "대전 방면"),
        TbtItem("TURN_ENTER_SERVICEAREA", 15000.0, "안성휴게소"),
        TbtItem("TURN_RESTAREA", 7500.0, "연곡졸음쉼터"),
        TbtItem("TURN_ARRIVE", 200.0, "도착"),
        TbtItem("TURN_RESTAREA", 100.0, "도착 뒤 항목"),
    )

    @Test
    fun tbtDistances_areCumulative_andOnlyRealRestStopsCount() {
        val stops = RouteRestStops.fromTbt(jeonjuTbt)
        assertEquals(listOf("기흥휴게소", "오산졸음쉼터", "안성휴게소", "연곡졸음쉼터"), stops.map { it.name })
        assertEquals("하이패스센터는 휴게소가 아니다", RestStopKind.SERVICE_AREA, stops[0].kind)
        assertEquals("기흥휴게소 = 80+380+730+1700+2600+1000+14000", 20_490, stops[0].distanceM)
        assertEquals("오산졸음쉼터 = 기흥 + 9300 (구간 거리를 그대로 쓰면 9.3 km 로 틀린다)", 29_790, stops[1].distanceM)
        assertEquals(RestStopKind.SHELTER, stops[1].kind)
        val later = RouteRestStops.advance(stops, 21_000)
        assertEquals("지나친 기흥휴게소는 빠진다", listOf("오산졸음쉼터", "안성휴게소", "연곡졸음쉼터"), later.map { it.name })
        assertEquals(8_790, later[0].distanceM)
    }

    @Test
    fun pickOnRoute_nearest_butServiceAreaWhenOnlySlightlyFarther() {
        fun s(name: String, kind: RestStopKind, d: Int) = RouteRestStop(name, kind, d)
        val sa = RestStopKind.SERVICE_AREA
        val sh = RestStopKind.SHELTER
        assertEquals("휴게소가 5 km 안쪽으로만 더 멀면 휴게소", "안성휴게소",
            RestStopFinder.pickOnRoute(listOf(s("오산졸음쉼터", sh, 9_000), s("안성휴게소", sa, 13_500)))?.name)
        assertEquals("더 멀면 가까운 졸음쉼터", "오산졸음쉼터",
            RestStopFinder.pickOnRoute(listOf(s("오산졸음쉼터", sh, 9_000), s("안성휴게소", sa, 24_500)))?.name)
        assertEquals("1 km 안쪽은 들어가기 늦어 건너뛴다", "안성휴게소",
            RestStopFinder.pickOnRoute(listOf(s("기흥휴게소", sa, 600), s("안성휴게소", sa, 20_000)))?.name)
        assertNull("80 km 밖뿐이면 없음", RestStopFinder.pickOnRoute(listOf(s("먼 휴게소", sa, 90_000))))
    }

    private val giheungBusan = ServiceAreaInfo("기흥", "부산", listOf("수유실", "약국"), "제주고사리육개장", true, 35.180, 129.075)
    private val anseongBusan = ServiceAreaInfo("안성", "부산", listOf("수유실", "샤워실", "세탁실"), "안성국밥", true, 35.180, 129.075)
    private val anseongSeoul = ServiceAreaInfo("안성", "서울", listOf("수유실", "내고장특산물", "약국"), "안성국밥", true, 37.566, 126.978)
    private val okcheonSleep = ServiceAreaInfo("옥산", "부산", listOf("수유실", "수면실", "샤워실"), "순두부 청국장", null, 35.180, 129.075)
    private val infos = listOf(giheungBusan, anseongBusan, anseongSeoul, okcheonSleep)

    @Test
    fun serviceArea_matchesDirectionByTravelHeading() {
        val pyeongtaek = GeoPoint(37.0, 127.1)
        assertEquals("남쪽으로 가면 부산 방향", anseongBusan, RestStopFinder.matchServiceArea(infos, "안성휴게소", 160.0, pyeongtaek))
        assertEquals("북쪽으로 가면 서울 방향", anseongSeoul, RestStopFinder.matchServiceArea(infos, "안성휴게소", 340.0, pyeongtaek))
        val unknown = RestStopFinder.matchServiceArea(infos, "안성휴게소", null, null)!!
        assertEquals("방향을 모르면 상·하행 공통 시설만", listOf("수유실"), unknown.amenities)
        assertNull("대표 음식은 차로마다 다를 수 있어 빼고", unknown.foods)
        assertNull("이름이 정확히 같지 않으면 붙이지 않는다", RestStopFinder.matchServiceArea(infos, "안성맞춤휴게소", 160.0, pyeongtaek))
    }

    @Test
    fun shelterOnRoute_getsCoordinatesOnlyWithKnownHeading() {
        assertEquals(osanSouthbound, RestStopFinder.matchShelter(all, "오산졸음쉼터", 165.0, GeoPoint(37.3, 127.05)))
        assertEquals(osanNorthbound, RestStopFinder.matchShelter(all, "오산졸음쉼터", 345.0, GeoPoint(37.0, 127.1)))
        assertNull("방향을 모르면 반대편 차로일 수 있어 좌표 없이", RestStopFinder.matchShelter(all, "오산졸음쉼터", null, null))
        assertEquals("(하) 같은 표기는 떼고 맞춘다", "오산", RouteRestStops.baseName("오산(하)졸음쉼터"))
    }

    @Test
    fun suggest_usesRouteWhenKnown_tableOnlyWithoutRoute() {
        val p = GeoPoint(37.30, 127.05)
        val route = RouteRestStops.fromTbt(jeonjuTbt)
        val onRoute = RestStopFinder.suggest(route, osanSouthbound to 12_000, all, infos, 165.0, p)!!
        assertEquals(RestStopSuggestion.Source.ON_ROUTE, onRoute.source)
        assertEquals("기흥휴게소", onRoute.name)
        assertEquals(giheungBusan, onRoute.facilities)
        assertNull("경로 정보가 있는데 경로 위에 없으면 표에서 고르지 않는다", RestStopFinder.suggest(emptyList(), osanSouthbound to 12_000, all, infos, 165.0, p))
        assertEquals("경로 정보가 없으면 표", RestStopSuggestion.Source.TABLE, RestStopFinder.suggest(null, osanSouthbound to 12_000, all, infos, 165.0, p)?.source)
        val shelter = RestStopFinder.suggest(listOf(RouteRestStop("오산졸음쉼터", RestStopKind.SHELTER, 9_000)), null, all, infos, 165.0, p)!!
        assertEquals("경로 위 졸음쉼터는 표와 맞춰 좌표(도착 감지)를 얻는다", osanSouthbound, shelter.area)
    }

    @Test
    fun suggestionText() {
        val s = RestStopSuggestion.fromTable(osanSouthbound, 12_400)
        assertEquals("오산 졸음쉼터", s.name)
        assertEquals("경부선 부산 방향", s.directionLabel)
        assertEquals("12.4km", s.distanceLabel)
        assertEquals("경부선 부산 방향 오산 졸음쉼터가 12킬로미터 앞에 있어요.", s.spokenTarget)
        val sa = RestStopSuggestion.onRoute(RouteRestStop("옥산휴게소", RestStopKind.SERVICE_AREA, 17_200), null, okcheonSleep)
        assertEquals("가는 길에 있는 옥산휴게소가 17킬로미터 앞이에요. 수면실과 수유실이 있어요.", sa.spokenTarget)
        assertEquals("수면실 · 수유실 · 대표 음식 순두부 청국장", sa.facilityLine)
        val fam = RestStopSuggestion.onRoute(RouteRestStop("기흥휴게소", RestStopKind.SERVICE_AREA, 20_490), null, giheungBusan)
        assertEquals("가는 길에 있는 기흥휴게소가 20킬로미터 앞이에요. 수유실과 가족사랑 화장실이 있어요.", fam.spokenTarget)
    }

    @Test
    fun motionHeading_priority() {
        val p = GeoPoint(37.0, 127.0)
        val dest = GeoPoint(35.18, 129.08)
        assertEquals(200.0, MotionHeading.resolve(200.0, 90f, p, dest)!!, 1e-9)
        assertEquals(90.0, MotionHeading.resolve(null, 90f, p, dest)!!, 1e-9)
        val toDest = MotionHeading.resolve(null, 0f, p, dest)!!
        assertTrue("heading 0 은 모름 → 목적지 쪽: $toDest", toDest in 120.0..150.0)
        assertNull(MotionHeading.resolve(null, null, p, null))
    }
}
