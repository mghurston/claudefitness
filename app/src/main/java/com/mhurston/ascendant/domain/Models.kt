package com.mhurston.ascendant.domain

import java.time.LocalDate

/** A single ad-hoc activity logged to one specific day (e.g. "Marathon", "Rock climbing").
 *  Lives only on that day — never appears as an option on other days.
 *
 *  [kcal] is the single calorie value that feeds XP directly (1 kcal = 1 XP). [reps] and
 *  [distanceMi] are optional metrics recorded for the log — they don't add XP on their own
 *  (only [kcal] does), but the entry dialog uses them to auto-estimate [kcal] from the same
 *  strength/walking models the rest of the app uses (override-able). */
data class OneOff(
    val name: String,
    val kcal: Int,
    val distanceMi: Double = 0.0,
    val reps: Int = 0,
    /** DistanceActivity name ("WALK"/"RUN"/"BIKE") the distance was logged as, or "" for older
     *  rows / rep-only entries. Remembered so editing re-opens with the right estimate model. */
    val activityId: String = ""
) {
    /** Short "60 reps · 5.0 mi" subtitle for display; empty when no metrics were recorded.
     *  Distance is shown in the user's units (mi/km); [distanceMi] is always stored in miles. */
    fun metricsLabel(unit: UnitSystem = UnitSystem.IMPERIAL): String = buildList {
        if (reps > 0) add("$reps reps")
        if (distanceMi > 0.0) add(Units.distanceLabel(distanceMi, unit))
    }.joinToString(" · ")
}

/** Activity choices for a distance-based one-off. Each carries a *gross* kcal-per-kg-per-mile
 *  rate so a logged distance can be turned into a calorie estimate (override-able). Cycling
 *  burn is strongly pace-dependent, so [BIKE] is a moderate-effort baseline. */
enum class DistanceActivity(val label: String, val kcalPerKgPerMile: Double) {
    WALK("Walk", Calories.WALK_KCAL_PER_KG_PER_MILE),
    RUN("Run", Calories.RUN_KCAL_PER_KG_PER_MILE),
    BIKE("Bike", Calories.BIKE_KCAL_PER_KG_PER_MILE);

    companion object {
        /** Resolve a stored OneOff.activityId back to its activity; WALK for ""/unknown. */
        fun forId(id: String): DistanceActivity = entries.firstOrNull { it.name == id } ?: WALK
    }
}

/** Plain, Android-free representation of one day's logged work (mirrors the spreadsheet row). */
data class DayData(
    val date: LocalDate,
    val pushups: Int = 0,
    val squats: Int = 0,
    val legLifts: Int = 0,
    val calfRaises: Int = 0,
    val curls: Int = 0,
    val miles: Double = 0.0,
    /** Calories eaten this day. -1 = not logged (the day simply has no diet XP term);
     *  0 = a deliberate zero-intake (fasting) day that counts its full deficit as XP. */
    val caloriesConsumed: Int = -1,
    /** Body weight (kg) in effect for this day. 0 = no weigh-in; callers carry the last known
     *  weight forward (Progression.carryForward) and fall back to the profile weight. Drives this
     *  day's BMR and body-weight-scaled activity burn so history stays anchored to what you
     *  actually weighed, not your current weight. */
    val weightKg: Double = 0.0,
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
    val oneOffs: List<OneOff> = emptyList(),
    /** Steps banked from Health Connect for this day (phone + any synced watch/app).
     *  XP-only: earns calories like everything else but never feeds the walking-miles goal,
     *  the END stat, or completion. See docs/Passive Activity Tracking. */
    val passiveSteps: Int = 0,
    /** Active calories banked from Health Connect for this day. Preferred kcal source for
     *  passive burn; when 0 (device reports steps only) we estimate from passiveSteps. */
    val passiveKcal: Int = 0
) {
    val strengthReps: Int get() = pushups + squats + legLifts + calfRaises + curls
    /** Reps from pinned custom exercises, counted as strength-equivalent for burn. */
    val customRepsTotal: Int get() = customReps.values.sumOf { it.coerceAtLeast(0) }
    /** Distance estimated from passively-tracked steps (~2000 steps/mi). This is the
     *  "tracked walked" portion of walking. Its calories are already counted via
     *  passiveKcal (or the step estimate) in Calories.activityBurn — so feeding it into
     *  the walking goal/completion below never double-counts XP. */
    val trackedMiles: Double get() = passiveSteps.coerceAtLeast(0) / Calories.STEPS_PER_MILE
    /** Total walking that fills the daily 5-mile goal & completion: manually-logged
     *  (treadmill / off-phone) miles + the step-estimated tracked distance. */
    val walkMiles: Double get() = miles + trackedMiles
    /** Calories from one-off activities (their own estimates). */
    val oneOffKcal: Int get() = oneOffs.sumOf { it.kcal.coerceAtLeast(0) }
    val hasStrength: Boolean get() = strengthReps > 0
    /** A real-movement day from passive tracking — enough steps to count as "active." */
    val hasPassiveMovement: Boolean get() = passiveSteps >= PASSIVE_ACTIVITY_THRESHOLD
    val hasActivity: Boolean
        get() = hasStrength || miles > 0.0 || customRepsTotal > 0 || oneOffKcal > 0 ||
            cardioMinutes.values.any { it > 0 } || hasPassiveMovement

    companion object {
        /** Passive steps at/above this count make a day "active" — it sustains the activity
         *  streak and resets the idle-decay anchor (strength streak stays strength-only). */
        const val PASSIVE_ACTIVITY_THRESHOLD = 1000
    }
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
    E("E"), D("D"), C("C"), B("B"), A("A"), S("S"), SS("SS"), NATIONAL("National-Class")
}

/** Fully derived character — a pure function of the immutable day log + today's date. */
data class CharacterState(
    val totalXp: Long,        // effective XP after idle decay (drives level/rank)
    val earnedXp: Long,       // XP earned from logged days (burn + diet − shortfall)
    val idlePenaltyXp: Long,  // total permanent decay from fully-unlogged days (interior + trailing)
    val trailingPenaltyXp: Long, // just the still-growing trailing gap (the "log today" nudge)
    val trailingChargedDays: Int = 0, // unlogged days actually charged in the trailing gap
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
