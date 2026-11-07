package com.nolleo.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.nolleo.app.app.ScreenGameKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

/**
 * 화면 전용 상태만 SavedStateHandle 에 둔다 (디버그 패널 열림, 정차 중 열어 둔 화면 게임).
 * 게임 세션 자체는 앱 범위(GameCoordinator)에 있고, 프로세스 재생성은 DataStore 저장본으로 복원한다.
 */
class MainViewModel(private val handle: SavedStateHandle) : ViewModel() {

    val debugOpen: StateFlow<Boolean> = handle.getStateFlow(KEY_DEBUG, false)

    /** 정차 중 열어 둔 화면 게임 (null = 게임 목록) */
    val openScreenGame: StateFlow<ScreenGameKind?> = handle.getStateFlow<String?>(KEY_SCREEN_GAME, null)
        .map { name -> ScreenGameKind.entries.firstOrNull { it.name == name } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ScreenGameKind.entries.firstOrNull { it.name == handle.get<String>(KEY_SCREEN_GAME) })

    fun setDebugOpen(open: Boolean) {
        handle[KEY_DEBUG] = open
    }

    fun setOpenScreenGame(kind: ScreenGameKind?) {
        handle[KEY_SCREEN_GAME] = kind?.name
    }

    private companion object {
        const val KEY_DEBUG = "debug_open"
        const val KEY_SCREEN_GAME = "screen_game"
    }
}
