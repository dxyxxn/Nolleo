package com.gildongmu.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * 화면 전용 상태만 SavedStateHandle 에 둔다 (디버그 패널·초성 퀴즈 열림 여부).
 * 게임 세션 자체는 앱 범위(GameCoordinator)에 있고, 프로세스 재생성은 DataStore 저장본으로 복원한다.
 */
class MainViewModel(private val handle: SavedStateHandle) : ViewModel() {

    val debugOpen: StateFlow<Boolean> = handle.getStateFlow(KEY_DEBUG, false)
    val chosungOpen: StateFlow<Boolean> = handle.getStateFlow(KEY_CHOSUNG, false)

    fun setDebugOpen(open: Boolean) {
        handle[KEY_DEBUG] = open
    }

    fun setChosungOpen(open: Boolean) {
        handle[KEY_CHOSUNG] = open
    }

    private companion object {
        const val KEY_DEBUG = "debug_open"
        const val KEY_CHOSUNG = "chosung_open"
    }
}
