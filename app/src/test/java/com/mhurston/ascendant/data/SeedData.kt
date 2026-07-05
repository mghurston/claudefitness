package com.mhurston.ascendant.data

/**
 * The real 30-day history (docs/assets/seed_history.csv) — TEST FIXTURE ONLY. First-launch
 * seeding was removed from the app (fresh installs start empty at Level 1); this dataset
 * lives on in test sources as the reference log the progression engine is tuned against.
 * Columns: date, pushups, squats, legLifts, calfRaises, curls, miles
 */
object SeedData {
    private val rows = listOf(
        Row("2025-06-01", 50, 50, 50, 50, 30, 1.5),
        Row("2025-06-02", 100, 100, 100, 100, 100, 3.0),
        Row("2025-06-03", 100, 100, 100, 100, 100, 4.0),
        Row("2025-06-04", 50, 100, 100, 100, 100, 4.0),
        Row("2025-06-05", 100, 100, 100, 100, 100, 1.5),
        Row("2025-06-06", 0, 0, 0, 0, 0, 3.0),
        Row("2025-06-07", 100, 100, 100, 100, 100, 6.0),
        Row("2025-06-08", 100, 100, 100, 100, 100, 4.0),
        Row("2025-06-09", 30, 10, 100, 100, 30, 4.0),
        Row("2025-06-10", 50, 50, 50, 50, 0, 3.0),
        Row("2025-06-11", 0, 0, 0, 0, 0, 5.0),
        Row("2025-06-12", 50, 100, 100, 120, 120, 3.0),
        Row("2025-06-13", 70, 60, 100, 100, 100, 3.0),
        Row("2025-06-14", 0, 0, 0, 0, 0, 6.0),
        Row("2025-06-15", 0, 0, 0, 0, 0, 4.0),
        Row("2025-06-16", 50, 30, 100, 100, 100, 3.0),
        Row("2025-06-17", 30, 30, 100, 30, 0, 4.0),
        Row("2025-06-18", 10, 10, 10, 10, 30, 4.0),
        Row("2025-06-19", 110, 110, 110, 110, 110, 4.0),
        Row("2025-06-20", 30, 30, 30, 30, 30, 4.0),
        Row("2025-06-21", 0, 0, 0, 0, 0, 5.0),
        Row("2025-06-22", 30, 30, 100, 100, 100, 1.5),
        Row("2025-06-23", 30, 30, 30, 100, 100, 5.0),
        Row("2025-06-24", 110, 110, 110, 110, 110, 5.5),
        Row("2025-06-25", 0, 0, 0, 0, 0, 4.0),
        Row("2025-06-26", 0, 0, 0, 0, 0, 5.0),
        Row("2025-06-27", 0, 0, 0, 0, 0, 3.0),
        Row("2025-06-28", 0, 0, 0, 0, 0, 5.0),
        Row("2025-06-29", 110, 110, 110, 110, 110, 3.0),
        Row("2025-06-30", 100, 100, 100, 100, 100, 5.0),
    )

    private data class Row(
        val date: String, val pushups: Int, val squats: Int, val legLifts: Int,
        val calfRaises: Int, val curls: Int, val miles: Double
    )

    fun entities(): List<WorkoutDayEntity> = rows.map {
        WorkoutDayEntity(
            date = it.date,
            pushups = it.pushups,
            squats = it.squats,
            legLifts = it.legLifts,
            calfRaises = it.calfRaises,
            curls = it.curls,
            miles = it.miles
        )
    }
}
