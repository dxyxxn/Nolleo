package com.nolleo.app.ui.voice

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.R
import com.nolleo.app.app.AppContainer
import com.nolleo.app.app.RestCareController
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.domain.engine.MicState
import com.nolleo.app.domain.engine.SessionPhase
import com.nolleo.app.domain.engine.SessionPlanner
import com.nolleo.app.domain.engine.SessionState
import com.nolleo.app.domain.model.GameMode
import com.nolleo.app.domain.model.PauseReason
import com.nolleo.app.domain.model.TripSourceKind
import com.nolleo.app.domain.model.UxState
import com.nolleo.app.domain.rest.RestStopSuggestion
import com.nolleo.app.ui.common.Avatar
import com.nolleo.app.ui.common.BandCard
import com.nolleo.app.ui.common.Chip
import com.nolleo.app.ui.common.Hairline
import com.nolleo.app.ui.common.NlCard
import com.nolleo.app.ui.common.PrimaryButton
import com.nolleo.app.ui.common.SecondaryButton
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Radius
import com.nolleo.app.ui.theme.Sp

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
    val stage1Visible by coordinator.stage1CardVisible.collectAsStateWithLifecycle()
    val restState by coordinator.restCare.state.collectAsStateWithLifecycle()
    val restDistance by coordinator.restCare.distanceM.collectAsStateWithLifecycle()
    val region by container.locationRepository.regionCode.collectAsStateWithLifecycle()
    val content by container.content.content.collectAsStateWithLifecycle()
    val s = state ?: return
    val c = Nl.colors

    val remainText = trip?.remainTimeSec?.let { "도착까지 ${TripUnits.formatDuration(it)}" } ?: "도착 시간 확인 중"
    val regionName = content?.region(region)?.name
    val paused = mode == GameMode.PAUSED

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth > 1000.dp && maxWidth > maxHeight
        Row(Modifier.fillMaxSize().padding(horizontal = Sp.xxl, vertical = Sp.xl), horizontalArrangement = Arrangement.spacedBy(Sp.xxl)) {
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // 상단 상태 줄: 점(정적) + 모드 + 남은 시간
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                    Box(Modifier.size(14.dp).background(if (paused) c.faint else c.success, CircleShape))
                    Text(
                        buildAnnotatedString {
                            append(if (paused) "일시정지" else "음성 모드")
                            withStyle(SpanStyle(color = c.muted)) { append(" · $remainText") }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        color = c.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(Sp.m))
                Row(horizontalArrangement = Arrangement.spacedBy(Sp.s)) {
                    if (s.plan.roundCount > 0 && s.roundGame != null) Chip("${s.roundIndex + 1}/${s.plan.roundCount}판 · ${s.roundGame.label}")
                    regionName?.let { Chip(it) }
                    if (tripKind == TripSourceKind.SIMULATED) Chip("시뮬레이션 여정")
                }
                Spacer(Modifier.height(Sp.xl))

                // 문제·차례·마이크 상태를 흰 카드 하나에 모아 한눈에 읽히게 한다
                Box(
                    Modifier.weight(1f).fillMaxWidth().background(c.card, Radius.card).padding(horizontal = Sp.xxl, vertical = Sp.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        paused -> PausedBlock(reasons)
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
                    if (stage1Visible) Stage1Overlay()
                }
                Spacer(Modifier.height(Sp.l))
                Hairline()
                Spacer(Modifier.height(Sp.l))
                BottomBar(
                    mode = mode,
                    s = s,
                    reasons = reasons,
                    onPause = coordinator::pause,
                    onResume = coordinator::resume,
                    onNextRound = coordinator::startNextRoundNow,
                    onEnd = coordinator::endSession,
                    // 쉬어 가기를 묻는 동안은 안내 카드의 버튼이 주 버튼 — 아래 버튼은 테두리로 낮춘다
                    quiet = rest is RestCareController.State.Offered,
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
    val c = Nl.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
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
                    color = c.ink,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "판 사이에는 말을 걸지 않아요 · 바로 하려면 '지금 시작'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SessionPhase.ALL_DONE -> {
                BigText("오늘 준비한 판을 모두 했어요")
                Text("도착하면 결과를 정리해 드릴게요.", style = MaterialTheme.typography.titleLarge, color = c.muted)
            }
            SessionPhase.FINISHING, SessionPhase.FINISHED -> BigText("오늘의 결과를 정리하고 있어요")
            else -> {
                s.currentPlayer?.let { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                        Avatar(p.name, strong = true)
                        Text("${p.name} 차례", style = MaterialTheme.typography.titleLarge, color = c.ink)
                    }
                }
                // 규제가 LIMIT_STRING_LENGTH 면 1줄 요약, 아니면 ≤ 3줄. 화면 글이 없으면 발화 문장을 화면 문구로(느낌표 없이)
                val text = if (ux.limitStringLength) limitedText(s) else (s.display ?: s.caption?.let(::screenSentence))
                Text(
                    text ?: "",
                    style = MaterialTheme.typography.headlineLarge,
                    color = c.ink,
                    textAlign = TextAlign.Center,
                    maxLines = if (ux.limitStringLength) 1 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                MicIndicator(s.mic)
                val line = s.lastAnswer
                if (line != null && !ux.noVoiceTranscription) {
                    Text(
                        "${line.playerName}: ${line.heard ?: "(응답 없음)"} → ${line.verdict}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * 글자 수 제한(LIMIT_STRING_LENGTH) 때의 한 줄: 문제를 내고 듣는 동안에는 그 차례의 짧은 글,
 * 그 밖(판 시작·결과·안내·마무리)에는 단계 이름 — 앞 차례의 글이 남거나 가운데가 비지 않게
 */
private fun limitedText(s: SessionState): String = when (s.phase) {
    SessionPhase.ASKING, SessionPhase.LISTENING -> s.shortDisplay ?: s.phase.label
    else -> s.phase.label
}

/** 발화 문장을 화면 문구로: UI 문구에는 느낌표를 쓰지 않는다 (발화는 그대로) */
private fun screenSentence(spoken: String): String = spoken.replace("! ", ". ").replace("!", "")

@Composable
private fun BigText(text: String) {
    Text(text, style = MaterialTheme.typography.headlineLarge, color = Nl.colors.ink, textAlign = TextAlign.Center, maxLines = 3)
}

/** 정적 상태 3종 (대기 / 듣는 중 / 말하는 중) — 애니메이션 없음 */
@Composable
private fun MicIndicator(mic: MicState) {
    val c = Nl.colors
    val (icon, label, tint) = when (mic) {
        MicState.LISTENING -> Triple(R.drawable.ic_mic, "듣는 중", c.success)
        MicState.SPEAKING -> Triple(R.drawable.ic_speaker, "말하는 중", c.ink)
        MicState.IDLE -> Triple(R.drawable.ic_hourglass, "대기", c.muted)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Sp.m),
        modifier = Modifier
            .background(c.sunk, Radius.pill)
            .padding(start = Sp.xl, end = Sp.xxl, top = Sp.m, bottom = Sp.m),
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = tint, modifier = Modifier.size(40.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = c.ink)
    }
}

@Composable
private fun PausedBlock(reasons: Set<PauseReason>) {
    val c = Nl.colors
    val main = reasons.firstOrNull { it == PauseReason.NAV_GUIDANCE } ?: reasons.firstOrNull { it == PauseReason.AUDIO_FOCUS } ?: reasons.firstOrNull()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
        Box(Modifier.size(112.dp).background(c.sunk, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_pause), contentDescription = null, tint = c.ink, modifier = Modifier.size(64.dp))
        }
        Text(main?.title ?: "잠시 멈췄어요", style = MaterialTheme.typography.headlineLarge, color = c.ink, textAlign = TextAlign.Center)
        Text(main?.detail ?: "", style = MaterialTheme.typography.titleLarge, color = c.muted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Stage1Overlay() {
    val c = Nl.colors
    BandCard(Modifier.fillMaxWidth()) {
        Text("잠깐 쉬어 갈까요?", style = MaterialTheme.typography.titleLarge, color = c.onBand)
        Text("반응이 조금 느려졌어요 · 다음 문제는 쉬운 걸로 갈게요", style = MaterialTheme.typography.bodyLarge, color = c.bandMuted, maxLines = 2)
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
    val c = Nl.colors
    val s = state.suggestion
    val target = s?.let { listOfNotNull(it.name, it.directionLabel).joinToString(" · ") }
    val distance = (distanceM ?: s?.distanceM)?.let(TripUnits::formatDistance)
    BandCard(Modifier.fillMaxWidth()) {
        when (state) {
            is RestCareController.State.Offered -> {
                Text("잠깐 쉬어 가요", style = MaterialTheme.typography.titleLarge, color = c.onBand)
                Text(
                    // 좁은 창에서 줄이 잘려도 거리는 보이게 앞에 둔다
                    when {
                        s == null -> "반응이 계속 느려졌어요 · 가까운 졸음쉼터나 휴게소에서 쉬어 가요"
                        distance != null -> "반응이 계속 느려졌어요 · $distance 앞 $target"
                        else -> "반응이 계속 느려졌어요 · $target"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.onBand,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                s?.facilityLine?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = c.bandMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.padding(top = Sp.s), horizontalArrangement = Arrangement.spacedBy(Sp.l)) {
                    if (s != null) PrimaryButton("쉬어 가기", onClick = onAccept, onBand = true)
                    SecondaryButton(if (s != null) "괜찮아요" else "닫기", onClick = onDecline, onBand = true)
                }
            }
            is RestCareController.State.Heading -> {
                Text("${s?.name ?: "쉼터"}로 가는 중", style = MaterialTheme.typography.titleLarge, color = c.onBand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(
                        distance?.let { "남은 거리 $it" },
                        when (s?.source) {
                            RestStopSuggestion.Source.ON_ROUTE -> "가는 길에 있어요"
                            else -> "화면으로만 안내"
                        },
                        "도착하면 알려 드릴게요",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.onBand,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                SecondaryButton("안내 그만두기", onClick = onCancel, onBand = true, modifier = Modifier.padding(top = Sp.s))
            }
            is RestCareController.State.Arrived -> {
                Text("${s?.name ?: "쉼터"}에 도착했어요", style = MaterialTheme.typography.titleLarge, color = c.onBand, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("차를 세우고 충분히 쉬었다 가요 · 다시 출발하면 이어서 해요", style = MaterialTheme.typography.bodyLarge, color = c.bandMuted, maxLines = 2)
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
    quiet: Boolean = false,
) {
    val c = Nl.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.l)) {
        Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = c.muted, modifier = Modifier.size(32.dp))
        Text(
            "화면 조작 잠금",
            style = MaterialTheme.typography.bodyLarge,
            color = c.muted,
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
            SecondaryButton("게임 끝내기", onClick = onEnd)
        }
        when {
            mode == GameMode.PAUSED -> MainButton(quiet, "계속", onResume, R.drawable.ic_play, enabled = userResumable)
            s.phase == SessionPhase.RESTING || s.phase == SessionPhase.WAITING -> MainButton(quiet, "지금 시작", onNextRound, R.drawable.ic_play)
            s.phase == SessionPhase.ALL_DONE -> MainButton(quiet, "한 판 더", onNextRound, R.drawable.ic_play)
            else -> MainButton(quiet, "일시정지", onPause, R.drawable.ic_pause)
        }
    }
}

/** 아래 줄의 주 동작. [quiet] = 쉬어 가기 권유 카드의 버튼이 화면의 주 버튼이라 테두리로 낮춘다 (채운 버튼은 화면에 하나) */
@Composable
private fun MainButton(quiet: Boolean, text: String, onClick: () -> Unit, icon: Int, enabled: Boolean = true) {
    if (quiet) SecondaryButton(text, onClick = onClick, enabled = enabled, icon = icon)
    else PrimaryButton(text, onClick = onClick, enabled = enabled, icon = icon)
}

@Composable
private fun ScorePanel(container: AppContainer, s: SessionState, modifier: Modifier) {
    val session by container.coordinator.session.collectAsStateWithLifecycle()
    val players = session?.players.orEmpty()
    val c = Nl.colors
    NlCard(modifier, spacing = Sp.l) {
        Text("가족 점수", style = MaterialTheme.typography.titleMedium, color = c.ink)
        players.sortedByDescending { s.scores[it.id] ?: 0 }.forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Avatar(p.name, strong = s.currentPlayer?.id == p.id)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = MaterialTheme.typography.titleSmall, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (p.isDriver) Text("운전", style = MaterialTheme.typography.labelSmall, color = c.muted)
                }
                Text("${s.scores[p.id] ?: 0}점", style = MaterialTheme.typography.titleMedium, color = c.ink)
            }
        }
        Hairline()
        Text("판 진행", style = MaterialTheme.typography.labelSmall, color = c.muted)
        Text(
            if (s.turnsInRound > 0) "${s.turnInRound} / ${s.turnsInRound} 문제" else "-",
            style = MaterialTheme.typography.titleMedium,
            color = c.ink,
        )
        if (s.difficultyCap < 3) Text("쉬운 문제 위주로 진행 중", style = MaterialTheme.typography.bodyMedium, color = c.muted)
    }
}
