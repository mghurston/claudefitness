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
    val notes: String = ""
) {
    fun toDayData(): DayData = DayData(
        date = LocalDate.parse(date),
        pushups = pushups,
        squats = squats,
        legLifts = legLifts,
        calfRaises = calfRaises,
        curls = curls,
        miles = miles,
        isRestDay = isRestDay
    )
}
