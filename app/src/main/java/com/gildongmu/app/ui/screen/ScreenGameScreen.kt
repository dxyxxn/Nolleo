package com.gildongmu.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gildongmu.app.app.AppContainer
import com.gildongmu.app.app.RestCareController
import com.gildongmu.app.core.navi.TripUnits
import com.gildongmu.app.domain.engine.SessionPlanner
import com.gildongmu.app.ui.common.ChoiceButton
import com.gildongmu.app.ui.theme.GdColors

/** SCREEN 모드 (기획서 화면안): 카드 2개(사자성어 / 그림 맞히기 — 후자는 "준비 중"), 하단 "정차 중에만 열려요" */
@Composable
fun ScreenGameScreen(
    container: AppContainer,
    idiomOpen: Boolean,
    onOpenIdiom: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip by container.tripRepository.snapshot.collectAsStateWithLifecycle()
    val sessionState by container.coordinator.sessionState.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("정차 중 · 화면 게임", style = MaterialTheme.typography.headlineSmall, color = GdColors.Good, modifier = Modifier.weight(1f))
            trip?.remainTimeSec?.let {
                Text("도착까지 ${TripUnits.formatDuration(it)}", style = MaterialTheme.typography.titleMedium, color = GdColors.Muted)
            }
            // 정차 중에는 언제든 게임을 끝내고 홈으로 갈 수 있다 (점수가 있으면 요약 화면을 거친다)
            ChoiceButton("게임 끝내기", selected = false, onClick = container.coordinator::endSession)
        }
        // 졸음 케어 2단계: 쉼터에 세워 둔 동안 한 줄 안내 (다시 출발하면 "잘 쉬셨나요?")
        val rest by container.coordinator.restCare.state.collectAsStateWithLifecycle()
        when (val r = rest) {
            is RestCareController.State.Arrived ->
                Text("${r.suggestion.name}에서 쉬는 중 · 충분히 쉬었다 가요", style = MaterialTheme.typography.titleMedium, color = GdColors.Good)
            is RestCareController.State.Heading ->
                Text("${r.suggestion.name}로 가는 중", style = MaterialTheme.typography.titleMedium, color = GdColors.Accent)
            else -> Unit
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val quiz = container.coordinator.idiomQuiz
            if (idiomOpen) {
                val close = {
                    quiz.close()
                    onOpenIdiom(false)
                }
                BackHandler(onBack = close)
                if (!quiz.isAvailable) {
                    Text("사자성어 콘텐츠를 불러오지 못했어요.", style = MaterialTheme.typography.titleLarge, color = GdColors.Warn)
                } else {
                    IdiomQuizPanel(quiz, onClose = close)
                }
            } else {
                GameCards(onOpenIdiom = {
                    onOpenIdiom(true)
                    quiz.open()
                })
            }
        }
        // 주차 중에는 여정 시간이 흐르지 않으므로 "주행 약 N분 뒤"로 안내한다
        val eta = SessionPlanner.secondsUntilNextRound(trip?.remainTimeSec, sessionState?.nextRoundAtRemainSec)
        val etaText = when {
            eta == null -> ""
            eta <= 60 -> " · 출발하면 바로 다음 음성 판"
            else -> " · 다음 음성 판: 주행 약 ${TripUnits.formatDuration(eta)} 뒤"
        }
        Text(
            "정차 중에만 열려요 · 출발하면 음성 게임으로 바뀌어요$etaText",
            style = MaterialTheme.typography.titleSmall,
            color = GdColors.Muted,
        )
    }
}

@Composable
private fun GameCards(onOpenIdiom: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight
        val cards: @Composable (Modifier) -> Unit = { m ->
            GameCard("사자성어", "뜻을 듣고 넷 중에 골라요 · 한자 풀이와 유래까지", enabled = true, onClick = onOpenIdiom, modifier = m)
            GameCard("그림 맞히기", "준비 중이에요", enabled = false, onClick = {}, modifier = m)
        }
        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) { cards(Modifier.weight(1f).fillMaxSize()) }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.dp)) { cards(Modifier.weight(1f).fillMaxWidth()) }
        }
    }
}

@Composable
private fun GameCard(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier
            .heightIn(min = 200.dp)
            .background(if (enabled) GdColors.SurfaceHigh else GdColors.Surface, RoundedCornerShape(28.dp))
            .border(2.dp, if (enabled) GdColors.Primary else GdColors.Outline, RoundedCornerShape(28.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.displayMedium, color = if (enabled) GdColors.Text else GdColors.Disabled)
        Spacer(Modifier.height(12.dp))
        Text(subtitle, style = MaterialTheme.typography.titleMedium, color = if (enabled) GdColors.Muted else GdColors.Disabled, textAlign = TextAlign.Center)
    }
}
