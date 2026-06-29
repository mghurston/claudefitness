package com.mhurston.ascendant

import com.mhurston.ascendant.domain.Quests
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * QA: weekly quests group days into Sunday–Saturday weeks, matching the Log calendar grid.
 * The boundary is the bug-prone part, so pin it explicitly.
 */
class QuestsWeekTest {

    @Test
    fun weekStart_isTheSundayOnOrBeforeTheDate() {
        // 2026-06-29 is a Monday → its week starts Sunday 2026-06-28.
        assertEquals(LocalDate.parse("2026-06-28"), Quests.weekStart(LocalDate.parse("2026-06-29")))
        // A Sunday is its own week start.
        assertEquals(LocalDate.parse("2026-06-28"), Quests.weekStart(LocalDate.parse("2026-06-28")))
        // The following Saturday still belongs to that week.
        assertEquals(LocalDate.parse("2026-06-28"), Quests.weekStart(LocalDate.parse("2026-07-04")))
    }

    @Test
    fun saturdayAndNextSunday_areDifferentWeeks() {
        val saturday = Quests.weekStart(LocalDate.parse("2026-07-04"))
        val nextSunday = Quests.weekStart(LocalDate.parse("2026-07-05"))
        assertEquals(LocalDate.parse("2026-06-28"), saturday)
        assertEquals(LocalDate.parse("2026-07-05"), nextSunday)
    }
}
