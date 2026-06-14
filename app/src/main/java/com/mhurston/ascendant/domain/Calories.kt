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

    /** Walking ≈ 0.57 kcal/kg/mile; strength ≈ small per-rep burn. */
    fun activityBurn(p: Profile, day: DayData): Double {
        if (p.weightKg <= 0) return 0.0
        val walk = 0.57 * p.weightKg * day.miles
        val strength = 0.0019 * p.weightKg * day.strengthReps
        return walk + strength
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
}
