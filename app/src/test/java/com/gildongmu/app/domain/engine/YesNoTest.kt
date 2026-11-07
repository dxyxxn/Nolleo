package com.gildongmu.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class YesNoTest {

    @Test
    fun yes() {
        listOf("네", "네!", "예", "응", "그래", "좋아요", "안내해 줘", "쉬어 갈래", "오케이").forEach {
            assertEquals(it, true, YesNo.parse(it))
        }
    }

    @Test
    fun no() {
        listOf("아니요", "아뇨", "괜찮아요", "싫어", "됐어", "아니 그냥 가자").forEach {
            assertEquals(it, false, YesNo.parse(it))
        }
    }

    @Test
    fun unknown() {
        listOf(null, "", "음", "부산").forEach { assertEquals(it, null, YesNo.parse(it)) }
    }
}
