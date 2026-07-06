package com.mhurston.ascendant

import com.mhurston.ascendant.data.SeedData
import com.mhurston.ascendant.domain.Achievements
import com.mhurston.ascendant.domain.DayData
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AchievementsTest {

    private fun seed() = SeedData.entities().map { it.toDayData() }

    @Test
    fun retroactiveUnlocks_fromImportedHistory() {
        val (state, _) = Progression.rebuild(seed())
        val statuses = Achievements.evaluate(seed(), state)
        val unlocked = statuses.count { it.unlocked }
        println("Achievements unlocked on import: $unlocked")
        // Spec: ~18 already earned on import. Allow a sensible band.
        assertTrue("expected >= 15 retroactive unlocks, got $unlocked", unlocked >= 15)
    }

    @Test
    fun specificRetroactiveAchievementsUnlock() {
        val (state, _) = Progression.rebuild(seed())
        val byId = Achievements.evaluate(seed(), state).associateBy { it.def.id }
        // rank_c is omitted: under the burn-based XP model the seed month lands at Rank D,
        // so the Rank C achievement no longer unlocks on import. These are activity-based
        // and unlock regardless of the XP curve.
        listOf("thousand_fists", "never_skip_cardio", "the_five", "century_walker",
            "over_capacity").forEach {
            assertTrue("$it should be unlocked on import", byId[it]?.unlocked == true)
        }
    }

    @Test
    fun personalRecords_andJournal_track() {
        val days = listOf(
            DayData(LocalDate.parse("2026-01-01"), pushups = 50, notes = "start"),
            DayData(LocalDate.parse("2026-01-02"), pushups = 60, notes = "better"), // PR
            DayData(LocalDate.parse("2026-01-03"), pushups = 55, notes = "ok"),      // no PR
            DayData(LocalDate.parse("2026-01-04"), pushups = 80, notes = "big")      // PR
        )
        val (state, _) = Progression.rebuild(days)
        val byId = Achievements.evaluate(days, state).associateBy { it.def.id }
        assertTrue("new_best unlocks after a PR", byId["new_best"]?.unlocked == true)
        assertEquals("two PR events counted", 2, byId["record_breaker"]?.current)
        assertEquals("four notes logged", 4, byId["field_notes"]?.current)
        // Mood achievements (self_aware, peak_state) were retired with the mood feature.
        assertEquals("mood badges are gone", null, byId["self_aware"])
        assertEquals(null, byId["peak_state"])
    }
}
