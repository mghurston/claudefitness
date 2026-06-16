package com.mhurston.ascendant.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mhurston.ascendant.domain.DayData
import java.time.LocalDate

/**
 * One logged day. Denormalized to match the original spreadsheet row exactly — the
 * completion formula and XP engine read precisely these columns. (Custom exercises
 * are a future extension per the design docs.)
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
    val caloriesConsumed: Int = 0,
    val isRestDay: Boolean = false,
    val notes: String = "",
    /** 0 = unset, 1 (drained) .. 5 (unstoppable). Journaling only — never affects XP. */
    val mood: Int = 0,
    /** Supplementary custom-exercise reps, encoded "id:reps,id:reps". Bonus XP only. */
    val customReps: String = "",
    /** Reps for the Push-ups alternatives (everything except the base Pushups column),
     *  encoded "id:reps,id:reps". These sum 1:1 into the push-ups total. See PushExercise. */
    val pushVariants: String = ""
) {
    fun toDayData(): DayData = DayData(
        date = LocalDate.parse(date),
        pushups = pushTotal(),
        squats = squats,
        legLifts = legLifts,
        calfRaises = calfRaises,
        curls = curls,
        miles = miles,
        isRestDay = isRestDay,
        notes = notes,
        mood = mood,
        customReps = decodeCustomReps(customReps)
    )

    /** Total push-ups reps across every variant (base column + alternatives) — what counts
     *  toward the push-ups goal, XP, and stats. */
    fun pushTotal(): Int = pushups + decodeCustomReps(pushVariants).values.sum()

    /** Reps for each push-up variant by PushExercise.id, including the base Pushups column.
     *  Only non-zero entries are present. */
    fun pushBreakdown(): Map<String, Int> = buildMap {
        if (pushups != 0) put(com.mhurston.ascendant.domain.PushExercise.PUSHUPS.id, pushups)
        putAll(decodeCustomReps(pushVariants))
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
    }
}
