package com.aulaf75.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand: violet primary (matches the F75's default RGB), cyan and pink accents, quiet neutral surfaces.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4A5FF),
    onPrimary = Color(0xFF221455),
    primaryContainer = Color(0xFF392A7C),
    onPrimaryContainer = Color(0xFFE7DEFF),
    secondary = Color(0xFF7FD3F5),
    onSecondary = Color(0xFF00344A),
    secondaryContainer = Color(0xFF124B63),
    onSecondaryContainer = Color(0xFFC6EBFF),
    tertiary = Color(0xFFF7A6CF),
    onTertiary = Color(0xFF55133A),
    tertiaryContainer = Color(0xFF6E2B51),
    onTertiaryContainer = Color(0xFFFFD8E9),
    background = Color(0xFF0F0E13),
    onBackground = Color(0xFFE6E1EA),
    surface = Color(0xFF0F0E13),
    onSurface = Color(0xFFE6E1EA),
    surfaceVariant = Color(0xFF2A2732),
    onSurfaceVariant = Color(0xFFC7C2D0),
    surfaceContainerLowest = Color(0xFF0A090D),
    surfaceContainerLow = Color(0xFF16141B),
    surfaceContainer = Color(0xFF1B1920),
    surfaceContainerHigh = Color(0xFF25232B),
    surfaceContainerHighest = Color(0xFF302D36),
    outline = Color(0xFF8E8998),
    outlineVariant = Color(0xFF3F3B47),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B45D6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF1C0C66),
    secondary = Color(0xFF006A8E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC6EBFF),
    onSecondaryContainer = Color(0xFF001E2C),
    tertiary = Color(0xFFA6326B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E9),
    onTertiaryContainer = Color(0xFF3E0024),
    background = Color(0xFFF8F6FB),
    onBackground = Color(0xFF1B1A20),
    surface = Color(0xFFF8F6FB),
    onSurface = Color(0xFF1B1A20),
    surfaceVariant = Color(0xFFE5E1EC),
    onSurfaceVariant = Color(0xFF47454F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF2F0F6),
    surfaceContainer = Color(0xFFECEAF1),
    surfaceContainerHigh = Color(0xFFE6E4EB),
    surfaceContainerHighest = Color(0xFFE0DEE6),
    outline = Color(0xFF78757F),
    outlineVariant = Color(0xFFC9C5D0),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Small uppercase section caption. */
val CaptionStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)

@Composable
fun AulaTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
