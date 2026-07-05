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
    // Surplus penalty: eating OVER your total burn costs XP — the mirror of the deficit bonus.
    // Unlike the bonus (a multiplier on the day's activity), this is a flat XP hit that applies
    // even on a no-activity day, so overeating always stings. A 600+ kcal surplus costs the most.
    private const val SURPLUS_FULL_PENALTY_KCAL = 600.0
    private const val SURPLUS_PENALTY_MAX_XP = 200.0

    // Inactivity decay — one rule, no arbitrary constant: every past day is scored against the
    // targets. 100% completion loses nothing; 0% loses a full day's worth; 40% loses 60% of
    // one. Earn by doing, lose the same scale by not doing. Today is never charged while it's
    // still in progress, because it can still be logged.

    /** XP a fully skipped day costs = base XP of a 100% target day (500 reps + 5 mi) at
     *  [weightKg] — the gains formula run in reverse, so the penalty scales with the same
     *  body weight the earnings do. ~730 XP at 96 kg, ~610 at 80 kg. */
    fun missedDayPenalty(weightKg: Double): Long = Math.round(
        Calories.strengthKcal(weightKg, 5 * REP_TARGET) + Calories.walkKcal(weightKg, MILE_TARGET)
    )

    /** Completion ratio 0..n — reproduces the spreadsheet formula exactly (Mapping §3). */
    fun completion(d: DayData): Double {
        return (d.pushups / 100.0 + d.squats / 100.0 + d.legLifts / 100.0 +
            d.calfRaises / 100.0 + d.curls / 100.0 + d.walkMiles / MILE_TARGET) / 6.0
    }

    /** Base XP before multipliers = calories burned through activity (1 kcal = 1 XP). */
    fun baseXp(p: Profile, d: DayData): Double = Calories.activityBurn(p, d) * XP_PER_KCAL

    /**
     * Carry weight and calories-consumed forward across the log: a day with no weigh-in inherits
     * the most recent prior weigh-in (falling back to [defaultWeightKg] before the first one), and
     * a day with no logged intake (-1) inherits the previous day's intake (-1 until the first
     * logged day). An explicit 0 is a logged fast and sticks — only the -1 sentinel is replaced.
     * "Carry forward unless changed" — so an unlogged day still uses yesterday's body weight for
     * its energy math and yesterday's intake for its deficit/surplus. Pure + deterministic;
     * the engine and UI both run the raw log through this before reading per-day weight/intake.
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

    /** XP for a day: calories burned, scaled by consistency + deficit multipliers, then docked
     *  for any calorie surplus. Can be negative on a day you ate over your burn and barely moved. */
    private fun dayXp(p: Profile, d: DayData, strengthStreak: Int): Long {
        val base = baseXp(p, d)
        // Calories.deficit is signed: > 0 under your burn (deficit), < 0 over it (surplus),
        // 0 when no food was logged.
        val deficit = Calories.deficit(p, d)
        var xp = 0.0
        if (base > 0.0) {
            val completionMult = if (completion(d) >= 1.0) 1.0 + COMPLETION_BONUS else 1.0
            val streakMult = 1.0 + STREAK_BONUS_PER_DAY * min(strengthStreak, STREAK_BONUS_CAP_DAYS)
            val deficitMult = if (deficit > 0)
                1.0 + DEFICIT_BONUS_MAX * min(deficit / DEFICIT_FULL_BONUS_KCAL, 1.0)
            else 1.0
            xp = base * completionMult * streakMult * deficitMult
        }
        if (deficit < 0.0) {
            val surplus = -deficit
            xp -= SURPLUS_PENALTY_MAX_XP * min(surplus / SURPLUS_FULL_PENALTY_KCAL, 1.0)
        }
        return Math.round(xp)
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

            val comp = completion(d)
            // Proportional shortfall — the "lose XP for what you didn't do" half of the rule.
            // A day is scored against the targets: 100% loses nothing, 0% loses a full day's
            // worth, 40% loses 60% of one. Only days at/after the decay anchor (pre-app history
            // is never punished) and strictly before today (today isn't over) are charged.
            val shortfall = if (today != null && decayAnchor != null &&
                !d.date.isBefore(decayAnchor) && d.date.isBefore(today)
            ) Math.round(
                (1.0 - comp.coerceAtMost(1.0)).coerceAtLeast(0.0) *
                    missedDayPenalty(Calories.weightFor(profile, d))
            ) else 0L
            val xp = dayXp(profile, d, strengthStreak) - shortfall
            totalXp += xp
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

        // --- Inactivity decay (PERMANENT) -------------------------------------
        // The other half of per-day scoring: days with NO log entry at all. (Logged days are
        // already charged their proportional shortfall in the loop above; this walk covers the
        // calendar days that never got a row, each costing a full missedDayPenalty at the body
        // weight carried into that day.) Never refunded: returning to activity doesn't erase a
        // past gap — you earn it back, you don't get it back. Pre-anchor history (e.g. an
        // imported backlog) is ignored, and today is never charged while it's still in progress.
        var idleDays = 0          // trailing idle days (drives the "log today" nudge)
        var interiorPenalty = 0L  // locked-in decay from before the last activity (unstoppable)
        var trailingPenalty = 0L  // the still-growing gap since the last activity
        var trailingCharged = 0   // unlogged days actually charged in that trailing gap
        if (today != null && decayAnchor != null) {
            val loggedDates = sorted.mapTo(HashSet()) { it.date }
            val lastActive = sorted.lastOrNull { it.hasActivity && !it.date.isAfter(today) }?.date
            var weight = profile.weightKg
            var idx = 0
            var day: LocalDate = decayAnchor
            while (day.isBefore(today)) {
                // Carry the body weight forward day by day, same as the earnings math.
                while (idx < sorted.size && !sorted[idx].date.isAfter(day)) {
                    if (sorted[idx].weightKg > 0.0) weight = sorted[idx].weightKg
                    idx++
                }
                if (day !in loggedDates) {
                    val pen = missedDayPenalty(weight)
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

    /** Everything the UI needs from one replay: character (with bonuses), per-day derived
     *  values, and the achievement list the bonus XP was computed from. */
    data class FullBuild(
        val state: CharacterState,
        val derived: Map<LocalDate, DayDerived>,
        val achievements: List<AchStatus>
    )

    /** Fold quest + achievement bonus XP into a base state and re-derive level/rank/title. */
    private fun withBonuses(base: CharacterState, questXp: Long, achXp: Long): CharacterState {
        val effective = (base.earnedXp + questXp + achXp - base.idlePenaltyXp).coerceAtLeast(0)
        val li = levelForXp(effective)
        return base.copy(
            totalXp = effective,
            questBonusXp = questXp,
            achievementBonusXp = achXp,
            level = li.level,
            rank = rankForLevel(li.level),
            title = titleForLevel(li.level),
            xpIntoLevel = li.into,
            xpForNextLevel = li.forNext
        )
    }

    /**
     * [rebuild] plus the reward loop: completed quests (replayed over the whole log) and
     * unlocked achievements pay the XP the UI advertises. Achievement XP can raise the level,
     * which can unlock level/rank achievements — so we iterate to a fixpoint. Unlocks only ever
     * add XP and XP only ever unlocks more, so this is monotone and converges in a few passes.
     */
    fun rebuildFull(
        days: List<DayData>,
        today: LocalDate? = null,
        decayAnchor: LocalDate? = null,
        profile: Profile = Profile()
    ): FullBuild {
        val (base, derived) = rebuild(days, today, decayAnchor, profile)
        val questXp = Quests.earnedXp(days, profile)
        var state = withBonuses(base, questXp, 0L)
        var ach = Achievements.evaluate(days, state)
        repeat(8) {
            val next = withBonuses(base, questXp, Achievements.unlockedXp(ach))
            val nextAch = Achievements.evaluate(days, next)
            val stable = nextAch.count { it.unlocked } == ach.count { it.unlocked }
            state = next
            ach = nextAch
            if (stable) return FullBuild(state, derived, ach)
        }
        return FullBuild(state, derived, ach)
    }
}
