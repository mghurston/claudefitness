package com.mhurston.ascendant.domain

import kotlin.math.roundToInt

/** US users default to imperial; metric is an opt-in toggle. Storage stays metric. */
enum class UnitSystem { IMPERIAL, METRIC }

object Units {
    private const val LB_PER_KG = 2.2046226218
    private const val CM_PER_IN = 2.54

    fun kgToLbs(kg: Double): Double = kg * LB_PER_KG
    fun lbsToKg(lbs: Double): Double = lbs / LB_PER_KG
    fun cmToInches(cm: Double): Double = cm / CM_PER_IN
    fun inchesToCm(inches: Double): Double = inches * CM_PER_IN

    /** cm → (feet, inches) rounded to the nearest inch. */
    fun cmToFeetInches(cm: Double): Pair<Int, Int> {
        val totalIn = (cm / CM_PER_IN).roundToInt()
        return (totalIn / 12) to (totalIn % 12)
    }

    fun feetInchesToCm(feet: Int, inches: Int): Double = (feet * 12 + inches) * CM_PER_IN
}
