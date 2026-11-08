package com.nolleo.app.app

import com.nolleo.app.core.navi.NaviPlace
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.RoundPacing

/** 홈 `목적지` 목록에서 고른 것 */
sealed interface DestinationChoice {
    /** 지금 경로 — 내비에 잡힌 길 안내, 디버그 패널 여정=시뮬이면 시뮬 경로 */
    data object CurrentRoute : DestinationChoice

    /** 내비 최근 목적지 ([NaviPlace.key]) — 출발할 때 그곳으로 길 안내를 요청한다 */
    data class Recent(val key: String) : DestinationChoice

    /** 목적지 없이 정한 시간만큼 */
    data object Timer : DestinationChoice
}

data class HomeState(
    val destination: DestinationChoice = DestinationChoice.Timer,
    /** "목적지 없이"의 시간 — [TIMER_MINUTES] 중 하나 */
    val timerMinutes: Int = DEFAULT_TIMER_MINUTES,
    val driverId: Int = 1,
    val passengerCount: Int = 1,
    val choice: GameChoice = GameChoice.ALTERNATE,
    /** 판 간격 (여유 있게 / 자주 / 쉬지 않고) */
    val pacing: RoundPacing = RoundPacing.RELAXED,
) {
    /**
     * 현재 경로가 새로 생기면(내비에서 길 안내 시작, 또는 여정=시뮬로 바꿈) 그것을 고른다.
     * 없어졌을 때는 상태를 바꾸지 않는다 — [effective] 가 그때 보일 줄로 맞춘다.
     */
    fun withRouteAppeared(available: Boolean): HomeState =
        if (available && destination != DestinationChoice.CurrentRoute) copy(destination = DestinationChoice.CurrentRoute) else this

    /**
     * 지금 실제로 고른 것 — 고른 줄이 사라졌으면 대신할 줄.
     * 최근 목적지는 길 안내 중에는 숨기므로(고르면 운전자의 길 안내를 바꾼다), 그 사이 고른 최근 목적지는 현재 경로로 본다.
     */
    fun effective(options: DestinationOptions): DestinationChoice = when (val d = destination) {
        DestinationChoice.CurrentRoute -> if (options.route != null) d else DestinationChoice.Timer
        is DestinationChoice.Recent -> when {
            options.recents.any { it.place.key == d.key } -> d
            options.route != null -> DestinationChoice.CurrentRoute
            else -> DestinationChoice.Timer
        }
        DestinationChoice.Timer -> d
    }

    /** 고른 것의 (남은 시간 초, 목적지 이름) — 미리보기·판 짜기. 예상 시간을 아직 모르면 null */
    fun timeAndName(options: DestinationOptions): Pair<Int, String>? = when (val d = effective(options)) {
        DestinationChoice.CurrentRoute -> options.route?.let { r -> r.remainSec?.let { it to r.destinationName } }
        is DestinationChoice.Recent -> options.recents.first { it.place.key == d.key }.let { o -> o.estimatedSec?.let { it to o.place.name } }
        DestinationChoice.Timer -> timerMinutes * 60 to timerName(timerMinutes)
    }

    companion object {
        /** "목적지 없이" 선택 막대의 네 칸 (11/8 결정: 30분~2시간) */
        val TIMER_MINUTES = listOf(30, 60, 90, 120)
        const val DEFAULT_TIMER_MINUTES = 60

        /** 선택 막대·목록 오른쪽 글: "30분" · "1시간" · "1시간 반" · "2시간" (좁은 창에서도 한 칸에 들어가게) */
        fun timerLabel(minutes: Int): String = if (minutes % 60 == 30 && minutes > 60) "${minutes / 60}시간 반" else TripUnits.formatDuration(minutes * 60)

        /** 요약·이어서 하기 배너에 보이는 이름 */
        fun timerName(minutes: Int): String = "목적지 없이 ${timerLabel(minutes)}"
    }
}

/** 홈 `목적지` 목록에 지금 보일 줄들 */
data class DestinationOptions(
    /** 현재 경로 줄 (내비 길 안내 또는 시뮬 경로). 없으면 null */
    val route: RouteOption? = null,
    /** 최근 목적지 — 현재 경로가 없을 때만, 최대 3곳 */
    val recents: List<RecentOption> = emptyList(),
)

data class RouteOption(
    val destinationName: String,
    val remainSec: Int?,
    /** 디버그 패널의 시뮬 경로 ("서울 → 부산") — 아니면 내비 길 안내 */
    val simulatedTitle: String? = null,
)

data class RecentOption(val place: NaviPlace, val estimatedSec: Int?)
