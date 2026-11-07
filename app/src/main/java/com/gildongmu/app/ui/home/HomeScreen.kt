package com.gildongmu.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.gildongmu.app.R
import com.gildongmu.app.app.AppContainer
import com.gildongmu.app.core.navi.DestinationPreset
import com.gildongmu.app.core.navi.TripUnits
import com.gildongmu.app.domain.engine.SessionPlanner
import com.gildongmu.app.domain.model.GameChoice
import com.gildongmu.app.domain.model.RoundPacing
import com.gildongmu.app.ui.common.ChoiceButton
import com.gildongmu.app.ui.common.GdCard
import com.gildongmu.app.ui.common.PrimaryButton
import com.gildongmu.app.ui.common.SectionTitle
import com.gildongmu.app.ui.theme.GdColors

/** IDLE: 여정 설정 — 프리셋 목적지·운전자·동승자·게임 선택 → 플랜 미리보기 → 출발 */
@Composable
fun HomeScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val coordinator = container.coordinator
    val home by coordinator.home.collectAsStateWithLifecycle()
    val preview by coordinator.planPreview.collectAsStateWithLifecycle()
    val restorable by coordinator.restorable.collectAsStateWithLifecycle()
    val detected by coordinator.detectedPassengers.collectAsStateWithLifecycle()
    val naviDest by container.naviTrip.activeNaviDestination.collectAsStateWithLifecycle()
    val ux by container.carRepository.ux.collectAsStateWithLifecycle()
    val message by coordinator.message.collectAsStateWithLifecycle()
    val settings by coordinator.settings.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth > 1000.dp && maxWidth > maxHeight
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            restorable?.let { snap ->
                GdCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("진행 중이던 여정이 있어요", style = MaterialTheme.typography.titleLarge, color = GdColors.Accent)
                        Text("${snap.destinationName} · ${snap.roundIndex + 1}번째 판", style = MaterialTheme.typography.bodyLarge, color = GdColors.Text)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PrimaryButton("이어서 하기", onClick = coordinator::resumeSaved)
                            ChoiceButton("새로 시작", selected = false, onClick = coordinator::discardSaved)
                        }
                    }
                }
            }
            message?.let {
                GdCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = GdColors.Warn, modifier = Modifier.weight(1f))
                        ChoiceButton("확인", selected = false, onClick = coordinator::dismissMessage)
                    }
                }
            }

            if (ux.noSetup) {
                // 주행 중 설정 화면 제한 (CarUxRestrictions NO_SETUP) — 받은 비트만 반영한다
                GdCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("주행 중에는 여정 설정을 할 수 없어요", style = MaterialTheme.typography.headlineSmall, color = GdColors.Text)
                        Text("정차한 뒤에 목적지와 게임을 골라 주세요.", style = MaterialTheme.typography.bodyLarge, color = GdColors.Muted)
                        preview?.let { p ->
                            Text("이전 설정: ${p.destinationName} · ${p.text}", style = MaterialTheme.typography.bodyLarge, color = GdColors.Text)
                            PrimaryButton("이전 설정으로 출발", onClick = coordinator::startSession)
                        }
                    }
                }
                return@Column
            }

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        DestinationSection(home.presetId, home.useNaviRoute, naviDest?.destinationName, naviDest?.remainTimeSec, coordinator::selectPreset, coordinator::selectNaviRoute)
                        PeopleSection(container, home.driverId, home.passengerCount, detected)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        GameSection(home.choice, coordinator::selectChoice)
                        PacingSection(home.pacing, coordinator::selectPacing)
                        PlanSection(preview, settings.demoMode, coordinator::startSession)
                    }
                }
            } else {
                DestinationSection(home.presetId, home.useNaviRoute, naviDest?.destinationName, naviDest?.remainTimeSec, coordinator::selectPreset, coordinator::selectNaviRoute)
                PeopleSection(container, home.driverId, home.passengerCount, detected)
                GameSection(home.choice, coordinator::selectChoice)
                PacingSection(home.pacing, coordinator::selectPacing)
                PlanSection(preview, settings.demoMode, coordinator::startSession)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DestinationSection(
    selectedId: String?,
    useNavi: Boolean,
    naviName: String?,
    naviRemain: Int?,
    onPreset: (String) -> Unit,
    onNavi: () -> Unit,
) {
    Column {
        SectionTitle("어디로 가세요?")
        if (naviName != null && naviRemain != null) {
            ChoiceButton(
                text = "내비 목적지: $naviName",
                subText = "남은 시간 ${TripUnits.formatDuration(naviRemain)} · 길 안내 그대로",
                selected = useNavi,
                onClick = onNavi,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        DestinationPreset.ALL.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { p ->
                    ChoiceButton(
                        text = p.title,
                        subText = p.subtitle,
                        selected = !useNavi && selectedId == p.id,
                        onClick = { onPreset(p.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeopleSection(container: AppContainer, driverId: Int, passengers: Int, detected: Int?) {
    val coordinator = container.coordinator
    Column {
        SectionTitle("누가 운전하나요?")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            coordinator.drivers.forEach { d ->
                ChoiceButton(d.name, selected = d.id == driverId, onClick = { coordinator.selectDriver(d.id) }, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(20.dp))
        SectionTitle("함께 타는 사람")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            (0..3).forEach { n ->
                ChoiceButton(if (n == 0) "혼자" else "${n}명", selected = n == passengers, onClick = { coordinator.setPassengers(n) }, modifier = Modifier.weight(1f))
            }
        }
        if (detected != null) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "좌석 센서: 동승자 ${detected.coerceAtMost(3)}명이 감지됐어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GdColors.Good,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detected.coerceAtMost(3) != passengers) {
                    ChoiceButton("적용", selected = false, onClick = { coordinator.setPassengers(detected) })
                }
            }
        }
    }
}

@Composable
private fun GameSection(choice: GameChoice, onChoice: (GameChoice) -> Unit) {
    Column {
        SectionTitle("어떤 게임을 할까요?")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameChoice.entries.forEach { g ->
                ChoiceButton(g.label, selected = g == choice, onClick = { onChoice(g) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** 판 간격 — 여정에 판을 얼마나 촘촘히 놓을지 (판 사이에는 말을 걸지 않는다) */
@Composable
private fun PacingSection(pacing: RoundPacing, onPacing: (RoundPacing) -> Unit) {
    Column {
        SectionTitle("얼마나 자주 할까요?")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RoundPacing.entries.forEach { p ->
                ChoiceButton(
                    text = p.label,
                    subText = p.detail,
                    selected = p == pacing,
                    onClick = { onPacing(p) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PlanSection(preview: com.gildongmu.app.app.PlanPreview?, demo: Boolean, onStart: () -> Unit) {
    GdCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("오늘의 게임 구성")
            if (preview == null) {
                Text("목적지를 고르면 남은 시간에 맞춰 판을 짜 드려요.", style = MaterialTheme.typography.bodyLarge, color = GdColors.Muted)
            } else {
                Text(preview.text, style = MaterialTheme.typography.headlineSmall, color = GdColors.Text)
                val rounds = preview.plan.rounds.take(6).joinToString(" · ") { "${it.index + 1}판 ${it.game.label}" } +
                    if (preview.plan.roundCount > 6) " …" else ""
                Text(rounds, style = MaterialTheme.typography.bodyMedium, color = GdColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                SessionPlanner.restBetweenRoundsSec(preview.plan)?.let { rest ->
                    Text("판 사이 약 ${TripUnits.formatDuration(rest)} 쉬어요", style = MaterialTheme.typography.bodyMedium, color = GdColors.Muted)
                }
                if (demo) Text("데모 모드: 판당 ${preview.plan.rounds.first().questionCount}문제", style = MaterialTheme.typography.labelMedium, color = GdColors.Accent)
            }
            PrimaryButton(
                "출발",
                onClick = onStart,
                enabled = preview != null,
                icon = R.drawable.ic_play,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "주행 중에는 음성으로, 정차 중에는 화면으로 놀아요.",
                style = MaterialTheme.typography.bodySmall,
                color = GdColors.Muted,
            )
        }
    }
}
