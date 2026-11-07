package com.gildongmu.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.gildongmu.app.app.IdiomQuizController
import com.gildongmu.app.domain.games.IdiomQuizGame
import com.gildongmu.app.domain.model.IdiomChar
import com.gildongmu.app.domain.model.IdiomItem
import com.gildongmu.app.ui.common.ChoiceButton
import com.gildongmu.app.ui.common.MinTouch
import com.gildongmu.app.ui.common.PrimaryButton
import com.gildongmu.app.ui.theme.GdColors

/**
 * 정차 중 화면 게임 "사자성어": 뜻(문제)을 읽어 주고 네 개 중에서 고른다.
 * 고르면 정답 사자성어의 한자 한 글자씩 훈·음을 보여 주고, 정답과 유래를 읽어 준 뒤 다음 문제로 넘어간다.
 * 가로로 넓은 창은 왼쪽 문제·보기 / 오른쪽 풀이, 좁은 창은 위아래로 놓는다.
 */
@Composable
fun IdiomQuizPanel(quiz: IdiomQuizController, onClose: () -> Unit) {
    val state by quiz.state.collectAsStateWithLifecycle()
    // 화면이 복원됐는데 문제가 없으면(프로세스 재생성 등) 소리 없이 문제만 준비한다
    LaunchedEffect(Unit) { quiz.prepare() }
    val q = state.question ?: return

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Header(state, onReplay = quiz::replay, onClose = onClose)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val wide = maxWidth > maxHeight && maxWidth >= 1000.dp
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) { QuestionAndChoices(q, state, quiz::choose) }
                    Column(
                        Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (state.revealed) {
                            Reveal(q, state, quiz::next)
                        } else {
                            Text(
                                "보기를 고르면 정답과 한자 풀이가 여기에 나와요",
                                style = MaterialTheme.typography.titleMedium,
                                color = GdColors.Muted,
                            )
                        }
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    QuestionAndChoices(q, state, quiz::choose)
                    if (state.revealed) Reveal(q, state, quiz::next)
                }
            }
        }
    }
}

@Composable
private fun Header(state: IdiomQuizController.UiState, onReplay: () -> Unit, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) {
            Text("사자성어 · ${state.score}점", style = MaterialTheme.typography.titleLarge, color = GdColors.Accent)
            val status = when {
                state.speaking -> "읽어 주는 중이에요"
                state.advancing -> "곧 다음 문제로 넘어가요"
                state.voiceStopped -> "소리가 멈췄어요 · 다시 듣기나 다음 문제를 눌러 주세요"
                state.answered > 0 -> "${state.answered}문제 중 ${state.correct}개 맞혔어요"
                else -> "뜻을 듣고 알맞은 사자성어를 골라요"
            }
            Text(
                status,
                style = MaterialTheme.typography.titleSmall,
                color = if (state.voiceStopped) GdColors.Warn else GdColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ChoiceButton("다시 듣기", selected = false, onClick = onReplay)
        ChoiceButton("닫기", selected = false, onClick = onClose)
    }
}

@Composable
private fun ColumnScope.QuestionAndChoices(
    q: IdiomQuizGame.IdiomQuestion,
    state: IdiomQuizController.UiState,
    onChoose: (Int) -> Unit,
) {
    Text(IdiomItem.QUESTION_LEAD, style = MaterialTheme.typography.titleMedium, color = GdColors.Muted)
    Text(q.item.meaning, style = MaterialTheme.typography.headlineSmall, color = GdColors.Text)
    q.choices.chunked(2).forEachIndexed { rowIndex, row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            row.forEachIndexed { colIndex, choice ->
                val index = rowIndex * 2 + colIndex
                val (container, content) = when {
                    !state.revealed -> GdColors.Primary to GdColors.OnPrimary
                    index == q.answerIndex -> GdColors.Good to GdColors.OnPrimary
                    index == state.picked -> GdColors.Danger to GdColors.OnPrimary
                    else -> GdColors.SurfaceHigh to GdColors.Muted
                }
                ChoiceTile(
                    text = choice.word,
                    container = container,
                    content = content,
                    onClick = { onChoose(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChoiceTile(text: String, container: Color, content: Color, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouch + 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text, style = MaterialTheme.typography.headlineMedium, maxLines = 1)
    }
}

@Composable
private fun Reveal(q: IdiomQuizGame.IdiomQuestion, state: IdiomQuizController.UiState, onNext: () -> Unit) {
    val item = q.item
    Text(
        if (state.pickedCorrect) "정답! +${IdiomQuizGame.POINTS}점" else "아쉬워요. 정답은 ${item.word}",
        style = MaterialTheme.typography.headlineSmall,
        color = if (state.pickedCorrect) GdColors.Good else GdColors.Warn,
    )
    if (item.chars.isNotEmpty()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item.chars.forEach { CharCell(it, Modifier.weight(1f)) }
        }
    } else if (item.hanja.isNotBlank()) {
        Text(item.hanja, style = MaterialTheme.typography.displayMedium, color = GdColors.Text)
    }
    item.origin?.let { origin ->
        Text("유래", style = MaterialTheme.typography.titleSmall, color = GdColors.Accent)
        Text(origin, style = MaterialTheme.typography.bodyLarge, color = GdColors.Text)
    }
    PrimaryButton("다음 문제", onClick = onNext)
}

/** 한자 한 글자와 그 아래 훈·음 (예: 家 / 집 가) */
@Composable
private fun CharCell(ch: IdiomChar, modifier: Modifier) {
    Column(
        modifier
            .background(GdColors.SurfaceHigh, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(ch.hanja, style = MaterialTheme.typography.displayMedium, color = GdColors.Text)
        Text(
            "${ch.hun} ${ch.eum}".trim(),
            style = MaterialTheme.typography.titleMedium,
            color = GdColors.Muted,
            textAlign = TextAlign.Center,
        )
    }
}
