package com.mhurston.ascendant

import com.mhurston.ascendant.data.SeedData
import com.mhurston.ascendant.domain.Calories
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.OneOff
import com.mhurston.ascendant.domain.Profile
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * QA: the progression engine is verified against the real 30-day dataset and the
 * simplified XP model (docs/XP Simplification Spec.md):
 *
 *   dayXp = burn − max(0, dailyBurnTarget − burn) + diet
 *
 * No multipliers, no quest/achievement XP, no caps. These run on the host JVM
 * before any build is allowed to reach a device.
 */
class ProgressionTest {

    private fun seedDays(): List<DayData> = SeedData.entities().map { it.toDayData() }

    private val profile = Profile() // 80 kg default → daily burn target 450

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

    // --- The core formula: dayXp = burn − shortfall + diet -------------------------------

    @Test
    fun dayXp_isFlatBurn_noMultipliers() {
        // No food logged, no scoring window → the day's XP is exactly its burn, 1 kcal = 1 XP.
        // A perfect streaky 100% day earns the same rate as any other calorie.
        val day = DayData(LocalDate.parse("2026-04-05"), 100, 100, 100, 100, 100, 5.0)
        val (_, derived) = Progression.rebuild(listOf(day))
        assertEquals(Math.round(Progression.baseXp(profile, day)), derived[day.date]!!.xp)
    }

    @Test
    fun pastDay_belowBurnTarget_losesExactlyTheGap() {
        val anchor = LocalDate.parse("2026-04-01")
        val today = LocalDate.parse("2026-04-02")
        val target = Calories.dailyBurnTarget(profile).toDouble()
        // 100 push-ups only: burn = 0.0033 × 80 × 100 = 26.4 kcal, far under the 450 target.
        val day = DayData(anchor, pushups = 100)
        val burn = Progression.baseXp(profile, day)
        val (_, derived) = Progression.rebuild(listOf(day), today, anchor)
        val expected = Math.round(burn) - Math.round(target - burn)
        assertEquals(expected, derived[day.date]!!.xp)
        assertTrue("a thin day is net negative", derived[day.date]!!.xp < 0)
    }

    @Test
    fun pastDay_atOrOverBurnTarget_losesNothing() {
        val anchor = LocalDate.parse("2026-04-01")
        val today = LocalDate.parse("2026-04-02")
        // Full day burns 612 kcal at 80 kg — over the 450 target → no shortfall.
        val day = DayData(anchor, 100, 100, 100, 100, 100, 5.0)
        val (_, scored) = Progression.rebuild(listOf(day), today, anchor)
        val (_, unscored) = Progression.rebuild(listOf(day))
        assertEquals(unscored[day.date]!!.xp, scored[day.date]!!.xp)
    }

    @Test
    fun today_isNeverChargedTheShortfall() {
        val anchor = LocalDate.parse("2026-04-01")
        val day = DayData(anchor, pushups = 10) // tiny burn
        // Today == the day itself → no shortfall while it can still be logged.
        val (_, derived) = Progression.rebuild(listOf(day), anchor, anchor)
        assertEquals(Math.round(Progression.baseXp(profile, day)), derived[day.date]!!.xp)
    }

    @Test
    fun diet_isSymmetricOneToOne() {
        // Shifting intake by N kcal shifts the day's XP by exactly N — deficit and surplus
        // are perfect mirrors, no caps, no thresholds.
        val base = DayData(LocalDate.parse("2026-04-10"), 100, 100, 100, 100, 100, 5.0)
        fun xpAt(consumed: Int): Long {
            val (_, derived) = Progression.rebuild(listOf(base.copy(caloriesConsumed = consumed)))
            return derived[base.date]!!.xp
        }
        assertEquals(1200L, xpAt(1000) - xpAt(2200))
        assertEquals(5000L, xpAt(1000) - xpAt(6000)) // way past the old ±200/600 caps
    }

    @Test
    fun diet_absentBeforeAnyIntakeIsEverEntered() {
        val day = DayData(LocalDate.parse("2026-04-11"), 100, 100, 100, 100, 100, 5.0)
        val (_, derived) = Progression.rebuild(listOf(day)) // consumed = -1, nothing to carry
        // Before the first intake entry there's no value in effect → no diet term, just burn.
        assertEquals(Math.round(Progression.baseXp(profile, day)), derived[day.date]!!.xp)
    }

    @Test
    fun fastedDay_earnsItsFullDeficitAsXp() {
        val day = DayData(LocalDate.parse("2026-04-12"), 100, 100, 100, 100, 100, 5.0)
        val burn = Progression.baseXp(profile, day)
        val total = Calories.bmrFor(profile, day) + burn
        val (unlogged, _) = Progression.rebuild(listOf(day))
        val (fasted, _) = Progression.rebuild(listOf(day.copy(caloriesConsumed = 0)))
        // A logged fast (0) adds the entire total burn as diet XP on top of the activity burn.
        assertEquals(total, (fasted.earnedXp - unlogged.earnedXp).toDouble(), 1.0)
    }

    @Test
    fun surplus_goesNegative_uncapped() {
        // No activity + heavy overeating → the day is deeply negative (old model capped at −200).
        val lazy = DayData(LocalDate.parse("2026-06-02"), caloriesConsumed = 4000)
        val (state, derived) = Progression.rebuild(listOf(lazy))
        assertTrue("overeating with no activity is negative XP", state.earnedXp < 0)
        assertTrue("and it is NOT capped at −200", derived[lazy.date]!!.xp < -1000)
    }

    // --- Seed dataset regression ----------------------------------------------------------

    @Test
    fun seedImport_landsWhereTheFlatModelPuts_it() {
        val (state, _) = Progression.rebuild(seedDays())
        println("Seed import → Level ${state.level}, Rank ${state.rank}, XP ${state.totalXp}, " +
            "STR ${state.stats.strength} END ${state.stats.endurance} AGI ${state.stats.agility} " +
            "DIS ${state.stats.discipline} CON ${state.stats.consistency}")
        // Flat burn-only XP (no multipliers, no food logged in the seed): the 30-day month
        // lands a couple levels below the old multiplier model.
        assertTrue("level should be 8..12 but was ${state.level}", state.level in 8..12)
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

    // --- Decay: unlogged days cost the daily burn target ----------------------------------

    @Test
    fun decay_costsTheDailyBurnTarget_perMissedDay() {
        val day = DayData(LocalDate.parse("2026-01-01"), 100, 100, 100, 100, 100, 5.0)
        val anchor = LocalDate.parse("2026-01-01")
        // Same day: no idle penalty.
        val (s0, _) = Progression.rebuild(listOf(day), LocalDate.parse("2026-01-01"), anchor)
        assertEquals(0L, s0.idlePenaltyXp)
        // 5 days later: today isn't over → 4 fully-missed days, each costing exactly the
        // profile's daily burn target (450 at the 80 kg default).
        val perDay = Calories.dailyBurnTarget(profile).toLong()
        assertEquals(450L, perDay)
        val (s5, _) = Progression.rebuild(listOf(day), LocalDate.parse("2026-01-06"), anchor)
        assertEquals(5, s5.idleDays)
        assertEquals(4L * perDay, s5.idlePenaltyXp)
        assertTrue("effective XP must drop", s5.totalXp < s0.totalXp)
        assertEquals(s0.earnedXp, s5.earnedXp) // earned (from logged days) is unchanged
    }

    @Test
    fun burnTarget_scalesWithTheBody() {
        // The penalty scale is personal: a bigger body has a higher BMR → higher daily target.
        assertTrue(Calories.dailyBurnTarget(Profile(weightKg = 96.0)) >
            Calories.dailyBurnTarget(Profile(weightKg = 80.0)))
    }

    @Test
    fun decay_doesNotRetroactivelyPunishYearOldSeedOnFirstOpen() {
        val today = LocalDate.parse("2026-06-13") // ~1 year after the seed's last day
        val (state, _) = Progression.rebuild(seedDays(), today, today)
        assertEquals("no penalty on first open", 0L, state.idlePenaltyXp)
    }

    @Test
    fun idleDecay_isPermanent_chargedForInteriorGapsEvenAfterReturning() {
        val anchor = LocalDate.parse("2026-01-01")
        val days = listOf(
            DayData(anchor, pushups = 100),
            DayData(LocalDate.parse("2026-01-05"), pushups = 100) // back after a 3-day gap
        )
        // "today" IS the return day. Permanent decay charges every unlogged interior day:
        // 01-02..01-04 = 3 days, each at the full daily burn target.
        val today = LocalDate.parse("2026-01-05")
        val (state, _) = Progression.rebuild(days, today, anchor)
        assertEquals(3L * Calories.dailyBurnTarget(profile), state.idlePenaltyXp)
        // The gap is closed (active today) so nothing is still "bleeding".
        assertEquals("no trailing bleed when active today", 0L, state.trailingPenaltyXp)
        // Returning doesn't refund; with only two thin days logged the level floors at 0 XP
        // (01-01 was mostly unfinished, so its own shortfall outweighs its reps).
        assertTrue("penalty persists after returning", state.idlePenaltyXp > 0)
        assertEquals("both charges floor the total at zero", 0L, state.totalXp)
    }

    @Test
    fun partialDays_loseTheUnmetShareOfTheBurnTarget() {
        val anchor = LocalDate.parse("2026-04-01")
        val today = LocalDate.parse("2026-04-02") // scoring applies to yesterday, not today
        val target = Calories.dailyBurnTarget(profile).toDouble()

        fun netFor(d: DayData): Long {
            val (_, derived) = Progression.rebuild(listOf(d), today, anchor)
            return derived[d.date]!!.xp
        }
        fun rawFor(d: DayData): Long {
            val (_, derived) = Progression.rebuild(listOf(d)) // no anchor → no scoring
            return derived[d.date]!!.xp
        }

        // Full day (612 kcal > 450 target): loses nothing.
        val full = DayData(LocalDate.parse("2026-04-01"), 100, 100, 100, 100, 100, 5.0)
        assertEquals(rawFor(full), netFor(full))

        // Half day: burn = 66 + 240 = 306 kcal → loses the 144-kcal gap.
        val half = DayData(LocalDate.parse("2026-04-01"), 50, 50, 50, 50, 50, 2.5)
        val halfBurn = Progression.baseXp(profile, half)
        assertEquals(rawFor(half) - Math.round(target - halfBurn), netFor(half))

        // Logged-but-empty day (notes only): loses the full target, same as never logging.
        val empty = DayData(LocalDate.parse("2026-04-01"), notes = "rough one")
        assertEquals(rawFor(empty) - Math.round(target), netFor(empty))

        // Burning past the target is never charged extra.
        val over = DayData(LocalDate.parse("2026-04-01"), 150, 150, 150, 150, 150, 7.0)
        assertEquals(rawFor(over), netFor(over))
    }

    // --- Carry-forward: the last entered weight AND intake stay in effect ------------------

    @Test
    fun carryForward_inheritsLastWeightAndIntakeForwardOnly() {
        val days = listOf(
            DayData(LocalDate.parse("2026-02-01"), weightKg = 90.0, caloriesConsumed = 2000),
            DayData(LocalDate.parse("2026-02-02")),                       // nothing logged
            DayData(LocalDate.parse("2026-02-03"), weightKg = 88.0)       // new weigh-in only
        )
        val r = Progression.carryForward(days, defaultWeightKg = 80.0)
        assertEquals(90.0, r[1].weightKg, 0.0)        // weight carried from 02-01
        assertEquals(2000, r[1].caloriesConsumed)     // intake carried from 02-01
        assertEquals(88.0, r[2].weightKg, 0.0)        // fresh weigh-in wins
        assertEquals(2000, r[2].caloriesConsumed)     // intake still in effect (unchanged)

        // Before any entries, weight falls back to the profile default; intake stays -1.
        val pre = Progression.carryForward(listOf(DayData(LocalDate.parse("2026-02-01"))), 80.0)
        assertEquals(80.0, pre[0].weightKg, 0.0)
        assertEquals(-1, pre[0].caloriesConsumed)
    }

    @Test
    fun carryForward_explicitZeroIsAFast_andSticksUntilChanged() {
        val days = listOf(
            DayData(LocalDate.parse("2026-02-01"), caloriesConsumed = 2000),
            DayData(LocalDate.parse("2026-02-02"), caloriesConsumed = 0),  // logged fast
            DayData(LocalDate.parse("2026-02-03"))                          // unlogged
        )
        val r = Progression.carryForward(days, 80.0)
        assertEquals("a logged 0 is kept, not replaced by yesterday", 0, r[1].caloriesConsumed)
        assertEquals("the fast carries forward like any entered value", 0, r[2].caloriesConsumed)
    }

    @Test
    fun carriedIntake_feedsTheNextDaysDietTerm() {
        // Enter 2000 kcal on day 1, log nothing on day 2: day 2's diet term uses the carried
        // 2000 — same as if it were re-entered — until the user changes it.
        val d1 = DayData(LocalDate.parse("2026-02-01"), 100, 100, 100, 100, 100, 5.0,
            caloriesConsumed = 2000)
        val d2raw = DayData(LocalDate.parse("2026-02-02"), 100, 100, 100, 100, 100, 5.0)
        val carried = Progression.carryForward(listOf(d1, d2raw), 80.0)
        val (_, derived) = Progression.rebuild(carried)
        val explicit = Progression.carryForward(
            listOf(d1, d2raw.copy(caloriesConsumed = 2000)), 80.0)
        val (_, derivedExplicit) = Progression.rebuild(explicit)
        assertEquals(derivedExplicit[d2raw.date]!!.xp, derived[d2raw.date]!!.xp)
    }

    // --- Activity sources all feed the same flat burn --------------------------------------

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
        // 1 kcal = 1 XP exactly.
        assertEquals(2600L, s1.earnedXp - s0.earnedXp)
    }

    @Test
    fun passiveKcal_addsFullXp_andIsXpOnly_notMilesOrEndurance() {
        val base = DayData(LocalDate.parse("2026-05-01"), pushups = 50)
        val withPassive = base.copy(passiveSteps = 8000, passiveKcal = 300)
        val (s0, _) = Progression.rebuild(listOf(base))
        val (s1, _) = Progression.rebuild(listOf(withPassive))
        // Passive burn reads max(measured, step estimate); either way it's flat 1:1 XP.
        assertTrue("passive calories become XP", s1.earnedXp > s0.earnedXp + 250)
        // Passive distance is XP-only: it must NOT inflate miles or the endurance stat.
        assertEquals("passive steps never add miles", s0.totalMiles, s1.totalMiles, 0.0)
        assertEquals("passive steps never build endurance", s0.stats.endurance, s1.stats.endurance)
    }

    @Test
    fun passiveSteps_estimateXp_whenNoCalorieRecord() {
        // Steps-only device (no active-calorie record) still earns XP via the step estimate.
        val base = DayData(LocalDate.parse("2026-05-02"))
        val stepsOnly = base.copy(passiveSteps = 10000)
        val (s0, _) = Progression.rebuild(listOf(base))
        val (s1, _) = Progression.rebuild(listOf(stepsOnly))
        assertTrue("steps alone earn estimated XP", s1.earnedXp > s0.earnedXp)
    }

    @Test
    fun passiveMovement_sustainsActivityStreak_butNotStrengthStreak() {
        // A day with only passive steps (>= threshold) counts as activity, not strength.
        val day = DayData(LocalDate.parse("2026-05-03"), passiveSteps = 2000)
        val (state, _) = Progression.rebuild(listOf(day))
        assertEquals("passive movement sustains the activity streak", 1, state.activityStreak)
        assertEquals("passive movement is not strength", 0, state.strengthStreak)
    }

    @Test
    fun passiveSteps_belowThreshold_doNotCountAsActivity() {
        val day = DayData(LocalDate.parse("2026-05-04"), passiveSteps = 500) // under 1000
        val (state, _) = Progression.rebuild(listOf(day))
        assertEquals("a few steps is not an active day", 0, state.activityStreak)
    }

    @Test
    fun reSync_overwritesPassiveTotals_neverDoubleCounts() {
        // Banking is overwrite-not-add: re-reading the same day with the same aggregate is idempotent.
        val day = DayData(LocalDate.parse("2026-05-05"), passiveKcal = 400)
        val (once, _) = Progression.rebuild(listOf(day))
        val (twice, _) = Progression.rebuild(listOf(day.copy(passiveKcal = 400)))
        assertEquals("re-syncing the same totals yields the same XP", once.earnedXp, twice.earnedXp)
    }

    // --- rebuildFull: quests + achievements are badges, never XP ---------------------------

    @Test
    fun rebuildFull_paysNoBonusXp_badgesOnly() {
        val day = DayData(LocalDate.parse("2026-03-02"), 100, 100, 100, 100, 100, 5.0)
        val (base, _) = Progression.rebuild(listOf(day))
        val full = Progression.rebuildFull(listOf(day))
        // Identical XP/level — achievements are evaluated but pay nothing.
        assertEquals(base.totalXp, full.state.totalXp)
        assertEquals(base.level, full.state.level)
        assertTrue("achievements still evaluate", full.achievements.isNotEmpty())
        assertTrue("a 100% day unlocks starter badges", full.achievements.any { it.unlocked })
    }

    @Test
    fun rebuildFull_levelAchievements_matchTheCalorieLevel() {
        val full = Progression.rebuildFull(seedDays())
        val byId = full.achievements.associateBy { it.def.id }
        listOf(5 to "apprentice", 10 to "stand_user").forEach { (lvl, id) ->
            assertEquals("$id unlock must track level ${full.state.level} vs threshold $lvl",
                full.state.level >= lvl, byId[id]?.unlocked == true)
        }
    }

    @Test
    fun cardioOnlyDay_countsAsActivity() {
        // Swim/bike minutes are real training: they sustain the activity streak (and thus
        // aren't treated as an idle day), even though they don't feed completion.
        val day = DayData(LocalDate.parse("2026-05-06"), cardioMinutes = mapOf("swim" to 30))
        val (state, _) = Progression.rebuild(listOf(day))
        assertEquals(1, state.activityStreak)
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
