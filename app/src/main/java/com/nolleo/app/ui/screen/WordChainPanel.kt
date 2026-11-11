package com.nolleo.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.R
import com.nolleo.app.app.ScreenWordChainController
import com.nolleo.app.domain.engine.MicState
import com.nolleo.app.domain.engine.WordChainPlay
import com.nolleo.app.domain.model.Player
import com.nolleo.app.ui.common.AnswerCountdown
import com.nolleo.app.ui.common.Avatar
import com.nolleo.app.ui.common.Chip
import com.nolleo.app.ui.common.MicIndicator
import com.nolleo.app.ui.common.NlCard
import com.nolleo.app.ui.common.PrimaryButton
import com.nolleo.app.ui.common.SecondaryButton
import com.nolleo.app.ui.common.WordChainRow
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Sp

/**
 * 정차 중 끝말잇기 한 판 — 주행 중 음성 판처럼 말로 잇는다(11/8 사용자 요청). 가운데 맞춘 위아래 배치:
 * 머리 줄(← 목록 · 제목·점수·상태 · 새로 시작) 아래 남은 높이의 가운데에 이어진 낱말(마지막 4개) → 누구 차례·무엇으로 시작 → 남은 초 →
 * 마이크 → 들은 말.
 * 멈췄거나 끝나면 가운데 카드에 "이어서 하기"·"다시 하기"(채운 버튼 하나).
 */
@Composable
fun WordChainPanel(game: ScreenWordChainController, nowMs: () -> Long, onClose: () -> Unit) {
    val ui by game.state.collectAsStateWithLifecycle()
    val s = ui.play
    val c = Nl.colors

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
        Header(ui, onRestart = game::restart, onClose = onClose)
        // 내용이 적으므로 남은 높이의 가운데에 둔다 (좁은 창에서 넘치면 스크롤)
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 1100.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Sp.l, Alignment.CenterVertically),
            ) {
                WordChainRow(s.words)
                val ended = s.ended
                when {
                    ended != null -> NlCard(Modifier.fillMaxWidth(), spacing = Sp.l, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ended.title, style = MaterialTheme.typography.headlineMedium, color = c.ink, textAlign = TextAlign.Center)
                        Text(
                            "모두 ${s.words.size}개 낱말을 이었어요 · ${s.score}점",
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.muted,
                            textAlign = TextAlign.Center,
                        )
                        PrimaryButton("다시 하기", onClick = game::restart, icon = R.drawable.ic_play)
                    }
                    !ui.running -> NlCard(Modifier.fillMaxWidth(), spacing = Sp.l, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (ui.voiceStopped) "소리가 멈췄어요" else if (ui.paused) "잠깐 멈췄어요" else "놀레오가 먼저 말해요",
                            style = MaterialTheme.typography.headlineSmall,
                            color = c.ink,
                            textAlign = TextAlign.Center,
                        )
                        s.needed?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center) }
                        PrimaryButton(if (ui.paused) "이어서 하기" else "시작하기", onClick = game::open, icon = R.drawable.ic_play)
                    }
                    else -> Turn(s, nowMs)
                }
                LastAnswer(s)
            }
        }
    }
}

@Composable
private fun Header(ui: ScreenWordChainController.UiState, onRestart: () -> Unit, onClose: () -> Unit) {
    val c = Nl.colors
    val s = ui.play
    // 머리 줄 하나: 왼쪽 ← 목록(닫기), 가운데 제목·점수·상태, 오른쪽 새로 시작
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.l)) {
        SecondaryButton("목록", onClick = onClose, icon = R.drawable.ic_back)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Text("끝말잇기", style = MaterialTheme.typography.titleLarge, color = c.ink)
                Chip("${s.score}점")
            }
            val status = when {
                s.ended != null -> "${s.words.size}개 낱말을 이었어요"
                ui.voiceStopped -> "소리가 멈췄어요 · 이어서 하기를 눌러 주세요"
                ui.running && s.phase == WordChainPlay.Phase.LISTENING -> "듣고 있어요 · 낱말 하나를 말해요"
                ui.running && s.phase == WordChainPlay.Phase.SPEAKING -> "놀레오가 말하는 중이에요"
                // 답 뒤 판정 줄을 더 보여 주는 동안(말하지도 듣지도 않음)은 지금까지의 기록
                s.answered > 0 -> "${s.answered}번 중 ${s.correct}번 이었어요"
                else -> "놀레오 · 운전자 · 동승자 차례로 말로 이어요"
            }
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (ui.voiceStopped) c.ink else c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 끝난 판은 가운데 "다시 하기"가 있으므로 오른쪽 버튼을 두지 않는다
        if (s.ended == null && s.words.isNotEmpty()) SecondaryButton("새로 시작", onClick = onRestart)
    }
}

/** 진행 중인 차례: 누구 차례 · 무엇으로 시작 · (듣는 중) 남은 초 · 마이크 */
@Composable
private fun Turn(s: WordChainPlay.State, nowMs: () -> Long) {
    val c = Nl.colors
    val player = s.player
    // 누구 차례와 무엇으로 시작은 한 단계 좁게 — 큰 글 위의 빈 곳까지 더해 낱말 줄–누구 차례 간격과 같아 보이게 (11/11)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.s)) {
        if (player != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Avatar(player.name, strong = player.id != Player.APP.id)
                Text("${player.name} 차례", style = MaterialTheme.typography.titleLarge, color = c.ink)
            }
        }
        s.needed?.let { Text(it, style = MaterialTheme.typography.headlineLarge, color = c.ink, textAlign = TextAlign.Center) }
    }
    val deadline = s.listenDeadlineMs
    val partial = s.partial
    when {
        s.phase == WordChainPlay.Phase.LISTENING && deadline != null -> AnswerCountdown(deadline, nowMs)
        s.phase == WordChainPlay.Phase.LISTENING && !partial.isNullOrBlank() ->
            Text(partial, style = MaterialTheme.typography.headlineSmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        else -> Unit
    }
    MicIndicator(
        when (s.phase) {
            WordChainPlay.Phase.LISTENING -> MicState.LISTENING
            WordChainPlay.Phase.SPEAKING -> MicState.SPEAKING
            else -> MicState.IDLE
        },
    )
}

@Composable
private fun LastAnswer(s: WordChainPlay.State) {
    val line = s.lastAnswer ?: return
    Text(
        "${line.playerName}: ${line.heard ?: "(응답 없음)"} → ${line.verdict}",
        style = MaterialTheme.typography.bodyLarge,
        color = Nl.colors.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
}
