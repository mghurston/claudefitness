package com.mhurston.ascendant.ui.theme

import androidx.compose.ui.graphics.Color

// ASCENDANT core palette (dark, anime-RPG). See docs/Style Guide.md for the full system.
val AscendantBg = Color(0xFF0B0B14)
val AscendantSurface = Color(0xFF151524)
val ManaPurple = Color(0xFF7C4DFF)
val AuraCyan = Color(0xFF18E0FF)
val XpGold = Color(0xFFFFC24B)
val DangerRed = Color(0xFFFF5470)
val TextHigh = Color(0xFFF2F2F7)
val TextDim = Color(0xFF9A9AB0)

// Accents from the Style Guide (§2.2 stat colors, §2.3 rarity glows).
val CrimsonRed = Color(0xFFFF2D55)   // STR / Mythic
val SuccessGreen = Color(0xFF3DDC84) // END / Uncommon / earned states

// Shared surfaces that used to be hardcoded per-screen — one source of truth.
val TrackDark = Color(0xFF22223A)  // progress-bar/ring track, empty day cells, locked medallions
val PanelAlt = Color(0xFF1B1B2A)   // raised/unselected chip background
val ScrimDark = Color(0xCC0E0E16)  // translucent caption scrim over the hero portrait
