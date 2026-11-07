package com.nolleo.app.app

/**
 * 3분할 화면(Driving view + 앱 창 2개, DEV_GUIDE §5.4·§6.3)에서 놀레오 창이 어디에 있는지 판단한다.
 * 앱은 창 위치를 직접 정할 수 없으므로, 운전석 쪽(왼쪽) 창에 있으면 기본 지도 앱을 실행해 자리를 바꾼다(MainActivity).
 * Android Rect 대신 정수만 쓰는 순수 로직 (JVM 단위 테스트 대상).
 */
object WindowPlacement {

    /** 창의 화면 좌표 (left ≤ x < right) */
    data class Box(val left: Int, val right: Int) {
        val width: Int get() = right - left
    }

    /**
     * 앱 창 2개 중 왼쪽(운전석 쪽)에 있는가:
     * 왼쪽에 Driving view 가 있고(left > 0, 좌핸들 배치) 오른쪽에 앱 창이 하나 더 들어갈 공간이 비어 있다.
     * 앱 영역 전체를 쓸 때(landscape)는 오른쪽 여백이 창 막대 정도뿐이라 해당하지 않는다.
     */
    fun isDriverSideSlot(now: Box, screenRight: Int): Boolean =
        now.left > 0 && now.width > 0 && screenRight - now.right > now.width / 3

    /**
     * 내비를 앞으로 불러 게임을 오른쪽 창으로 옮겨야 하는가.
     *  - 왼쪽 창에 처음 보였거나([previous] 없음) 다시 앞으로 나왔거나([cameToFront]) 넓은 창에서 줄어들어 들어왔으면 옮긴다.
     *  - 같은 크기로 자리만 바뀐 경우는 사용자가 창 막대·세 손가락 스와이프로 직접 옮긴 것이므로 그대로 둔다.
     */
    fun shouldMoveToPassengerSide(previous: Box?, now: Box, screenRight: Int, cameToFront: Boolean): Boolean {
        if (!isDriverSideSlot(now, screenRight)) return false
        if (cameToFront || previous == null) return true
        if (previous == now) return false
        val movedByUser = previous.width == now.width && previous.left != now.left
        return !movedByUser
    }
}
