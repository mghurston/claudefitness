package com.mhurston.ascendant

import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.data.WorkoutDayEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ExporterCsvTest {

    @Test
    fun csv_countsVariantsAndTrackedWalking() {
        val day = WorkoutDayEntity(
            date = "2026-07-10",
            pushups = 50,
            pushVariants = "inclinepush:30",
            legLifts = 40,
            coreVariants = "crunches:60",
            miles = 1.0,
            passiveSteps = 8000 // ≈ 4.0 tracked miles → totalmiles 5.00
        )
        val lines = Exporter.toCsv(listOf(day)).trim().lines()
        assertEquals(
            "date,pushups,squats,leglifts,calfraises,curls,miles,completion,steps,totalmiles",
            lines[0]
        )
        val cols = lines[1].split(",")
        assertEquals("pushups include variants", "80", cols[1])
        assertEquals("leglifts include core variants", "100", cols[3])
        assertEquals("steps exported", "8000", cols[8])
        assertEquals("totalmiles = manual + tracked", "5.00", cols[9])
    }
}
