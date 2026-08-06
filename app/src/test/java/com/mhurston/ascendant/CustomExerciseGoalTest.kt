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
 * A pinned custom exercise can be pointed at one of the daily goals, and then its reps fill that
 * goal exactly like a built-in variant. Unassigned (NONE) customs keep the original behavior:
 * calories only, no effect on completion or stats.
 */
class CustomExerciseGoalTest {

    private val profile = Profile(weightKg = 90.0, heightCm = 180.0, age = 45)

    /** 100 reps of one custom exercise and nothing else. */
    private fun day(id: String = "c1", reps: Int = 100) =
        WorkoutDayEntity(date = "2026-08-05", customReps = "$id:$reps")

    @Test
    fun assignedCustom_fillsItsGoal_unassignedDoesNot() {
        val e = day()
        assertEquals("unassigned: no goal credit", 0, e.toDayData().pushups)
        assertEquals("assigned: fills the push goal",
            100, e.toDayData(mapOf("c1" to ExerciseGoal.PUSH)).pushups)
        assertEquals("assigned: fills the core goal",
            100, e.toDayData(mapOf("c1" to ExerciseGoal.CORE)).legLifts)
        assertEquals("assigned: fills the curls goal",
            100, e.toDayData(mapOf("c1" to ExerciseGoal.CURLS)).curls)
        assertEquals("assigned: fills the squats goal",
            100, e.toDayData(mapOf("c1" to ExerciseGoal.SQUATS)).squats)
        assertEquals("assigned: fills the calf-raise goal",
            100, e.toDayData(mapOf("c1" to ExerciseGoal.CALF_RAISES)).calfRaises)
    }

    @Test
    fun assignedCustom_raisesCompletion_byExactlyOneSixth() {
        val e = day()
        val before = Progression.completion(e.toDayData())
        val after = Progression.completion(e.toDayData(mapOf("c1" to ExerciseGoal.PUSH)))
        assertEquals("no goal credit before assignment", 0.0, before, 1e-9)
        // One of six goals filled to 100 % = 1/6 of the day.
        assertEquals(1.0 / 6.0, after, 1e-9)
    }

    @Test
    fun assignedCustom_doesNotChangeCalories() {
        val e = day()
        val loose = Calories.activityBurn(profile, e.toDayData())
        val assigned = Calories.activityBurn(profile, e.toDayData(mapOf("c1" to ExerciseGoal.PUSH)))
        // The reps move from customRepsTotal into strengthReps — same per-rep burn, counted once.
        assertEquals("burn is identical either way", loose, assigned, 1e-9)
        assertTrue("and it is nonzero, so the equality is not two zeros", loose > 0.0)
    }

    @Test
    fun assignedCustom_isRemovedFromTheBurnOnlyMap() {
        val e = day()
        assertEquals(mapOf("c1" to 100), e.toDayData().customReps)
        assertEquals("credited reps must not stay in customReps (would double count)",
            emptyMap<String, Int>(), e.toDayData(mapOf("c1" to ExerciseGoal.PUSH)).customReps)
    }

    @Test
    fun onlyTheAssignedCustomIsCredited_othersStayExtraOnly() {
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100,c2:50")
        val d = e.toDayData(mapOf("c1" to ExerciseGoal.PUSH))
        assertEquals(100, d.pushups)
        assertEquals(mapOf("c2" to 50), d.customReps)
    }

    @Test
    fun assignedCustom_feedsStats() {
        val e = day(reps = 200)
        val (loose, _) = Progression.rebuild(listOf(e.toDayData()), profile = profile)
        val (assigned, _) = Progression.rebuild(
            listOf(e.toDayData(mapOf("c1" to ExerciseGoal.PUSH))), profile = profile
        )
        assertEquals("extra-only reps build no STR", 0, loose.stats.strength)
        assertEquals("credited reps build STR like push-ups (floor(sqrt(200/50)) = 2)",
            2, assigned.stats.strength)
        assertEquals("XP is unchanged — calories are the same reps either way",
            loose.earnedXp, assigned.earnedXp)
    }

    @Test
    fun goalCreditStacksOnTopOfTheBuiltInColumn() {
        val e = WorkoutDayEntity(date = "2026-08-05", pushups = 40, pushVariants = "dips:10",
            customReps = "c1:50")
        assertEquals("base + variant only", 50, e.pushTotal())
        assertEquals("base + variant + credited custom",
            100, e.pushTotal(mapOf("c1" to ExerciseGoal.PUSH)))
    }

    @Test
    fun csvExport_scoresCreditedCustoms() {
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100")
        val plain = Exporter.toCsv(listOf(e)).trim().lines()[1].split(",")
        val credited = Exporter.toCsv(listOf(e), mapOf("c1" to ExerciseGoal.CURLS))
            .trim().lines()[1].split(",")
        assertEquals("curls column, no assignment", "0", plain[5])
        assertEquals("curls column picks up the credited custom", "100", credited[5])
        assertEquals("completion follows", "0.1667", credited[7])
    }

    @Test
    fun archivedAndUnknownIdsAreSafe() {
        val e = day()
        // An id with no entry in the map (e.g. a definition deleted before this build) is
        // extra-only, exactly as it was.
        assertEquals(0, e.toDayData(mapOf("other" to ExerciseGoal.PUSH)).pushups)
        assertEquals(0, e.toDayData(mapOf("c1" to ExerciseGoal.NONE)).pushups)
        assertEquals(mapOf("c1" to 100), e.toDayData(mapOf("c1" to ExerciseGoal.NONE)).customReps)
    }
}
