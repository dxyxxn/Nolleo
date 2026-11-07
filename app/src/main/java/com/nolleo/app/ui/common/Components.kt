package com.nolleo.app.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nolleo.app.R
import com.nolleo.app.ui.theme.Nl
import com.nolleo.app.ui.theme.Radius
import com.nolleo.app.ui.theme.Sp

/** 터치 타깃 ≥ 76dp (DEV_GUIDE §6) */
val MinTouch: Dp = 76.dp

/**
 * 누를 수 있는 면 — Material 물결(ripple) 대신, 누르는 동안만 살짝 어두워지고 떼면 바로 원래 색으로 돌아온다.
 * 물결은 퍼졌다 사라지는 데 시간이 걸려 "회색이 됐다가 천천히 돌아오는" 것처럼 보였다(11/8 사용자 피드백).
 * 주행 중 애니메이션을 쓰지 않는 규칙(DEV_GUIDE §6.1)과도 맞는다. [shape] 는 눌림 색을 칠할 모양.
 */
@Composable
fun Modifier.pressable(
    onClick: () -> Unit,
    shape: Shape,
    enabled: Boolean = true,
    role: Role = Role.Button,
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val overlay = Nl.colors.pressed
    return this
        .clip(shape)
        .drawWithContent {
            drawContent()
            if (pressed && enabled) drawRect(overlay)
        }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

/**
 * 주 버튼 — 화면마다 하나만 채운 버튼으로 둔다. 노란 알약에 검은 글, 높이 76dp 이상.
 * [onBand] 는 안내 카드(노랑) 위에 놓을 때 색을 뒤집는다.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    onBand: Boolean = false,
) {
    val c = Nl.colors
    val container = when {
        !enabled -> c.sunk
        onBand -> c.bandAction
        else -> c.action
    }
    val content = when {
        !enabled -> c.faint
        onBand -> c.onBandAction
        else -> c.onAction
    }
    ButtonBody(text, icon, content, modifier.background(container, Radius.pill).pressable(onClick, Radius.pill, enabled), horizontal = Sp.xxl)
}

/** 보조 버튼 — 테두리만 있는 알약. 끝내기·다시 듣기 같은 두 번째 행동 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    onBand: Boolean = false,
) {
    val c = Nl.colors
    val ink = if (onBand) c.onBand else c.ink
    ButtonBody(
        text,
        icon,
        if (enabled) ink else c.faint,
        modifier.border(1.5.dp, if (enabled) ink.copy(alpha = 0.35f) else c.line, Radius.pill).pressable(onClick, Radius.pill, enabled),
        horizontal = Sp.xl,
    )
}

@Composable
private fun ButtonBody(text: String, @DrawableRes icon: Int?, content: Color, modifier: Modifier, horizontal: Dp) {
    Row(
        modifier.heightIn(min = MinTouch).padding(horizontal = horizontal, vertical = Sp.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(Sp.m))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * 선택 막대 — 몇 개 중 하나를 고르는 것을 한 줄로 묶는다(흩어진 버튼 대신).
 * 고른 칸은 흰 칸이 떠 보이고 굵은 글, 나머지는 회색 바탕에 보조 글.
 */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Nl.colors
    Row(
        modifier.fillMaxWidth().background(c.sunk, Radius.tile).padding(Sp.xs),
        horizontalArrangement = Arrangement.spacedBy(Sp.xs),
    ) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = MinTouch - Sp.s)
                    .then(if (on) Modifier.shadow(2.dp, Radius.chip) else Modifier)
                    .background(if (on) c.segmentOn else Color.Transparent, Radius.chip)
                    .pressable({ onSelect(option) }, Radius.chip, enabled, Role.RadioButton)
                    .padding(horizontal = Sp.s),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = if (on) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (on) c.ink else c.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** 설정 한 줄: 왼쪽에 짧은 이름, 오른쪽에 선택 막대. 이름 칸 폭을 맞춰 줄이 가지런히 보이게 한다 */
@Composable
fun SettingRow(label: String, modifier: Modifier = Modifier, labelWidth: Dp = 88.dp, content: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Nl.colors.muted, modifier = Modifier.width(labelWidth), maxLines = 1)
        Box(Modifier.weight(1f)) { content() }
    }
}

/** 목록의 고르는 줄 (목적지): 라디오 점 + 제목·설명 + 오른쪽 값. 고른 줄은 옅은 노랑 바탕 */
@Composable
fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    enabled: Boolean = true,
) {
    val c = Nl.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouch + Sp.s)
            .background(if (selected) c.selectedFill else Color.Transparent, Radius.tile)
            .pressable(onClick, Radius.tile, enabled, Role.RadioButton)
            .padding(horizontal = Sp.l, vertical = Sp.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Sp.l),
    ) {
        Box(
            Modifier.size(28.dp).border(if (selected) 9.dp else 2.dp, if (selected) c.indicator else c.faint, CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodyMedium, color = if (selected) c.ink else c.muted, maxLines = 1)
        }
    }
}

/** 작은 알약 꼬리표 (판 번호·지역·점수) */
@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    val c = Nl.colors
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = c.muted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.background(c.sunk, Radius.pill).padding(horizontal = Sp.l, vertical = 6.dp),
    )
}

/** 카드 — 바탕 위의 흰 면 한 장. 그림자 없이, 안쪽 여백 24dp. 내용을 묶어 한눈에 읽히게 하는 단위 */
@Composable
fun NlCard(
    modifier: Modifier = Modifier,
    padding: Dp = Sp.xl,
    spacing: Dp = Sp.m,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.background(Nl.colors.card, Radius.card).padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** 안내 카드 — 졸음 케어처럼 꼭 봐야 하는 것만. 낮에는 노란 면, 밤에는 어두운 호박색 + 노란 테두리 */
@Composable
fun BandCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Nl.colors
    Column(
        modifier.background(c.band, Radius.card).border(2.dp, c.bandBorder, Radius.card).padding(Sp.xl),
        verticalArrangement = Arrangement.spacedBy(Sp.s),
        content = content,
    )
}

/** 가족 아바타 — 이름 첫 글자를 동그라미에 (아빠 → 아). [strong] 은 지금 차례인 사람 */
@Composable
fun Avatar(name: String, modifier: Modifier = Modifier, size: Dp = 44.dp, strong: Boolean = false) {
    val c = Nl.colors
    Box(
        modifier.size(size).background(if (strong) c.strong else c.sunk, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1),
            style = MaterialTheme.typography.titleSmall,
            color = if (strong) c.onStrong else c.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** 1dp 구분선 */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Nl.colors.line))
}

/** 앱 로고 — 런처 아이콘과 같은 그림(차콜 바탕에 노란 언덕 너머 길과 웃는 해)을 둥근 칸에 */
@Composable
fun Logo(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Image(
        painterResource(R.drawable.ic_logo),
        contentDescription = null,
        modifier = modifier.size(size).clip(Radius.chip).border(1.dp, Nl.colors.line, Radius.chip),
    )
}
