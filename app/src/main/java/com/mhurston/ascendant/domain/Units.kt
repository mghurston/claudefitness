package com.mhurston.ascendant.domain

import kotlin.math.roundToInt

/** US users default to imperial; metric is an opt-in toggle. Storage stays metric. */
enum class UnitSystem { IMPERIAL, METRIC }

object Units {
    private const val LB_PER_KG = 2.2046226218
    private const val CM_PER_IN = 2.54
    private const val KM_PER_MILE = 1.609344

    fun kgToLbs(kg: Double): Double = kg * LB_PER_KG
    fun lbsToKg(lbs: Double): Double = lbs / LB_PER_KG
    fun cmToInches(cm: Double): Double = cm / CM_PER_IN
    fun inchesToCm(inches: Double): Double = inches * CM_PER_IN

    /** Distance is stored internally in miles; convert only at the UI boundary. */
    fun milesToKm(miles: Double): Double = miles * KM_PER_MILE
    fun kmToMiles(km: Double): Double = km / KM_PER_MILE

    /** Short distance label in the user's units, e.g. "5.0 mi" or "8.0 km". */
    fun distanceLabel(miles: Double, unit: UnitSystem): String {
        val (value, suffix) = if (unit == UnitSystem.METRIC) milesToKm(miles) to "km" else miles to "mi"
        return String.format(java.util.Locale.US, "%.1f %s", value, suffix)
    }

    /** Distance unit suffix on its own ("mi" / "km"). */
    fun distanceSuffix(unit: UnitSystem): String = if (unit == UnitSystem.METRIC) "km" else "mi"

    /** cm → (feet, inches) rounded to the nearest inch. */
    fun cmToFeetInches(cm: Double): Pair<Int, Int> {
        val totalIn = (cm / CM_PER_IN).roundToInt()
        return (totalIn / 12) to (totalIn % 12)
    }

    fun feetInchesToCm(feet: Int, inches: Int): Double = (feet * 12 + inches) * CM_PER_IN
}
