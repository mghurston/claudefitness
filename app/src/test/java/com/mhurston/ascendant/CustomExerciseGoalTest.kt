package com.mhurston.ascendant

import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.Calories
import com.mhurston.ascendant.domain.ExerciseGoal
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A pinned custom exercise can be pointed at one of the four training categories, and then its
 * work fills that category's goal exactly like a built-in exercise. Upper and Lower cover two
 * 100-rep slots each, so their reps split across the pair; Core is a single slot; Cardio is
 * measured in miles, so an exercise pointed at it is logged as distance. Unassigned (NONE)
 * customs keep the original behavior: calories only, no effect on completion or stats.
 */
class CustomExerciseGoalTest {

    private val profile = Profile(weightKg = 90.0, heightCm = 180.0, age = 45)

    /** 100 reps of one custom exercise and nothing else. */
    private fun day(id: String = "c1", reps: Int = 100) =
        WorkoutDayEntity(date = "2026-08-05", customReps = "$id:$reps")

    @Test
    fun assignedCustom_fillsItsCategory_unassignedDoesNot() {
        val e = day()
        assertEquals("unassigned: no goal credit", 0, e.toDayData().pushups)
        val upper = e.toDayData(mapOf("c1" to ExerciseGoal.UPPER))
        assertEquals("Upper splits across push-ups and curls", 50, upper.pushups)
        assertEquals(50, upper.curls)
        val core = e.toDayData(mapOf("c1" to ExerciseGoal.CORE))
        assertEquals("Core is one slot, so it takes the full amount", 100, core.legLifts)
        val lower = e.toDayData(mapOf("c1" to ExerciseGoal.LOWER))
        assertEquals("Lower splits across squats and calf raises", 50, lower.squats)
        assertEquals(50, lower.calfRaises)
    }

    @Test
    fun oddRepsSplitWithoutLosingOrInventingOne() {
        val e = day(reps = 101)
        val d = e.toDayData(mapOf("c1" to ExerciseGoal.UPPER))
        assertEquals("odd rep goes to the first slot", 51, d.pushups)
        assertEquals(50, d.curls)
        assertEquals("the halves sum back to what was logged", 101, d.pushups + d.curls)
    }

    @Test
    fun oneHundredReps_areOneSixthOfTheDay_inEveryRepCategory() {
        val e = day()
        assertEquals("no goal credit before assignment", 0.0,
            Progression.completion(e.toDayData()), 1e-9)
        // A two-slot category is worth 200 reps, so 100 reps fills half of each of its two
        // slots — the same 1/6 of the day that 100 reps into single-slot Core is worth.
        listOf(ExerciseGoal.UPPER, ExerciseGoal.CORE, ExerciseGoal.LOWER).forEach { goal ->
            assertEquals("$goal", 1.0 / 6.0,
                Progression.completion(e.toDayData(mapOf("c1" to goal))), 1e-9)
        }
    }

    @Test
    fun cardioCustom_isLoggedInMiles_andFillsTheMileGoal() {
        // 2.50 mi, stored as hundredths in its own column.
        val e = WorkoutDayEntity(date = "2026-08-05", customDistance = "c1:250")
        assertEquals("unassigned: the distance scores nothing",
            0.0, e.toDayData().miles, 1e-9)
        val d = e.toDayData(mapOf("c1" to ExerciseGoal.CARDIO))
        assertEquals("distance lands on the walking miles", 2.5, d.miles, 1e-9)
        assertEquals("half of the 5-mile goal = half of one sixth of the day",
            0.5 / 6.0, Progression.completion(d), 1e-9)
        assertEquals("and the section header shows the same total",
            2.5, e.cardioMiles(mapOf("c1" to ExerciseGoal.CARDIO)), 1e-9)
    }

    @Test
    fun cardioCustomMiles_burnLikeWalkedMiles() {
        val e = WorkoutDayEntity(date = "2026-08-05", customDistance = "c1:250")
        val assigned = Calories.activityBurn(
            profile, e.toDayData(mapOf("c1" to ExerciseGoal.CARDIO)))
        val walked = Calories.activityBurn(
            profile, WorkoutDayEntity(date = "2026-08-05", miles = 2.5).toDayData())
        assertEquals("a Cardio custom's miles burn exactly like manual miles", walked, assigned, 1e-9)
        assertTrue("and that is a real, nonzero burn", walked > 0.0)
    }

    @Test
    fun repsOnACardioAssignedExercise_stillEarnTheirCalories() {
        // Reps logged before the exercise was pointed at Cardio must not silently vanish:
        // they no longer fill a rep goal, but they keep burning.
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100")
        val d = e.toDayData(mapOf("c1" to ExerciseGoal.CARDIO))
        assertEquals("reps stay in the burn-only map", mapOf("c1" to 100), d.customReps)
        assertEquals("and fill no rep goal", 0, d.pushups + d.curls + d.legLifts)
    }

    @Test
    fun assignedCustom_doesNotChangeCalories() {
        val e = day()
        val loose = Calories.activityBurn(profile, e.toDayData())
        val assigned = Calories.activityBurn(profile, e.toDayData(mapOf("c1" to ExerciseGoal.UPPER)))
        // The reps move from customRepsTotal into strengthReps — same per-rep burn, counted once.
        assertEquals("burn is identical either way", loose, assigned, 1e-9)
        assertTrue("and it is nonzero, so the equality is not two zeros", loose > 0.0)
    }

    @Test
    fun assignedCustom_isRemovedFromTheBurnOnlyMap() {
        val e = day()
        assertEquals(mapOf("c1" to 100), e.toDayData().customReps)
        assertEquals("credited reps must not stay in customReps (would double count)",
            emptyMap<String, Int>(), e.toDayData(mapOf("c1" to ExerciseGoal.UPPER)).customReps)
    }

    @Test
    fun onlyTheAssignedCustomIsCredited_othersStayExtraOnly() {
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100,c2:50")
        val d = e.toDayData(mapOf("c1" to ExerciseGoal.UPPER))
        assertEquals(50, d.pushups)
        assertEquals(50, d.curls)
        assertEquals(mapOf("c2" to 50), d.customReps)
    }

    @Test
    fun assignedCustom_feedsStats() {
        val e = day(reps = 400)
        val (loose, _) = Progression.rebuild(listOf(e.toDayData()), profile = profile)
        val (assigned, _) = Progression.rebuild(
            listOf(e.toDayData(mapOf("c1" to ExerciseGoal.UPPER))), profile = profile
        )
        assertEquals("extra-only reps build no STR", 0, loose.stats.strength)
        assertEquals("credited reps build STR like the built-ins (floor(sqrt(400/50)) = 2)",
            2, assigned.stats.strength)
        assertEquals("XP is unchanged — calories are the same reps either way",
            loose.earnedXp, assigned.earnedXp)
    }

    @Test
    fun categoryCreditStacksOnTopOfTheBuiltInColumns() {
        val e = WorkoutDayEntity(date = "2026-08-05", pushups = 40, pushVariants = "dips:10",
            curls = 20, customReps = "c1:50")
        assertEquals("the per-exercise line still shows only its own reps", 50, e.pushTotal())
        assertEquals("the category adds the custom on top",
            50, e.customRepsCredited(ExerciseGoal.UPPER, mapOf("c1" to ExerciseGoal.UPPER)))
        val d = e.toDayData(mapOf("c1" to ExerciseGoal.UPPER))
        assertEquals("built-in push + half the custom", 75, d.pushups)
        assertEquals("built-in curls + the other half", 45, d.curls)
    }

    @Test
    fun legacyPerSlotGoalsFoldIntoTheirCategory() {
        // Builds before the categories stored one goal per exercise slot. Those names must keep
        // resolving, or a v0.4.0/0.4.1 backup would restore every assignment as "extra only".
        assertEquals(ExerciseGoal.UPPER, ExerciseGoal.forName("PUSH"))
        assertEquals(ExerciseGoal.UPPER, ExerciseGoal.forName("CURLS"))
        assertEquals(ExerciseGoal.LOWER, ExerciseGoal.forName("SQUATS"))
        assertEquals(ExerciseGoal.LOWER, ExerciseGoal.forName("CALF_RAISES"))
        assertEquals(ExerciseGoal.CORE, ExerciseGoal.forName("CORE"))
        assertEquals("an unknown name is extra-only", ExerciseGoal.NONE, ExerciseGoal.forName("XYZ"))
    }

    @Test
    fun csvExport_scoresCreditedCustoms() {
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100")
        val plain = Exporter.toCsv(listOf(e)).trim().lines()[1].split(",")
        val credited = Exporter.toCsv(listOf(e), mapOf("c1" to ExerciseGoal.UPPER))
            .trim().lines()[1].split(",")
        assertEquals("curls column, no assignment", "0", plain[5])
        assertEquals("curls column picks up half the credited custom", "50", credited[5])
        assertEquals("push-ups column picks up the other half", "50", credited[1])
        assertEquals("completion follows", "0.1667", credited[7])
    }

    @Test
    fun jsonBackup_carriesCardioDistance() {
        // fromJson needs Android's org.json (not available to a JVM unit test), so this covers
        // the write side: a backup that omitted the column would silently drop logged miles.
        val e = WorkoutDayEntity(date = "2026-08-05", customDistance = "c1:250")
        val json = Exporter.toJson(listOf(e), profile, exportedAt = "2026-08-05T00:00:00Z")
        assertTrue("backup must carry the Cardio distance column\n$json",
            json.contains("\"customDistance\": \"c1:250\""))
    }

    @Test
    fun archivedAndUnknownIdsAreSafe() {
        val e = day()
        // An id with no entry in the map (e.g. a definition deleted before this build) is
        // extra-only, exactly as it was.
        assertEquals(0, e.toDayData(mapOf("other" to ExerciseGoal.UPPER)).pushups)
        assertEquals(0, e.toDayData(mapOf("c1" to ExerciseGoal.NONE)).pushups)
        assertEquals(mapOf("c1" to 100), e.toDayData(mapOf("c1" to ExerciseGoal.NONE)).customReps)
    }
}
