package com.nolleo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Nolleo 디자인 시스템 — 한 벌의 색·간격·모서리·글자만 쓴다.
 *
 * 색(11/8 사용자 선택 "옐로"): 글은 거의 검정, 바탕은 연회색·흰 카드. 밤(시스템 야간 모드)에는 차콜 바탕·흰 글.
 * 노랑은 **주 버튼과 고른 표시, 꼭 봐야 하는 안내 카드에만** 쓰고 나머지는 중립색이다. 노랑 위의 글은 항상 검정.
 * 정답·오답 두 상태색은 화면 게임의 답 표시와 음성 화면의 진행·듣는 중 표시에만 쓴다. 보라색은 쓰지 않는다.
 *
 * 대비(WCAG): 낮 — 글 #16181D / 흰 카드 17:1, 보조 글 #5B616E / 바탕 #F3F4F6 5.6:1, 노랑 #FFC53D 위 검정 글 12:1,
 * 정답 #1B7F43 위 흰 글 5.0:1, 오답 #C62828 위 흰 글 5.6:1. 밤 — 글 #F1F3F5 / 카드 #1A1D22 15:1, 보조 글 #A7AEB9 7.6:1,
 * 흐린 글 #6E7580 3.6:1(비활성 전용), 안내 카드 글 #FFE9A8 / #2B2410 13:1. 글 쌍은 모두 4.5:1 이상 (DEV_GUIDE §6.1).
 */
@Immutable
data class NlPalette(
    /** 앱 바탕 */
    val ground: Color,
    /** 카드·패널 */
    val card: Color,
    /** 선택 막대 바탕·칩·한자 칸 (바탕보다 한 단계 짙음) */
    val sunk: Color,
    /** 선택 막대에서 고른 칸 (막대 바탕보다 밝게 떠 보이게) */
    val segmentOn: Color,
    /** 1dp 구분선·테두리 */
    val line: Color,
    val ink: Color,
    /** 보조 글 (4.5:1 이상) */
    val muted: Color,
    /** 비활성 글 */
    val faint: Color,
    /** 주 버튼 — 화면마다 채운 버튼은 하나 */
    val action: Color,
    val onAction: Color,
    /** 고른 표시(라디오 점) — 낮에는 노랑이 흰 바탕에서 잘 안 보여 검정, 밤에는 노랑 */
    val indicator: Color,
    /** 고른 목적지 줄의 옅은 바탕 */
    val selectedFill: Color,
    /** 누르는 동안 덮는 색 (물결 대신 즉시 바뀌고 떼면 바로 사라짐) */
    val pressed: Color,
    /** 지금 차례인 사람 아바타 */
    val strong: Color,
    val onStrong: Color,
    /** 꼭 봐야 하는 안내 카드(졸음 케어 말 걸기·쉬어 가기) */
    val band: Color,
    val bandBorder: Color,
    val onBand: Color,
    val bandMuted: Color,
    /** 안내 카드 위 주 버튼 */
    val bandAction: Color,
    val onBandAction: Color,
    val success: Color,
    val onSuccess: Color,
    val danger: Color,
    val onDanger: Color,
)

private val Ink = Color(0xFF16181D)
private val Yellow = Color(0xFFFFC53D)
private val NightYellow = Color(0xFFFFD043)

val DayPalette = NlPalette(
    ground = Color(0xFFF3F4F6),
    card = Color(0xFFFFFFFF),
    sunk = Color(0xFFE9EBEF),
    segmentOn = Color(0xFFFFFFFF),
    line = Ink.copy(alpha = 0.10f),
    ink = Ink,
    muted = Color(0xFF5B616E),
    faint = Color(0xFF7D838E),
    action = Yellow,
    onAction = Ink,
    indicator = Ink,
    selectedFill = Yellow.copy(alpha = 0.22f),
    pressed = Ink.copy(alpha = 0.10f),
    strong = Ink,
    onStrong = Yellow,
    band = Yellow,
    bandBorder = Yellow,
    onBand = Ink,
    bandMuted = Color(0xFF3D3420),
    bandAction = Ink,
    onBandAction = Yellow,
    success = Color(0xFF1B7F43),
    onSuccess = Color(0xFFFFFFFF),
    danger = Color(0xFFC62828),
    onDanger = Color(0xFFFFFFFF),
)

val NightPalette = NlPalette(
    ground = Color(0xFF0F1114),
    card = Color(0xFF1A1D22),
    sunk = Color(0xFF25292F),
    segmentOn = Color(0xFF41474F),
    line = Color.White.copy(alpha = 0.10f),
    ink = Color(0xFFF1F3F5),
    muted = Color(0xFFA7AEB9),
    faint = Color(0xFF6E7580),
    action = NightYellow,
    onAction = Ink,
    indicator = NightYellow,
    selectedFill = NightYellow.copy(alpha = 0.12f),
    pressed = Color.White.copy(alpha = 0.12f),
    strong = NightYellow,
    onStrong = Ink,
    // 밤에 갑자기 밝은 노란 면이 뜨면 눈부시므로 어두운 호박색 + 노란 테두리
    band = Color(0xFF2B2410),
    bandBorder = NightYellow,
    onBand = Color(0xFFFFE9A8),
    bandMuted = Color(0xFFD9C690),
    bandAction = NightYellow,
    onBandAction = Ink,
    success = Color(0xFF4ADE80),
    onSuccess = Color(0xFF052E16),
    danger = Color(0xFFF87171),
    onDanger = Color(0xFF3B0A0A),
)

/** 간격 4·8·12·16·24·32·48 만 쓴다 */
object Sp {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp
}

/** 모서리 한 벌: 칩·막대 칸 14 · 줄·타일 20 · 카드 28 · 버튼은 알약 */
object Radius {
    val chip = RoundedCornerShape(14.dp)
    val tile = RoundedCornerShape(20.dp)
    val card = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(percent = 50)
}

private val LocalPalette = staticCompositionLocalOf { DayPalette }

/** 어디서든 `Nl.colors.ink` 처럼 쓴다 */
object Nl {
    val colors: NlPalette
        @Composable @ReadOnlyComposable get() = LocalPalette.current
}

// 플랫폼 기본 글꼴(Asta Sans)을 그대로 쓴다 — 400 Regular, 600 Bold 두 굵기만 (500 은 플랫폼에서 Regular 로 매핑됨).
// 가장 작은 글자는 21sp (DEV_GUIDE §6.5: 12sp 이상·실제 18px 이상). 주행 화면 본문은 33sp 이상.
// 11/8 사용자 요청으로 두 번 키웠다 — "아주 살짝만"(1~4sp) 뒤 "더 키워 보자, 특히 홈"(다시 2~8sp, 약 8~11%).
// 에뮬레이터 화면은 160dpi(1dp = 1px)라 2560px 화면에서 글이 작아 보였다. 홈은 줄마다 한 단계 큰 스타일을 쓴다.
private val Regular = FontWeight.Normal
private val Strong = FontWeight.SemiBold

private val typography = Typography(
    displayLarge = TextStyle(fontSize = 84.sp, fontWeight = Strong, lineHeight = 96.sp, letterSpacing = (-0.02).em),
    displayMedium = TextStyle(fontSize = 66.sp, fontWeight = Regular, lineHeight = 76.sp, letterSpacing = (-0.02).em),
    headlineLarge = TextStyle(fontSize = 50.sp, fontWeight = Strong, lineHeight = 64.sp, letterSpacing = (-0.02).em),
    headlineMedium = TextStyle(fontSize = 42.sp, fontWeight = Strong, lineHeight = 52.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontSize = 35.sp, fontWeight = Regular, lineHeight = 46.sp, letterSpacing = (-0.01).em),
    titleLarge = TextStyle(fontSize = 33.sp, fontWeight = Strong, lineHeight = 42.sp, letterSpacing = (-0.01).em),
    titleMedium = TextStyle(fontSize = 29.sp, fontWeight = Strong, lineHeight = 38.sp, letterSpacing = (-0.01).em),
    titleSmall = TextStyle(fontSize = 26.sp, fontWeight = Strong, lineHeight = 34.sp),
    bodyLarge = TextStyle(fontSize = 28.sp, fontWeight = Regular, lineHeight = 39.sp),
    bodyMedium = TextStyle(fontSize = 24.sp, fontWeight = Regular, lineHeight = 34.sp),
    bodySmall = TextStyle(fontSize = 22.sp, fontWeight = Regular, lineHeight = 30.sp),
    labelLarge = TextStyle(fontSize = 28.sp, fontWeight = Strong, lineHeight = 34.sp),
    labelMedium = TextStyle(fontSize = 23.sp, fontWeight = Strong, lineHeight = 30.sp),
    labelSmall = TextStyle(fontSize = 21.sp, fontWeight = Regular, lineHeight = 28.sp),
)

@Composable
fun NolleoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = if (dark) NightPalette else DayPalette
    // Material 기본값(보라 계열)이 슬라이더·입력칸 등에 새어 나오지 않도록 모든 역할을 우리 색으로 채운다
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = p.ink, onPrimary = p.card, primaryContainer = p.sunk, onPrimaryContainer = p.ink, inversePrimary = p.action,
        secondary = p.ink, onSecondary = p.card, secondaryContainer = p.sunk, onSecondaryContainer = p.ink,
        tertiary = p.ink, onTertiary = p.card, tertiaryContainer = p.sunk, onTertiaryContainer = p.ink,
        background = p.ground, onBackground = p.ink,
        surface = p.card, onSurface = p.ink, surfaceVariant = p.sunk, onSurfaceVariant = p.muted, surfaceTint = p.ink,
        inverseSurface = p.ink, inverseOnSurface = p.card,
        error = p.danger, onError = p.onDanger, errorContainer = p.sunk, onErrorContainer = p.danger,
        outline = p.muted, outlineVariant = p.line,
        surfaceBright = p.card, surfaceDim = p.ground,
        surfaceContainerLowest = p.card, surfaceContainerLow = p.card, surfaceContainer = p.card,
        surfaceContainerHigh = p.sunk, surfaceContainerHighest = p.sunk,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
