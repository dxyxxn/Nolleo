package com.gildongmu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 다크 테마 기본 (DEV_GUIDE §6). 모든 텍스트/배경 조합은 대비 ≥ 4.5:1 을 목표로 골랐다.
 *  - OnBackground #F2F5F8 on #0F1419 ≈ 17:1, Primary #5CC8FF on #0F1419 ≈ 10:1, Muted #B8C4D0 on #1A2129 ≈ 9:1
 * 폰트는 시스템 기본(Asta Sans 가 있으면 플랫폼이 적용).
 */
object GdColors {
    val Background = Color(0xFF0F1419)
    val Surface = Color(0xFF1A2129)
    val SurfaceHigh = Color(0xFF243039)
    val Primary = Color(0xFF5CC8FF)
    val OnPrimary = Color(0xFF00131F)
    val Accent = Color(0xFFFFD166)
    val OnAccent = Color(0xFF241A00)
    val Good = Color(0xFF7EE787)
    val Warn = Color(0xFFFFB86B)
    val Danger = Color(0xFFFF8A80)
    val Text = Color(0xFFF2F5F8)
    val Muted = Color(0xFFB8C4D0)
    val Disabled = Color(0xFF7D8894)
    val Outline = Color(0xFF3A4652)
}

private val colorScheme = darkColorScheme(
    primary = GdColors.Primary,
    onPrimary = GdColors.OnPrimary,
    secondary = GdColors.Accent,
    onSecondary = GdColors.OnAccent,
    background = GdColors.Background,
    onBackground = GdColors.Text,
    surface = GdColors.Surface,
    onSurface = GdColors.Text,
    surfaceVariant = GdColors.SurfaceHigh,
    onSurfaceVariant = GdColors.Muted,
    error = GdColors.Danger,
    outline = GdColors.Outline,
)

// 최소 12sp(실제 ≥ 18px) — 여기서 가장 작은 글자는 16sp. 주행 화면 본문은 28sp 이상.
private val typography = Typography(
    displayLarge = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Bold, lineHeight = 84.sp),
    displayMedium = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, lineHeight = 58.sp),
    headlineLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 52.sp),
    headlineMedium = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp),
    headlineSmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold, lineHeight = 40.sp),
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 36.sp),
    titleMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 30.sp),
    bodyLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    bodyMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    bodySmall = TextStyle(fontSize = 18.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    labelMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    labelSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp),
)

@Composable
fun GildongmuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
}
