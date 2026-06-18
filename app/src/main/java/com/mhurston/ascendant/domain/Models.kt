package com.mhurston.ascendant.domain

import java.time.LocalDate

/** A single ad-hoc activity logged to one specific day (e.g. "Marathon", "Rock climbing").
 *  Lives only on that day — never appears as an option on other days. Carries its own
 *  calorie estimate, which feeds XP directly under the burn-based model. */
data class OneOff(val name: String, val kcal: Int)

/** Plain, Android-free representation of one day's logged work (mirrors the spreadsheet row). */
data class DayData(
    val date: LocalDate,
    val pushups: Int = 0,
    val squats: Int = 0,
    val legLifts: Int = 0,
    val calfRaises: Int = 0,
    val curls: Int = 0,
    val miles: Double = 0.0,
    val caloriesConsumed: Int = 0,
    val isRestDay: Boolean = false,
    val notes: String = "",
    val mood: Int = 0, // 0 = unset, 1..5 (see WorkoutDayEntity.mood)
    /** Pinned recurring custom exercises: customExerciseId -> reps for the day.
     *  Counted as strength-equivalent burn (so they earn XP via calories like the core). */
    val customReps: Map<String, Int> = emptyMap(),
    /** Time-based extra cardio: CardioActivity.id -> minutes for the day. Burns calories
     *  via the MET formula (see Calories), separate from the walking-miles goal. */
    val cardioMinutes: Map<String, Int> = emptyMap(),
    /** Ad-hoc one-off activities logged to this day only (name + calorie estimate). */
    val oneOffs: List<OneOff> = emptyList()
) {
    val strengthReps: Int get() = pushups + squats + legLifts + calfRaises + curls
    /** Reps from pinned custom exercises, counted as strength-equivalent for burn. */
    val customRepsTotal: Int get() = customReps.values.sumOf { it.coerceAtLeast(0) }
    /** Calories from one-off activities (their own estimates). */
    val oneOffKcal: Int get() = oneOffs.sumOf { it.kcal.coerceAtLeast(0) }
    val hasStrength: Boolean get() = strengthReps > 0
    val hasActivity: Boolean get() = hasStrength || miles > 0.0 || customRepsTotal > 0 || oneOffKcal > 0
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

/**
 * The Core goal can be satisfied by any of these equivalent exercises — reps from all of them
 * sum 1:1 toward the same daily target (Progression.REP_TARGET). [LEG_LIFTS] is kept in
 * WorkoutDayEntity.legLifts (its own column, backward-compatible); the rest are stored in
 * WorkoutDayEntity.coreVariants. Fixed built-in list.
 */
enum class CoreExercise(val id: String, val label: String) {
    LEG_LIFTS("leglifts", "Leg Lifts"),
    SITUPS("situps", "Sit-ups"),
    HIGH_KNEES("high_knees", "High Knees");

    companion object {
        /** Variants stored in the encoded coreVariants column (everything except [LEG_LIFTS]). */
        val EXTRAS: List<CoreExercise> = entries.filter { it != LEG_LIFTS }
    }
}

/**
 * Extra cardio logged by the minute (not distance), so it can't sensibly fill the walking-miles
 * goal. Instead each minute burns calories via the MET formula and feeds XP directly — these are
 * "their own thing," separate from the 5-mile walking target. Stored in WorkoutDayEntity.cardioMinutes.
 */
enum class CardioActivity(val id: String, val label: String, val met: Double) {
    BIKE("bike", "Bike Riding", 8.0),
    SWIM("swim", "Swimming", 7.0);

    companion object {
        fun metFor(id: String): Double = entries.firstOrNull { it.id == id }?.met ?: 0.0
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
