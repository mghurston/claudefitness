package com.mhurston.ascendant.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure, deterministic progression engine. The entire character (XP, level, rank,
 * stats, streaks) is replayed from the raw day log — never stored as the source of
 * truth. Tuned to the real 30-day dataset (see docs/Leveling System.md).
 */
object Progression {

    // Daily targets (docs/Product Design Document.md §3.1)
    const val REP_TARGET = 100
    const val MILE_TARGET = 5.0

    // --- Burn-based XP (the core "less arbitrary" model) ----------------------
    // 1 calorie burned through activity = 1 XP. Activity burn (walking + strength +
    // pinned customs + one-offs) comes from Calories.activityBurn, so XP, the Energy
    // screen, and the day log all read the same number.
    const val XP_PER_KCAL = 1.0

    // Multipliers (kept deliberately few so XP stays legible):
    private const val COMPLETION_BONUS = 0.25      // +25% on days you hit 100% of your targets
    private const val STREAK_BONUS_PER_DAY = 0.05  // +5%/strength-day, capped below
    private const val STREAK_BONUS_CAP_DAYS = 10   // → max +50%
    // Deficit bonus: eating under your total burn earns up to +50%, scaling with the gap.
    private const val DEFICIT_FULL_BONUS_KCAL = 600.0 // a 600+ kcal deficit = the full bonus
    private const val DEFICIT_BONUS_MAX = 0.50

    // Inactivity decay: each idle day costs roughly half of a solid active day's XP.
    const val DECAY_PER_IDLE_DAY = 150L
    const val DECAY_GRACE_DAYS = 1 // today is always free; decay starts on the 2nd missed day

    /** Completion ratio 0..n — reproduces the spreadsheet formula exactly (Mapping §3). */
    fun completion(d: DayData): Double {
        return (d.pushups / 100.0 + d.squats / 100.0 + d.legLifts / 100.0 +
            d.calfRaises / 100.0 + d.curls / 100.0 + d.walkMiles / MILE_TARGET) / 6.0
    }

    /** Base XP before multipliers = calories burned through activity (1 kcal = 1 XP). */
    fun baseXp(p: Profile, d: DayData): Double = Calories.activityBurn(p, d) * XP_PER_KCAL

    /** XP for a day: calories burned, scaled by consistency + deficit multipliers. */
    private fun dayXp(p: Profile, d: DayData, strengthStreak: Int): Long {
        val base = baseXp(p, d)
        if (base <= 0.0) return 0L
        val completionMult = if (completion(d) >= 1.0) 1.0 + COMPLETION_BONUS else 1.0
        val streakMult = 1.0 + STREAK_BONUS_PER_DAY * min(strengthStreak, STREAK_BONUS_CAP_DAYS)
        val deficit = Calories.deficit(p, d)
        val deficitMult = if (deficit > 0)
            1.0 + DEFICIT_BONUS_MAX * min(deficit / DEFICIT_FULL_BONUS_KCAL, 1.0)
        else 1.0
        return Math.round(base * completionMult * streakMult * deficitMult)
    }

    /** XP required to advance FROM level n to n+1 (Leveling §4). */
    fun xpToNext(n: Int): Long = Math.round(100.0 * Math.pow(n.toDouble(), 1.5))

    fun rankForLevel(level: Int): Rank = when {
        level >= 100 -> Rank.NATIONAL
        level >= 75 -> Rank.SS
        level >= 50 -> Rank.S
        level >= 35 -> Rank.A
        level >= 20 -> Rank.B
        level >= 10 -> Rank.C
        level >= 5 -> Rank.D
        else -> Rank.E
    }

    fun titleForLevel(level: Int): String = when {
        level >= 100 -> "ASCENDANT"
        level >= 75 -> "Transcendent"
        level >= 50 -> "The Pinnacle"
        level >= 40 -> "Beyond Limits"
        level >= 30 -> "Crimson Resolve"
        level >= 25 -> "Iron Will"
        level >= 20 -> "Ascendant I"
        level >= 15 -> "Hamon Adept"
        level >= 10 -> "Stand User"
        level >= 5 -> "Awakened"
        else -> "Novice"
    }

    private data class LevelInfo(val level: Int, val into: Long, val forNext: Long)

    private fun levelForXp(totalXp: Long): LevelInfo {
        var level = 1
        var remaining = totalXp
        while (true) {
            val need = xpToNext(level)
            if (remaining < need) return LevelInfo(level, remaining, need)
            remaining -= need
            level++
            if (level > 999) return LevelInfo(level, 0, xpToNext(level))
        }
    }

    /**
     * Replay the whole log → full character + per-day derived values.
     *
     * @param today current date; with [decayAnchor], enables inactivity decay.
     * @param decayAnchor the date the user first started using the app — idle days
     *   before this don't count (so a year-old seed import isn't retroactively decayed).
     *   Pass null for either to disable decay (used by pure-log tests).
     */
    fun rebuild(
        days: List<DayData>,
        today: LocalDate? = null,
        decayAnchor: LocalDate? = null,
        profile: Profile = Profile()
    ): Pair<CharacterState, Map<LocalDate, DayDerived>> {
        val sorted = days.sortedBy { it.date }
        val derived = LinkedHashMap<LocalDate, DayDerived>()

        var totalXp = 0L
        var strengthStreak = 0
        var activityStreak = 0
        var longestStrength = 0
        var perfectStreak = 0
        var prev: LocalDate? = null

        var totalStrengthReps = 0
        var totalAgiReps = 0
        var totalMiles = 0.0
        var daysGe80 = 0
        var daysTrained = 0

        for (d in sorted) {
            // A calendar gap > 1 day means missed days → streaks broken.
            val gap = prev?.let { ChronoUnit.DAYS.between(it, d.date) } ?: 1L
            if (gap > 1L) {
                strengthStreak = 0; activityStreak = 0; perfectStreak = 0
            }

            if (!d.isRestDay) {
                strengthStreak = if (d.hasStrength) strengthStreak + 1 else 0
                activityStreak = if (d.hasActivity) activityStreak + 1 else 0
                perfectStreak = if (completion(d) >= 1.0) perfectStreak + 1 else 0
            }
            longestStrength = maxOf(longestStrength, strengthStreak)

            val xp = dayXp(profile, d, strengthStreak)
            totalXp += xp
            val comp = completion(d)
            derived[d.date] = DayDerived(comp, xp)

            totalStrengthReps += d.pushups + d.squats + d.curls
            totalAgiReps += d.legLifts + d.calfRaises
            totalMiles += d.miles
            if (comp >= 0.8) daysGe80++
            if (d.hasActivity) daysTrained++

            prev = d.date
        }

        val str = floor(sqrt(totalStrengthReps / 50.0)).toInt()
        val end = floor(sqrt(totalMiles * 4.0)).toInt()
        val agi = floor(sqrt(totalAgiReps / 60.0)).toInt()
        val dis = floor(daysGe80 * 1.5).toInt()
        val con = longestStrength + strengthStreak / 2

        // --- Inactivity decay -------------------------------------------------
        var idleDays = 0
        var penalty = 0L
        if (today != null && decayAnchor != null) {
            val lastActive = sorted.lastOrNull { it.hasActivity && !it.date.isAfter(today) }?.date
            // Don't count idle time before the user started using the app, or before
            // their last activity — whichever is later.
            val effectiveStart = listOfNotNull(lastActive, decayAnchor).maxOrNull() ?: decayAnchor
            idleDays = ChronoUnit.DAYS.between(effectiveStart, today).toInt().coerceAtLeast(0)
            val penalizedDays = (idleDays - DECAY_GRACE_DAYS).coerceAtLeast(0)
            penalty = penalizedDays.toLong() * DECAY_PER_IDLE_DAY
        }
        val earnedXp = totalXp
        val effectiveXp = (earnedXp - penalty).coerceAtLeast(0)

        val li = levelForXp(effectiveXp)
        val state = CharacterState(
            totalXp = effectiveXp,
            earnedXp = earnedXp,
            idlePenaltyXp = penalty,
            idleDays = idleDays,
            level = li.level,
            rank = rankForLevel(li.level),
            title = titleForLevel(li.level),
            xpIntoLevel = li.into,
            xpForNextLevel = li.forNext,
            stats = Stats(str, end, agi, dis, con),
            strengthStreak = strengthStreak,
            activityStreak = activityStreak,
            perfectStreak = perfectStreak,
            longestStrengthStreak = longestStrength,
            daysTrained = daysTrained,
            totalStrengthReps = totalStrengthReps,
            totalMiles = totalMiles
        )
        return state to derived
    }
}
