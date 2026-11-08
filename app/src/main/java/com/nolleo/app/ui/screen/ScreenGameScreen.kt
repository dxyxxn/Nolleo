package com.nolleo.app.ui.screen

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.app.AppContainer
import com.nolleo.app.app.RestCareController
import com.nolleo.app.app.ScreenGameKind
import com.nolleo.app.app.ScreenQuizController
import com.nolleo.app.app.ScreenWordChainController
import com.nolleo.app.core.navi.TripUnits
import com.nolleo.app.domain.engine.SessionPlanner
import com.nolleo.app.ui.common.Chip
import com.nolleo.app.ui.common.NlCard
import com.nolleo.app.ui.common.isWideLayout
import com.nolleo.app.ui.common.SecondaryButton
import com.nolleo.app.ui.common.pressable
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Radius
import com.nolleo.app.ui.theme.Sp

/**
 * SCREEN 모드: 정차 중 화면 게임 목록 — 열 수 있는 게임 3개(사자성어 · 지역 퀴즈 · 끝말잇기)와 준비 중 2개(틀린 그림 찾기 · 노래방).
 * 사자성어·지역 퀴즈는 보기를 누르는 게임, 끝말잇기는 말로 잇는 게임이다. 모두 주행 중 음성 판과 따로 논다
 * (점수·진행이 이어지지 않음). 하단 "정차 중에만 열려요".
 */
@Composable
fun ScreenGameScreen(
    container: AppContainer,
    openGame: ScreenGameKind?,
    onOpenGame: (ScreenGameKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trip by container.tripRepository.snapshot.collectAsStateWithLifecycle()
    val sessionState by container.coordinator.sessionState.collectAsStateWithLifecycle()
    val session by container.coordinator.session.collectAsStateWithLifecycle()
    val c = Nl.colors

    Column(modifier.fillMaxSize().padding(horizontal = Sp.xxl, vertical = Sp.xl), verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
        // 게임 목록에서만 보이는 머리 줄. 게임을 열면 그 화면의 머리 줄(← 목록 · 제목 · 다시 듣기) 하나만 남겨
        // 위쪽에 버튼이 두 줄로 흩어지지 않게 한다.
        if (openGame == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Box(Modifier.size(14.dp).background(c.indicator, CircleShape))
                Text(
                    buildAnnotatedString {
                        append("정차 중")
                        withStyle(SpanStyle(color = c.muted)) { append(" · 화면 게임") }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = c.ink,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                trip?.remainTimeSec?.let {
                    Chip(TripUnits.remainLabel(it, session?.noDestination == true))
                }
                // 정차 중에는 언제든 게임을 끝내고 홈으로 갈 수 있다 (점수가 있으면 요약 화면을 거친다)
                SecondaryButton("게임 끝내기", onClick = container.coordinator::endSession)
            }
        }
        // 졸음 케어 2단계: 쉼터에 세워 둔 동안 한 줄 안내 (다시 출발하면 "잘 쉬셨나요?")
        val rest by container.coordinator.restCare.state.collectAsStateWithLifecycle()
        when (val r = rest) {
            is RestCareController.State.Arrived ->
                Text("${r.suggestion.name}에서 쉬는 중 · 충분히 쉬었다 가요", style = MaterialTheme.typography.bodyLarge, color = c.ink)
            is RestCareController.State.Heading ->
                Text("${r.suggestion.name}로 가는 중", style = MaterialTheme.typography.bodyLarge, color = c.ink)
            else -> Unit
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (openGame != null) {
                val game = container.coordinator.screenGame(openGame)
                val close = {
                    game.close()
                    onOpenGame(null)
                }
                BackHandler(onBack = close)
                when {
                    !game.isAvailable ->
                        Text("${openGame.title} 콘텐츠를 불러오지 못했어요.", style = MaterialTheme.typography.titleLarge, color = c.ink)
                    game is ScreenWordChainController -> WordChainPanel(game, container.clock::elapsedMs, onClose = close)
                    game is ScreenQuizController -> ScreenQuizPanel(game, onClose = close)
                }
            } else {
                GameCards(container, onOpen = { kind ->
                    onOpenGame(kind)
                    container.coordinator.screenGame(kind).open()
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
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 게임 카드 묶음: 넓은 창은 위에 열 수 있는 3개, 아래에 준비 중 2개(낮게). 좁은 창은 위아래로 쌓고 스크롤 */
@Composable
private fun GameCards(container: AppContainer, onOpen: (ScreenGameKind) -> Unit) {
    val region by container.locationRepository.regionCode.collectAsStateWithLifecycle()
    val content by container.content.content.collectAsStateWithLifecycle()
    val regionName = content?.region(region)?.name
    val idiom: @Composable (Modifier) -> Unit = { m ->
        GameCard("사자성어", "뜻을 듣고 넷 중에 골라요 · 한자 풀이와 유래까지", { onOpen(ScreenGameKind.IDIOM) }, m) { TileRow(listOf("溫", "故", "知", "新"), "온고지신") }
    }
    val regionQuiz: @Composable (Modifier) -> Unit = { m ->
        GameCard("지역 퀴즈", "지금 지나는 곳 문제를 보기에서 골라요", { onOpen(ScreenGameKind.REGION) }, m) { RegionVisual(regionName) }
    }
    val word: @Composable (Modifier) -> Unit = { m ->
        GameCard("끝말잇기", "놀레오와 말로 이어 가요 · 두음법칙까지", { onOpen(ScreenGameKind.WORD) }, m) { ChainVisual() }
    }
    val soon: @Composable (Modifier) -> Unit = { m ->
        Row(m, horizontalArrangement = Arrangement.spacedBy(Sp.xl)) {
            SoonCard("틀린 그림 찾기", "?", Modifier.weight(1f).fillMaxHeight())
            SoonCard("노래방", "♪", Modifier.weight(1f).fillMaxHeight())
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (isWideLayout()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
                Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.xl)) {
                    idiom(Modifier.weight(1f).fillMaxHeight())
                    regionQuiz(Modifier.weight(1f).fillMaxHeight())
                    word(Modifier.weight(1f).fillMaxHeight())
                }
                soon(Modifier.fillMaxWidth().height(120.dp))
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
                idiom(Modifier.fillMaxWidth().height(420.dp))
                regionQuiz(Modifier.fillMaxWidth().height(420.dp))
                word(Modifier.fillMaxWidth().height(420.dp))
                soon(Modifier.fillMaxWidth().height(120.dp))
            }
        }
    }
}

/**
 * 열 수 있는 게임 카드 — 위쪽은 게임에서 실제로 보게 될 것, 아래쪽은 이름·설명과 "시작하기"(카드 전체도 눌림).
 * 세 게임이 대등하므로 "시작하기"는 테두리 버튼(화면마다 채운 버튼은 하나 원칙).
 */
@Composable
private fun GameCard(title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier, visual: @Composable () -> Unit) {
    val c = Nl.colors
    NlCard(modifier.pressable(onClick, Radius.card), padding = Sp.xl, spacing = Sp.m) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { visual() }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.ink)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        SecondaryButton("시작하기", onClick = onClick, modifier = Modifier.fillMaxWidth())
    }
}

/** 준비 중 카드 — 낮게, 흐리게(비활성 글자색), 눌리지 않음 */
@Composable
private fun SoonCard(title: String, mark: String, modifier: Modifier) {
    val c = Nl.colors
    NlCard(modifier, padding = Sp.xl) {
        Row(
            Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Sp.l),
        ) {
            Box(Modifier.size(64.dp).border(1.dp, c.line, Radius.tile), contentAlignment = Alignment.Center) {
                Text(mark, style = MaterialTheme.typography.headlineMedium, color = c.faint)
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = c.faint)
                Text("준비 중이에요", style = MaterialTheme.typography.bodyMedium, color = c.faint)
            }
        }
    }
}

@Composable
private fun TileRow(chars: List<String>, reading: String) {
    val c = Nl.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.s)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Sp.s)) {
            chars.forEach { ch ->
                Box(Modifier.size(72.dp).background(c.sunk, Radius.tile), contentAlignment = Alignment.Center) {
                    Text(ch, style = MaterialTheme.typography.headlineLarge, color = c.ink)
                }
            }
        }
        Text(reading, style = MaterialTheme.typography.bodyLarge, color = c.muted)
    }
}

/** 지역 퀴즈 카드 그림: 지금 지나는 시/도 이름 (모르면 "우리나라") */
@Composable
private fun RegionVisual(regionName: String?) {
    val c = Nl.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Sp.s)) {
        Text(if (regionName != null) "지금 지나는 곳" else "위치를 모르면 전국에서", style = MaterialTheme.typography.bodyLarge, color = c.muted)
        Box(Modifier.background(c.sunk, Radius.tile).padding(horizontal = Sp.xl, vertical = Sp.m), contentAlignment = Alignment.Center) {
            Text(regionName ?: "우리나라", style = MaterialTheme.typography.headlineLarge, color = c.ink, textAlign = TextAlign.Center)
        }
        Text("음식 · 지명 · 문화 · 특산물", style = MaterialTheme.typography.bodyMedium, color = c.muted)
    }
}

/** 끝말잇기 카드 그림: 낱말이 이어지는 모양 */
@Composable
private fun ChainVisual() {
    val c = Nl.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.s)) {
        listOf("기차", "차표", "표범").forEachIndexed { i, w ->
            if (i > 0) Text("→", style = MaterialTheme.typography.titleLarge, color = c.muted)
            Box(Modifier.background(c.sunk, Radius.tile).padding(horizontal = Sp.m, vertical = Sp.m), contentAlignment = Alignment.Center) {
                Text(w, style = MaterialTheme.typography.headlineSmall, color = c.ink, maxLines = 1)
            }
        }
        Spacer(Modifier.size(0.dp))
    }
}
