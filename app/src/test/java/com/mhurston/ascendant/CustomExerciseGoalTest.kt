package com.mhurston.ascendant

import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.Calories
import com.mhurston.ascendant.domain.CardioIntensity
import com.mhurston.ascendant.domain.CardioMode
import com.mhurston.ascendant.domain.CardioRate
import com.mhurston.ascendant.domain.CustomExercise
import com.mhurston.ascendant.domain.ExerciseGoal
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A pinned custom exercise can be pointed at one of the four training categories, and then its
 * work fills that category's goal exactly like a built-in exercise. Upper and Lower cover two
 * 100-rep slots each, so their reps split across the pair; Core is a single slot. Cardio is
 * scored in calories, so an exercise pointed at it is logged as distance or minutes and counts
 * for what that actually burns. Unassigned (NONE) customs keep the original behavior: calories
 * only, no effect on completion or stats.
 */
class CustomExerciseGoalTest {

    private val profile = Profile(weightKg = 90.0, heightCm = 180.0, age = 45)

    /** 100 reps of one custom exercise and nothing else. */
    private fun day(id: String = "c1", reps: Int = 100) =
        WorkoutDayEntity(date = "2026-08-05", customReps = "$id:$reps")

    /** The scoring map for a single exercise "c1" pointed at [goal]. */
    private fun spec(
        goal: ExerciseGoal,
        mode: CardioMode = CardioMode.DISTANCE,
        rate: CardioRate = CardioRate.WALK,
        intensity: CardioIntensity = CardioIntensity.MODERATE
    ): Map<String, CustomExercise> = mapOf(
        "c1" to CustomExercise("c1", "test exercise", goal = goal, cardioMode = mode,
            cardioRate = rate, cardioIntensity = intensity)
    )

    @Test
    fun assignedCustom_fillsItsCategory_unassignedDoesNot() {
        val e = day()
        assertEquals("unassigned: no goal credit", 0, e.toDayData().pushups)
        val upper = e.toDayData(spec(ExerciseGoal.UPPER))
        assertEquals("Upper splits across push-ups and curls", 50, upper.pushups)
        assertEquals(50, upper.curls)
        val core = e.toDayData(spec(ExerciseGoal.CORE))
        assertEquals("Core is one slot, so it takes the full amount", 100, core.legLifts)
        val lower = e.toDayData(spec(ExerciseGoal.LOWER))
        assertEquals("Lower splits across squats and calf raises", 50, lower.squats)
        assertEquals(50, lower.calfRaises)
    }

    @Test
    fun oddRepsSplitWithoutLosingOrInventingOne() {
        val e = day(reps = 101)
        val d = e.toDayData(spec(ExerciseGoal.UPPER))
        assertEquals("odd rep goes to the first slot", 51, d.pushups)
        assertEquals(50, d.curls)
        assertEquals("the halves sum back to what was logged", 101, d.pushups + d.curls)
    }

    @Test
    fun oneHundredReps_areOneSixthOfTheDay_inEveryRepCategory() {
        val e = day()
        assertEquals("no goal credit before assignment", 0.0,
            Progression.completion(e.toDayData(), profile), 1e-9)
        // A two-slot category is worth 200 reps, so 100 reps fills half of each of its two
        // slots — the same 1/6 of the day that 100 reps into single-slot Core is worth.
        listOf(ExerciseGoal.UPPER, ExerciseGoal.CORE, ExerciseGoal.LOWER).forEach { goal ->
            assertEquals("$goal", 1.0 / 6.0,
                Progression.completion(e.toDayData(spec(goal)), profile), 1e-9)
        }
    }

    // --- Cardio: a calorie goal, so each kind of cardio counts for what it burns -----------

    @Test
    fun walkingFiveMiles_stillFillsTheCardioGoalExactly() {
        // The whole point of the calorie target: it is set to what the 5-mile walking goal
        // burns, so walking has not changed at all — at any body weight.
        listOf(60.0, 90.7, 120.0).forEach { kg ->
            val p = profile.copy(weightKg = kg)
            val d = WorkoutDayEntity(date = "2026-08-05", miles = 5.0).toDayData()
            assertEquals("at $kg kg", 1.0, Calories.cardioFraction(p, d), 1e-9)
        }
    }

    @Test
    fun cardioTarget_isSixKcalPerKg() {
        // 1.2 kcal/kg/mi x 5 mi. For a 90.7 kg (200 lb) body that is ~544 kcal.
        val d = WorkoutDayEntity(date = "2026-08-05").toDayData()
        assertEquals(544.0, Calories.cardioTarget(profile.copy(weightKg = 90.7), d), 1.0)
    }

    @Test
    fun cycledMilesAreWorthLessThanWalkedOnes() {
        // Five miles on a bike is ~204 kcal against ~544 walked, so it fills 37.5% of the goal,
        // not 100%. This is the bug the calorie goal exists to fix.
        val e = WorkoutDayEntity(date = "2026-08-05", customDistance = "c1:500")
        val onBike = e.toDayData(spec(ExerciseGoal.CARDIO, rate = CardioRate.BIKE))
        assertEquals(Calories.BIKE_KCAL_PER_KG_PER_MILE / Calories.WALK_KCAL_PER_KG_PER_MILE,
            Calories.cardioFraction(profile, onBike), 1e-9)
        val onFoot = e.toDayData(spec(ExerciseGoal.CARDIO, rate = CardioRate.WALK))
        assertEquals("the same five miles walked fills it",
            1.0, Calories.cardioFraction(profile, onFoot), 1e-9)
        assertTrue("and walking burns more than cycling for the distance",
            Calories.activityBurn(profile, onFoot) > Calories.activityBurn(profile, onBike))
    }

    @Test
    fun cardioCustomMiles_burnAtTheirOwnRate() {
        val e = WorkoutDayEntity(date = "2026-08-05", customDistance = "c1:250")
        val cycled = Calories.activityBurn(
            profile, e.toDayData(spec(ExerciseGoal.CARDIO, rate = CardioRate.BIKE)))
        // 2.5 mi at 0.45 kcal/kg/mi on a 90 kg body.
        assertEquals(0.45 * 90.0 * 2.5, cycled, 1e-9)
        assertEquals("unassigned, the distance burns nothing",
            0.0, Calories.activityBurn(profile, e.toDayData()), 1e-9)
    }

    @Test
    fun cardioCustomMinutes_burnByMet_andWeightCancelsFromTheGoal() {
        // 60 minutes at MODERATE (MET 6): 6 x 3.5 x 90 / 200 = 9.45 kcal/min → 567 kcal.
        val e = WorkoutDayEntity(date = "2026-08-05", customMinutes = "c1:60")
        val d = e.toDayData(spec(ExerciseGoal.CARDIO, mode = CardioMode.MINUTES))
        assertEquals(6.0 * 3.5 * 90.0 / 200.0 * 60, Calories.activityBurn(profile, d), 1e-9)
        // Body weight scales the burn and the target identically, so the fraction is the same
        // for anyone — an hour of moderate work is the same share of the goal at any weight.
        val heavy = profile.copy(weightKg = 120.0)
        assertEquals(
            Calories.cardioFraction(profile, d),
            Calories.cardioFraction(heavy, e.toDayData(
                spec(ExerciseGoal.CARDIO, mode = CardioMode.MINUTES)).copy(weightKg = 120.0)),
            1e-9
        )
    }

    @Test
    fun cardioCustomMiles_neverCountAsWalkedMiles() {
        // Walking achievements, lifetime miles and the END stat all read walkMiles. A rowed or
        // cycled mile must not appear there, however many calories it was worth.
        val e = WorkoutDayEntity(date = "2026-08-05", customDistance = "c1:500")
        val d = e.toDayData(spec(ExerciseGoal.CARDIO, rate = CardioRate.BIKE))
        assertEquals("no walked miles", 0.0, d.walkMiles, 1e-9)
        // 5 cycled miles are worth 5 x 0.45/1.2 = 1.875 walked ones.
        assertEquals("but the day did do cardio", 5.0 * 0.45 / 1.2, d.cardioEquivMiles, 1e-9)
        assertTrue("and it counts as an active day", d.hasActivity)
    }

    @Test
    fun bikeAndSwimMinutes_nowFillTheCardioGoal() {
        // They always burned calories; before the calorie goal they moved completion by zero.
        val e = WorkoutDayEntity(date = "2026-08-05", cardioMinutes = "bike:30")
        val d = e.toDayData()
        assertTrue("30 min of cycling is a real share of the goal",
            Calories.cardioFraction(profile, d) > 0.5)
        assertEquals("and it is exactly its burn over the target",
            Calories.activityBurn(profile, d) / Calories.cardioTarget(profile, d),
            Calories.cardioFraction(profile, d), 1e-9)
    }

    @Test
    fun repsNeverCountAsCardio() {
        // Strength has its own five goals; letting reps fill cardio too would pay them twice.
        val e = WorkoutDayEntity(date = "2026-08-05", pushups = 100, squats = 100,
            customReps = "c1:200")
        val d = e.toDayData()
        assertTrue("the reps burn calories", Calories.activityBurn(profile, d) > 0.0)
        assertEquals("but none of it is cardio", 0.0, Calories.cardioKcal(profile, d), 1e-9)
    }

    @Test
    fun repsOnACardioAssignedExercise_stillEarnTheirCalories() {
        // Reps logged before the exercise was pointed at Cardio must not silently vanish:
        // they no longer fill a rep goal, but they keep burning.
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100")
        val d = e.toDayData(spec(ExerciseGoal.CARDIO))
        assertEquals("reps stay in the burn-only map", mapOf("c1" to 100), d.customReps)
        assertEquals("and fill no rep goal", 0, d.pushups + d.curls + d.legLifts)
    }

    @Test
    fun assignedCustom_doesNotChangeCalories() {
        val e = day()
        val loose = Calories.activityBurn(profile, e.toDayData())
        val assigned = Calories.activityBurn(profile, e.toDayData(spec(ExerciseGoal.UPPER)))
        // The reps move from customRepsTotal into strengthReps — same per-rep burn, counted once.
        assertEquals("burn is identical either way", loose, assigned, 1e-9)
        assertTrue("and it is nonzero, so the equality is not two zeros", loose > 0.0)
    }

    @Test
    fun assignedCustom_isRemovedFromTheBurnOnlyMap() {
        val e = day()
        assertEquals(mapOf("c1" to 100), e.toDayData().customReps)
        assertEquals("credited reps must not stay in customReps (would double count)",
            emptyMap<String, Int>(), e.toDayData(spec(ExerciseGoal.UPPER)).customReps)
    }

    @Test
    fun onlyTheAssignedCustomIsCredited_othersStayExtraOnly() {
        val e = WorkoutDayEntity(date = "2026-08-05", customReps = "c1:100,c2:50")
        val d = e.toDayData(spec(ExerciseGoal.UPPER))
        assertEquals(50, d.pushups)
        assertEquals(50, d.curls)
        assertEquals(mapOf("c2" to 50), d.customReps)
    }

    @Test
    fun assignedCustom_feedsStats() {
        val e = day(reps = 400)
        val (loose, _) = Progression.rebuild(listOf(e.toDayData()), profile = profile)
        val (assigned, _) = Progression.rebuild(
            listOf(e.toDayData(spec(ExerciseGoal.UPPER))), profile = profile
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
            50, e.customRepsCredited(ExerciseGoal.UPPER, spec(ExerciseGoal.UPPER)))
        val d = e.toDayData(spec(ExerciseGoal.UPPER))
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
        val credited = Exporter.toCsv(listOf(e), spec(ExerciseGoal.UPPER), profile)
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
        val otherId = mapOf("other" to CustomExercise("other", "x", goal = ExerciseGoal.UPPER))
        assertEquals(0, e.toDayData(otherId).pushups)
        assertEquals(0, e.toDayData(spec(ExerciseGoal.NONE)).pushups)
        assertEquals(mapOf("c1" to 100), e.toDayData(spec(ExerciseGoal.NONE)).customReps)
    }
}
