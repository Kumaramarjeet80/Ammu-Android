package com.ammu.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Color.White,
    secondary = AccentGreenLight,
    background = DarkBackground,
    surface = DarkCard,
    surfaceVariant = DarkSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = DarkBorder
)

private val AmoledColorScheme = darkColorScheme(
    primary = AccentGreen,
    onPrimary = Color.White,
    secondary = AccentGreenLight,
    background = AmoledBlack,
    surface = Color(0xFF070707),
    surfaceVariant = Color(0xFF101010),
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Color(0xFF222222)
)

@Composable
fun AmmuTheme(
    isAmoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isAmoledMode) AmoledColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
