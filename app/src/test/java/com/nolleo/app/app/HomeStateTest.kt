package com.nolleo.app.app

import com.nolleo.app.core.navi.NaviPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class HomeStateTest {

    private val busan = NaviPlace("부산역", "부산광역시 동구 중앙대로 206", 35.115225, 129.042243, "", null)
    private val dream = NaviPlace("소프트웨어드림센터", "경기도 성남시 수정구", 37.41275833, 127.09481944, "36khp2hdj2cfcm5nqhe3", null)

    private val navi = DestinationOptions(route = RouteOption("부산역", 4 * 3600))
    private val recents = DestinationOptions(recents = listOf(RecentOption(busan, 15_167), RecentOption(dream, null)))
    private val nothing = DestinationOptions()

    @Test
    fun routeAppears_selectsIt_andNoChangeKeepsInstance() {
        assertEquals(DestinationChoice.CurrentRoute, HomeState().withRouteAppeared(available = true).destination)
        val timer = HomeState()
        assertSame(timer, timer.withRouteAppeared(available = false))
        val route = HomeState(destination = DestinationChoice.CurrentRoute)
        assertSame(route, route.withRouteAppeared(available = true))
    }

    @Test
    fun routeEnds_effectiveFallsBackToTimer() {
        val home = HomeState(destination = DestinationChoice.CurrentRoute)
        assertEquals(DestinationChoice.CurrentRoute, home.effective(navi))
        assertEquals(DestinationChoice.Timer, home.effective(recents))
    }

    @Test
    fun recentHiddenDuringGuidance_becomesCurrentRoute_andMissingRecentBecomesTimer() {
        val home = HomeState(destination = DestinationChoice.Recent(busan.key))
        assertEquals(DestinationChoice.Recent(busan.key), home.effective(recents))
        // 길 안내 중에는 최근 목적지를 숨긴다 (그 사이 고른 곳으로 길 안내를 시작했을 수 있다)
        assertEquals(DestinationChoice.CurrentRoute, home.effective(navi))
        assertEquals(DestinationChoice.Timer, home.effective(nothing))
    }

    @Test
    fun timeAndName_perChoice() {
        assertEquals(4 * 3600 to "부산역", HomeState(destination = DestinationChoice.CurrentRoute).timeAndName(navi))
        assertEquals(15_167 to "부산역", HomeState(destination = DestinationChoice.Recent(busan.key)).timeAndName(recents))
        assertNull("예상 시간을 아직 모름", HomeState(destination = DestinationChoice.Recent(dream.key)).timeAndName(recents))
        assertEquals(90 * 60 to "목적지 없이 1시간 반", HomeState(timerMinutes = 90).timeAndName(nothing))
    }

    @Test
    fun timerLabels_fitOneSegment() {
        assertEquals(listOf("30분", "1시간", "1시간 반", "2시간"), HomeState.TIMER_MINUTES.map(HomeState::timerLabel))
    }
}
