package com.example.lifeloggerapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = SageGreen,
    onPrimary          = Color.White,
    primaryContainer   = SageGreenLight,
    onPrimaryContainer = DarkText,
    secondary          = SageGreen,
    onSecondary        = Color.White,
    secondaryContainer = SageGreenLight,
    onSecondaryContainer = DarkText,
    background         = CreamBackground,
    onBackground       = DarkText,
    surface            = CreamSurface,
    onSurface          = DarkText,
    surfaceVariant     = CreamSurface2,
    onSurfaceVariant   = MutedText,
    outline            = Color(0xFFD4D7CC),
)

private val DarkColorScheme = darkColorScheme(
    primary            = SageGreenDark,
    onPrimary          = Color(0xFF1A1F18),
    primaryContainer   = Color(0xFF2E3A28),
    onPrimaryContainer = SageGreenLight,
    secondary          = SageGreenDark,
    onSecondary        = Color(0xFF1A1F18),
    secondaryContainer = Color(0xFF252B22),
    onSecondaryContainer = SageGreenLight,
    background         = DarkBackground,
    onBackground       = LightText,
    surface            = DarkSurface,
    onSurface          = LightText,
    surfaceVariant     = DarkSurface2,
    onSurfaceVariant   = LightTextMuted,
    outline            = DarkBorder,
)

@Composable
fun LifeLoggerAppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}