package com.yamp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    onPrimary = TextPrimary,
    primaryContainer = AccentPurpleLight,
    secondary = AccentCyan,
    onSecondary = TextPrimary,
    secondaryContainer = AccentCyanLight,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = TextPrimary,
    outline = DividerColor
)

@Composable
fun YampTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = YampTypography,
        shapes = YampShapes,
        content = content
    )
}
