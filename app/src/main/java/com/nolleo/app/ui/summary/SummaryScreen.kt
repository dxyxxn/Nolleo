package com.nolleo.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.app.AppContainer
import com.nolleo.app.domain.engine.SessionSummary
import com.nolleo.app.domain.model.GameContent
import com.nolleo.app.ui.common.Avatar
import com.nolleo.app.ui.common.Chip
import com.nolleo.app.ui.common.Hairline
import com.nolleo.app.ui.common.NlCard
import com.nolleo.app.ui.common.PrimaryButton
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Sp

/** 도착 / 요약 — 오늘의 점수·정답 수·플레이어별 점수·지나온 지역. 이동 앨범(2차)의 자리. */
@Composable
fun SummaryScreen(container: AppContainer, summary: SessionSummary, modifier: Modifier = Modifier) {
    val content by container.content.content.collectAsStateWithLifecycle()
    val ux by container.carRepository.ux.collectAsStateWithLifecycle()
    // 주행 중(Distraction Optimization)에는 스크롤·카드 여러 장 대신 한눈에 읽는 3줄만 — 정차하면 전체 기록
    if (ux.requiresDO) {
        DrivingSummary(summary, onDone = container.coordinator::dismissSummary, modifier = modifier)
        return
    }
    val c = Nl.colors
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth > 1000.dp && maxWidth > maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Sp.xxl, vertical = Sp.xl),
            verticalArrangement = Arrangement.spacedBy(Sp.xl),
        ) {
            Column {
                Text(summaryTitle(summary), style = MaterialTheme.typography.headlineMedium, color = c.ink)
                Text(summary.destinationName, style = MaterialTheme.typography.bodyLarge, color = c.muted)
            }
            if (wide) {
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Sp.xl)) {
                    NlCard(Modifier.weight(1f).fillMaxHeight()) { TodayBlock(summary) }
                    NlCard(Modifier.weight(1f).fillMaxHeight()) { ScoresBlock(summary) }
                }
            } else {
                NlCard(Modifier.fillMaxWidth()) { TodayBlock(summary) }
                NlCard(Modifier.fillMaxWidth()) { ScoresBlock(summary) }
            }
            NlCard(Modifier.fillMaxWidth()) { RegionsBlock(summary, content) }

            if (summary.stage1Count > 0 || summary.stage2Count > 0) {
                Text(
                    buildString {
                        if (summary.stage1Count > 0) append("가는 길에 쉬운 문제로 ${summary.stage1Count}번 바꿔 드렸어요. ")
                        if (summary.stage2Count > 0) append("휴게소·졸음쉼터에서 쉬어 가기를 ${summary.stage2Count}번 권했어요. ")
                        append("장거리 운전, 수고 많으셨어요.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.muted,
                )
            }
            PrimaryButton("처음으로", onClick = container.coordinator::dismissSummary, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** 주행 중 요약: 제목 · 맞힌 수와 1등 한 줄 · 안내 한 줄 + "처음으로" (스크롤 없음) */
@Composable
private fun DrivingSummary(summary: SessionSummary, onDone: () -> Unit, modifier: Modifier) {
    val c = Nl.colors
    Column(modifier.fillMaxSize().padding(horizontal = Sp.xxl, vertical = Sp.xl), verticalArrangement = Arrangement.Center) {
        NlCard(Modifier.fillMaxWidth(), spacing = Sp.l) {
            Text(summaryTitle(summary), style = MaterialTheme.typography.headlineMedium, color = c.ink, maxLines = 1)
            Text(
                listOfNotNull("${summary.totalCorrect}번 맞혔어요", summary.winner?.let { "1등 ${it.name}" }).joinToString(" · "),
                style = MaterialTheme.typography.titleLarge,
                color = c.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text("차를 세우면 오늘의 기록을 모두 보여 드려요", style = MaterialTheme.typography.bodyLarge, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            PrimaryButton("처음으로", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun summaryTitle(summary: SessionSummary) = if (summary.endedByUser) "여기까지 했어요" else "도착했어요"

/** 오늘의 기록: 맞힌 수(크게) · 1등 · 함께한 게임 · 끝말잇기 최장 */
@Composable
private fun ColumnScope.TodayBlock(summary: SessionSummary) {
    val c = Nl.colors
    Text("오늘의 기록", style = MaterialTheme.typography.titleMedium, color = c.ink)
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Sp.s)) {
        Text("${summary.totalCorrect}", style = MaterialTheme.typography.displayLarge, color = c.ink)
        Text("번 맞혔어요", style = MaterialTheme.typography.titleLarge, color = c.ink, modifier = Modifier.padding(bottom = Sp.m))
    }
    summary.winner?.let { w ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
            Avatar(w.name, strong = true)
            Text("오늘의 1등: ${w.name}", style = MaterialTheme.typography.titleMedium, color = c.ink)
        }
    }
    val rounds = summary.roundsPlayed.groupingBy { it }.eachCount().entries.joinToString(" · ") { "${it.key.label} ${it.value}판" }
    if (rounds.isNotEmpty()) Text("함께한 게임: $rounds", style = MaterialTheme.typography.bodyLarge, color = c.muted)
    if (summary.longestChain > 0) Text("끝말잇기 최장 ${summary.longestChain}개 낱말", style = MaterialTheme.typography.bodyLarge, color = c.muted)
}

@Composable
private fun ColumnScope.ScoresBlock(summary: SessionSummary) {
    val c = Nl.colors
    Text("가족 점수", style = MaterialTheme.typography.titleMedium, color = c.ink)
    summary.players.sortedByDescending { summary.scores[it.id] ?: 0 }.forEachIndexed { i, p ->
        if (i > 0) Hairline()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
            Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = c.muted)
            Avatar(p.name)
            Text(p.name, style = MaterialTheme.typography.titleSmall, color = c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${summary.scores[p.id] ?: 0}점 · 정답 ${summary.correctCounts[p.id] ?: 0}",
                style = MaterialTheme.typography.titleSmall,
                color = c.ink,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.RegionsBlock(summary: SessionSummary, content: GameContent?) {
    val c = Nl.colors
    Text("지나온 지역", style = MaterialTheme.typography.titleMedium, color = c.ink)
    if (summary.regionsVisited.isEmpty()) {
        Text("지역 정보가 없었어요 (일반 퀴즈로 진행)", style = MaterialTheme.typography.bodyLarge, color = c.muted)
    } else {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Sp.s), verticalArrangement = Arrangement.spacedBy(Sp.s)) {
            summary.regionsVisited.forEach { code -> Chip(content?.region(code)?.name ?: code) }
        }
    }
    Text("이동 앨범(경로 위 게임 기록)은 다음 버전에서 만나요.", style = MaterialTheme.typography.bodySmall, color = c.muted)
}
