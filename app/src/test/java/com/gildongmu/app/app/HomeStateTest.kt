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
    fun noChange_returnsSameInstance() {
        val picked = HomeState(presetId = "seoul_busan", useNaviRoute = true)
        assertSame(picked, picked.withNaviRoute(available = true))
        val preset = HomeState(presetId = "seoul_busan")
        assertSame(preset, preset.withNaviRoute(available = false))
    }
}
