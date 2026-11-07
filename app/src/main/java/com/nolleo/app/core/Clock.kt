package com.nolleo.app.core

import android.os.SystemClock

/** 시간 추상화. 도메인 로직은 이 인터페이스만 보고, 테스트는 가짜 시계를 넣는다. */
interface Clock {
    /** 단조 증가 시각(ms). 지연·응답 시간 측정용. */
    fun elapsedMs(): Long

    /** 벽시계 시각(ms). 기록·쿨다운 표시용. */
    fun wallMs(): Long
}

object SystemClockSource : Clock {
    override fun elapsedMs(): Long = SystemClock.elapsedRealtime()
    override fun wallMs(): Long = System.currentTimeMillis()
}
