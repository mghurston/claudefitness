package com.mhurston.ascendant

import com.mhurston.ascendant.data.SeedData
import com.mhurston.ascendant.domain.Achievements
import com.mhurston.ascendant.domain.Progression
import org.junit.Assert.assertTrue
import org.junit.Test

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
        listOf("thousand_fists", "never_skip_cardio", "the_five", "century_walker",
            "over_capacity", "rank_c").forEach {
            assertTrue("$it should be unlocked on import", byId[it]?.unlocked == true)
        }
    }
}
