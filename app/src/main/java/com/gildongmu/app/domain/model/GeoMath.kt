package com.gildongmu.app.domain.model

import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** 위경도 거리·방위각 (순수 함수 — 단위 테스트 대상). 방위각은 북 = 0°, 시계 방향. */
object GeoMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceM(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } +
            cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(dLon / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }

    fun bearingDeg(from: GeoPoint, to: GeoPoint): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360) % 360
    }

    /** 두 방위각의 차이 (0~180°) */
    fun angleDiff(a: Double, b: Double): Double = abs(((a - b) % 360 + 540) % 360 - 180)

    /** a → b 를 [fraction](0~1) 만큼 간 지점 (짧은 구간용 선형 보간) */
    fun lerp(a: GeoPoint, b: GeoPoint, fraction: Double): GeoPoint {
        val f = fraction.coerceIn(0.0, 1.0)
        return GeoPoint(a.latitude + (b.latitude - a.latitude) * f, a.longitude + (b.longitude - a.longitude) * f)
    }
}
