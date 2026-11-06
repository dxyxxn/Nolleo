package com.gildongmu.app.ui

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gildongmu.app.app.AppContainer
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.ui.common.Badge
import com.gildongmu.app.ui.debug.DebugPanel
import com.gildongmu.app.ui.home.HomeScreen
import com.gildongmu.app.ui.screen.ScreenGameScreen
import com.gildongmu.app.ui.summary.SummaryScreen
import com.gildongmu.app.ui.theme.GdColors
import com.gildongmu.app.ui.voice.VoiceGameScreen

/** 모드별 화면 전환. 주행 중 애니메이션 금지 → 전환 효과 없이 바로 바꾼다. */
@Composable
fun GildongmuRoot(container: AppContainer, vm: MainViewModel) {
    val coordinator = container.coordinator
    val mode by coordinator.gameMode.collectAsStateWithLifecycle()
    val summary by coordinator.summary.collectAsStateWithLifecycle()
    val session by coordinator.session.collectAsStateWithLifecycle()
    val debugOpen by vm.debugOpen.collectAsStateWithLifecycle()
    val chosungOpen by vm.chosungOpen.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(GdColors.Background).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(container, onLongPress = { vm.setDebugOpen(true) })
            val body = Modifier.weight(1f)
            when {
                session == null && summary != null -> SummaryScreen(container, summary!!, body)
                mode == GameMode.SCREEN -> ScreenGameScreen(container, chosungOpen, vm::setChosungOpen, body)
                mode == GameMode.VOICE || mode == GameMode.PAUSED -> {
                    // 출발(SCREEN→VOICE)하면 화면 게임을 즉시 닫는다
                    if (chosungOpen) vm.setChosungOpen(false)
                    VoiceGameScreen(container, body)
                }
                else -> HomeScreen(container, body)
            }
        }
        if (debugOpen) DebugPanel(container, onClose = { vm.setDebugOpen(false) })
    }
}

@Composable
private fun TitleBar(container: AppContainer, onLongPress: () -> Unit) {
    val ttsKind by container.speechRouter.ttsKind.collectAsStateWithLifecycle()
    val sttKind by container.speechRouter.sttKind.collectAsStateWithLifecycle()
    val tripKind by container.tripRepository.activeKind.collectAsStateWithLifecycle()
    val carKind by container.carRepository.activeKind.collectAsStateWithLifecycle()
    val session by container.coordinator.session.collectAsStateWithLifecycle()
    val tripLabel = if (session != null) tripKind.label else "여정 대기"
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "길동무",
            style = MaterialTheme.typography.headlineMedium,
            color = GdColors.Primary,
            modifier = Modifier.pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
        )
        Text(
            "가는 길이 놀이가 되는 가족 게임",
            style = MaterialTheme.typography.titleSmall,
            color = GdColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // 선택된 엔진·소스 배지 (지시서 §5.4: 홈 화면 하단 작은 배지 → 모든 화면 상단에 표시)
        Badge("차량 ${carKind.label} · $tripLabel · TTS ${ttsKind.label}/STT ${sttKind.label}", textColor = GdColors.Muted)
    }
}
