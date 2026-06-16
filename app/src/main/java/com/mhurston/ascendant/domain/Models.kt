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
    val isRestDay: Boolean = false,
    val notes: String = "",
    val mood: Int = 0, // 0 = unset, 1..5 (see WorkoutDayEntity.mood)
    /** Supplementary user-defined exercises: customExerciseId -> reps for the day.
     *  Earns bonus XP only — never affects completion %, stats, or streaks. */
    val customReps: Map<String, Int> = emptyMap()
) {
    val strengthReps: Int get() = pushups + squats + legLifts + calfRaises + curls
    val hasStrength: Boolean get() = strengthReps > 0
    val hasActivity: Boolean get() = hasStrength || miles > 0.0
}

/**
 * The Push-ups goal can be satisfied by any of these equivalent exercises — reps from all
 * of them sum 1:1 toward the same daily target (Progression.REP_TARGET). [PUSHUPS] is kept
 * in WorkoutDayEntity.pushups (its own column, backward-compatible); the rest are stored in
 * WorkoutDayEntity.pushVariants. Fixed built-in list.
 */
enum class PushExercise(val id: String, val label: String) {
    PUSHUPS("pushups", "Pushups"),
    DUMBBELL_CHEST_FLY("db_chest_fly", "Standing Dumbbell Chest Fly"),
    UPWARD_CHEST_FLY("upward_chest_fly", "Standing Upward Chest Fly"),
    PULL_UPS("pull_ups", "Pull Ups"),
    CHIN_UPS("chin_ups", "Chin Ups"),
    DIPS("dips", "Dips");

    companion object {
        /** Variants stored in the encoded pushVariants column (everything except [PUSHUPS]). */
        val EXTRAS: List<PushExercise> = entries.filter { it != PUSHUPS }
    }
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

/** A user-defined supplementary exercise (e.g. "Pull-ups", "Plank seconds").
 *  [archived] = removed from today's options but kept so past logs still resolve its name. */
data class CustomExercise(val id: String, val name: String, val archived: Boolean = false)

/** Per-day derived values used by the dashboard/history. */
data class DayDerived(
    val completion: Double, // 0..n (can exceed 1.0 on overdrive)
    val xp: Long
)
