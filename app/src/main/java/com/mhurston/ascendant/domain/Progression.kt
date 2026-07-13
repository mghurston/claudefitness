package com.mhurston.ascendant.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.floor
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

    // --- The whole XP model (docs/XP Simplification Spec.md) ------------------
    //
    //   dayXp = burn − shortfall + diet
    //
    // burn      = Calories.activityBurn — gross activity kcal, 1 kcal = 1 XP.
    // shortfall = max(0, dailyBurnTarget − burn) — charged on past days only (today can
    //             still be logged); a fully unlogged day loses the full target.
    // diet      = (BMR + burn) − consumed — symmetric 1:1, UNCAPPED. Eat under your burn →
    //             gain; over → lose. Intake carries forward until changed (carryForward), so
    //             the value you last entered keeps applying; before any entry there is no
    //             diet term (Calories.deficit returns 0 for the -1 sentinel).
    //
    // There are deliberately NO multipliers, bonuses, or caps. Do not add any without
    // asking the user first — every prior "small bonus" got ripped out here.
    const val XP_PER_KCAL = 1.0

    /** Completion ratio 0..n — reproduces the spreadsheet formula exactly (Mapping §3). */
    fun completion(d: DayData): Double {
        return (d.pushups / 100.0 + d.squats / 100.0 + d.legLifts / 100.0 +
            d.calfRaises / 100.0 + d.curls / 100.0 + d.walkMiles / MILE_TARGET) / 6.0
    }

    /** Base XP = calories burned through activity (1 kcal = 1 XP). */
    fun baseXp(p: Profile, d: DayData): Double = Calories.activityBurn(p, d) * XP_PER_KCAL

    /**
     * Carry body weight and calories-consumed forward across the log: a day with no weigh-in
     * inherits the most recent prior weigh-in (falling back to [defaultWeightKg] before the
     * first one), and a day with no logged intake (-1) inherits the previous day's intake
     * (-1 until the first logged day → no diet term). An explicit 0 is a logged fast and, like
     * any entered value, stays in effect until changed. "Carry forward unless changed": the
     * number you last entered keeps applying, so an unlogged day uses yesterday's body weight
     * AND yesterday's intake for its diet term. Pure + deterministic; the engine and UI both
     * run the raw log through this before reading per-day weight/intake.
     */
    fun carryForward(days: List<DayData>, defaultWeightKg: Double): List<DayData> {
        var weight = defaultWeightKg
        var consumed = -1
        return days.sortedBy { it.date }.map { d ->
            if (d.weightKg > 0.0) weight = d.weightKg
            if (d.caloriesConsumed >= 0) consumed = d.caloriesConsumed
            d.copy(weightKg = weight, caloriesConsumed = consumed)
        }
    }

    /** XP for a day = burn + diet: calories burned (1:1) plus the signed diet term —
     *  Calories.deficit is > 0 under your total burn (gain), < 0 over it (lose), 0 when no
     *  food was logged. Uncapped both ways, so the day can be negative. */
    private fun dayXp(p: Profile, d: DayData): Long =
        Math.round(baseXp(p, d) + Calories.deficit(p, d))

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
        level >= 15 -> "Breath Adept"
        level >= 10 -> "Aura Wielder"
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

        // The one penalty scale: your personal daily burn target (~25% of BMR). Miss it,
        // lose the gap; skip the day entirely, lose the whole thing.
        val burnTarget = Calories.dailyBurnTarget(profile).toDouble()

        var totalXp = 0L
        var strengthStreak = 0
        var activityStreak = 0
        var longestStrength = 0
        var perfectStreak = 0
        var prev: LocalDate? = null

        var totalStrengthReps = 0 // all five exercises — the Hero row + rep achievements
        var strStatReps = 0       // pushups+squats+curls only — the STR formula's input
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

            val comp = completion(d)
            // Shortfall — the "lose XP for what you didn't do" half of the rule. A past day
            // that burned less than the daily target loses exactly the gap. Only days at/after
            // the decay anchor (pre-app history is never punished) and strictly before today
            // (today isn't over) are charged.
            val shortfall = if (today != null && decayAnchor != null &&
                !d.date.isBefore(decayAnchor) && d.date.isBefore(today)
            ) Math.round((burnTarget - baseXp(profile, d)).coerceAtLeast(0.0)) else 0L
            val xp = dayXp(profile, d) - shortfall
            totalXp += xp
            derived[d.date] = DayDerived(comp, xp)

            // Lifetime reps count every core exercise (matches the per-day strengthReps that
            // drives burn/streaks); STR's input stays the push/squat/curl subset — leg lifts
            // and calf raises grow AGI instead, never both.
            totalStrengthReps += d.strengthReps
            strStatReps += d.pushups + d.squats + d.curls
            totalAgiReps += d.legLifts + d.calfRaises
            totalMiles += d.miles
            if (comp >= 0.8) daysGe80++
            if (d.hasActivity) daysTrained++

            prev = d.date
        }

        val str = floor(sqrt(strStatReps / 50.0)).toInt()
        val end = floor(sqrt(totalMiles * 4.0)).toInt()
        val agi = floor(sqrt(totalAgiReps / 60.0)).toInt()
        val dis = floor(daysGe80 * 1.5).toInt()
        val con = longestStrength + strengthStreak / 2

        // --- Inactivity decay (PERMANENT) -------------------------------------
        // The other half of per-day scoring: days with NO log entry at all. (Logged days are
        // already charged their burn-target shortfall in the loop above; this walk covers the
        // calendar days that never got a row, each costing the full daily burn target — the
        // same rule, with burn = 0.) Never refunded: returning to activity doesn't erase a
        // past gap — you earn it back, you don't get it back. Pre-anchor history (e.g. an
        // imported backlog) is ignored, and today is never charged while it's still in progress.
        var idleDays = 0          // trailing idle days (drives the "log today" nudge)
        var interiorPenalty = 0L  // locked-in decay from before the last activity (unstoppable)
        var trailingPenalty = 0L  // the still-growing gap since the last activity
        var trailingCharged = 0   // unlogged days actually charged in that trailing gap
        if (today != null && decayAnchor != null) {
            val loggedDates = sorted.mapTo(HashSet()) { it.date }
            val lastActive = sorted.lastOrNull { it.hasActivity && !it.date.isAfter(today) }?.date
            val pen = Math.round(burnTarget)
            var day: LocalDate = decayAnchor
            while (day.isBefore(today)) {
                if (day !in loggedDates) {
                    if (lastActive == null || day.isAfter(lastActive)) {
                        trailingPenalty += pen
                        trailingCharged++
                    } else interiorPenalty += pen
                }
                day = day.plusDays(1)
            }
            val effectiveStart = listOfNotNull(lastActive, decayAnchor).maxOrNull() ?: decayAnchor
            idleDays = ChronoUnit.DAYS.between(effectiveStart, today).toInt().coerceAtLeast(0)
        }
        val penalty = interiorPenalty + trailingPenalty
        val earnedXp = totalXp
        val effectiveXp = (earnedXp - penalty).coerceAtLeast(0)

        val li = levelForXp(effectiveXp)
        val state = CharacterState(
            totalXp = effectiveXp,
            earnedXp = earnedXp,
            idlePenaltyXp = penalty,
            trailingPenaltyXp = trailingPenalty,
            trailingChargedDays = trailingCharged,
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

    /** Everything the UI needs from one replay: character, per-day derived values, and the
     *  achievement statuses evaluated against that character. */
    data class FullBuild(
        val state: CharacterState,
        val derived: Map<LocalDate, DayDerived>,
        val achievements: List<AchStatus>
    )

    /** [rebuild] plus one achievements pass. Quests and achievements are badges only — they
     *  pay no XP (docs/XP Simplification Spec.md), so a single evaluation is exact. */
    fun rebuildFull(
        days: List<DayData>,
        today: LocalDate? = null,
        decayAnchor: LocalDate? = null,
        profile: Profile = Profile()
    ): FullBuild {
        val (state, derived) = rebuild(days, today, decayAnchor, profile)
        return FullBuild(state, derived, Achievements.evaluate(days, state))
    }
}
