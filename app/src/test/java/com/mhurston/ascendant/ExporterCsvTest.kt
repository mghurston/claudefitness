package com.mhurston.ascendant

import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.Profile
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
        val lines = Exporter.toCsv(listOf(day), profile = Profile(weightKg = 80.0)).trim().lines()
        assertEquals(
            "date,pushups,squats,leglifts,calfraises,curls,miles,completion,steps,totalmiles," +
                "cardiokcal,cardiotarget",
            lines[0]
        )
        val cols = lines[1].split(",")
        assertEquals("pushups include variants", "80", cols[1])
        assertEquals("leglifts include core variants", "100", cols[3])
        assertEquals("steps exported", "8000", cols[8])
        assertEquals("totalmiles = manual + tracked", "5.00", cols[9])
        // Cardio is a calorie goal: 5 walked miles at 1.2 kcal/kg/mi on an 80 kg body is
        // 480 kcal, which is exactly the target (6 kcal per kg).
        assertEquals("cardio calories", "480", cols[10])
        assertEquals("target = what walking 5 miles burns", "480", cols[11])
    }
}
