package com.nolleo.app.core.navi

import com.nolleo.app.domain.model.NaviStatus
import com.nolleo.app.domain.model.TripSourceKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 내비 남은 시간 주기 조회 조건 — 길 안내 중 && (앱이 보임 || 실제 내비 여정으로 게임 중) */
class NaviPollPolicyTest {

    private fun poll(status: NaviStatus?, visible: Boolean, tripActive: Boolean, kind: TripSourceKind) =
        TripRepository.shouldPollNavi(status, visible, tripActive, kind)

    @Test
    fun routeGuiding_andAppVisible_polls() {
        assertTrue(poll(NaviStatus.ROUTE_DRIVE, visible = true, tripActive = false, kind = TripSourceKind.SIMULATED))
    }

    @Test
    fun routeGuiding_hiddenButRealNaviTrip_polls() {
        assertTrue(poll(NaviStatus.ROUTE_DRIVE, visible = false, tripActive = true, kind = TripSourceKind.REAL_NAVI))
    }

    @Test
    fun hiddenWithSimulatedTrip_doesNotPoll() {
        assertFalse(poll(NaviStatus.ROUTE_DRIVE, visible = false, tripActive = true, kind = TripSourceKind.SIMULATED))
        assertFalse(poll(NaviStatus.ROUTE_DRIVE, visible = false, tripActive = false, kind = TripSourceKind.REAL_NAVI))
    }

    @Test
    fun noRouteGuidance_neverPolls() {
        listOf(null, NaviStatus.FREE_DRIVE, NaviStatus.INACTIVE, NaviStatus.INITIALIZING).forEach { s ->
            assertFalse(poll(s, visible = true, tripActive = true, kind = TripSourceKind.REAL_NAVI))
        }
    }
}
