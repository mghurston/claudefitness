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

    fun bmr(p: Profile): Double {
        if (p.weightKg <= 0 || p.heightCm <= 0 || p.age <= 0) return 0.0
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
        return if (p.sex == Sex.MALE) base + 5 else base - 161
    }

    /** Walking ≈ 0.57 kcal/kg/mile; strength (core + pinned customs) ≈ small per-rep burn;
     *  one-off activities carry their own calorie estimate. This single number drives both
     *  the Energy screen's "activity" line and the burn-based XP engine, so they always agree. */
    fun activityBurn(p: Profile, day: DayData): Double {
        if (p.weightKg <= 0) return 0.0
        val walk = 0.57 * p.weightKg * day.miles
        val strength = 0.0019 * p.weightKg * (day.strengthReps + day.customRepsTotal)
        // MET-based time cardio (bike/swim): kcal/min = MET × 3.5 × kg / 200.
        val cardio = day.cardioMinutes.entries.sumOf { (id, min) ->
            com.mhurston.ascendant.domain.CardioActivity.metFor(id) * 3.5 * p.weightKg / 200.0 *
                min.coerceAtLeast(0)
        }
        return walk + strength + cardio + day.oneOffKcal
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
