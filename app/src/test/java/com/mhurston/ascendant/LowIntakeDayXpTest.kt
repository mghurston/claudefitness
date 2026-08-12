package com.mhurston.ascendant

import com.mhurston.ascendant.domain.Calories
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The question this file exists to answer, asked 2026-08-11: what happens to a day with a
 * large calorie deficit and only PARTIAL training — does the training shortfall eat the
 * deficit, or does the day still gain?
 *
 * Answer, locked in below: it gains, decisively. For a 90.7 kg (200 lb) body the diet term is
 * worth ~5x the shortfall, so a 28.5%-completion day on 500 kcal is +1773 XP. That is the
 * approved model (docs/XP Simplification Spec.md — symmetric, UNCAPPED diet), not a bug: the
 * user was shown these exact numbers, was offered an intake floor, and declined it. If a change
 * makes this file fail, the model changed — go ask before "fixing" the test.
 *
 * To score a new what-if, add a DayData and a [scoreDay] call; the helper returns every
 * component so the arithmetic is inspectable rather than a single opaque number.
 */
class LowIntakeDayXpTest {

    private val kg = 200 * 0.45359237   // 200 lb
    private val cm = 72 * 2.54          // 6'0"
    private val profile = Profile(age = 30, heightCm = cm, weightKg = kg)

    private val today = LocalDate.parse("2026-08-11")
    private val yesterday = today.minusDays(1)
    private val anchor = today.minusDays(30)

    /** Every term of `dayXp = burn − shortfall + diet` for one day, plus the engine's own total. */
    private data class Score(
        val burn: Double,      // Calories.activityBurn — the 1:1 XP base
        val diet: Double,      // (BMR + burn) − eaten; +ve under, −ve over
        val shortfall: Long,   // max(0, dailyBurnTarget − burn), past days only
        val completion: Double,
        val xp: Long           // what Progression.rebuild actually banked
    )

    private fun scoreDay(d: DayData): Score {
        val burn = Calories.activityBurn(profile, d)
        val target = Calories.dailyBurnTarget(profile).toDouble()
        val shortfall = if (!d.date.isBefore(anchor) && d.date.isBefore(today))
            Math.round((target - burn).coerceAtLeast(0.0)) else 0L
        val (_, derived) = Progression.rebuild(listOf(d), today, anchor, profile)
        return Score(
            burn = burn,
            diet = Calories.deficit(profile, d),
            shortfall = shortfall,
            completion = Progression.completion(d, profile),
            xp = derived.getValue(d.date).xp
        )
    }

    /** The day as asked: 3 walked miles, 30 reps each of upper/lower/core, 500 kcal eaten. */
    private fun partialTrainingDay(date: LocalDate, eaten: Int = 500) = DayData(
        date = date, pushups = 30, squats = 30, legLifts = 30,
        miles = 3.0, caloriesConsumed = eaten, weightKg = kg
    )

    @Test
    fun bigDeficitWithPartialTraining_stillGainsXp() {
        val s = scoreDay(partialTrainingDay(yesterday))

        // The components, so a failure says WHICH half moved.
        assertEquals("daily burn target", 693, Calories.dailyBurnTarget(profile))
        assertEquals("activity burn", 353.5, s.burn, 0.5)
        assertEquals("diet term", 1758.7, s.diet, 0.5)
        assertEquals("shortfall for the 71.5% of goals skipped", 339L, s.shortfall)
        assertEquals("completion", 0.285, s.completion, 0.001)

        assertEquals("day XP", 1773L, s.xp)
        assertTrue("a big deficit must out-earn the training shortfall", s.diet > s.shortfall * 4)
    }

    @Test
    fun sameDayWhileStillToday_isNotChargedTheShortfall() {
        // Today is never charged for goals still reachable, so it reads 339 higher until midnight.
        assertEquals(2112L, scoreDay(partialTrainingDay(today)).xp)
    }

    @Test
    fun withoutTheDeficit_theSameTrainingIsANegativeDay() {
        // Identical training, maintenance-ish intake: the shortfall is now unopposed.
        val s = scoreDay(partialTrainingDay(yesterday, eaten = 2400))
        assertEquals(-127L, s.xp)
        assertTrue("eating over total burn must cost XP", s.diet < 0)
    }

    @Test
    fun intakeIsUncapped_soStarvingOutEarnsTrainingOnItsOwn() {
        // 500 kcal and ZERO exercise still gains, despite the full 693 shortfall. This is the
        // consequence the user was shown and explicitly accepted; do not clamp it silently.
        val nothingLogged = DayData(date = yesterday, caloriesConsumed = 500, weightKg = kg)
        val s = scoreDay(nothingLogged)
        assertEquals("full target charged — nothing was trained", 693L, s.shortfall)
        assertEquals(712L, s.xp)
    }

    @Test
    fun theFullDayStillPaysBest() {
        // Sanity on the incentive: same 500 kcal, all six goals filled, beats every partial day.
        val perfect = DayData(
            date = yesterday, pushups = 100, squats = 100, legLifts = 100,
            calfRaises = 100, curls = 100, miles = 5.0, caloriesConsumed = 500, weightKg = kg
        )
        val s = scoreDay(perfect)
        assertEquals("a 100% day owes nothing", 0L, s.shortfall)
        assertEquals(1.0, s.completion, 0.001)
        assertEquals(2793L, s.xp)
        assertTrue(s.xp > scoreDay(partialTrainingDay(yesterday)).xp)
    }
}
