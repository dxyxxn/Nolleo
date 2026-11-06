package com.gildongmu.app.core.navi

import com.gildongmu.app.core.log.AppLog

/**
 * NaviHelper 값의 단위 변환을 한 곳에 모은다 (지시서 §5.2 단위 주의).
 *
 * 근거: PLEOS_CONNECT_API_REFERENCE.md
 *  - DestinationInfo.remainTime  "estimated remaining travel time ... (in seconds)"
 *  - DestinationInfo.remainDistance "(in meters)"
 *  - RouteStartInfo.duration "(in seconds)", distance "(in meters)"
 *  - DrivingState.duration "(in seconds)"
 * 에뮬레이터 실측(2026-11-06, Pleos Connect Emulator 2.1.0 순정 내비):
 *  - 서울→부산역 요청: onRouteStarted duration=14820, distance=378000 → 4시간 7분 / 378km (평균 92km/h) ⇒ 초·미터
 *  - 근처 POI 요청: duration=60, distance=10 → 내비 화면 "1분 · 10m" ⇒ 초·미터
 * 그래도 펌웨어별 차이에 대비해, 거리 대비 시간이 물리적으로 불가능하면(평균 252 km/h 초과)
 * 분 단위로 온 것으로 보고 60 을 곱한 뒤 로그를 남긴다.
 */
object TripUnits {
    private const val TAG = "TripUnits"
    private const val MAX_PLAUSIBLE_MPS = 70.0

    fun remainTimeSec(rawTime: Int, distanceM: Int?): Int = toSeconds(rawTime, distanceM, "remainTime")

    fun durationSec(rawDuration: Int, distanceM: Int?): Int = toSeconds(rawDuration, distanceM, "duration")

    fun distanceM(raw: Int): Int = raw.coerceAtLeast(0)

    private fun toSeconds(raw: Int, distanceM: Int?, label: String): Int {
        if (raw <= 0) return 0
        if (distanceM != null && distanceM > 2_000) {
            val impliedMps = distanceM.toDouble() / raw
            if (impliedMps > MAX_PLAUSIBLE_MPS) {
                val asMinutes = raw * 60
                AppLog.w(TAG, "raw $label=$raw for ${distanceM}m implies ${"%.0f".format(impliedMps)} m/s → treating as minutes ($asMinutes s)")
                return asMinutes
            }
        }
        return raw
    }

    /** "4시간 10분", "12분", "1분 미만" */
    fun formatDuration(sec: Int): String {
        if (sec < 60) return "1분 미만"
        val totalMin = sec / 60
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h == 0 -> "${m}분"
            m == 0 -> "${h}시간"
            else -> "${h}시간 ${m}분"
        }
    }

    fun formatDistance(m: Int): String =
        if (m >= 1_000) "%.1fkm".format(m / 1000.0) else "${m}m"
}
