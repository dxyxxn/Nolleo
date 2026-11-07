package com.nolleo.app.core.navi

import com.nolleo.app.domain.rest.RestStopKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시뮬 여정의 경로 위 휴게소·졸음쉼터 (에뮬레이터 내비 TBT 에서 옮긴 값) 일관성 */
class DestinationPresetTest {

    @Test
    fun simRestStops_areOrderedAlongTheRoute_andNamedLikeTbt() {
        DestinationPreset.ALL.filter { it.restStops != null }.forEach { p ->
            val stops = p.restStops!!
            assertTrue("${p.id} 경로 거리", p.routeKm > 0)
            assertEquals("${p.id} 경로 순서", stops.sortedBy { it.km }, stops)
            stops.forEach { s ->
                assertTrue("${p.id} ${s.name} 경로 안", s.km in 0.0..p.routeKm)
                val suffix = if (s.kind == RestStopKind.SERVICE_AREA) "휴게소" else "졸음쉼터"
                assertTrue("${p.id} ${s.name} 은 TBT 처럼 '$suffix' 로 끝난다", s.name.endsWith(suffix))
            }
            assertTrue("${p.id} 휴게소와 졸음쉼터가 섞여 있다", stops.any { it.kind == RestStopKind.SERVICE_AREA } && stops.any { it.kind == RestStopKind.SHELTER })
        }
    }

    @Test
    fun nearbyDemo_hasNoRouteInfo_soTableIsUsed() {
        assertNull(DestinationPreset.NEARBY_DEMO.restStops)
    }
}
