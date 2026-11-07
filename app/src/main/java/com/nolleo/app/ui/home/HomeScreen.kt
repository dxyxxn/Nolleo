package com.nolleo.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.nolleo.app.R
import com.nolleo.app.app.AppContainer
import com.nolleo.app.app.PlanPreview
import com.nolleo.app.core.navi.DestinationPreset
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.domain.engine.SessionPlanner
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.RoundPacing
import com.nolleo.app.ui.common.ChoiceRow
import com.nolleo.app.ui.common.Hairline
import com.nolleo.app.ui.common.NlCard
import com.nolleo.app.ui.common.PrimaryButton
import com.nolleo.app.ui.common.SecondaryButton
import com.nolleo.app.ui.common.SegmentedControl
import com.nolleo.app.ui.common.SettingRow
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Sp

/**
 * IDLE: 여정 설정. 화면을 두 덩어리로만 나눈다 —
 * 왼쪽 `목적지` 목록(하나 고르기), 오른쪽 `오늘의 게임` 카드(미리보기 + 운전·함께·게임·간격 선택 막대 + 출발 버튼 하나).
 * 좁은 창은 위아래로 놓고 스크롤.
 */
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
    val c = Nl.colors

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth > 1000.dp && maxWidth > maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Sp.xxl, vertical = Sp.xl),
            verticalArrangement = Arrangement.spacedBy(Sp.xl),
        ) {
            restorable?.let { snap ->
                Banner(
                    title = "진행 중이던 여정이 있어요",
                    detail = "${snap.destinationName} · ${snap.roundIndex + 1}번째 판",
                ) {
                    SecondaryButton("새로 시작", onClick = coordinator::discardSaved)
                    PrimaryButton("이어서 하기", onClick = coordinator::resumeSaved, icon = R.drawable.ic_play)
                }
            }
            message?.let {
                Banner(title = it, detail = null) {
                    SecondaryButton("확인", onClick = coordinator::dismissMessage)
                }
            }

            if (ux.noSetup) {
                // 주행 중 설정 화면 제한 (CarUxRestrictions NO_SETUP) — 받은 비트만 반영한다
                NlCard(Modifier.fillMaxWidth(), spacing = Sp.l) {
                    Text("주행 중에는 여정 설정을 할 수 없어요", style = MaterialTheme.typography.headlineMedium, color = c.ink)
                    Text("정차한 뒤에 목적지와 게임을 골라 주세요.", style = MaterialTheme.typography.bodyLarge, color = c.muted)
                    preview?.let { p ->
                        Text("이전 설정: ${p.destinationName} · ${p.text}", style = MaterialTheme.typography.bodyLarge, color = c.ink)
                        PrimaryButton("이전 설정으로 출발", onClick = coordinator::startSession, icon = R.drawable.ic_play)
                    }
                }
                return@Column
            }

            val destination: @Composable (Modifier) -> Unit = { m ->
                DestinationCard(
                    selectedId = home.presetId,
                    useNavi = home.useNaviRoute,
                    naviName = naviDest?.destinationName,
                    naviRemain = naviDest?.remainTimeSec,
                    onPreset = coordinator::selectPreset,
                    onNavi = coordinator::selectNaviRoute,
                    modifier = m,
                )
            }
            val game: @Composable (Modifier) -> Unit = { m ->
                GameCard(
                    container = container,
                    preview = preview,
                    demo = settings.demoMode,
                    driverId = home.driverId,
                    passengers = home.passengerCount,
                    detected = detected,
                    choice = home.choice,
                    pacing = home.pacing,
                    // 이어서 할 여정이 있으면 그쪽이 주 버튼이므로 출발은 보조 버튼으로 (화면마다 채운 버튼은 하나)
                    primaryStart = restorable == null,
                    fillHeight = wide,
                    modifier = m,
                )
            }
            if (wide) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Sp.xl)) {
                    destination(Modifier.weight(1f).fillMaxHeight())
                    game(Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                destination(Modifier.fillMaxWidth())
                game(Modifier.fillMaxWidth())
            }
        }
    }
}

/** 맨 위 알림 한 줄: 제목·설명 왼쪽, 버튼 오른쪽 */
@Composable
private fun Banner(title: String, detail: String?, actions: @Composable () -> Unit) {
    val c = Nl.colors
    NlCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

@Composable
private fun DestinationCard(
    selectedId: String?,
    useNavi: Boolean,
    naviName: String?,
    naviRemain: Int?,
    onPreset: (String) -> Unit,
    onNavi: () -> Unit,
    modifier: Modifier,
) {
    val c = Nl.colors
    NlCard(modifier, spacing = Sp.xs) {
        Text("목적지", style = MaterialTheme.typography.titleMedium, color = c.ink, modifier = Modifier.padding(bottom = Sp.s))
        if (naviName != null && naviRemain != null) {
            ChoiceRow(
                title = "내비 목적지: $naviName",
                subtitle = "길 안내 그대로",
                trailing = TripUnits.formatDuration(naviRemain),
                selected = useNavi,
                onClick = onNavi,
            )
        }
        DestinationPreset.ALL.forEach { p ->
            ChoiceRow(
                title = p.title,
                subtitle = p.poiName,
                trailing = "약 ${TripUnits.formatDuration(p.estimatedSec)}",
                selected = !useNavi && selectedId == p.id,
                onClick = { onPreset(p.id) },
            )
        }
        Text(
            "남은 시간에 맞춰 판을 나눠요",
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
            modifier = Modifier.padding(top = Sp.s, start = Sp.l),
        )
    }
}

@Composable
private fun GameCard(
    container: AppContainer,
    preview: PlanPreview?,
    demo: Boolean,
    driverId: Int,
    passengers: Int,
    detected: Int?,
    choice: GameChoice,
    pacing: RoundPacing,
    primaryStart: Boolean,
    fillHeight: Boolean,
    modifier: Modifier,
) {
    val coordinator = container.coordinator
    val c = Nl.colors
    NlCard(modifier, spacing = Sp.l) {
        Text("오늘의 게임", style = MaterialTheme.typography.titleMedium, color = c.ink)
        PlanSummary(preview, demo)
        Hairline()
        val drivers = coordinator.drivers
        SettingRow("운전") {
            SegmentedControl(
                options = drivers,
                selected = drivers.firstOrNull { it.id == driverId } ?: drivers.first(),
                label = { it.name },
                onSelect = { coordinator.selectDriver(it.id) },
            )
        }
        SettingRow("함께") {
            SegmentedControl(
                options = (0..3).toList(),
                selected = passengers,
                label = { if (it == 0) "혼자" else "${it}명" },
                onSelect = coordinator::setPassengers,
            )
        }
        val suggested = detected?.coerceAtMost(3)
        if (suggested != null && suggested != passengers) {
            Row(Modifier.fillMaxWidth().padding(start = 88.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Text(
                    "좌석 센서가 동승자 ${suggested}명을 감지했어요",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.muted,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                SecondaryButton(if (suggested == 0) "혼자로" else "${suggested}명으로", onClick = { coordinator.setPassengers(suggested) })
            }
        }
        SettingRow("게임") {
            SegmentedControl(options = GameChoice.entries, selected = choice, label = { it.label }, onSelect = coordinator::selectChoice)
        }
        SettingRow("간격") {
            SegmentedControl(options = RoundPacing.entries, selected = pacing, label = { it.label }, onSelect = coordinator::selectPacing)
        }
        if (fillHeight) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(Sp.xs))
        if (primaryStart) {
            PrimaryButton("출발", onClick = coordinator::startSession, enabled = preview != null, icon = R.drawable.ic_play, modifier = Modifier.fillMaxWidth())
        } else {
            SecondaryButton("출발", onClick = coordinator::startSession, enabled = preview != null, icon = R.drawable.ic_play, modifier = Modifier.fillMaxWidth())
        }
        Text(
            "주행 중에는 음성으로, 정차 중에는 화면으로 놀아요",
            style = MaterialTheme.typography.labelSmall,
            color = c.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** 미리보기: "4시간 10분 → 5판 (판당 약 10분)" 크게, 판 구성과 쉬는 시간은 한 줄씩 */
@Composable
private fun ColumnScope.PlanSummary(preview: PlanPreview?, demo: Boolean) {
    val c = Nl.colors
    if (preview == null) {
        Text("목적지를 고르면 남은 시간에 맞춰 판을 짜 드려요.", style = MaterialTheme.typography.bodyLarge, color = c.muted)
        return
    }
    Text(preview.text, style = MaterialTheme.typography.headlineMedium, color = c.ink)
    val rounds = preview.plan.rounds.take(6).joinToString(" · ") { "${it.index + 1}판 ${it.game.label}" } +
        if (preview.plan.roundCount > 6) " …" else ""
    Text(rounds, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    val rest = SessionPlanner.restBetweenRoundsSec(preview.plan)?.let { "판 사이 약 ${TripUnits.formatDuration(it)} 쉬어요" }
    val line = listOfNotNull(rest, if (demo) "데모 모드: 판당 ${preview.plan.rounds.first().questionCount}문제" else null).joinToString(" · ")
    if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
}
