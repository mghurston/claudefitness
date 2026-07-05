package com.mhurston.ascendant.domain

import java.time.DayOfWeek
import java.time.temporal.ChronoUnit

enum class Rarity(val xp: Int) {
    COMMON(100), UNCOMMON(250), RARE(500), EPIC(1000), LEGENDARY(2500), MYTHIC(5000)
}

/**
 * One achievement. Unified model: [value] over the player's facts vs a [target];
 * unlocked when value >= target, and value/target drives the progress bar.
 */
data class AchDef(
    val id: String,
    val title: String,
    val desc: String,
    val rarity: Rarity,
    val target: Int,
    val hidden: Boolean = false,
    val category: String = "General",
    val value: (AchFacts) -> Int
)

data class AchStatus(
    val def: AchDef,
    val unlocked: Boolean,
    val current: Int,
    val target: Int
)

/** Pre-computed aggregates every achievement reads. Pure function of the log + state. */
data class AchFacts(
    val state: CharacterState,
    val pushups: Int, val squats: Int, val legLifts: Int, val calfRaises: Int, val curls: Int,
    val miles: Int,
    val bestPushups: Int, val bestSquats: Int, val bestLegLifts: Int,
    val bestCalfRaises: Int, val bestCurls: Int, val bestMiles: Int,
    val full100Days: Int,
    val perfectWeds: Int, val perfectFris: Int, val perfectSats: Int,
    val longestWalkStreak: Int,
    val anyAllSix: Boolean,
    val overdriveDay: Boolean,
    val doubleOverdrive: Boolean,
    val activeDays: Int,
    val loggedDays: Int,
    val notesDays: Int,      // days with a non-blank note
    val moodDays: Int,       // days with a logged mood (1..5)
    val peakMoodDays: Int,   // days logged at mood 5 (Unstoppable)
    val prEvents: Int        // times a metric beat its previous personal best
) {
    private fun b(c: Boolean) = if (c) 1 else 0
    fun flag(c: Boolean) = b(c)
}

object Achievements {

    private fun rankAtLeast(s: CharacterState, r: Rank) = s.rank.ordinal >= r.ordinal

    /** Tag a group of achievements with a user-facing category (used to group the Trophies UI). */
    private fun cat(name: String, defs: List<AchDef>): List<AchDef> = defs.map { it.copy(category = name) }

    /** Display order of categories on the Trophies screen. */
    val CATEGORY_ORDER: List<String> = listOf(
        "Getting Started", "Push-ups", "Squats", "Arms & Core", "Walking",
        "Streaks", "Boss Days", "Levels & Ranks", "Stats", "Mastery",
        "Personal Records", "Journal"
    )

    val ALL: List<AchDef> =
        cat("Getting Started", listOf(
        AchDef("first_blood", "First Blood", "Log your very first workout.", Rarity.COMMON, 1) { it.flag(it.activeDays >= 1) },
        AchDef("awakening", "The Awakening", "Complete your first full 100% day.", Rarity.COMMON, 1) { it.flag(it.full100Days >= 1) },
        AchDef("hello_world", "Hello, World", "Log all six exercises in a single day.", Rarity.COMMON, 1) { it.flag(it.anyAllSix) },
        AchDef("import_complete", "Thirty Days on Record", "Log 30 total days of training.", Rarity.UNCOMMON, 30) { it.loggedDays },

        )) +
        cat("Push-ups", listOf(
        AchDef("push_start", "Push Start", "100 lifetime push-ups.", Rarity.COMMON, 100) { it.pushups },
        AchDef("press_forward", "Press Forward", "500 lifetime push-ups.", Rarity.COMMON, 500) { it.pushups },
        AchDef("thousand_fists", "Thousand Fists", "1,000 lifetime push-ups.", Rarity.UNCOMMON, 1000) { it.pushups },
        AchDef("iron_arms", "Iron Arms", "5,000 lifetime push-ups.", Rarity.RARE, 5000) { it.pushups },
        AchDef("ten_k_push", "Ten-Thousand Reps", "10,000 lifetime push-ups.", Rarity.EPIC, 10000) { it.pushups },
        AchDef("the_hundred", "The Hundred", "100 push-ups in a single day.", Rarity.COMMON, 100) { it.bestPushups },
        AchDef("over_capacity", "Over Capacity", "110+ push-ups in a day.", Rarity.UNCOMMON, 110) { it.bestPushups },

        )) +
        cat("Squats", listOf(
        AchDef("knees_bent", "Knees Bent", "100 lifetime squats.", Rarity.COMMON, 100) { it.squats },
        AchDef("leg_day", "Leg Day", "1,000 lifetime squats.", Rarity.UNCOMMON, 1000) { it.squats },
        AchDef("pillars", "Pillars", "5,000 lifetime squats.", Rarity.RARE, 5000) { it.squats },
        AchDef("quad_god", "Quad God", "10,000 lifetime squats.", Rarity.EPIC, 10000) { it.squats },
        AchDef("squat_century", "Squat Century", "100 squats in one day.", Rarity.COMMON, 100) { it.bestSquats },
        AchDef("deep_resolve", "Deep Resolve", "110+ squats in one day.", Rarity.UNCOMMON, 110) { it.bestSquats },

        )) +
        cat("Arms & Core", listOf(
        AchDef("curl_up", "Curl Up", "1,000 lifetime curls.", Rarity.UNCOMMON, 1000) { it.curls },
        AchDef("peak_contraction", "Peak Contraction", "5,000 lifetime curls.", Rarity.RARE, 5000) { it.curls },
        AchDef("guns_loaded", "Guns Loaded", "120 curls in a day.", Rarity.UNCOMMON, 120) { it.bestCurls },
        AchDef("core_ignition", "Core Ignition", "1,000 lifetime leg lifts.", Rarity.UNCOMMON, 1000) { it.legLifts },
        AchDef("hanging_tough", "Hanging Tough", "5,000 lifetime leg lifts.", Rarity.RARE, 5000) { it.legLifts },
        AchDef("calf_awakening", "Calf Awakening", "1,000 lifetime calf raises.", Rarity.UNCOMMON, 1000) { it.calfRaises },
        AchDef("mountain_stance", "Mountain Stance", "5,000 lifetime calf raises.", Rarity.RARE, 5000) { it.calfRaises },
        AchDef("tiptoe_titan", "Tiptoe Titan", "120 calf raises in a day.", Rarity.UNCOMMON, 120) { it.bestCalfRaises },

        )) +
        cat("Walking", listOf(
        AchDef("first_mile", "First Mile", "Walk 1 lifetime mile.", Rarity.COMMON, 1) { it.miles },
        AchDef("marathoner", "Marathoner", "26 lifetime miles.", Rarity.COMMON, 26) { it.miles },
        AchDef("century_walker", "Century Walker", "100 lifetime miles.", Rarity.UNCOMMON, 100) { it.miles },
        AchDef("five_hundred_club", "500 Club", "500 lifetime miles.", Rarity.RARE, 500) { it.miles },
        AchDef("cross_country", "Cross-Country", "1,000 lifetime miles.", Rarity.EPIC, 1000) { it.miles },
        AchDef("long_road", "The Long Road", "Walk 5+ miles in one day.", Rarity.COMMON, 5) { it.bestMiles },
        AchDef("six_mile_soul", "Six-Mile Soul", "Walk 6+ miles in one day.", Rarity.UNCOMMON, 6) { it.bestMiles },
        AchDef("never_skip_cardio", "Never Skip Cardio", "30-day walking streak.", Rarity.RARE, 30) { it.longestWalkStreak },
        AchDef("pilgrimage", "Pilgrimage", "50-day walking streak.", Rarity.EPIC, 50) { it.longestWalkStreak },

        )) +
        cat("Streaks", listOf(
        AchDef("spark", "Spark", "2-day strength streak.", Rarity.COMMON, 2) { it.state.longestStrengthStreak },
        AchDef("momentum", "Momentum", "3-day strength streak.", Rarity.COMMON, 3) { it.state.longestStrengthStreak },
        AchDef("the_five", "The Five", "5-day strength streak.", Rarity.UNCOMMON, 5) { it.state.longestStrengthStreak },
        AchDef("break_record", "Break the Record", "6-day strength streak.", Rarity.RARE, 6) { it.state.longestStrengthStreak },
        AchDef("lucky_seven", "Lucky Seven", "7-day strength streak.", Rarity.RARE, 7) { it.state.longestStrengthStreak },
        AchDef("fortnight", "Fortnight Fighter", "14-day strength streak.", Rarity.EPIC, 14) { it.state.longestStrengthStreak },
        AchDef("three_weeks", "Three Weeks Strong", "21-day strength streak.", Rarity.EPIC, 21) { it.state.longestStrengthStreak },
        AchDef("unbroken", "The Unbroken", "30-day strength streak.", Rarity.LEGENDARY, 30) { it.state.longestStrengthStreak },
        AchDef("fifty_resolve", "Fifty Resolve", "50-day strength streak.", Rarity.LEGENDARY, 50) { it.state.longestStrengthStreak },
        AchDef("hundred_hunter", "Hundred-Day Legend", "100-day strength streak.", Rarity.MYTHIC, 100) { it.state.longestStrengthStreak },
        AchDef("perfect_week", "Perfect Week", "100% completion 7 days in a row.", Rarity.EPIC, 7) { it.state.perfectStreak },
        AchDef("no_zero_days", "No Zero Days", "30 days with some activity.", Rarity.RARE, 30) { it.activeDays },

        )) +
        cat("Boss Days", listOf(
        AchDef("hump_day_hero", "Hump-Day Hero", "100% on a Wednesday.", Rarity.UNCOMMON, 1) { it.flag(it.perfectWeds >= 1) },
        AchDef("wed_warlord", "Wednesday Warlord", "100% on 4 Wednesdays.", Rarity.RARE, 4) { it.perfectWeds },
        AchDef("friday_fever", "Friday Night Fever", "100% on a Friday.", Rarity.UNCOMMON, 1) { it.flag(it.perfectFris >= 1) },
        AchDef("tgif_titan", "TGIF Titan", "100% on 4 Fridays.", Rarity.RARE, 4) { it.perfectFris },
        AchDef("saturday_slayer", "Saturday Slayer", "100% on a Saturday.", Rarity.UNCOMMON, 1) { it.flag(it.perfectSats >= 1) },
        AchDef("weekend_wont_win", "Weekend Won't Win", "100% on 4 Saturdays.", Rarity.RARE, 4) { it.perfectSats },

        )) +
        cat("Levels & Ranks", listOf(
        AchDef("apprentice", "Apprentice", "Reach Level 5.", Rarity.COMMON, 5) { it.state.level },
        AchDef("stand_user", "Aura Wielder", "Reach Level 10.", Rarity.UNCOMMON, 10) { it.state.level },
        AchDef("rank_c", "Rank Up: C", "Reach Rank C.", Rarity.UNCOMMON, 1) { it.flag(rankAtLeast(it.state, Rank.C)) },
        AchDef("rank_b", "Rank Up: B", "Reach Rank B (Lv 20).", Rarity.RARE, 1) { it.flag(rankAtLeast(it.state, Rank.B)) },
        AchDef("rank_a", "Rank Up: A", "Reach Rank A (Lv 35).", Rarity.EPIC, 1) { it.flag(rankAtLeast(it.state, Rank.A)) },
        AchDef("rank_s", "Rank Up: S", "Reach Rank S (Lv 50).", Rarity.LEGENDARY, 1) { it.flag(rankAtLeast(it.state, Rank.S)) },
        AchDef("double_s", "Double-S", "Reach Rank SS (Lv 75).", Rarity.LEGENDARY, 1) { it.flag(rankAtLeast(it.state, Rank.SS)) },
        AchDef("ascendant_100", "ASCENDANT", "Reach Level 100.", Rarity.MYTHIC, 100) { it.state.level },
        AchDef("ten_k_reps", "10K Reps", "10,000 total strength reps.", Rarity.RARE, 10000) { it.state.totalStrengthReps },
        AchDef("twentyfive_k", "25K Reps", "25,000 total strength reps.", Rarity.EPIC, 25000) { it.state.totalStrengthReps },
        AchDef("fifty_k", "50K Reps", "50,000 total strength reps.", Rarity.LEGENDARY, 50000) { it.state.totalStrengthReps },
        AchDef("xp_magnate", "XP Magnate", "Earn 100,000 lifetime XP.", Rarity.EPIC, 100000) { it.state.earnedXp.toInt().coerceAtLeast(0) },

        )) +
        cat("Stats", listOf(
        AchDef("mighty", "Mighty", "Reach STR 10.", Rarity.UNCOMMON, 10) { it.state.stats.strength },
        AchDef("herculean", "Herculean", "Reach STR 25.", Rarity.EPIC, 25) { it.state.stats.strength },
        AchDef("marathon_lungs", "Marathon Lungs", "Reach END 25.", Rarity.UNCOMMON, 25) { it.state.stats.endurance },
        AchDef("unbreathing", "Unbreathing", "Reach END 50.", Rarity.EPIC, 50) { it.state.stats.endurance },
        AchDef("nimble", "Nimble", "Reach AGI 15.", Rarity.UNCOMMON, 15) { it.state.stats.agility },
        AchDef("disciplined", "Disciplined", "Reach DIS 25.", Rarity.RARE, 25) { it.state.stats.discipline },
        AchDef("iron_will", "Iron Will", "Reach DIS 50.", Rarity.EPIC, 50) { it.state.stats.discipline },
        AchDef("resolute", "Resolute", "Reach CON 15.", Rarity.RARE, 15) { it.state.stats.consistency },
        AchDef("unwavering", "Unwavering", "Reach CON 30.", Rarity.EPIC, 30) { it.state.stats.consistency },
        AchDef("balanced_soul", "Balanced Soul", "All five stats ≥ 15.", Rarity.RARE, 1) {
            val s = it.state.stats
            it.flag(minOf(s.strength, s.endurance, s.agility, s.discipline, s.consistency) >= 15)
        },
        AchDef("well_rounded", "Well-Rounded Warrior", "All five stats ≥ 25.", Rarity.LEGENDARY, 1) {
            val s = it.state.stats
            it.flag(minOf(s.strength, s.endurance, s.agility, s.discipline, s.consistency) >= 25)
        },

        )) +
        cat("Mastery", listOf(
        AchDef("overdrive", "Overdrive", "Exceed every daily target in one day.", Rarity.RARE, 1) { it.flag(it.overdriveDay) },
        AchDef("double_overdrive", "Double Overdrive", "Exceed every target on back-to-back days.", Rarity.EPIC, 1, hidden = true) { it.flag(it.doubleOverdrive) },

        )) +
        cat("Personal Records", listOf(
        AchDef("new_best", "New Personal Best", "Beat one of your own records.", Rarity.UNCOMMON, 1) { it.flag(it.prEvents >= 1) },
        AchDef("record_breaker", "Record Breaker", "Set 10 personal records.", Rarity.RARE, 10) { it.prEvents },
        AchDef("relentless", "Relentless", "Set 25 personal records.", Rarity.EPIC, 25) { it.prEvents },

        )) +
        cat("Journal", listOf(
        AchDef("field_notes", "Field Notes", "Write a note on 5 days.", Rarity.COMMON, 5) { it.notesDays },
        AchDef("dear_diary", "Dear Diary", "Write a note on 25 days.", Rarity.RARE, 25) { it.notesDays },
        AchDef("self_aware", "Self-Aware", "Log your mood on 10 days.", Rarity.UNCOMMON, 10) { it.moodDays },
        AchDef("peak_state", "Peak State", "Finish a day feeling Unstoppable.", Rarity.UNCOMMON, 1) { it.flag(it.peakMoodDays >= 1) },
        AchDef("beyond_sheet", "Beyond the Sheet", "1,000 total logged days.", Rarity.MYTHIC, 1000) { it.loggedDays },
        AchDef("the_collector", "The Collector", "Unlock 50 achievements.", Rarity.LEGENDARY, 50, hidden = true) {
            // Counted post-hoc in evaluate(); placeholder value filled there.
            0
        }
    ))

    fun computeFacts(days: List<DayData>, state: CharacterState): AchFacts {
        val sorted = days.sortedBy { it.date }
        var p = 0; var s = 0; var l = 0; var cr = 0; var cu = 0; var mi = 0.0
        var bp = 0; var bs = 0; var bl = 0; var bc = 0; var bcu = 0; var bm = 0.0
        var full100 = 0; var pw = 0; var pf = 0; var ps = 0
        var anySix = false; var overdrive = false; var doubleOver = false
        var active = 0
        var walkStreak = 0; var longestWalk = 0
        var prevDate: java.time.LocalDate? = null
        var prevOver = false
        var notesDays = 0; var moodDays = 0; var peakMoodDays = 0
        var prEvents = 0
        // running personal bests per metric (0 = never logged yet)
        var rbP = 0; var rbS = 0; var rbL = 0; var rbC = 0; var rbCu = 0; var rbM = 0.0

        for (d in sorted) {
            // Personal-record events: a metric strictly beating its prior (nonzero) best.
            if (rbP > 0 && d.pushups > rbP) prEvents++
            if (rbS > 0 && d.squats > rbS) prEvents++
            if (rbL > 0 && d.legLifts > rbL) prEvents++
            if (rbC > 0 && d.calfRaises > rbC) prEvents++
            if (rbCu > 0 && d.curls > rbCu) prEvents++
            if (rbM > 0.0 && d.miles > rbM) prEvents++
            rbP = maxOf(rbP, d.pushups); rbS = maxOf(rbS, d.squats); rbL = maxOf(rbL, d.legLifts)
            rbC = maxOf(rbC, d.calfRaises); rbCu = maxOf(rbCu, d.curls); rbM = maxOf(rbM, d.miles)
            if (d.notes.isNotBlank()) notesDays++
            if (d.mood in 1..5) moodDays++
            if (d.mood == 5) peakMoodDays++

            p += d.pushups; s += d.squats; l += d.legLifts; cr += d.calfRaises; cu += d.curls; mi += d.miles
            bp = maxOf(bp, d.pushups); bs = maxOf(bs, d.squats); bl = maxOf(bl, d.legLifts)
            bc = maxOf(bc, d.calfRaises); bcu = maxOf(bcu, d.curls); bm = maxOf(bm, d.miles)
            val comp = Progression.completion(d)
            if (comp >= 1.0) {
                full100++
                when (d.date.dayOfWeek) {
                    DayOfWeek.WEDNESDAY -> pw++
                    DayOfWeek.FRIDAY -> pf++
                    DayOfWeek.SATURDAY -> ps++
                    else -> {}
                }
            }
            if (d.pushups > 0 && d.squats > 0 && d.legLifts > 0 && d.calfRaises > 0 && d.curls > 0 && d.miles > 0) anySix = true
            val over = d.pushups > 100 && d.squats > 100 && d.legLifts > 100 && d.calfRaises > 100 && d.curls > 100 && d.miles > 5.0
            if (over) overdrive = true
            if (over && prevOver && prevDate != null && ChronoUnit.DAYS.between(prevDate, d.date) == 1L) doubleOver = true
            if (d.hasActivity) active++
            // walking streak (consecutive calendar days with miles > 0)
            val gap = prevDate?.let { ChronoUnit.DAYS.between(it, d.date) } ?: 1L
            if (gap > 1L) walkStreak = 0
            walkStreak = if (d.miles > 0) walkStreak + 1 else 0
            longestWalk = maxOf(longestWalk, walkStreak)
            prevDate = d.date
            prevOver = over
        }

        return AchFacts(
            state = state,
            pushups = p, squats = s, legLifts = l, calfRaises = cr, curls = cu, miles = mi.toInt(),
            bestPushups = bp, bestSquats = bs, bestLegLifts = bl, bestCalfRaises = bc, bestCurls = bcu, bestMiles = bm.toInt(),
            full100Days = full100, perfectWeds = pw, perfectFris = pf, perfectSats = ps,
            longestWalkStreak = longestWalk, anyAllSix = anySix,
            overdriveDay = overdrive, doubleOverdrive = doubleOver,
            activeDays = active, loggedDays = sorted.size,
            notesDays = notesDays, moodDays = moodDays, peakMoodDays = peakMoodDays,
            prEvents = prEvents
        )
    }

    fun evaluate(days: List<DayData>, state: CharacterState): List<AchStatus> {
        val facts = computeFacts(days, state)
        val base = ALL.map { def ->
            val v = def.value(facts)
            AchStatus(def, unlocked = v >= def.target, current = v.coerceAtMost(def.target), target = def.target)
        }
        // "The Collector" depends on how many others are unlocked.
        val unlockedCount = base.count { it.unlocked && it.def.id != "the_collector" }
        return base.map { st ->
            if (st.def.id == "the_collector")
                st.copy(unlocked = unlockedCount >= st.def.target, current = unlockedCount.coerceAtMost(st.def.target))
            else st
        }
    }

    fun unlockedXp(statuses: List<AchStatus>): Long =
        statuses.filter { it.unlocked }.sumOf { it.def.rarity.xp.toLong() }
}
