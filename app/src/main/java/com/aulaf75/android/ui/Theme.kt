package com.aulaf75.android.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Lavanta + gök mavisi + pembe: klavyenin pastel tuşları ve mor RGB'siyle uyumlu.
private val Dark = darkColorScheme(
    primary = Color(0xFFB9A7FF),
    onPrimary = Color(0xFF26185A),
    primaryContainer = Color(0xFF3B2D7A),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFF8FD8FF),
    onSecondary = Color(0xFF003549),
    secondaryContainer = Color(0xFF16485F),
    onSecondaryContainer = Color(0xFFC7ECFF),
    tertiary = Color(0xFFFFA8D2),
    onTertiary = Color(0xFF5A1238),
    tertiaryContainer = Color(0xFF6E2A4F),
    onTertiaryContainer = Color(0xFFFFD9E9),
    background = Color(0xFF110E18),
    onBackground = Color(0xFFE8E1F2),
    surface = Color(0xFF110E18),
    onSurface = Color(0xFFE8E1F2),
    surfaceVariant = Color(0xFF2A2536),
    onSurfaceVariant = Color(0xFFC9C2D6),
    surfaceContainerLowest = Color(0xFF0C0A12),
    surfaceContainerLow = Color(0xFF19151F),
    surfaceContainer = Color(0xFF1E1A26),
    surfaceContainerHigh = Color(0xFF282331),
    surfaceContainerHighest = Color(0xFF332D3D),
    outline = Color(0xFF8F88A0),
    outlineVariant = Color(0xFF443D52),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val Light = lightColorScheme(
    primary = Color(0xFF6B4FE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6DEFF),
    onPrimaryContainer = Color(0xFF22126B),
    secondary = Color(0xFF00739A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC7ECFF),
    onSecondaryContainer = Color(0xFF001E2C),
    tertiary = Color(0xFFB0306E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E9),
    onTertiaryContainer = Color(0xFF3E0024),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1C1A22),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1C1A22),
    surfaceVariant = Color(0xFFE6E0F0),
    onSurfaceVariant = Color(0xFF48454F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F2FC),
    surfaceContainer = Color(0xFFF0ECF8),
    surfaceContainerHigh = Color(0xFFEAE6F3),
    surfaceContainerHighest = Color(0xFFE4E0ED),
    outline = Color(0xFF79757F),
    outlineVariant = Color(0xFFCAC4D3),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AulaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        shapes = AppShapes,
        content = content,
    )
}
