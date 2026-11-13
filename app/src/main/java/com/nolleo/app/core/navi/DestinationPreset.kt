package com.nolleo.app.core.navi

import com.nolleo.app.domain.rest.RestStopKind

/**
 * 시뮬레이션 여정의 경로 위 휴게소·졸음쉼터 — 에뮬레이터 순정 내비가 같은 목적지로 준 TBT 목록을 누적 거리로 옮긴 것.
 * [km] 는 내비 경로의 출발점부터 거리. 시뮬은 "간 비율 × [DestinationPreset.routeKm]" 로 지금 위치를 정한다.
 */
data class SimRestStop(val name: String, val kind: RestStopKind, val km: Double)

/** 시뮬레이션 여정의 경유 지점 (여정 시작 후 [atMin] 분에 이 지역을 지난다). */
data class RouteWaypoint(
    val atMin: Int,
    val regionCode: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * 시뮬레이션 경로 (디버그 패널에서 고름, 여정=시뮬일 때 홈 "현재 경로" 자리에 보인다). 시연용이다 —
 * 에뮬레이터는 차가 움직이지 않아 실제 내비 여정은 남은 시간이 줄지 않는다. 11/8 부터 홈에는 프리셋을 두지 않는다
 * (실제 사용은 현재 경로·내비 최근 목적지·목적지 없이 시간).
 * 경로·쉼 장소·지나는 지역은 에뮬레이터 순정 내비가 같은 목적지로 준 경로를 옮긴 것이다.
 */
data class DestinationPreset(
    val id: String,
    /** "서울 → 부산" */
    val title: String,
    val poiName: String,
    val latitude: Double,
    val longitude: Double,
    val estimatedMinutes: Int,
    val timeline: List<RouteWaypoint>,
    /** 시뮬레이션에서 "분기점 안내 300 m" TBT 를 낼 시점(분) */
    val tbtAtMin: List<Int>,
    /** 실제 1분 = 여정 N분 */
    val defaultTimeScale: Float,
    /** 내비 경로 거리(km, 에뮬레이터 순정 내비 기준) — 시뮬 여정의 경로 위 쉼 장소 거리 계산용 */
    val routeKm: Double = 0.0,
    /** 시뮬 여정의 경로 위 휴게소·졸음쉼터. null = 경로 정보 없음(졸음쉼터 표에서 고른다) */
    val restStops: List<SimRestStop>? = null,
) {
    val estimatedSec: Int get() = estimatedMinutes * 60

    companion object {
        private val SEOUL_START = RouteWaypoint(0, "seoul", "서울", 37.5665, 126.9780)
        private val SA = RestStopKind.SERVICE_AREA
        private val SH = RestStopKind.SHELTER

        val SEOUL_BUSAN = DestinationPreset(
            id = "busan",
            title = "서울 → 부산",
            poiName = "부산역",
            latitude = 35.115225,
            longitude = 129.042243,
            estimatedMinutes = 250,
            // 에뮬레이터 순정 내비의 부산역 경로(378 km)와 같은 길: 영동선 → 중부내륙선 → 대구 → 청도 → 부산.
            // 11분(약 17 km)에 서울을 벗어나 경기도로 — 첫 쉼 장소(용인졸음쉼터, 12분쯤)는 경기도다 (11/13)
            timeline = listOf(
                SEOUL_START,
                RouteWaypoint(11, "gyeonggi", "경기 성남", 37.4000, 127.1100),
                RouteWaypoint(17, "gyeonggi", "경기 용인", 37.2470, 127.2090),
                RouteWaypoint(38, "gyeonggi", "경기 여주", 37.2980, 127.6370),
                RouteWaypoint(63, "chungbuk", "충북 충주", 36.9910, 127.9260),
                RouteWaypoint(99, "gyeongbuk", "경북 문경", 36.5860, 128.1870),
                RouteWaypoint(132, "gyeongbuk", "경북 구미", 36.1195, 128.3446),
                RouteWaypoint(165, "daegu", "대구", 35.8714, 128.6014),
                RouteWaypoint(198, "gyeongbuk", "경북 청도", 35.6470, 128.7340),
                RouteWaypoint(235, "busan", "부산 진입", 35.2700, 129.0900),
                RouteWaypoint(250, "busan", "부산역", 35.115225, 129.042243),
            ),
            tbtAtMin = listOf(45, 160),
            defaultTimeScale = SimulatedTripSource.DEFAULT_TIME_SCALE,
            routeKm = 378.0,
            // 11/7 에뮬레이터 내비 TBT (TURN_ENTER_SERVICEAREA·TURN_RESTAREA, 구간 거리 누적)
            restStops = listOf(
                SimRestStop("용인졸음쉼터", SH, 18.13), SimRestStop("용인휴게소", SA, 28.53), SimRestStop("덕평휴게소", SA, 41.23),
                SimRestStop("이천졸음쉼터", SH, 48.73), SimRestStop("여주휴게소", SA, 57.93), SimRestStop("가남졸음쉼터", SH, 68.07),
                SimRestStop("노은졸음쉼터", SH, 89.07), SimRestStop("충주휴게소", SA, 95.37), SimRestStop("괴산휴게소", SA, 121.37),
                SimRestStop("문경졸음쉼터", SH, 136.37), SimRestStop("문경휴게소", SA, 152.37), SimRestStop("사벌졸음쉼터", SH, 172.37),
                SimRestStop("선산휴게소", SA, 195.37), SimRestStop("아포졸음쉼터", SH, 204.47), SimRestStop("선기졸음쉼터", SH, 214.19),
                SimRestStop("칠곡휴게소", SA, 233.19), SimRestStop("하빈졸음쉼터", SH, 244.19), SimRestStop("검단졸음쉼터", SH, 259.19),
                SimRestStop("남천졸음쉼터", SH, 283.59), SimRestStop("청도새마을휴게소", SA, 301.59), SimRestStop("상남(하)졸음쉼터", SH, 320.59),
                SimRestStop("덕산졸음쉼터", SH, 344.79),
            ),
        )

        val SEOUL_GANGNEUNG = DestinationPreset(
            id = "gangneung",
            title = "서울 → 강릉",
            poiName = "강릉역",
            latitude = 37.7642,
            longitude = 128.8996,
            estimatedMinutes = 160,
            timeline = listOf(
                SEOUL_START,
                RouteWaypoint(15, "gyeonggi", "경기 하남", 37.5393, 127.2149),
                RouteWaypoint(60, "gangwon", "강원 원주", 37.3422, 127.9202),
                RouteWaypoint(110, "gangwon", "강원 평창", 37.3705, 128.3903),
                RouteWaypoint(160, "gangwon", "강릉역", 37.7642, 128.8996),
            ),
            tbtAtMin = listOf(30, 100),
            defaultTimeScale = SimulatedTripSource.DEFAULT_TIME_SCALE,
            routeKm = 195.0,
            restStops = listOf(
                SimRestStop("경기광주휴게소", SA, 31.9), SimRestStop("대신졸음쉼터", SH, 46.9), SimRestStop("양평휴게소", SA, 57.9),
                SimRestStop("원주졸음쉼터", SH, 76.08), SimRestStop("소초졸음쉼터", SH, 84.76), SimRestStop("횡성휴게소", SA, 100.76),
                SimRestStop("삽교졸음쉼터", SH, 114.76), SimRestStop("평창휴게소", SA, 133.76), SimRestStop("월정졸음쉼터", SH, 147.76),
                SimRestStop("강릉대관령휴게소", SA, 175.76),
            ),
        )

        val SEOUL_JEONJU = DestinationPreset(
            id = "jeonju",
            title = "서울 → 전주",
            poiName = "전주한옥마을",
            latitude = 35.8147,
            longitude = 127.1526,
            estimatedMinutes = 170,
            timeline = listOf(
                SEOUL_START,
                RouteWaypoint(20, "gyeonggi", "경기 평택", 36.9921, 127.1129),
                RouteWaypoint(60, "chungnam", "충남 천안", 36.8151, 127.1139),
                RouteWaypoint(95, "chungnam", "충남 논산", 36.1871, 127.0987),
                RouteWaypoint(130, "jeonbuk", "전북 익산", 35.9483, 126.9576),
                RouteWaypoint(170, "jeonbuk", "전주 한옥마을", 35.8147, 127.1526),
            ),
            tbtAtMin = listOf(40, 115),
            defaultTimeScale = SimulatedTripSource.DEFAULT_TIME_SCALE,
            routeKm = 195.0,
            restStops = listOf(
                SimRestStop("기흥휴게소", SA, 20.49), SimRestStop("오산졸음쉼터", SH, 29.79), SimRestStop("안성휴게소", SA, 46.79),
                SimRestStop("연곡졸음쉼터", SH, 54.29), SimRestStop("입장졸음쉼터", SH, 58.79), SimRestStop("망향휴게소", SA, 65.39),
                SimRestStop("풍세(하)졸음쉼터", SH, 79.63), SimRestStop("정안알밤휴게소", SA, 99.63), SimRestStop("이인(하)졸음쉼터", SH, 120.63),
                SimRestStop("탄천휴게소", SA, 128.03), SimRestStop("남논산졸음쉼터", SH, 152.03), SimRestStop("익산미륵사지휴게소", SA, 158.63),
                SimRestStop("완주졸음쉼터", SH, 172.33),
            ),
        )

        /** 시연용 짧은 여정 — 샘플 NaviTestData 의 POI(소프트웨어드림센터) 좌표 */
        val NEARBY_DEMO = DestinationPreset(
            id = "nearby",
            title = "근처 10분",
            poiName = "소프트웨어드림센터",
            latitude = 37.41275833,
            longitude = 127.09481944,
            estimatedMinutes = 10,
            timeline = listOf(
                RouteWaypoint(0, "seoul", "서울 강남", 37.48544722, 127.03636666),
                RouteWaypoint(5, "gyeonggi", "경기 성남", 37.4201, 127.1265),
                RouteWaypoint(10, "gyeonggi", "소프트웨어드림센터", 37.41275833, 127.09481944),
            ),
            tbtAtMin = listOf(3),
            defaultTimeScale = 2f,
        )

        val ALL = listOf(SEOUL_BUSAN, SEOUL_GANGNEUNG, SEOUL_JEONJU, NEARBY_DEMO)

        /** 디버그 패널에서 고르기 전의 시뮬 경로 (3분 시연의 서울 → 부산) */
        val DEFAULT = SEOUL_BUSAN

        fun byId(id: String?): DestinationPreset? = ALL.firstOrNull { it.id == id }
    }
}
