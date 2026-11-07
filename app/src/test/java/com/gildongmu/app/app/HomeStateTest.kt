package com.gildongmu.app.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {

    @Test
    fun naviRouteAppears_selectsIt() {
        val home = HomeState(presetId = "seoul_busan").withNaviRoute(available = true)
        assertTrue(home.useNaviRoute)
    }

    @Test
    fun naviRouteEnds_fallsBackToPreset() {
        val home = HomeState(presetId = "seoul_busan", useNaviRoute = true).withNaviRoute(available = false)
        assertFalse(home.useNaviRoute)
    }

    @Test
    fun simulatedTripMode_showsCardButDoesNotAutoSelect() {
        val home = HomeState(presetId = "seoul_busan").withNaviRoute(available = true, autoSelect = false)
        assertFalse(home.useNaviRoute)
        // 길 안내가 끝나면 이미 고른 내비 목적지는 자동 선택 여부와 상관없이 해제
        assertFalse(HomeState(useNaviRoute = true).withNaviRoute(available = false, autoSelect = false).useNaviRoute)
    }

    @Test
    fun noChange_returnsSameInstance() {
        val picked = HomeState(presetId = "seoul_busan", useNaviRoute = true)
        assertSame(picked, picked.withNaviRoute(available = true))
        val preset = HomeState(presetId = "seoul_busan")
        assertSame(preset, preset.withNaviRoute(available = false))
    }
}
