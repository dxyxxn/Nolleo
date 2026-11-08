package com.nolleo.app.core.navi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NaviPlaceTest {

    private fun place(name: String, lat: Double, lng: Double) = NaviPlace(name, "", lat, lng, "", null)

    @Test
    fun clean_dropsUnregisteredAndDuplicates_keepsOrder() {
        val busan = place("부산역", 35.115225, 129.042243)
        val dream = place("소프트웨어드림센터", 37.41275833, 127.09481944)
        val list = listOf(
            place("", 0.0, 0.0), // 등록 안 된 집 (에뮬레이터는 빈 이름·0 좌표로 보낸다)
            busan,
            place("이름만", 0.0, 0.0),
            dream,
            busan.copy(name = "부산역(다시)"),
            place("범위 밖", 123.0, 10.0),
        )
        assertEquals(listOf(busan, dream), NaviPlace.clean(list))
    }

    @Test
    fun sameSpot_matchesTripAnswerByDestination() {
        val busan = place("부산역", 35.115225, 129.042243)
        assertTrue(NaviPlace.sameSpot(busan, 35.1160, 129.0425))
        assertFalse(NaviPlace.sameSpot(busan, 37.41275833, 127.09481944))
        assertEquals("35.11523,129.04224", busan.key)
    }
}
