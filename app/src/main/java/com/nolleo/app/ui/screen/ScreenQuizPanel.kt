package com.nolleo.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nolleo.app.R
import com.nolleo.app.app.ScreenQuizController
import com.nolleo.app.domain.games.ScreenGame
import com.nolleo.app.domain.games.ScreenQuestion
import com.nolleo.app.domain.model.IdiomChar
import com.nolleo.app.ui.common.Chip
import com.nolleo.app.ui.common.MinTouch
import com.nolleo.app.ui.common.NlCard
import com.nolleo.app.ui.common.PrimaryButton
import com.nolleo.app.ui.common.SecondaryButton
import com.nolleo.app.ui.common.pressable
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Radius
import com.nolleo.app.ui.theme.Sp

/**
 * 정차 중 화면 게임 한 판(사자성어·지역 퀴즈·끝말잇기)을 그린다. 창 크기와 상관없이 가운데 맞춘 위아래 배치 —
 * 머리 줄(← 목록 · 제목·점수·상태 · 다시 듣기) → 문제 → 보기(2열) → 고르면 풀이 카드(가운데 "다음 문제").
 * 11/8 사용자 요청: 넓은 창도 좌우 나눔 대신 가운데 위아래로, 좁은 창은 "다음 문제" 버튼을 가운데로.
 */
@Composable
fun ScreenQuizPanel(quiz: ScreenQuizController, onClose: () -> Unit) {
    val state by quiz.state.collectAsStateWithLifecycle()
    // 화면이 복원됐는데 문제가 없으면(프로세스 재생성 등) 소리 없이 문제만 준비한다
    LaunchedEffect(quiz) { quiz.prepare() }
    val q = state.question ?: return

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(Sp.xl)) {
        Header(quiz.kind.title, state, onReplay = quiz::replay, onClose = onClose)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 1100.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Sp.l),
            ) {
                QuestionAndChoices(q, state, quiz::choose)
                if (state.revealed) {
                    NlCard(Modifier.fillMaxWidth(), spacing = Sp.l, horizontalAlignment = Alignment.CenterHorizontally) {
                        Reveal(q, state, quiz::next)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String, state: ScreenQuizController.UiState, onReplay: () -> Unit, onClose: () -> Unit) {
    val c = Nl.colors
    // 머리 줄 하나: 왼쪽 ← 목록(닫기), 가운데 제목·점수·상태, 오른쪽 다시 듣기
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.l)) {
        SecondaryButton("목록", onClick = onClose, icon = R.drawable.ic_back)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.ink)
                Chip("${state.score}점")
            }
            val status = when {
                state.speaking -> "읽어 주는 중이에요"
                state.advancing -> "곧 다음 문제로 넘어가요"
                state.voiceStopped -> "소리가 멈췄어요 · 다시 듣기나 다음 문제를 눌러 주세요"
                state.answered > 0 -> listOfNotNull("${state.answered}문제 중 ${state.correct}개 맞혔어요", state.progress).joinToString(" · ")
                else -> "듣고 알맞은 답을 골라요"
            }
            Text(
                status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.voiceStopped) c.ink else c.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SecondaryButton("다시 듣기", onClick = onReplay, icon = R.drawable.ic_speaker)
    }
}

@Composable
private fun ColumnScope.QuestionAndChoices(q: ScreenQuestion, state: ScreenQuizController.UiState, onChoose: (Int) -> Unit) {
    val c = Nl.colors
    Text(q.lead, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center)
    Text(q.prompt, style = MaterialTheme.typography.headlineSmall, color = c.ink, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = Sp.s))
    q.choices.chunked(2).forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.l)) {
            row.forEachIndexed { colIndex, choice ->
                val index = rowIndex * 2 + colIndex
                // 고르기 전에는 모두 같은 보기, 고른 뒤에는 정답만 초록·고른 오답만 빨강, 나머지는 흐리게
                val look = when {
                    !state.revealed -> TileLook(c.card, c.ink, c.line)
                    index == q.answerIndex -> TileLook(c.success, c.onSuccess, c.success)
                    index == state.picked -> TileLook(c.danger, c.onDanger, c.danger)
                    else -> TileLook(c.ground, c.faint, c.line)
                }
                ChoiceTile(choice, look, onClick = { onChoose(index) }, enabled = !state.revealed, modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class TileLook(val container: Color, val content: Color, val border: Color)

@Composable
private fun ChoiceTile(text: String, look: TileLook, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    Box(
        modifier
            .heightIn(min = MinTouch + 16.dp)
            .background(look.container, Radius.tile)
            .border(1.dp, look.border, Radius.tile)
            .pressable(onClick, Radius.tile, enabled)
            .padding(horizontal = Sp.l, vertical = Sp.m),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium, color = look.content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 고른 뒤 풀이 카드 — 가운데 맞춤: 결과 → (끝말잇기 이어진 말 / 사자성어 한자 풀이·유래 / 지역 퀴즈 힌트) → 다음 문제 */
@Composable
private fun ColumnScope.Reveal(q: ScreenQuestion, state: ScreenQuizController.UiState, onNext: () -> Unit) {
    val c = Nl.colors
    Text(
        if (state.pickedCorrect) "정답이에요 · +${ScreenGame.POINTS}점" else "아쉬워요 · 정답은 ${q.answer}",
        style = MaterialTheme.typography.titleLarge,
        color = if (state.pickedCorrect) c.success else c.danger,
        textAlign = TextAlign.Center,
    )
    q.revealLine?.let { Text(it, style = MaterialTheme.typography.headlineMedium, color = c.ink, textAlign = TextAlign.Center) }
    val idiom = q.idiom
    if (idiom != null) {
        if (idiom.chars.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Sp.m)) {
                idiom.chars.forEach { CharCell(it, Modifier.weight(1f)) }
            }
        } else if (idiom.hanja.isNotBlank()) {
            Text(idiom.hanja, style = MaterialTheme.typography.displayMedium, color = c.ink)
        }
        idiom.origin?.let { origin ->
            Text("유래", style = MaterialTheme.typography.titleSmall, color = c.ink)
            Text(origin, style = MaterialTheme.typography.bodyLarge, color = c.ink, textAlign = TextAlign.Center)
        }
    }
    q.note?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = c.muted, textAlign = TextAlign.Center) }
    PrimaryButton("다음 문제", onClick = onNext, modifier = Modifier.padding(top = Sp.s))
}

/** 한자 한 글자와 그 아래 훈·음 (예: 家 / 집 가) */
@Composable
private fun CharCell(ch: IdiomChar, modifier: Modifier) {
    val c = Nl.colors
    Column(
        modifier
            .background(c.sunk, Radius.tile)
            .padding(horizontal = Sp.l, vertical = Sp.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(ch.hanja, style = MaterialTheme.typography.displayMedium, color = c.ink)
        Text(
            "${ch.hun} ${ch.eum}".trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = c.muted,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
