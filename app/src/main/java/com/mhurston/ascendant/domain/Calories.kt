package com.mhurston.ascendant.domain

enum class Sex { MALE, FEMALE }

data class Profile(
    val sex: Sex = Sex.MALE,
    val age: Int = 30,
    val heightCm: Double = 178.0,
    val weightKg: Double = 80.0,
    val goalWeightKg: Double = 0.0,  // 0 = no goal set
    val startWeightKg: Double = 0.0  // captured when a goal is first set
) {
    /** 0..1 progress from start weight toward the goal (direction-aware). */
    val goalProgress: Float
        get() {
            if (goalWeightKg <= 0.0 || startWeightKg <= 0.0) return 0f
            val total = startWeightKg - goalWeightKg
            if (total == 0.0) return 1f
            val done = startWeightKg - weightKg
            return (done / total).coerceIn(0.0, 1.0).toFloat()
        }

    val kgToGoal: Double get() = if (goalWeightKg > 0) weightKg - goalWeightKg else 0.0
    val goalReached: Boolean
        get() = goalWeightKg > 0 && (
            (startWeightKg >= goalWeightKg && weightKg <= goalWeightKg) ||
                (startWeightKg < goalWeightKg && weightKg >= goalWeightKg)
            )
}

data class EnergyEstimate(
    val bmr: Int,
    val activityBurn: Int,
    val totalBurn: Int,
    val consumed: Int,
    val net: Int // consumed - totalBurn (>0 surplus, <0 deficit)
)

/** Calorie estimates: Mifflin–St Jeor BMR + activity from the day's logged work. */
object Calories {

    /** Rough steps-per-mile used to estimate passive burn when a device reports steps but no
     *  active-calorie record. Walking burn then reuses the same WALK_KCAL_PER_KG_PER_MILE model. */
    const val STEPS_PER_MILE = 2000.0

    // ── Activity-energy model ────────────────────────────────────────────────────────────────
    // Every activity's calorie burn scales with the body weight entered on the Energy tab, via
    // the ACSM/Compendium MET formula:  kcal/min = MET × 3.5 × weightKg / 200.
    // These are *gross* calories — the same thing a treadmill or fitness tracker shows and what
    // "walking a mile burns ~X calories" references quote — so the numbers match expectations.
    // Burn is 1:1 XP (Progression.XP_PER_KCAL), so a heavier body, or one doing more work, earns
    // proportionally more. The constants below are the only place these rates are defined.

    /** Gross walking energy, per kg, per mile. Walking is logged as distance (not time), and
     *  gross kcal/mile is nearly pace-independent across normal speeds (a faster pace means a
     *  higher MET but proportionally less time). We use ~3.5 MET at ~3 mph (20 min/mi):
     *  3.5 × 3.5 × kg / 200 × 20 ≈ 1.2 kcal/kg/mile. For a 96 kg (212 lb) walker that's
     *  ≈115 kcal/mile — squarely in the commonly cited 105–120 kcal/mile for that body. */
    const val WALK_KCAL_PER_KG_PER_MILE = 1.2

    /** Gross strength/calisthenics energy, per kg, per rep. General moderate effort (~3.8 MET)
     *  at ~3 s per rep: 3.8 × 3.5 × kg / 200 × (3/60) ≈ 0.0033 kcal/kg/rep — ≈32 kcal per 100
     *  reps for a 96 kg lifter. Covers the core lifts and pinned custom exercises alike. */
    const val STRENGTH_KCAL_PER_KG_PER_REP = 0.0033

    /** Gross walking calories for [miles] at [weightKg]. */
    fun walkKcal(weightKg: Double, miles: Double): Double =
        WALK_KCAL_PER_KG_PER_MILE * weightKg.coerceAtLeast(0.0) * miles.coerceAtLeast(0.0)

    /** Gross calories for [reps] strength/calisthenics reps at [weightKg]. Single source of
     *  truth shared by the burn engine and the UI's per-exercise rep-XP preview. */
    fun strengthKcal(weightKg: Double, reps: Int): Double =
        STRENGTH_KCAL_PER_KG_PER_REP * weightKg.coerceAtLeast(0.0) * reps.coerceAtLeast(0)

    /** Display-only daily step goal for the passive "Steps today" ring. Not part of the
     *  workout completion formula or any RPG goal — purely a movement target for the ring. */
    const val PASSIVE_STEP_GOAL = 10_000

    fun bmr(p: Profile): Double {
        if (p.weightKg <= 0 || p.heightCm <= 0 || p.age <= 0) return 0.0
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
        return if (p.sex == Sex.MALE) base + 5 else base - 161
    }

    /** Total gross activity calories for the day, all scaled by the entered body weight:
     *  walking (treadmill/manual miles) + strength (core lifts + pinned customs) + time cardio
     *  (bike/swim via MET) + one-off activities + passive walking from Health Connect. This one
     *  number drives both the Energy screen's "activity" line and the burn-based XP engine, so
     *  they always agree. */
    fun activityBurn(p: Profile, day: DayData): Double {
        if (p.weightKg <= 0) return 0.0
        val walk = walkKcal(p.weightKg, day.miles)
        val strength = strengthKcal(p.weightKg, day.strengthReps + day.customRepsTotal)
        // MET-based time cardio (bike/swim): kcal/min = MET × 3.5 × kg / 200.
        val cardio = day.cardioMinutes.entries.sumOf { (id, min) ->
            com.mhurston.ascendant.domain.CardioActivity.metFor(id) * 3.5 * p.weightKg / 200.0 *
                min.coerceAtLeast(0)
        }
        // Passive activity from Health Connect. Phone pedometers report a conservative net
        // active-calorie number, so take the GREATER of the measured value and our own
        // step-based walking estimate — that way measured workout data is never discarded, but
        // plain walking can never read below the honest moderate-pace estimate. (max, not sum:
        // both describe the same steps, so adding would double count.)
        val stepEstimate = walkKcal(p.weightKg, day.passiveSteps.coerceAtLeast(0) / STEPS_PER_MILE)
        val passive = maxOf(day.passiveKcal.toDouble(), stepEstimate)
        return walk + strength + cardio + day.oneOffKcal + passive
    }

    fun estimate(p: Profile, day: DayData, consumed: Int): EnergyEstimate {
        val b = bmr(p)
        val a = activityBurn(p, day)
        val total = b + a
        return EnergyEstimate(
            bmr = Math.round(b).toInt(),
            activityBurn = Math.round(a).toInt(),
            totalBurn = Math.round(total).toInt(),
            consumed = consumed,
            net = Math.round(consumed - total).toInt()
        )
    }

    /** Personalized daily active-burn goal ≈ 25% of BMR, rounded to a clean 25. Scales with each
     *  person's body (sex/height/weight/age), so it's right for anyone without hardcoded targets.
     *  Falls back to a flat default until the profile is filled in. */
    fun dailyBurnTarget(p: Profile): Int {
        val b = bmr(p)
        if (b <= 0) return 400
        return (Math.round(b * 0.25 / 25.0) * 25).toInt().coerceAtLeast(150)
    }

    /** Weekly active-burn goal = six daily targets (one rest day allowed). */
    fun weeklyBurnTarget(p: Profile): Int = dailyBurnTarget(p) * 6

    /** Calorie deficit for a day (total burn − consumed), positive when in a deficit.
     *  Zero when no food was logged (consumed == 0) — we can't claim a deficit we can't see. */
    fun deficit(p: Profile, day: DayData): Double {
        if (day.caloriesConsumed <= 0) return 0.0
        val total = bmr(p) + activityBurn(p, day)
        return total - day.caloriesConsumed
    }
}
