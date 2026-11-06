package com.gildongmu.app.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gildongmu.app.app.AppContainer
import com.gildongmu.app.domain.engine.SessionSummary
import com.gildongmu.app.ui.common.Badge
import com.gildongmu.app.ui.common.GdCard
import com.gildongmu.app.ui.common.PrimaryButton
import com.gildongmu.app.ui.common.SectionTitle
import com.gildongmu.app.ui.theme.GdColors

/** 도착 / 요약 — 오늘의 점수·정답 수·플레이어별 점수·지나온 지역. 이동 앨범(2차)의 자리. */
@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun SummaryScreen(container: AppContainer, summary: SessionSummary, modifier: Modifier = Modifier) {
    val content by container.content.content.collectAsStateWithLifecycle()
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(if (summary.endedByUser) "오늘의 길동무 · 여기까지 했어요" else "도착! 오늘의 길동무", style = MaterialTheme.typography.headlineLarge, color = GdColors.Primary)
        Text(summary.destinationName, style = MaterialTheme.typography.titleLarge, color = GdColors.Muted)

        GdCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("모두 ${summary.totalCorrect}번 맞혔어요", style = MaterialTheme.typography.headlineMedium, color = GdColors.Text)
                summary.winner?.let { Text("오늘의 1등: ${it.name}", style = MaterialTheme.typography.titleLarge, color = GdColors.Accent) }
                val rounds = summary.roundsPlayed.groupingBy { it }.eachCount().entries.joinToString(" · ") { "${it.key.label} ${it.value}판" }
                if (rounds.isNotEmpty()) Text("함께한 게임: $rounds", style = MaterialTheme.typography.bodyLarge, color = GdColors.Muted)
                if (summary.longestChain > 0) Text("끝말잇기 최장 ${summary.longestChain}개 낱말", style = MaterialTheme.typography.bodyLarge, color = GdColors.Muted)
            }
        }

        GdCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("가족 점수")
                summary.players.sortedByDescending { summary.scores[it.id] ?: 0 }.forEachIndexed { i, p ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}. ${p.name}", style = MaterialTheme.typography.titleLarge, color = GdColors.Text, modifier = Modifier.weight(1f))
                        Text(
                            "${summary.scores[p.id] ?: 0}점 · 정답 ${summary.correctCounts[p.id] ?: 0}",
                            style = MaterialTheme.typography.titleMedium,
                            color = GdColors.Accent,
                        )
                    }
                }
            }
        }

        GdCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("지나온 지역")
                if (summary.regionsVisited.isEmpty()) {
                    Text("지역 정보가 없었어요 (일반 퀴즈로 진행)", style = MaterialTheme.typography.bodyLarge, color = GdColors.Muted)
                } else {

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        summary.regionsVisited.forEach { code -> Badge(content?.region(code)?.name ?: code) }
                    }
                }
                Text("이동 앨범(경로 위 게임 기록)은 다음 버전에서 만나요.", style = MaterialTheme.typography.bodySmall, color = GdColors.Muted)
            }
        }

        if (summary.stage1Count > 0) {
            Text(
                "가는 길에 쉬운 문제로 ${summary.stage1Count}번 바꿔 드렸어요. 장거리 운전, 수고 많으셨어요!",
                style = MaterialTheme.typography.bodyLarge,
                color = GdColors.Muted,
            )
        }
        PrimaryButton("처음으로", onClick = container.coordinator::dismissSummary, modifier = Modifier.fillMaxWidth())
    }
}
