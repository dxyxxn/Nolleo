package com.gildongmu.app.core.navi

/** 시뮬레이션 여정의 경유 지점 (여정 시작 후 [atMin] 분에 이 지역을 지난다). */
data class RouteWaypoint(
    val atMin: Int,
    val regionCode: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
)

/**
 * 홈 화면의 프리셋 목적지. 실제 내비(NaviHelper.requestRoute)와 시뮬레이션 여정이 같은 정보를 쓴다.
 *
 * poiId: 지시서 §5.2 에 따라 샘플(pleos-sample-navi-app data/sample/NaviTestData.kt)의 실제 값을 재사용한다.
 * 샘플에는 수도권 POI 2개뿐이라 장거리 프리셋은 poiId 를 비워 좌표·주소로 요청한다.
 * TODO(verify): 순정 내비가 빈 poiId 요청을 받아 주는지 — onError 면 TripRepository 가 시뮬레이션으로 넘어간다.
 */
data class DestinationPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val poiName: String,
    val latitude: Double,
    val longitude: Double,
    val poiId: String,
    val poiSubId: String,
    val address: String,
    val estimatedMinutes: Int,
    val timeline: List<RouteWaypoint>,
    /** 시뮬레이션에서 "분기점 안내 300 m" TBT 를 낼 시점(분) */
    val tbtAtMin: List<Int>,
    /** 실제 1분 = 여정 N분 */
    val defaultTimeScale: Float,
) {
    val estimatedSec: Int get() = estimatedMinutes * 60

    companion object {
        private val SEOUL_START = RouteWaypoint(0, "seoul", "서울", 37.5665, 126.9780)

        val SEOUL_BUSAN = DestinationPreset(
            id = "busan",
            title = "서울 → 부산",
            subtitle = "부산역 · 약 4시간 10분",
            poiName = "부산역",
            latitude = 35.115225,
            longitude = 129.042243,
            poiId = "",
            poiSubId = "0",
            address = "부산광역시 동구 중앙대로 206",
            estimatedMinutes = 250,
            timeline = listOf(
                SEOUL_START,
                RouteWaypoint(20, "gyeonggi", "경기 용인", 37.2411, 127.1776),
                RouteWaypoint(70, "chungbuk", "충북 청주", 36.6424, 127.4890),
                RouteWaypoint(100, "daejeon", "대전", 36.3504, 127.3845),
                RouteWaypoint(150, "gyeongbuk", "경북 구미", 36.1195, 128.3446),
                RouteWaypoint(190, "daegu", "대구", 35.8714, 128.6014),
                RouteWaypoint(230, "busan", "부산 진입", 35.2700, 129.0900),
                RouteWaypoint(250, "busan", "부산역", 35.115225, 129.042243),
            ),
            tbtAtMin = listOf(45, 160),
            defaultTimeScale = 30f,
        )

        val SEOUL_GANGNEUNG = DestinationPreset(
            id = "gangneung",
            title = "서울 → 강릉",
            subtitle = "강릉역 · 약 2시간 40분",
            poiName = "강릉역",
            latitude = 37.7642,
            longitude = 128.8996,
            poiId = "",
            poiSubId = "0",
            address = "강원특별자치도 강릉시 용지로 176",
            estimatedMinutes = 160,
            timeline = listOf(
                SEOUL_START,
                RouteWaypoint(15, "gyeonggi", "경기 하남", 37.5393, 127.2149),
                RouteWaypoint(60, "gangwon", "강원 원주", 37.3422, 127.9202),
                RouteWaypoint(110, "gangwon", "강원 평창", 37.3705, 128.3903),
                RouteWaypoint(160, "gangwon", "강릉역", 37.7642, 128.8996),
            ),
            tbtAtMin = listOf(30, 100),
            defaultTimeScale = 30f,
        )

        val SEOUL_JEONJU = DestinationPreset(
            id = "jeonju",
            title = "서울 → 전주",
            subtitle = "전주 한옥마을 · 약 2시간 50분",
            poiName = "전주한옥마을",
            latitude = 35.8147,
            longitude = 127.1526,
            poiId = "",
            poiSubId = "0",
            address = "전북특별자치도 전주시 완산구 기린대로 99",
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
            defaultTimeScale = 30f,
        )

        /** 시연용 짧은 여정 — 샘플 NaviTestData 의 실제 POI(소프트웨어드림센터) 재사용 */
        val NEARBY_DEMO = DestinationPreset(
            id = "nearby",
            title = "근처 10분",
            subtitle = "시연용 · 소프트웨어드림센터",
            poiName = "소프트웨어드림센터",
            latitude = 37.41275833,
            longitude = 127.09481944,
            poiId = "36khp2hdj2cfcm5nqhe3",
            poiSubId = "0",
            address = "경기도 성남시 수정구 창업로40번길 6",
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

        fun byId(id: String?): DestinationPreset? = ALL.firstOrNull { it.id == id }
    }
}
