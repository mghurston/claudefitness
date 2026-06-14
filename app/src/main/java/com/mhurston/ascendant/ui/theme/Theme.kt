package com.mhurston.ascendant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AscendantColors = darkColorScheme(
    primary = ManaPurple,
    secondary = AuraCyan,
    tertiary = XpGold,
    background = AscendantBg,
    surface = AscendantBg,
    error = DangerRed,
    onPrimary = TextHigh,
    onSecondary = AscendantBg,
    onBackground = TextHigh,
    onSurface = TextHigh
)

@Composable
fun AscendantTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // ASCENDANT is always dark for now — the RPG aesthetic depends on it.
    MaterialTheme(
        colorScheme = AscendantColors,
        typography = Typography,
        content = content
    )
}
