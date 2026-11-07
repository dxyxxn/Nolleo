package com.nolleo.app.core

/** 입력 소스 강제 선택 (car=auto|real|sim, trip=…, location=…). */
enum class SourceMode(val label: String) {
    AUTO("자동"),
    REAL("실제"),
    SIM("시뮬"),
}
