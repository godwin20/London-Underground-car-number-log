package com.keithstack.carlog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CarLogColorScheme = lightColorScheme(
    background = ColorBg,
    surface = ColorBg,
    surfaceVariant = ColorSurface,
    onBackground = ColorText,
    onSurface = ColorText,
    primary = ColorAccent,
    onPrimary = Neutral100,
    secondary = Accent700,
    outline = ColorDivider,
    outlineVariant = ColorDivider,
)

@Composable
fun CarLogTheme(content: @Composable () -> Unit) {
    // Light theme only, matching the original web app's single palette.
    MaterialTheme(
        colorScheme = CarLogColorScheme,
        typography = CarLogTypography,
        content = content,
    )
}
