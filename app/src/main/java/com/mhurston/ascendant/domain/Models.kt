package com.mhurston.ascendant.domain

import java.time.LocalDate

/** Plain, Android-free representation of one day's logged work (mirrors the spreadsheet row). */
data class DayData(
    val date: LocalDate,
    val pushups: Int = 0,
    val squats: Int = 0,
    val legLifts: Int = 0,
    val calfRaises: Int = 0,
    val curls: Int = 0,
    val miles: Double = 0.0,
    val isRestDay: Boolean = false
) {
    val strengthReps: Int get() = pushups + squats + legLifts + calfRaises + curls
    val hasStrength: Boolean get() = strengthReps > 0
    val hasActivity: Boolean get() = hasStrength || miles > 0.0
}

/** The five RPG attributes (Leveling System §5). */
data class Stats(
    val strength: Int,
    val endurance: Int,
    val agility: Int,
    val discipline: Int,
    val consistency: Int
)

enum class Rank(val label: String) {
    E("E"), D("D"), C("C"), B("B"), A("A"), S("S"), SS("SS"), NATIONAL("National-Level Hunter")
}

/** Fully derived character — a pure function of the immutable day log + today's date. */
data class CharacterState(
    val totalXp: Long,        // effective XP after idle decay (drives level/rank)
    val earnedXp: Long,       // gross XP earned from the log, before decay
    val idlePenaltyXp: Long,  // XP currently shaved off by inactivity
    val idleDays: Int,        // consecutive idle days counted toward decay
    val level: Int,
    val rank: Rank,
    val title: String,
    val xpIntoLevel: Long,
    val xpForNextLevel: Long,
    val stats: Stats,
    val strengthStreak: Int,
    val activityStreak: Int,
    val perfectStreak: Int,
    val longestStrengthStreak: Int,
    val daysTrained: Int,
    val totalStrengthReps: Int,
    val totalMiles: Double
) {
    val levelProgress: Float
        get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel.toFloat())
}

/** Per-day derived values used by the dashboard/history. */
data class DayDerived(
    val completion: Double, // 0..n (can exceed 1.0 on overdrive)
    val xp: Long
)
