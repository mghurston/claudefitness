package com.mhurston.ascendant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mhurston.ascendant.domain.CardioMode
import com.mhurston.ascendant.domain.CustomExercise
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.ExerciseGoal
import com.mhurston.ascendant.domain.OneOff
import java.time.LocalDate

/**
 * One logged day. Denormalized to match the original spreadsheet row exactly — the
 * completion formula and XP engine read precisely these columns.
 */
@Entity(tableName = "workout_day")
data class WorkoutDayEntity(
    @PrimaryKey val date: String, // ISO yyyy-MM-dd
    val pushups: Int = 0,
    val squats: Int = 0,
    val legLifts: Int = 0,
    val calfRaises: Int = 0,
    val curls: Int = 0,
    val miles: Double = 0.0,
    /** Calories eaten this day. -1 = not logged (the last logged value is carried forward at
     *  read time); 0 = a deliberate zero-intake (fasting) day. See Progression.carryForward. */
    val caloriesConsumed: Int = -1,
    /** Body weight (kg) recorded on this day. 0 = no weigh-in (the last known weight is carried
     *  forward at read time). Weigh-ins are occasional — not required daily. */
    val weightKg: Double = 0.0,
    val isRestDay: Boolean = false,
    val notes: String = "",
    /** RETIRED (v0.2.2): the mood picker was removed from the UI as write-only noise. The
     *  column stays so old rows/backups keep their data, but nothing reads or writes it. */
    val mood: Int = 0,
    /** Pinned recurring custom-exercise reps, encoded "id:reps,id:reps". Counted as
     *  strength-equivalent burn, so they earn XP via calories like the core lifts. */
    val customReps: String = "",
    /** Reps for the Push-ups alternatives (everything except the base Pushups column),
     *  encoded "id:reps,id:reps". These sum 1:1 into the push-ups total. See PushExercise. */
    val pushVariants: String = "",
    /** Reps for the Core alternatives (everything except the base legLifts column),
     *  encoded "id:reps,id:reps". These sum 1:1 into the core total. See CoreExercise. */
    val coreVariants: String = "",
    /** Time-based extra cardio, encoded "id:minutes,id:minutes". Burns calories via MET;
     *  does not count toward the walking-miles goal. See CardioActivity. */
    val cardioMinutes: String = "",
    /** Ad-hoc one-off activities logged to this day only. Encoded with control-char
     *  delimiters (unit-sep U+001F between name and kcal, record-sep U+001E between entries)
     *  so arbitrary user names are safe. Stays in history; never an option on other days. */
    val oneOffs: String = "",
    /** Distance logged against DISTANCE-mode Cardio customs, encoded "id:hundredthsOfAMile"
     *  (e.g. "c123:250" = 2.50 mi). Its own column so an exercise moved between Cardio and a
     *  rep category never reads one number as the other. */
    val customDistance: String = "",
    /** Minutes logged against MINUTES-mode Cardio customs, encoded "id:minutes". Separate from
     *  [cardioMinutes], which holds the built-in bike/swim activities and is burned through
     *  CardioActivity's fixed METs. */
    val customMinutes: String = "",
    /** Steps banked from Health Connect for this day (overwritten on each sync, never summed). */
    val passiveSteps: Int = 0,
    /** Active calories banked from Health Connect for this day (preferred passive kcal source). */
    val passiveKcal: Int = 0
) {
    /**
     * @param specs customExerciseId -> its definition (category, and for Cardio how it is
     *   measured). Reps for a category-assigned custom are folded into that category's slots
     *   here and dropped from the customReps map, so they count toward completion/stats exactly
     *   like a built-in variant while their calories stay counted exactly once
     *   (DayData.strengthReps instead of customRepsTotal — same per-rep burn either way). Upper
     *   and Lower cover two slots each, so their reps split evenly across the pair (odd
     *   remainder to the first). Cardio customs convert to walk-equivalent miles instead. An id
     *   missing from the map, or set to NONE, keeps the original behavior: calories only.
     */
    fun toDayData(specs: Map<String, CustomExercise> = emptyMap()): DayData {
        val credit = customRepsByGoal(specs)
        val upper = credit(ExerciseGoal.UPPER)
        val lower = credit(ExerciseGoal.LOWER)
        return DayData(
            date = LocalDate.parse(date),
            pushups = pushTotal() + half(upper, first = true),
            squats = squats + half(lower, first = true),
            legLifts = coreTotal() + credit(ExerciseGoal.CORE),
            calfRaises = calfRaises + half(lower, first = false),
            curls = curls + half(upper, first = false),
            miles = miles,
            caloriesConsumed = caloriesConsumed,
            weightKg = weightKg,
            isRestDay = isRestDay,
            notes = notes,
            customReps = uncreditedCustomReps(specs),
            cardioMinutes = decodeCustomReps(cardioMinutes),
            oneOffs = decodeOneOffs(oneOffs),
            // Cardio customs stay out of `miles` on purpose: they burn what they are worth via
            // this field, but a rowed or cycled mile must never count as a walked one.
            cardioEquivMiles = cardioEquivMiles(specs),
            passiveSteps = passiveSteps,
            passiveKcal = passiveKcal
        )
    }

    /** Split a two-slot category's reps down the middle; an odd rep goes to the first slot so
     *  the two halves always sum back to the original total (no rep is lost or invented). */
    private fun half(reps: Int, first: Boolean): Int =
        if (first) reps - reps / 2 else reps / 2

    private fun goalOf(specs: Map<String, CustomExercise>, id: String): ExerciseGoal =
        specs[id]?.goal ?: ExerciseGoal.NONE

    /** Reps this day logged against each category from assigned custom exercises. */
    private fun customRepsByGoal(specs: Map<String, CustomExercise>): (ExerciseGoal) -> Int {
        if (specs.isEmpty()) return { _ -> 0 }
        val byGoal = decodeCustomReps(customReps).entries
            .groupBy({ goalOf(specs, it.key) }, { it.value.coerceAtLeast(0) })
            .mapValues { (_, v) -> v.sum() }
        return { goal -> byGoal[goal] ?: 0 }
    }

    /** Total reps from customs assigned to [goal] — what the category header adds on top of its
     *  built-in exercises. Kept out of [pushTotal]/[coreTotal] etc. so each per-exercise line
     *  keeps showing only the reps actually logged against that exercise. */
    fun customRepsCredited(goal: ExerciseGoal, specs: Map<String, CustomExercise>): Int =
        if (goal == ExerciseGoal.NONE) 0 else customRepsByGoal(specs)(goal)

    /** Custom reps that fill no rep goal — the burn-only leftovers that stay in
     *  DayData.customReps. Includes reps left on an exercise that has since been pointed at
     *  Cardio (its distance/time is what counts now), so those reps keep earning their calories. */
    private fun uncreditedCustomReps(specs: Map<String, CustomExercise>): Map<String, Int> =
        decodeCustomReps(customReps).filterKeys {
            val g = goalOf(specs, it)
            g == ExerciseGoal.NONE || g.isDistance
        }

    /** Distance estimated from passive steps (~2000 steps/mi) — the "tracked walked" part of
     *  walking. Mirrors DayData.trackedMiles so the UI can read it off the entity directly. */
    val trackedMiles: Double
        get() = passiveSteps.coerceAtLeast(0) / com.mhurston.ascendant.domain.Calories.STEPS_PER_MILE

    /** Total walking toward the 5-mi goal = manual/treadmill miles + tracked. */
    val walkMiles: Double get() = miles + trackedMiles


    /** Total push-ups reps across every variant (base column + alternatives) — what counts
     *  toward the push-ups goal, XP, and stats. Customs assigned to Upper Body are counted at
     *  the category level instead (see [customRepsCredited]). */
    fun pushTotal(): Int = pushups + decodeCustomReps(pushVariants).values.sum()

    /** Reps for each push-up variant by PushExercise.id, including the base Pushups column.
     *  Only non-zero entries are present. */
    fun pushBreakdown(): Map<String, Int> = buildMap {
        if (pushups != 0) put(com.mhurston.ascendant.domain.PushExercise.PUSHUPS.id, pushups)
        putAll(decodeCustomReps(pushVariants))
    }

    /** Total core reps across every variant (legLifts column + alternatives). Customs assigned
     *  to Core are counted at the category level (see [customRepsCredited]). */
    fun coreTotal(): Int = legLifts + decodeCustomReps(coreVariants).values.sum()

    /** Reps logged today for each custom exercise assigned to [goal] (id -> reps), for showing
     *  them inside their category's section alongside the built-in exercises. */
    fun customRepsForGoal(goal: ExerciseGoal, specs: Map<String, CustomExercise>): Map<String, Int> =
        if (goal == ExerciseGoal.NONE || goal.isDistance) emptyMap()
        else decodeCustomReps(customReps).filterKeys { goalOf(specs, it) == goal }

    /** What each Cardio custom logged today, in its own unit: miles for a DISTANCE exercise,
     *  minutes for a MINUTES one. This is the number its row shows and its controls edit. */
    fun cardioAmounts(specs: Map<String, CustomExercise>): Map<String, Double> = buildMap {
        decodeCustomReps(customDistance).forEach { (id, hundredths) ->
            val ex = specs[id] ?: return@forEach
            if (ex.goal == ExerciseGoal.CARDIO && ex.cardioMode == CardioMode.DISTANCE) {
                put(id, hundredths.coerceAtLeast(0) / 100.0)
            }
        }
        decodeCustomReps(customMinutes).forEach { (id, min) ->
            val ex = specs[id] ?: return@forEach
            if (ex.goal == ExerciseGoal.CARDIO && ex.cardioMode == CardioMode.MINUTES) {
                put(id, min.coerceAtLeast(0).toDouble())
            }
        }
    }

    /** Every Cardio custom's work for the day, converted to the miles of walking that burn the
     *  same — the single number the burn engine and the Cardio goal both read. */
    fun cardioEquivMiles(specs: Map<String, CustomExercise>): Double =
        cardioAmounts(specs).entries.sumOf { (id, amount) ->
            specs[id]?.walkEquivalentMiles(amount) ?: 0.0
        }

    /** Reps for each core variant by CoreExercise.id, including the base legLifts column.
     *  Only non-zero entries are present. */
    fun coreBreakdown(): Map<String, Int> = buildMap {
        if (legLifts != 0) put(com.mhurston.ascendant.domain.CoreExercise.LEG_LIFTS.id, legLifts)
        putAll(decodeCustomReps(coreVariants))
    }

    companion object {
        /** "id:reps,id:reps" -> map. Ignores malformed/zero entries. */
        fun decodeCustomReps(encoded: String): Map<String, Int> =
            if (encoded.isBlank()) emptyMap() else buildMap {
                encoded.split(",").forEach { part ->
                    val i = part.lastIndexOf(':')
                    if (i > 0) {
                        val id = part.substring(0, i)
                        val reps = part.substring(i + 1).toIntOrNull() ?: 0
                        if (reps != 0) put(id, reps)
                    }
                }
            }

        fun encodeCustomReps(map: Map<String, Int>): String =
            map.filterValues { it != 0 }.entries.joinToString(",") { "${it.key}:${it.value}" }

        private const val ONEOFF_UNIT = ''   // between a one-off's name and its kcal
        private const val ONEOFF_RECORD = '' // between one-off entries

        // Record layout: name <US> kcal <US> distanceMi <US> reps <US> activityId. Older rows
        // wrote fewer fields — decode tolerates the missing ones so existing logs still resolve.
        fun decodeOneOffs(encoded: String): List<OneOff> =
            if (encoded.isBlank()) emptyList() else encoded.split(ONEOFF_RECORD).mapNotNull { rec ->
                if (rec.isBlank()) return@mapNotNull null
                val parts = rec.split(ONEOFF_UNIT)
                val name = parts.getOrNull(0)?.trim().orEmpty()
                if (name.isEmpty()) return@mapNotNull null
                val kcal = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val distanceMi = parts.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                val reps = parts.getOrNull(3)?.toIntOrNull() ?: 0
                val activityId = parts.getOrNull(4)?.trim().orEmpty()
                OneOff(name, kcal, distanceMi, reps, activityId)
            }

        fun encodeOneOffs(list: List<OneOff>): String =
            list.filter { it.name.isNotBlank() }.joinToString(ONEOFF_RECORD.toString()) { o ->
                // Strip delimiter chars from the name so the encoding stays unambiguous.
                val safe = o.name.replace(ONEOFF_UNIT, ' ').replace(ONEOFF_RECORD, ' ').trim()
                "$safe$ONEOFF_UNIT${o.kcal}$ONEOFF_UNIT${o.distanceMi}$ONEOFF_UNIT${o.reps}" +
                    "$ONEOFF_UNIT${o.activityId}"
            }
    }
}
