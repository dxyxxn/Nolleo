package com.nolleo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.app.AppContainer
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.ui.common.Hairline
import com.nolleo.app.ui.common.Logo
import com.nolleo.app.ui.debug.DebugPanel
import com.nolleo.app.ui.home.HomeScreen
import com.nolleo.app.ui.screen.ScreenGameScreen
import com.nolleo.app.ui.summary.SummaryScreen
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Sp
import com.nolleo.app.ui.voice.VoiceGameScreen

/** 모드별 화면 전환. 주행 중 애니메이션 금지 → 전환 효과 없이 바로 바꾼다. */
@Composable
fun NolleoRoot(container: AppContainer, vm: MainViewModel) {
    val coordinator = container.coordinator
    val mode by coordinator.gameMode.collectAsStateWithLifecycle()
    val summary by coordinator.summary.collectAsStateWithLifecycle()
    val session by coordinator.session.collectAsStateWithLifecycle()
    val debugOpen by vm.debugOpen.collectAsStateWithLifecycle()
    val openGame by vm.openScreenGame.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(Nl.colors.ground).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(onLongPress = { vm.setDebugOpen(true) })
            Hairline()
            val body = Modifier.weight(1f)
            when {
                session == null && summary != null -> SummaryScreen(container, summary!!, body)
                mode == GameMode.SCREEN -> ScreenGameScreen(container, openGame, vm::setOpenScreenGame, body)
                mode == GameMode.VOICE || mode == GameMode.PAUSED -> {
                    // 출발(SCREEN→VOICE)하면 화면 게임을 즉시 닫는다
                    if (openGame != null) vm.setOpenScreenGame(null)
                    VoiceGameScreen(container, body)
                }
                else -> HomeScreen(container, body)
            }
        }
        if (debugOpen) DebugPanel(container, onClose = { vm.setDebugOpen(false) })
    }
}

/**
 * 제목 줄: 왼쪽에 로고 + 영문 이름 Nolleo (길게 누르면 디버그 패널). 그 밖에는 아무것도 두지 않는다 —
 * 입력 소스(차량·여정·음성) 표시는 11/8 사용자 결정으로 화면에서 빼고 디버그 패널에서만 본다.
 */
@Composable
private fun TitleBar(onLongPress: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Sp.xxl, vertical = Sp.m)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Sp.m),
    ) {
        Logo()
        Text(
            "Nolleo",
            // 영문 이름은 글자 모양을 살리려고 이 한 곳만 700(Asta Sans ExtraBold)
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = Nl.colors.ink,
            maxLines = 1,
        )
    }
}
