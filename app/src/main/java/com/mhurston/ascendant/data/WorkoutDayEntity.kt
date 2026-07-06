package com.mhurston.ascendant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mhurston.ascendant.domain.DayData
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
    /** Steps banked from Health Connect for this day (overwritten on each sync, never summed). */
    val passiveSteps: Int = 0,
    /** Active calories banked from Health Connect for this day (preferred passive kcal source). */
    val passiveKcal: Int = 0
) {
    fun toDayData(): DayData = DayData(
        date = LocalDate.parse(date),
        pushups = pushTotal(),
        squats = squats,
        legLifts = coreTotal(),
        calfRaises = calfRaises,
        curls = curls,
        miles = miles,
        caloriesConsumed = caloriesConsumed,
        weightKg = weightKg,
        isRestDay = isRestDay,
        notes = notes,
        customReps = decodeCustomReps(customReps),
        cardioMinutes = decodeCustomReps(cardioMinutes),
        oneOffs = decodeOneOffs(oneOffs),
        passiveSteps = passiveSteps,
        passiveKcal = passiveKcal
    )

    /** Distance estimated from passive steps (~2000 steps/mi) — the "tracked walked" part of
     *  walking. Mirrors DayData.trackedMiles so the UI can read it off the entity directly. */
    val trackedMiles: Double
        get() = passiveSteps.coerceAtLeast(0) / com.mhurston.ascendant.domain.Calories.STEPS_PER_MILE

    /** Total walking toward the 5-mi goal = manual/treadmill miles + tracked. */
    val walkMiles: Double get() = miles + trackedMiles

    /** Total push-ups reps across every variant (base column + alternatives) — what counts
     *  toward the push-ups goal, XP, and stats. */
    fun pushTotal(): Int = pushups + decodeCustomReps(pushVariants).values.sum()

    /** Reps for each push-up variant by PushExercise.id, including the base Pushups column.
     *  Only non-zero entries are present. */
    fun pushBreakdown(): Map<String, Int> = buildMap {
        if (pushups != 0) put(com.mhurston.ascendant.domain.PushExercise.PUSHUPS.id, pushups)
        putAll(decodeCustomReps(pushVariants))
    }

    /** Total core reps across every variant (legLifts column + alternatives) — what counts
     *  toward the core goal, completion, XP, and stats. */
    fun coreTotal(): Int = legLifts + decodeCustomReps(coreVariants).values.sum()

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
