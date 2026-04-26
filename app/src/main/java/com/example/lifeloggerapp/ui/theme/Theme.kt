package com.example.lifeloggerapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // This fixes the "Color.White" error
import androidx.compose.ui.platform.LocalView

private val DarkColorScheme = darkColorScheme(
    primary = SageGreen,
    secondary = SageGreenLight,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = SageGreen,
    secondary = SageGreenLight,
    background = CreamBackground,
    surface = CreamBackground,
    onPrimary = Color.White,
    onBackground = DarkText,
    onSurface = DarkText
)

@Composable
fun LifeLoggerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}