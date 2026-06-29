package com.mhurston.ascendant.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.TextDim
import com.mhurston.ascendant.ui.theme.TextHigh
import com.mhurston.ascendant.ui.theme.XpGold

/**
 * The app's complete text system — seven semantic roles built on the four-step type scale in
 * theme/Type.kt. EVERY screen renders text through one of these so size, weight, font, color, and
 * casing live in exactly one place. Do not pass raw `MaterialTheme.typography.*` + ad-hoc colors
 * for these roles; add a role here instead.
 *
 *   ScreenTitle    — the one big per-screen header. Forced UPPERCASE (HUD feel).   "WORKOUT LOG"
 *   ScreenSubtitle — the dim line directly under a screen title. Sentence case.    "12 days recorded"
 *   SectionHeader  — a divider within a screen. Title Case, cyan by default.        "Today's Training"
 *   BodyText       — primary readable copy and list-item names.
 *   Caption        — small secondary / hint / metadata text (dim).
 *   StatValue      — emphasized numbers (XP, counts, %). Gold by default.
 *   ActionText     — tappable inline actions / links. Purple by default.
 *
 * Casing is enforced by the style (ScreenTitle uppercases its input), so callers pass natural text
 * and never have to remember the convention. Subtitles are authored in sentence case at the source.
 */

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) =
    Text(text.uppercase(), modifier = modifier, color = ManaPurple,
        style = MaterialTheme.typography.headlineMedium)

@Composable
fun ScreenSubtitle(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier = modifier, color = TextDim,
        style = MaterialTheme.typography.labelMedium)

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, color: Color = AuraCyan) =
    Text(text, modifier = modifier, color = color,
        style = MaterialTheme.typography.titleLarge)

@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextHigh,
    textAlign: TextAlign? = null
) = Text(text, modifier = modifier, color = color, textAlign = textAlign,
    style = MaterialTheme.typography.bodyLarge)

@Composable
fun Caption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextDim,
    textAlign: TextAlign? = null
) = Text(text, modifier = modifier, color = color, textAlign = textAlign,
    style = MaterialTheme.typography.labelMedium)

@Composable
fun StatValue(text: String, modifier: Modifier = Modifier, color: Color = XpGold) =
    Text(text, modifier = modifier, color = color,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

@Composable
fun ActionText(text: String, modifier: Modifier = Modifier, color: Color = ManaPurple) =
    Text(text, modifier = modifier, color = color,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
