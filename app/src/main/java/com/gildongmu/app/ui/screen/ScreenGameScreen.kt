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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gildongmu.app.app.AppContainer
import com.gildongmu.app.core.navi.TripUnits
import com.gildongmu.app.domain.games.ChosungQuizGame
import com.gildongmu.app.ui.common.ChoiceButton
import com.gildongmu.app.ui.common.MinTouch
import com.gildongmu.app.ui.common.PrimaryButton
import com.gildongmu.app.ui.theme.GdColors

/** SCREEN 모드 (기획서 화면안): 카드 2개(초성 퀴즈 / 그림 맞히기 — 후자는 "준비 중"), 하단 "정차 중에만 열려요" */
@Composable
fun ScreenGameScreen(
    container: AppContainer,
    chosungOpen: Boolean,
    onOpenChosung: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip by container.tripRepository.snapshot.collectAsStateWithLifecycle()
    val sessionState by container.coordinator.sessionState.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("정차 중 · 화면 게임", style = MaterialTheme.typography.headlineSmall, color = GdColors.Good, modifier = Modifier.weight(1f))
            trip?.remainTimeSec?.let {
                Text("도착까지 ${TripUnits.formatDuration(it)}", style = MaterialTheme.typography.titleMedium, color = GdColors.Muted)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (chosungOpen) {
                BackHandler { onOpenChosung(false) }
                val game = container.coordinator.chosungGame
                if (game == null) {
                    Text("초성 퀴즈 콘텐츠를 불러오지 못했어요.", style = MaterialTheme.typography.titleLarge, color = GdColors.Warn)
                } else {
                    ChosungQuiz(game, onClose = { onOpenChosung(false) })
                }
            } else {
                GameCards(onOpenChosung = { onOpenChosung(true) })
            }
        }
        val next = sessionState?.nextRoundAtRemainSec
        Text(
            "정차 중에만 열려요 · 출발하면 음성 게임으로 바뀌어요" + (next?.let { " · 다음 음성 판: 남은 ${TripUnits.formatDuration(it)}" } ?: ""),
            style = MaterialTheme.typography.titleSmall,
            color = GdColors.Muted,
        )
    }
}

@Composable
private fun GameCards(onOpenChosung: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight
        val cards: @Composable (Modifier) -> Unit = { m ->
            GameCard("초성 퀴즈", "ㅅㄱ → 사과? 초성을 보고 낱말을 골라요", enabled = true, onClick = onOpenChosung, modifier = m)
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

@Composable
private fun ChosungQuiz(game: ChosungQuizGame, onClose: () -> Unit) {
    var question by remember { mutableStateOf(game.next()) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(game.score) }
    val q = question ?: return

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("초성 퀴즈 · ${score}점", style = MaterialTheme.typography.titleLarge, color = GdColors.Accent, modifier = Modifier.weight(1f))
            ChoiceButton("닫기", selected = false, onClick = onClose)
        }
        Text(q.chosung, style = MaterialTheme.typography.displayLarge, color = GdColors.Text)
        Text("힌트: ${q.item.hint}", style = MaterialTheme.typography.titleLarge, color = GdColors.Muted, textAlign = TextAlign.Center)
        q.choices.chunked(2).forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEachIndexed { colIndex, word ->
                    val index = rowIndex * 2 + colIndex
                    val revealed = picked != null
                    val color = when {
                        revealed && index == q.answerIndex -> GdColors.Good
                        revealed && index == picked -> GdColors.Danger
                        else -> GdColors.Primary
                    }
                    PrimaryButton(
                        word,
                        onClick = {
                            if (picked == null) {
                                picked = index
                                game.answer(q, index)
                                score = game.score
                            }
                        },
                        color = color,
                        modifier = Modifier.weight(1f).heightIn(min = MinTouch + 12.dp),
                    )
                }
            }
        }
        picked?.let { p ->
            Text(
                if (p == q.answerIndex) "정답! +10점" else "아쉬워요. 정답은 ${q.item.word}",
                style = MaterialTheme.typography.headlineSmall,
                color = if (p == q.answerIndex) GdColors.Good else GdColors.Warn,
            )
            PrimaryButton("다음 문제", onClick = {
                question = game.next()
                picked = null
            })
        }
    }
}
