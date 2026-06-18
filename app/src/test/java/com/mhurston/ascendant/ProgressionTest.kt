package com.mhurston.ascendant

import com.mhurston.ascendant.data.SeedData
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.OneOff
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * QA: the progression engine is verified against the real 30-day dataset and the
 * design spec (docs/Leveling System.md). These run on the host JVM before any build
 * is allowed to reach a device.
 */
class ProgressionTest {

    private fun seedDays(): List<DayData> = SeedData.entities().map { it.toDayData() }

    @Test
    fun completionParity_fullDay_is100Percent() {
        // 2025-06-30: 100/100/100/100/100 + 5.0 mi → exactly 100%.
        val day = DayData(LocalDate.parse("2025-06-30"), 100, 100, 100, 100, 100, 5.0)
        assertEquals(1.0, Progression.completion(day), 0.0001)
    }

    @Test
    fun completionParity_partialDay() {
        // 2025-06-01: 50/50/50/50/30 + 1.5 mi → (0.5+0.5+0.5+0.5+0.3+0.3)/6 = 0.4333…
        val day = DayData(LocalDate.parse("2025-06-01"), 50, 50, 50, 50, 30, 1.5)
        assertEquals(2.6 / 6.0, Progression.completion(day), 0.0001)
    }

    @Test
    fun completionParity_skipDayWithWalkOnly() {
        val day = DayData(LocalDate.parse("2025-06-06"), 0, 0, 0, 0, 0, 3.0)
        assertEquals((3.0 / 5.0) / 6.0, Progression.completion(day), 0.0001)
    }

    @Test
    fun seedImport_landsAroundLevel8_RankD_underCalorieModel() {
        val (state, _) = Progression.rebuild(seedDays())
        println("Seed import → Level ${state.level}, Rank ${state.rank}, XP ${state.totalXp}, " +
            "STR ${state.stats.strength} END ${state.stats.endurance} AGI ${state.stats.agility} " +
            "DIS ${state.stats.discipline} CON ${state.stats.consistency}")
        // Burn-based XP (1 kcal = 1 XP): the 30-day month lands ~Level 8, Rank D.
        assertTrue("level should be 6..10 but was ${state.level}", state.level in 6..10)
        assertEquals("rank should be D", "D", state.rank.label)
    }

    @Test
    fun seedImport_statsAreReasonable() {
        val (state, _) = Progression.rebuild(seedDays())
        val s = state.stats
        assertTrue("END should dominate (walker profile)", s.endurance > s.strength)
        assertTrue("STR positive", s.strength > 0)
        assertTrue("AGI positive", s.agility > 0)
        assertTrue("DIS positive", s.discipline > 0)
        assertTrue("CON positive", s.consistency > 0)
    }

    @Test
    fun xpCurve_matchesSpec() {
        assertEquals(100L, Progression.xpToNext(1))
        assertEquals(1118L, Progression.xpToNext(5))   // round(100 * 5^1.5)
        assertEquals(3162L, Progression.xpToNext(10))  // round(100 * 10^1.5)
    }

    @Test
    fun emptyLog_isLevel1RankE_noCrash() {
        val (state, derived) = Progression.rebuild(emptyList())
        assertEquals(1, state.level)
        assertEquals("E", state.rank.label)
        assertTrue(derived.isEmpty())
    }

    @Test
    fun decay_costsHalfADayPerIdleDayAfterGrace() {
        val day = DayData(LocalDate.parse("2026-01-01"), 100, 100, 100, 100, 100, 5.0)
        val anchor = LocalDate.parse("2026-01-01")
        // Same day: no idle penalty.
        val (s0, _) = Progression.rebuild(listOf(day), LocalDate.parse("2026-01-01"), anchor)
        assertEquals(0L, s0.idlePenaltyXp)
        // 5 days later: 1 grace day → 4 penalized × 255 = 1020 XP shaved off.
        val (s5, _) = Progression.rebuild(listOf(day), LocalDate.parse("2026-01-06"), anchor)
        assertEquals(5, s5.idleDays)
        assertEquals(4L * Progression.DECAY_PER_IDLE_DAY, s5.idlePenaltyXp)
        assertTrue("effective XP must drop", s5.totalXp < s0.totalXp)
        assertEquals(s0.earnedXp, s5.earnedXp) // earned (gross) is unchanged
    }

    @Test
    fun decay_doesNotRetroactivelyPunishYearOldSeedOnFirstOpen() {
        val today = LocalDate.parse("2026-06-13") // ~1 year after the seed's last day
        val (state, _) = Progression.rebuild(seedDays(), today, today)
        assertEquals("no penalty on first open", 0L, state.idlePenaltyXp)
        assertEquals("D", state.rank.label) // imports at Rank D under the calorie model
    }

    @Test
    fun customReps_addCalorieXp_withoutChangingCompletionOrStats() {
        val base = DayData(LocalDate.parse("2026-02-01"), 100, 100, 100, 100, 100, 5.0)
        val withCustom = base.copy(customReps = mapOf("c1" to 200))
        // Completion is identical — pinned customs never change the tuned core formula.
        assertEquals(Progression.completion(base), Progression.completion(withCustom), 0.0)
        val (s0, _) = Progression.rebuild(listOf(base))
        val (s1, _) = Progression.rebuild(listOf(withCustom))
        assertTrue("custom reps burn calories → more XP", s1.earnedXp > s0.earnedXp)
        assertEquals(s0.stats.strength, s1.stats.strength)    // core stats count only core lifts
        assertEquals(s0.stats.consistency, s1.stats.consistency)
    }

    @Test
    fun oneOffs_addTheirCaloriesDirectlyToXp() {
        val base = DayData(LocalDate.parse("2026-03-01"), pushups = 50)
        val withRun = base.copy(oneOffs = listOf(OneOff("Marathon", 2600)))
        val (s0, _) = Progression.rebuild(listOf(base))
        val (s1, _) = Progression.rebuild(listOf(withRun))
        // 1 kcal = 1 XP, so a 2600-kcal one-off adds well over 2000 XP.
        assertTrue("one-off calories become XP", s1.earnedXp > s0.earnedXp + 2000)
    }

    @Test
    fun deficit_earnsBonusXp_onlyWhenFoodIsLogged() {
        val day = DayData(LocalDate.parse("2026-04-01"), 100, 100, 100, 100, 100, 5.0)
        val deficitDay = day.copy(caloriesConsumed = 100) // tiny intake → large deficit
        val (s0, _) = Progression.rebuild(listOf(day))         // no food logged → no bonus
        val (s1, _) = Progression.rebuild(listOf(deficitDay))  // big deficit → bonus
        assertTrue("a logged deficit multiplies XP", s1.earnedXp > s0.earnedXp)
    }

    @Test
    fun streakBreaksOnCalendarGap() {
        val days = listOf(
            DayData(LocalDate.parse("2025-07-01"), pushups = 50),
            DayData(LocalDate.parse("2025-07-02"), pushups = 50),
            // gap (skipped 07-03) → streak resets
            DayData(LocalDate.parse("2025-07-04"), pushups = 50)
        )
        val (state, _) = Progression.rebuild(days)
        assertEquals(1, state.strengthStreak)
        assertEquals(2, state.longestStrengthStreak)
    }
}
