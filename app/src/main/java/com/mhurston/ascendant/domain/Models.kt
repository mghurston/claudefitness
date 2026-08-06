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
    /** Calories eaten this day. -1 = not logged (carryForward substitutes the last entered
     *  value; no diet XP term before any entry). 0 = a deliberate zero-intake (fasting) day
     *  that counts its full deficit as XP and carries forward like any entered value. */
    val caloriesConsumed: Int = -1,
    /** Body weight (kg) in effect for this day. 0 = no weigh-in; callers carry the last known
     *  weight forward (Progression.carryForward) and fall back to the profile weight. Drives this
     *  day's BMR and body-weight-scaled activity burn so history stays anchored to what you
     *  actually weighed, not your current weight. */
    val weightKg: Double = 0.0,
    val isRestDay: Boolean = false,
    val notes: String = "",
    /** Pinned recurring custom exercises: customExerciseId -> reps for the day.
     *  Counted as strength-equivalent burn (so they earn XP via calories like the core). */
    val customReps: Map<String, Int> = emptyMap(),
    /** Time-based extra cardio: CardioActivity.id -> minutes for the day. Burns calories
     *  via the MET formula (see Calories), separate from the walking-miles goal. */
    val cardioMinutes: Map<String, Int> = emptyMap(),
    /** Ad-hoc one-off activities logged to this day only (name + calorie estimate). */
    val oneOffs: List<OneOff> = emptyList(),
    /** Non-walking cardio from pinned customs (a rower, a bike, an elliptical), expressed as
     *  the miles you would have to WALK to burn the same calories. Kept out of [walkMiles] on
     *  purpose: it earns its true calories and fills the Cardio goal, but a bike mile is not a
     *  walked mile, so it must never touch walking achievements or lifetime miles. */
    val cardioEquivMiles: Double = 0.0,
    /** Steps banked from Health Connect for this day (phone + any synced watch/app). Counted
     *  as walking at ~2000 steps/mi ([trackedMiles]), so they earn calories and feed the
     *  walking total, the Cardio goal, and END alike. See docs/Passive Activity Tracking. */
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
            cardioMinutes.values.any { it > 0 } || cardioEquivMiles > 0.0 || hasPassiveMovement

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
    /** Lifetime WALKED miles (manual + step-tracked) — the Records row and walking achievements. */
    val totalMiles: Double,
    /** Lifetime cardio of every kind, as the walked miles it is worth. What END is built from;
     *  equal to [totalMiles] for a log whose only cardio was walking. */
    val enduranceMiles: Double = 0.0
) {
    val levelProgress: Float
        get() = if (xpForNextLevel <= 0) 0f else (xpIntoLevel.toFloat() / xpForNextLevel.toFloat())
}

/**
 * Which training category a custom exercise's reps count toward — the same four groups the
 * Train tab shows. [NONE] is the original behavior: the reps burn calories (= XP) but fill no
 * goal, so they never move completion or stats.
 *
 * [UPPER] and [LOWER] each cover two 100-rep slots (push/curls and squats/calf raises), for a
 * 200-rep category goal, so their reps split evenly across the pair — 100 reps is half the
 * category either way, which is exactly what it contributes to completion. [CORE] is a single
 * 100-rep slot and takes the full amount.
 *
 * [CARDIO] is the odd one out: its goal is 5 MILES, not reps, so an exercise pointed at it is
 * logged as a distance (rowing, elliptical, a rower's 500 m piece) and fills the mile goal.
 * That distance lives in its own per-day column, so switching an exercise between Cardio and a
 * rep category never reinterprets a number you already logged.
 */
enum class ExerciseGoal(val label: String) {
    NONE("Extra only (XP)"),
    UPPER("Upper Body"),
    CORE("Core"),
    LOWER("Lower Body"),
    CARDIO("Cardio (distance)");

    /** Cardio is measured in distance; every other category is measured in reps. */
    val isDistance: Boolean get() = this == CARDIO

    companion object {
        /** Older builds stored one goal per exercise slot; fold those into their category so
         *  definitions and backups written before the categories still resolve. */
        fun forName(name: String): ExerciseGoal = when (name) {
            "PUSH", "CURLS" -> UPPER
            "SQUATS", "CALF_RAISES" -> LOWER
            else -> entries.firstOrNull { it.name == name } ?: NONE
        }
    }
}

/** How a Cardio-assigned custom exercise is measured. Rowing and cycling are logged by
 *  distance; an elliptical or a class is easier to log by time. Ignored for every other goal. */
enum class CardioMode(val label: String) {
    DISTANCE("Distance (miles)"),
    MINUTES("Time (minutes)")
}

/** Per-mile burn rate for a DISTANCE-mode cardio custom. A mile is not a unit of effort: at
 *  90 kg a walked mile is ~109 kcal and a cycled one ~41, so the exercise carries the rate its
 *  own miles are worth. Values are the per-kg-per-mile constants in [Calories]. */
enum class CardioRate(val label: String, val kcalPerKgPerMile: Double) {
    WALK("Walking pace", Calories.WALK_KCAL_PER_KG_PER_MILE),
    RUN("Running pace", Calories.RUN_KCAL_PER_KG_PER_MILE),
    BIKE("Cycling pace", Calories.BIKE_KCAL_PER_KG_PER_MILE)
}

/** Effort for a MINUTES-mode cardio custom, as a MET. Same formula the built-in bike (8.0) and
 *  swim (7.0) use: kcal/min = MET x 3.5 x kg / 200. */
enum class CardioIntensity(val label: String, val met: Double) {
    LIGHT("Light", 4.0),
    MODERATE("Moderate", 6.0),
    HARD("Hard", 9.0)
}

/** A user-defined supplementary exercise (e.g. "Pull-ups", "Plank seconds").
 *  [archived] = removed from today's options but kept so past logs still resolve its name.
 *  [goal] = the category its work counts toward (NONE = calories only).
 *  [cardioMode]/[cardioRate]/[cardioIntensity] only apply when [goal] is CARDIO, and decide
 *  both how the exercise is logged and what a unit of it burns. */
data class CustomExercise(
    val id: String,
    val name: String,
    val archived: Boolean = false,
    val goal: ExerciseGoal = ExerciseGoal.NONE,
    val cardioMode: CardioMode = CardioMode.DISTANCE,
    val cardioRate: CardioRate = CardioRate.WALK,
    val cardioIntensity: CardioIntensity = CardioIntensity.MODERATE
) {
    /** Walk-equivalent miles for [amount] of this exercise (miles in DISTANCE mode, minutes in
     *  MINUTES mode) — the miles of walking that burn the same calories. Body weight cancels
     *  out of both conversions, so this is a pure ratio and history never depends on today's
     *  weigh-in. Only meaningful for a CARDIO exercise. */
    fun walkEquivalentMiles(amount: Double): Double = when (cardioMode) {
        CardioMode.DISTANCE ->
            amount * (cardioRate.kcalPerKgPerMile / Calories.WALK_KCAL_PER_KG_PER_MILE)
        // kcal/min = MET x 3.5 x kg / 200, and a walked mile = 1.2 x kg, so the kg divides out.
        CardioMode.MINUTES ->
            amount * cardioIntensity.met * 3.5 / 200.0 / Calories.WALK_KCAL_PER_KG_PER_MILE
    }

    /** The unit this exercise is logged in, for labels ("mi" / "min"). */
    val unitLabel: String
        get() = if (goal == ExerciseGoal.CARDIO && cardioMode == CardioMode.MINUTES) "min" else "mi"
}

/** Per-day derived values used by the dashboard/history. */
data class DayDerived(
    val completion: Double, // 0..n (can exceed 1.0 on overdrive)
    val xp: Long
)
