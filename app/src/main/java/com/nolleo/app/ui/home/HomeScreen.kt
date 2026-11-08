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
import com.nolleo.app.app.DestinationChoice
import com.nolleo.app.app.DestinationOptions
import com.nolleo.app.app.HomeState
import com.nolleo.app.app.PlanPreview
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.domain.engine.SessionPlanner
import com.nolleo.app.domain.model.GameChoice
import com.nolleo.app.domain.model.RoundPacing
import com.nolleo.app.ui.common.ChoiceRow
import com.nolleo.app.ui.common.SettingLabelWidth
import com.nolleo.app.ui.common.isWideLayout
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
 * 왼쪽 `목적지` 목록(현재 경로 · 내비 최근 목적지 · 목적지 없이 시간, 하나 고르기),
 * 오른쪽 `오늘의 게임` 카드(미리보기 + 운전·함께·게임·간격 선택 막대 + 출발 버튼 하나).
 * 좁은 창은 위아래로 놓고 스크롤.
 */
@Composable
fun HomeScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val coordinator = container.coordinator
    val home by coordinator.home.collectAsStateWithLifecycle()
    val preview by coordinator.planPreview.collectAsStateWithLifecycle()
    val restorable by coordinator.restorable.collectAsStateWithLifecycle()
    val detected by coordinator.detectedPassengers.collectAsStateWithLifecycle()
    val options by coordinator.destinationOptions.collectAsStateWithLifecycle()
    val ux by container.carRepository.ux.collectAsStateWithLifecycle()
    val message by coordinator.message.collectAsStateWithLifecycle()
    val settings by coordinator.settings.collectAsStateWithLifecycle()
    val c = Nl.colors

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = isWideLayout()
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
                        // 이어서 할 여정이 있으면 위 배너의 "이어서 하기"가 주 버튼 (화면마다 채운 버튼은 하나)
                        if (restorable == null) {
                            PrimaryButton("이전 설정으로 출발", onClick = coordinator::startSession, icon = R.drawable.ic_play)
                        } else {
                            SecondaryButton("이전 설정으로 출발", onClick = coordinator::startSession, icon = R.drawable.ic_play)
                        }
                    }
                }
                return@Column
            }

            val destination: @Composable (Modifier) -> Unit = { m ->
                DestinationCard(
                    options = options,
                    selected = home.effective(options),
                    timerMinutes = home.timerMinutes,
                    onSelect = coordinator::selectDestination,
                    onTimer = coordinator::selectTimer,
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
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (detail != null) Text(detail, style = MaterialTheme.typography.bodyLarge, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}

/**
 * 목적지 목록 (최대 5줄 — 주행 중 긴 목록 금지):
 *  - 현재 경로: 내비에 잡힌 길 안내 (디버그 패널 여정=시뮬이면 시뮬 경로)
 *  - 최근 목적지: 길 안내가 없을 때만 내비 최근 목적지 3곳 — 출발을 누를 때 그곳으로 길 안내를 건다
 *    (길 안내 중에는 숨긴다: 고르면 운전자의 길 안내를 바꾸게 된다)
 *  - 목적지 없이: 정한 시간만큼 (30분 · 1시간 · 1시간 반 · 2시간)
 */
@Composable
private fun DestinationCard(
    options: DestinationOptions,
    selected: DestinationChoice,
    timerMinutes: Int,
    onSelect: (DestinationChoice) -> Unit,
    onTimer: (Int) -> Unit,
    modifier: Modifier,
) {
    val c = Nl.colors
    NlCard(modifier, spacing = Sp.xs) {
        Text("목적지", style = MaterialTheme.typography.titleLarge, color = c.ink, modifier = Modifier.padding(bottom = Sp.s))
        options.route?.let { r ->
            ChoiceRow(
                title = r.simulatedTitle ?: "현재 경로: ${r.destinationName}",
                subtitle = if (r.simulatedTitle != null) "시뮬 여정 · ${r.destinationName}" else "내비 길 안내 그대로",
                trailing = r.remainSec?.let { if (r.simulatedTitle != null) "약 ${TripUnits.formatDuration(it)}" else TripUnits.formatDuration(it) },
                selected = selected == DestinationChoice.CurrentRoute,
                onClick = { onSelect(DestinationChoice.CurrentRoute) },
            )
        }
        options.recents.forEach { o ->
            val choice = DestinationChoice.Recent(o.place.key)
            ChoiceRow(
                title = o.place.name,
                subtitle = listOf("최근 목적지", o.place.address).filter { it.isNotBlank() }.joinToString(" · "),
                trailing = o.estimatedSec?.let { "약 ${TripUnits.formatDuration(it)}" } ?: "시간 확인 중",
                selected = selected == choice,
                onClick = { onSelect(choice) },
            )
        }
        ChoiceRow(
            title = "목적지 없이",
            subtitle = "정한 시간만큼 놀아요",
            trailing = HomeState.timerLabel(timerMinutes),
            selected = selected == DestinationChoice.Timer,
            onClick = { onSelect(DestinationChoice.Timer) },
        )
        // 시간 칸을 누르면 그 시간으로 "목적지 없이"를 고른다
        SegmentedControl(
            options = HomeState.TIMER_MINUTES,
            selected = timerMinutes,
            label = HomeState::timerLabel,
            onSelect = onTimer,
            modifier = Modifier.padding(start = Sp.l, top = Sp.xs),
        )
        Text(
            "남은 시간에 맞춰 판을 나눠요",
            style = MaterialTheme.typography.bodyLarge,
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
        Text("오늘의 게임", style = MaterialTheme.typography.titleLarge, color = c.ink)
        PlanSummary(preview, demo, choice)
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
            Row(Modifier.fillMaxWidth().padding(start = SettingLabelWidth), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Text(
                    "좌석 센서가 동승자 ${suggested}명을 감지했어요",
                    style = MaterialTheme.typography.bodyLarge,
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
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/**
 * 미리보기: "4시간 10분 → 5판 (판당 약 10분)" 크게, 판 구성과 쉬는 시간은 한 줄씩.
 * 랜덤이면 판 구성을 보이지 않는다 — 무엇이 나올지 모르고 시작하는 재미(11/8 사용자 요청). 판이 시작될 때 알린다.
 */
@Composable
private fun ColumnScope.PlanSummary(preview: PlanPreview?, demo: Boolean, choice: GameChoice) {
    val c = Nl.colors
    if (preview == null) {
        // 최근 목적지의 예상 시간(내비 requestTrip)을 아직 받지 못함
        Text("예상 시간을 확인하고 있어요. 받으면 남은 시간에 맞춰 판을 짜 드려요.", style = MaterialTheme.typography.titleSmall, color = c.muted)
        return
    }
    Text(preview.text, style = MaterialTheme.typography.headlineLarge, color = c.ink)
    val rounds = if (choice == GameChoice.RANDOM) {
        RANDOM_PREVIEW
    } else {
        preview.plan.rounds.take(6).joinToString(" · ") { "${it.index + 1}판 ${it.game.label}" } +
            if (preview.plan.roundCount > 6) " …" else ""
    }
    Text(rounds, style = MaterialTheme.typography.bodyLarge, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    val rest = SessionPlanner.restBetweenRoundsSec(preview.plan)?.let { "판 사이 약 ${TripUnits.formatDuration(it)} 쉬어요" }
    val line = listOfNotNull(rest, if (demo) "데모 모드: 판당 ${preview.plan.rounds.first().questionCount}문제" else null).joinToString(" · ")
    if (line.isNotEmpty()) Text(line, style = MaterialTheme.typography.bodyLarge, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** 랜덤일 때 판 구성 대신 보이는 줄 */
private const val RANDOM_PREVIEW = "판마다 무엇이 나올지는 시작해야 알아요"
