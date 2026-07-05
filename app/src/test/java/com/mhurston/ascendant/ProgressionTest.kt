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
    fun seedImport_landsAroundLevel11_RankC_underCalorieModel() {
        val (state, _) = Progression.rebuild(seedDays())
        println("Seed import → Level ${state.level}, Rank ${state.rank}, XP ${state.totalXp}, " +
            "STR ${state.stats.strength} END ${state.stats.endurance} AGI ${state.stats.agility} " +
            "DIS ${state.stats.discipline} CON ${state.stats.consistency}")
        // Burn-based XP (1 kcal = 1 XP) with the gross MET activity model: the 30-day month
        // lands ~Level 11, Rank C (every mile/rep is body-weight-scaled gross calories).
        assertTrue("level should be 9..13 but was ${state.level}", state.level in 9..13)
        assertEquals("rank should be C", "C", state.rank.label)
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
    fun decay_costsAFullTargetDayPerMissedDay_afterTodayGrace() {
        val day = DayData(LocalDate.parse("2026-01-01"), 100, 100, 100, 100, 100, 5.0)
        val anchor = LocalDate.parse("2026-01-01")
        // Same day: no idle penalty.
        val (s0, _) = Progression.rebuild(listOf(day), LocalDate.parse("2026-01-01"), anchor)
        assertEquals(0L, s0.idlePenaltyXp)
        // 5 days later: today isn't over (1 grace day) → 4 fully-missed days, each costing
        // exactly what a 100% target day earns at the profile weight (default 80 kg).
        val perDay = Progression.missedDayPenalty(80.0)
        assertTrue("penalty must be a real day's worth", perDay > 500)
        val (s5, _) = Progression.rebuild(listOf(day), LocalDate.parse("2026-01-06"), anchor)
        assertEquals(5, s5.idleDays)
        assertEquals(4L * perDay, s5.idlePenaltyXp)
        assertTrue("effective XP must drop", s5.totalXp < s0.totalXp)
        assertEquals(s0.earnedXp, s5.earnedXp) // earned (gross) is unchanged
    }

    @Test
    fun decay_scalesWithBodyWeight_likeTheGainsDo() {
        // The penalty is the gains formula in reverse, so it must scale with body weight —
        // a heavier body both earns and loses more per day.
        assertTrue(Progression.missedDayPenalty(96.0) > Progression.missedDayPenalty(80.0))
        // And it equals a full target day's base XP at that weight.
        val day = DayData(LocalDate.parse("2026-01-01"), 100, 100, 100, 100, 100, 5.0,
            weightKg = 96.0)
        val base = Math.round(Progression.baseXp(
            com.mhurston.ascendant.domain.Profile(weightKg = 96.0), day))
        assertEquals(base, Progression.missedDayPenalty(96.0))
    }

    @Test
    fun decay_doesNotRetroactivelyPunishYearOldSeedOnFirstOpen() {
        val today = LocalDate.parse("2026-06-13") // ~1 year after the seed's last day
        val (state, _) = Progression.rebuild(seedDays(), today, today)
        assertEquals("no penalty on first open", 0L, state.idlePenaltyXp)
        assertEquals("C", state.rank.label) // imports at Rank C under the gross calorie model
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
    fun passiveKcal_addsFullXp_andIsXpOnly_notMilesOrEndurance() {
        val base = DayData(LocalDate.parse("2026-05-01"), pushups = 50)
        val withPassive = base.copy(passiveSteps = 8000, passiveKcal = 300)
        val (s0, _) = Progression.rebuild(listOf(base))
        val (s1, _) = Progression.rebuild(listOf(withPassive))
        // 1 kcal = 1 XP at full rate → ~300 more XP (before/with the same streak multiplier).
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

    @Test
    fun surplus_docksXp_belowANoFoodDay_andGoesNegativeWhenLazy() {
        val full = DayData(LocalDate.parse("2026-06-01"), 100, 100, 100, 100, 100, 5.0)
        val over = full.copy(caloriesConsumed = 6000) // eats well over a big day's burn
        val (noFood, _) = Progression.rebuild(listOf(full)) // no food → no surplus penalty
        val (surplus, _) = Progression.rebuild(listOf(over))
        assertTrue("a logged surplus costs XP vs not logging", surplus.earnedXp < noFood.earnedXp)

        // No activity + overeating → the day is net-negative XP (you go backwards).
        val lazy = DayData(LocalDate.parse("2026-06-02"), caloriesConsumed = 4000)
        val (lazyState, _) = Progression.rebuild(listOf(lazy))
        assertTrue("overeating with no activity is negative XP", lazyState.earnedXp < 0)
    }

    @Test
    fun idleDecay_isPermanent_chargedForInteriorGapsEvenAfterReturning() {
        val anchor = LocalDate.parse("2026-01-01")
        val days = listOf(
            DayData(anchor, pushups = 100),
            DayData(LocalDate.parse("2026-01-05"), pushups = 100) // back after a 3-day gap
        )
        // "today" IS the return day, so the old recoverable model would refund (0 penalty).
        // Permanent decay charges every unlogged interior day: 01-02..01-04 = 3 penalized days,
        // each at a full target day's XP (profile default 80 kg — no weigh-ins in this log).
        val today = LocalDate.parse("2026-01-05")
        val (state, _) = Progression.rebuild(days, today, anchor)
        assertEquals(3L * Progression.missedDayPenalty(80.0), state.idlePenaltyXp)
        // The gap is closed (active today) so nothing is still "bleeding" — the whole penalty is
        // locked-in interior decay, and the dashboard nudge (trailing) is silent.
        assertEquals("no trailing bleed when active today", 0L, state.trailingPenaltyXp)
        // Returning doesn't refund: the charge persists, and with only two thin days logged the
        // level floors at 0 XP (earnedXp itself is negative here — the 01-01 day was mostly
        // unfinished, so its own shortfall outweighs the reps).
        assertTrue("penalty persists after returning", state.idlePenaltyXp > 0)
        assertEquals("both charges floor the total at zero", 0L, state.totalXp)
    }

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
        assertEquals(2000, r[2].caloriesConsumed)     // intake still carried (none logged since)

        // Before any weigh-in, weight falls back to the profile default; intake stays unlogged (-1).
        val pre = Progression.carryForward(listOf(DayData(LocalDate.parse("2026-02-01"))), 80.0)
        assertEquals(80.0, pre[0].weightKg, 0.0)
        assertEquals(-1, pre[0].caloriesConsumed)
    }

    @Test
    fun carryForward_explicitZeroIsAFast_andSticks() {
        val days = listOf(
            DayData(LocalDate.parse("2026-02-01"), caloriesConsumed = 2000),
            DayData(LocalDate.parse("2026-02-02"), caloriesConsumed = 0),  // logged fast
            DayData(LocalDate.parse("2026-02-03"))                          // unlogged
        )
        val r = Progression.carryForward(days, 80.0)
        assertEquals("a logged 0 is kept, not replaced by yesterday", 0, r[1].caloriesConsumed)
        assertEquals("the fast itself carries forward like any logged value", 0, r[2].caloriesConsumed)
    }

    @Test
    fun fastingDay_earnsTheFullDeficitBonus() {
        val active = DayData(LocalDate.parse("2026-02-10"), 100, 100, 100, 100, 100, 5.0)
        val (unlogged, _) = Progression.rebuild(listOf(active))                              // -1
        val (fasted, _) = Progression.rebuild(listOf(active.copy(caloriesConsumed = 0)))     // fast
        // A fast is a huge deficit → the deficit multiplier applies; unlogged food earns none.
        assertTrue("a logged fast must out-earn an unlogged day", fasted.earnedXp > unlogged.earnedXp)
    }

    // --- rebuildFull: quests + achievements pay the XP the UI advertises -----------------

    @Test
    fun rebuildFull_grantsQuestAndAchievementBonusXp() {
        val day = DayData(LocalDate.parse("2026-03-02"), 100, 100, 100, 100, 100, 5.0)
        val (base, _) = Progression.rebuild(listOf(day))
        val full = Progression.rebuildFull(listOf(day))
        // A 100% day clears several daily quests and unlocks starter achievements.
        assertTrue("quest XP granted", full.state.questBonusXp > 0)
        assertTrue("achievement XP granted", full.state.achievementBonusXp > 0)
        assertEquals("total = earned + bonuses (no decay here)",
            base.earnedXp + full.state.questBonusXp + full.state.achievementBonusXp,
            full.state.totalXp)
        // The achievements list returned is the one the bonus was computed from.
        val achXp = com.mhurston.ascendant.domain.Achievements.unlockedXp(full.achievements)
        assertEquals(achXp, full.state.achievementBonusXp)
    }

    @Test
    fun rebuildFull_fixpoint_levelAchievementsSelfConsistent() {
        // A month of full days generates enough bonus XP to cross level thresholds; the
        // fixpoint must leave no achievement whose condition is met by the final state locked.
        val full = Progression.rebuildFull(seedDays())
        val byId = full.achievements.associateBy { it.def.id }
        val level = full.state.level
        listOf(5 to "apprentice", 10 to "stand_user").forEach { (lvl, id) ->
            if (level >= lvl) {
                assertTrue("$id must be unlocked at level $level", byId[id]?.unlocked == true)
            }
        }
        // And the advertised bonus matches the unlocked set exactly (converged, not mid-step).
        assertEquals(
            com.mhurston.ascendant.domain.Achievements.unlockedXp(full.achievements),
            full.state.achievementBonusXp
        )
    }

    @Test
    fun questXpReplay_paysDailyAndWeeklyRewards() {
        val profile = com.mhurston.ascendant.domain.Profile()
        // One 100% day: d_full(150 or 180) + d_burn + d_pushups + d_safety + d_walk + all-clear.
        val day = DayData(LocalDate.parse("2026-03-02"), 100, 100, 100, 100, 100, 5.0)
        val one = com.mhurston.ascendant.domain.Quests.earnedXp(listOf(day), profile)
        assertTrue("a perfect day pays at least the five dailies + all-clear", one >= 480)
        // Five consecutive strength days also clear weekly quests (strength 5/7 + streak 5).
        val week = (0..4).map { day.copy(date = LocalDate.parse("2026-03-02").plusDays(it.toLong())) }
        val five = com.mhurston.ascendant.domain.Quests.earnedXp(week, profile)
        assertTrue("weekly rewards land on top of dailies", five > one * 5)
    }

    @Test
    fun partialDays_loseTheirUnfinishedShare() {
        val anchor = LocalDate.parse("2026-04-01")
        val today = LocalDate.parse("2026-04-02") // scoring applies to yesterday, not today
        val perDay = Progression.missedDayPenalty(80.0)

        fun netFor(d: DayData): Long {
            val (_, derived) = Progression.rebuild(listOf(d), today, anchor)
            return derived[d.date]!!.xp
        }
        fun rawFor(d: DayData): Long {
            val (_, derived) = Progression.rebuild(listOf(d)) // no anchor → no scoring
            return derived[d.date]!!.xp
        }

        // 100% day: loses nothing — net equals the unscored XP.
        val full = DayData(LocalDate.parse("2026-04-01"), 100, 100, 100, 100, 100, 5.0)
        assertEquals(rawFor(full), netFor(full))

        // 50% day: loses exactly half a day's worth.
        val half = DayData(LocalDate.parse("2026-04-01"), 50, 50, 50, 50, 50, 2.5)
        assertEquals(rawFor(half) - Math.round(0.5 * perDay), netFor(half))

        // Logged-but-empty day (notes only): loses the full day, same as never logging.
        val empty = DayData(LocalDate.parse("2026-04-01"), notes = "rough one")
        assertEquals(rawFor(empty) - perDay, netFor(empty))

        // Overdrive (>100%) is capped at zero loss — never charged extra.
        val over = DayData(LocalDate.parse("2026-04-01"), 150, 150, 150, 150, 150, 7.0)
        assertEquals(rawFor(over), netFor(over))
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
