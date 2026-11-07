package com.gildongmu.app.ui.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gildongmu.app.R
import com.gildongmu.app.app.AppContainer
import com.gildongmu.app.app.RestCareController
import com.gildongmu.app.app.Stage1Card
import com.gildongmu.app.core.navi.TripUnits
import com.gildongmu.app.domain.engine.MicState
import com.gildongmu.app.domain.engine.SessionPhase
import com.gildongmu.app.domain.engine.SessionPlanner
import com.gildongmu.app.domain.engine.SessionState
import com.gildongmu.app.domain.model.GameMode
import com.gildongmu.app.domain.model.PauseReason
import com.gildongmu.app.domain.model.TripSourceKind
import com.gildongmu.app.domain.model.UxState
import com.gildongmu.app.domain.rest.RestStopSuggestion
import com.gildongmu.app.ui.common.Badge
import com.gildongmu.app.ui.common.ChoiceButton
import com.gildongmu.app.ui.common.PrimaryButton
import com.gildongmu.app.ui.theme.GdColors

/**
 * VOICE / PAUSED 화면 (기획서 화면안): 상단 "음성 모드 · 도착까지 1시간 12분", 가운데 현재 문제 ≤ 3줄,
 * 하단 마이크 상태(정적 3종 — 주행 중 애니메이션 금지) + "화면 조작 잠금". 터치 가능한 것은 버튼 1개.
 */
@Composable
fun VoiceGameScreen(container: AppContainer, modifier: Modifier = Modifier) {
    val coordinator = container.coordinator
    val mode by coordinator.gameMode.collectAsStateWithLifecycle()
    val state by coordinator.sessionState.collectAsStateWithLifecycle()
    val trip by container.tripRepository.snapshot.collectAsStateWithLifecycle()
    val tripKind by container.tripRepository.activeKind.collectAsStateWithLifecycle()
    val ux by container.carRepository.ux.collectAsStateWithLifecycle()
    val reasons by container.modeController.pauseReasons.collectAsStateWithLifecycle()
    val stage1 by coordinator.stage1Card.collectAsStateWithLifecycle()
    val restState by coordinator.restCare.state.collectAsStateWithLifecycle()
    val restDistance by coordinator.restCare.distanceM.collectAsStateWithLifecycle()
    val region by container.locationRepository.regionCode.collectAsStateWithLifecycle()
    val content by container.content.content.collectAsStateWithLifecycle()
    val s = state ?: return

    val remainText = trip?.remainTimeSec?.let { "도착까지 ${TripUnits.formatDuration(it)}" } ?: "도착 시간 확인 중"
    val regionName = content?.region(region)?.name

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth > 1000.dp && maxWidth > maxHeight
        Row(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // 상단 상태 줄
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (mode == GameMode.PAUSED) "일시정지 · $remainText" else "음성 모드 · $remainText",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (mode == GameMode.PAUSED) GdColors.Warn else GdColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (s.plan.roundCount > 0 && s.roundGame != null) Badge("${s.roundIndex + 1}/${s.plan.roundCount}판 · ${s.roundGame.label}")
                    regionName?.let { Badge(it) }
                    if (tripKind == TripSourceKind.SIMULATED) Badge("시뮬레이션 여정", color = GdColors.Outline)
                }
                Spacer(Modifier.height(24.dp))

                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when {
                        mode == GameMode.PAUSED -> PausedBlock(reasons, coordinator::resume)
                        else -> CenterBlock(s, ux, trip?.remainTimeSec)
                    }
                }

                val rest = restState
                if (rest != null) {
                    RestOverlay(
                        state = rest,
                        distanceM = restDistance,
                        onAccept = coordinator::acceptRest,
                        onDecline = coordinator::declineRest,
                        onCancel = coordinator::cancelRest,
                    )
                } else {
                    stage1?.let { Stage1Overlay(it) }
                }
                Spacer(Modifier.height(16.dp))
                BottomBar(
                    mode = mode,
                    s = s,
                    reasons = reasons,
                    onPause = coordinator::pause,
                    onResume = coordinator::resume,
                    onNextRound = coordinator::startNextRoundNow,
                    onEnd = coordinator::endSession,
                )
            }
            if (wide) {
                ScorePanel(container, s, Modifier.width(380.dp).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun CenterBlock(s: SessionState, ux: UxState, remainSec: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
        when (s.phase) {
            SessionPhase.WAITING -> BigText("곧 음성 게임을 시작해요")
            SessionPhase.RESTING -> {
                // 판은 여정 전체에 나눠 배치된다(예: 4시간 → 5판). 판 사이는 운전에 집중하도록 말을 걸지 않는 시간.
                BigText("쉬는 시간이에요")
                val eta = SessionPlanner.secondsUntilNextRound(remainSec, s.nextRoundAtRemainSec)
                Text(
                    when {
                        eta == null -> "다음 판을 준비하고 있어요"
                        eta <= 60 -> "곧 다음 판을 시작해요"
                        else -> "다음 판은 약 ${TripUnits.formatDuration(eta)} 뒤에 시작해요"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = GdColors.Text,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "판 사이에는 말을 걸지 않아요 · 바로 하려면 '지금 시작'",
                    style = MaterialTheme.typography.titleMedium,
                    color = GdColors.Muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SessionPhase.ALL_DONE -> {
                BigText("오늘 준비한 판을 모두 했어요!")
                Text("도착하면 결과를 정리해 드릴게요.", style = MaterialTheme.typography.titleLarge, color = GdColors.Muted)
            }
            SessionPhase.FINISHING, SessionPhase.FINISHED -> BigText("오늘의 결과를 정리하고 있어요")
            else -> {
                s.currentPlayer?.let { p ->
                    Text("${p.name} 차례", style = MaterialTheme.typography.titleLarge, color = GdColors.Accent)
                }
                // 규제가 LIMIT_STRING_LENGTH 면 1줄 요약, 아니면 ≤ 3줄
                val text = if (ux.limitStringLength) s.shortDisplay else (s.display ?: s.caption)
                Text(
                    text ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = GdColors.Text,
                    textAlign = TextAlign.Center,
                    maxLines = if (ux.limitStringLength) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                MicIndicator(s.mic)
                val line = s.lastAnswer
                if (line != null && !ux.noVoiceTranscription) {
                    Text(
                        "${line.playerName}: ${line.heard ?: "(응답 없음)"} → ${line.verdict}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GdColors.Muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun BigText(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, color = GdColors.Text, textAlign = TextAlign.Center, maxLines = 3)
}

/** 정적 상태 3종 (대기 / 듣는 중 / 말하는 중) — 애니메이션 없음 */
@Composable
private fun MicIndicator(mic: MicState) {
    val (icon, label, color) = when (mic) {
        MicState.LISTENING -> Triple(R.drawable.ic_mic, "듣는 중", GdColors.Good)
        MicState.SPEAKING -> Triple(R.drawable.ic_speaker, "말하는 중", GdColors.Primary)
        MicState.IDLE -> Triple(R.drawable.ic_hourglass, "대기", GdColors.Muted)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .border(2.dp, color, RoundedCornerShape(40.dp))
            .padding(horizontal = 28.dp, vertical = 12.dp),
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = color, modifier = Modifier.size(48.dp))
        Text(label, style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
private fun PausedBlock(reasons: Set<PauseReason>, onResume: () -> Unit) {
    val main = reasons.firstOrNull { it == PauseReason.NAV_GUIDANCE } ?: reasons.firstOrNull { it == PauseReason.AUDIO_FOCUS } ?: reasons.firstOrNull()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Icon(painterResource(R.drawable.ic_pause), contentDescription = null, tint = GdColors.Warn, modifier = Modifier.size(96.dp))
        Text(main?.title ?: "잠시 멈췄어요", style = MaterialTheme.typography.headlineLarge, color = GdColors.Text, textAlign = TextAlign.Center)
        Text(main?.detail ?: "", style = MaterialTheme.typography.titleLarge, color = GdColors.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Stage1Overlay(card: Stage1Card) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(GdColors.SurfaceHigh, RoundedCornerShape(24.dp))
            .border(2.dp, GdColors.Accent, RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("잠깐 쉬어 갈까요?", style = MaterialTheme.typography.headlineSmall, color = GdColors.Accent)
        Text("반응이 조금 느려졌어요 · 다음 문제는 쉬운 걸로 갈게요", style = MaterialTheme.typography.titleMedium, color = GdColors.Text, maxLines = 2)
    }
}

/** 졸음 케어 2단계: 쉬어 가기 권유 → 쉼터로 가는 중 → 도착. 글은 3줄 이하, 버튼은 크게 (주행 중에도 한 번 누르면 끝) */
@Composable
private fun RestOverlay(
    state: RestCareController.State,
    distanceM: Int?,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onCancel: () -> Unit,
) {
    val s = state.suggestion
    val target = s?.let { listOfNotNull(it.name, it.directionLabel).joinToString(" · ") }
    val distance = (distanceM ?: s?.distanceM)?.let { d -> if (d >= 1_000) "${(d / 100) / 10.0}km" else "${d}m" }
    Column(
        Modifier
            .fillMaxWidth()
            .background(GdColors.SurfaceHigh, RoundedCornerShape(24.dp))
            .border(2.dp, GdColors.Accent, RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state) {
            is RestCareController.State.Offered -> {
                Text("잠깐 쉬어 가요", style = MaterialTheme.typography.headlineSmall, color = GdColors.Accent)
                Text(
                    if (s != null) "반응이 계속 느려졌어요 · $target ${distance ?: ""} 앞" else "반응이 계속 느려졌어요 · 가까운 졸음쉼터나 휴게소에서 쉬어 가요",
                    style = MaterialTheme.typography.titleMedium,
                    color = GdColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                s?.facilityLine?.let {
                    Text(it, style = MaterialTheme.typography.titleMedium, color = GdColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (s != null) PrimaryButton("쉬어 가기", onClick = onAccept, color = GdColors.Accent, contentColor = GdColors.OnAccent)
                    ChoiceButton(if (s != null) "괜찮아요" else "닫기", selected = false, onClick = onDecline)
                }
            }
            is RestCareController.State.Heading -> {
                Text("${s?.name ?: "쉼터"}로 가는 중", style = MaterialTheme.typography.headlineSmall, color = GdColors.Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        distance?.let { "남은 거리 $it" },
                        when (s?.source) {
                            RestStopSuggestion.Source.ON_ROUTE -> "가는 길에 있어요"
                            else -> "화면으로만 안내"
                        },
                        "도착하면 알려 드릴게요",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.titleMedium,
                    color = GdColors.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ChoiceButton("안내 그만두기", selected = false, onClick = onCancel)
            }
            is RestCareController.State.Arrived -> {
                Text("${s?.name ?: "쉼터"}에 도착했어요", style = MaterialTheme.typography.headlineSmall, color = GdColors.Good, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("차를 세우고 충분히 쉬었다 가요 · 다시 출발하면 이어서 해요", style = MaterialTheme.typography.titleMedium, color = GdColors.Text, maxLines = 2)
            }
        }
    }
}

@Composable
private fun BottomBar(
    mode: GameMode,
    s: SessionState,
    reasons: Set<PauseReason>,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onNextRound: () -> Unit,
    onEnd: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = GdColors.Muted, modifier = Modifier.size(36.dp))
        Text(
            "화면 조작 잠금",
            style = MaterialTheme.typography.titleMedium,
            color = GdColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        // 문제를 내고 듣는 동안에는 "일시정지" 하나만. 멈춰 있거나 쉬는 동안에만 "게임 끝내기"를 함께 보여 준다.
        val userResumable = reasons.any {
            it == PauseReason.USER || it == PauseReason.BACKGROUND || it == PauseReason.AUDIO_UNAVAILABLE ||
                it == PauseReason.REST_STOP || it == PauseReason.REST_AREA
        }
        val idle = s.phase == SessionPhase.RESTING || s.phase == SessionPhase.WAITING || s.phase == SessionPhase.ALL_DONE
        if (mode == GameMode.PAUSED || idle) {
            ChoiceButton("게임 끝내기", selected = false, onClick = onEnd)
        }
        when {
            mode == GameMode.PAUSED -> PrimaryButton("계속", onClick = onResume, enabled = userResumable, icon = R.drawable.ic_play, color = GdColors.Accent, contentColor = GdColors.OnAccent)
            s.phase == SessionPhase.RESTING || s.phase == SessionPhase.WAITING ->
                PrimaryButton("지금 시작", onClick = onNextRound, icon = R.drawable.ic_play)
            s.phase == SessionPhase.ALL_DONE -> PrimaryButton("한 판 더", onClick = onNextRound, icon = R.drawable.ic_play)
            else -> PrimaryButton("일시정지", onClick = onPause, icon = R.drawable.ic_pause)
        }
    }
}

@Composable
private fun ScorePanel(container: AppContainer, s: SessionState, modifier: Modifier) {
    val session by container.coordinator.session.collectAsStateWithLifecycle()
    val players = session?.players.orEmpty()
    Column(
        modifier.background(GdColors.Surface, RoundedCornerShape(24.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("가족 점수", style = MaterialTheme.typography.titleLarge, color = GdColors.Muted)
        players.sortedByDescending { s.scores[it.id] ?: 0 }.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name + if (p.isDriver) " (운전)" else "", style = MaterialTheme.typography.titleMedium, color = GdColors.Text, modifier = Modifier.weight(1f))
                Text("${s.scores[p.id] ?: 0}점", style = MaterialTheme.typography.titleMedium, color = GdColors.Accent)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("판 진행", style = MaterialTheme.typography.titleSmall, color = GdColors.Muted)
        Text(
            if (s.turnsInRound > 0) "${s.turnInRound} / ${s.turnsInRound} 문제" else "-",
            style = MaterialTheme.typography.titleMedium,
            color = GdColors.Text,
        )
        if (s.difficultyCap < 3) Text("쉬운 문제 위주로 진행 중", style = MaterialTheme.typography.bodyMedium, color = GdColors.Accent)
    }
}
