package com.mhurston.ascendant

import com.mhurston.ascendant.data.Exporter
import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.CardioIntensity
import com.mhurston.ascendant.domain.CardioMode
import com.mhurston.ascendant.domain.CardioRate
import com.mhurston.ascendant.domain.CustomExercise
import com.mhurston.ascendant.domain.ExerciseGoal
import com.mhurston.ascendant.domain.OneOff
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Sex
import com.mhurston.ascendant.domain.UnitSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Backup export → restore, through the real JSON parser.
 *
 * This is the path that loses data silently: a field the writer emits but the reader forgets
 * (or the other way round) costs logged days with no error anywhere. It used to be untestable
 * off a device because android.jar's org.json stubs throw "not mocked"; the test classpath now
 * carries a real org.json so the round trip runs on the JVM.
 */
class BackupRoundTripTest {

    private val profile = Profile(sex = Sex.MALE, age = 45, heightCm = 180.0,
        weightKg = 94.3, goalWeightKg = 88.0, startWeightKg = 100.0)

    /** One day with something in every column that can hold a value. */
    private val day = WorkoutDayEntity(
        date = "2026-08-05",
        pushups = 40, squats = 30, legLifts = 20, calfRaises = 10, curls = 50,
        miles = 2.5,
        caloriesConsumed = 2100,
        weightKg = 94.3,
        isRestDay = false,
        notes = "felt strong, \"quoted\" text and a \\ backslash",
        customReps = "c1:60",
        pushVariants = "dips:15",
        coreVariants = "situps:25",
        cardioMinutes = "bike:30",
        customDistance = "c2:250",
        customMinutes = "c3:45",
        oneOffs = WorkoutDayEntity.encodeOneOffs(
            listOf(OneOff("Trail run", kcal = 600, distanceMi = 5.0, activityId = "RUN"))
        ),
        passiveSteps = 8000,
        passiveKcal = 320
    )

    private val exercises = listOf(
        CustomExercise("c1", "overhead press", goal = ExerciseGoal.UPPER),
        CustomExercise("c2", "rower", goal = ExerciseGoal.CARDIO,
            cardioMode = CardioMode.DISTANCE, cardioRate = CardioRate.BIKE),
        CustomExercise("c3", "elliptical", goal = ExerciseGoal.CARDIO,
            cardioMode = CardioMode.MINUTES, cardioIntensity = CardioIntensity.HARD),
        CustomExercise("c4", "chest press", archived = true)
    )

    private fun roundTrip() = Exporter.fromJson(
        Exporter.toJson(listOf(day), profile, exportedAt = "2026-08-06T12:00:00Z",
            customExercises = exercises, unitSystem = UnitSystem.IMPERIAL)
    )

    @Test
    fun everyDayColumnSurvives() {
        assertEquals("the day itself must round-trip byte for byte", day, roundTrip().days.single())
    }

    @Test
    fun everyExerciseSettingSurvives() {
        val back = roundTrip().customExercises.associateBy { it.id }
        assertEquals("all four definitions restored", 4, back.size)
        exercises.forEach { assertEquals("definition ${it.id}", it, back[it.id]) }
        // Spelled out, because these three are what decide a cardio exercise's calories.
        assertEquals(CardioMode.DISTANCE, back["c2"]!!.cardioMode)
        assertEquals(CardioRate.BIKE, back["c2"]!!.cardioRate)
        assertEquals(CardioMode.MINUTES, back["c3"]!!.cardioMode)
        assertEquals(CardioIntensity.HARD, back["c3"]!!.cardioIntensity)
        assertTrue("archived flag survives", back["c4"]!!.archived)
    }

    @Test
    fun profileSurvives() {
        assertEquals(profile, roundTrip().profile)
    }

    @Test
    fun aRestoredDayScoresIdentically() {
        // The real check: same completion, same burn, same cardio credit after the round trip.
        val specs = exercises.associateBy { it.id }
        val before = day.toDayData(specs)
        val after = roundTrip().days.single().toDayData(specs)
        assertEquals(before, after)
        assertEquals(
            com.mhurston.ascendant.domain.Progression.completion(before, profile),
            com.mhurston.ascendant.domain.Progression.completion(after, profile),
            0.0
        )
        assertEquals(
            com.mhurston.ascendant.domain.Calories.activityBurn(profile, before),
            com.mhurston.ascendant.domain.Calories.activityBurn(profile, after),
            0.0
        )
    }

    @Test
    fun olderBackupsStillRestore() {
        // A schema-3 backup: no cardio columns on the day, per-slot goals on the exercises.
        val old = """
            {"schema": 3, "exportedAt": "2026-07-01T00:00:00Z",
             "profile": {"sex": "MALE", "age": 45, "heightCm": 180.0, "weightKg": 94.3,
                         "goalWeightKg": 0.0, "startWeightKg": 0.0},
             "customExercises": [
               {"id": "c1", "name": "overhead press", "archived": false, "goal": "PUSH"},
               {"id": "c2", "name": "bicycle kicks", "archived": false, "goal": "CALF_RAISES"}],
             "days": [
               {"date": "2026-07-01", "pushups": 10, "squats": 0, "legLifts": 0,
                "calfRaises": 0, "curls": 0, "miles": 1.0, "caloriesConsumed": 2000,
                "customReps": "c1:20", "notes": ""}]}
        """.trimIndent()
        val back = Exporter.fromJson(old)
        val byId = back.customExercises.associateBy { it.id }
        assertEquals("PUSH folds into its category", ExerciseGoal.UPPER, byId["c1"]!!.goal)
        assertEquals(ExerciseGoal.LOWER, byId["c2"]!!.goal)
        assertEquals("cardio settings take their defaults",
            CardioMode.DISTANCE, byId["c1"]!!.cardioMode)
        val d = back.days.single()
        assertEquals("", d.customDistance)
        assertEquals("", d.customMinutes)
        assertEquals(1.0, d.miles, 1e-9)
        assertEquals("c1:20", d.customReps)
    }

    @Test
    fun schemaOneZeroCaloriesStillMeanNotLogged() {
        // Schema 1 wrote 0 for "no food logged"; the current model spells that -1, and 0 is a
        // deliberate fasting day. Getting this backwards would invent a fast on every old day.
        val v1 = """
            {"schema": 1, "days": [{"date": "2026-06-01", "pushups": 0, "squats": 0,
             "legLifts": 0, "calfRaises": 0, "curls": 0, "miles": 0.0, "caloriesConsumed": 0}]}
        """.trimIndent()
        assertEquals(-1, Exporter.fromJson(v1).days.single().caloriesConsumed)
        val v5 = """
            {"schema": 5, "days": [{"date": "2026-06-01", "pushups": 0, "squats": 0,
             "legLifts": 0, "calfRaises": 0, "curls": 0, "miles": 0.0, "caloriesConsumed": 0}]}
        """.trimIndent()
        assertEquals("but a current backup's 0 is a real fasting day",
            0, Exporter.fromJson(v5).days.single().caloriesConsumed)
    }

    @Test
    fun controlCharactersInOneOffsSurvive() {
        // One-offs are encoded with U+001F / U+001E delimiters, which are illegal raw in JSON.
        // If the escaping regressed, every one-off name would come back mangled or the parse
        // would throw outright.
        val back = roundTrip().days.single()
        val offs = WorkoutDayEntity.decodeOneOffs(back.oneOffs)
        assertEquals(1, offs.size)
        assertEquals("Trail run", offs[0].name)
        assertEquals(600, offs[0].kcal)
        assertEquals(5.0, offs[0].distanceMi, 1e-9)
        assertEquals("RUN", offs[0].activityId)
    }
}
