package com.mhurston.ascendant.domain

import java.time.DayOfWeek
import java.time.LocalDate

enum class Cadence { DAILY, WEEKLY }

data class Quest(
    val id: String,
    val title: String,
    val desc: String,
    val current: Int,
    val target: Int,
    val xpReward: Int,
    val cadence: Cadence
) {
    val done: Boolean get() = current >= target
    val progress: Float get() = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
}

/** Generates today's daily quests + this week's weekly quests from the log. Day-aware. */
object Quests {

    // Daily/weekly active-burn goals — the universal "something" every logged activity counts
    // toward (reps, miles, pinned customs, one-offs, and bike/swim cardio all burn calories).
    // Targets are personalized from the profile (see Calories.dailyBurnTarget).

    fun generate(today: LocalDate, todayDay: DayData, allDays: List<DayData>, profile: Profile): List<Quest> {
        val daily = dailyQuests(today, todayDay, profile)
        val weekly = weeklyQuests(today, allDays, profile)
        return daily + weekly
    }

    private fun dailyQuests(today: LocalDate, d: DayData, profile: Profile): List<Quest> {
        val compPct = (Progression.completion(d) * 100).toInt()
        val anyReps = d.strengthReps
        val burn = Math.round(Calories.activityBurn(profile, d)).toInt()
        val burnTarget = Calories.dailyBurnTarget(profile)
        val isBossDay = today.dayOfWeek in setOf(DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        val coreXp = if (isBossDay) 180 else 150 // weak-day quests pay a little more
        return listOf(
            Quest("d_full", if (isBossDay) "Boss Rush — clear the day" else "Clear the day",
                if (isBossDay)
                    "Boss day (Wed/Fri/Sat): hit 100% across all of today's training targets for bonus XP."
                else
                    "Hit 100% across all of today's training targets.",
                compPct, 100, coreXp, Cadence.DAILY),
            Quest("d_burn", "Burn $burnTarget active calories",
                "Everything counts — reps, miles, cardio, extras.", burn, burnTarget, 120, Cadence.DAILY),
            Quest("d_pushups", "Complete 100 push-ups",
                "The lift you skip most.", d.pushups, 100, 100, Cadence.DAILY),
            Quest("d_safety", "Do 20 reps of anything",
                "Bare minimum keeps the streak alive.", anyReps, 20, 50, Cadence.DAILY),
            Quest("d_walk", "Walk 4 miles", "Bonus cardio — tracked steps + treadmill.",
                (d.walkMiles).toInt(), 4, 60, Cadence.DAILY)
        )
    }

    private fun weeklyQuests(today: LocalDate, allDays: List<DayData>, profile: Profile): List<Quest> {
        val week = allDays.filter { isSameWeek(it.date, today) }
        val strengthDays = week.count { it.hasStrength }
        val weekMiles = week.sumOf { it.walkMiles }.toInt()
        val weekBurn = Math.round(week.sumOf { Calories.activityBurn(profile, it) }).toInt()
        val weekTarget = Calories.weeklyBurnTarget(profile)
        val bestStreakThisWeek = currentStreakWithinWeek(week, today)
        return listOf(
            Quest("w_strength5", "Train strength 5 of 7 days",
                "Beat your average active rate.", strengthDays, 5, 400, Cadence.WEEKLY),
            Quest("w_burn", "Burn $weekTarget calories this week",
                "Every active minute adds up.", weekBurn, weekTarget, 450, Cadence.WEEKLY),
            Quest("w_miles25", "Walk 25 miles this week",
                "You average ~27.", weekMiles, 25, 400, Cadence.WEEKLY),
            Quest("w_streak5", "Hold a 5-day strength streak",
                "Tie then break your record.", bestStreakThisWeek, 5, 500, Cadence.WEEKLY)
        )
    }

    /** Sunday that begins [date]'s week. Sunday-first to match the Log calendar's grid
     *  (CalendarScreen uses the same `dayOfWeek.value % 7` offset), so a quest "week" and the
     *  visual week always cover the same Sun–Sat span. */
    internal fun weekStart(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value % 7).toLong()) // ISO Mon=1..Sun=7 → Sun=0, Sat=6

    private fun isSameWeek(a: LocalDate, b: LocalDate): Boolean = weekStart(a) == weekStart(b)

    private fun currentStreakWithinWeek(week: List<DayData>, today: LocalDate): Int {
        // Longest run of consecutive strength days within this week.
        val sorted = week.sortedBy { it.date }
        var best = 0; var run = 0; var prev: LocalDate? = null
        for (d in sorted) {
            val consecutive = prev?.let { java.time.temporal.ChronoUnit.DAYS.between(it, d.date) == 1L } ?: true
            run = if (d.hasStrength && consecutive) run + 1 else if (d.hasStrength) 1 else 0
            best = maxOf(best, run)
            prev = d.date
        }
        return best
    }
}
