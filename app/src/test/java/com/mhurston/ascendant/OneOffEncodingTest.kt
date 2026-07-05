package com.mhurston.ascendant

import com.mhurston.ascendant.data.WorkoutDayEntity
import com.mhurston.ascendant.domain.OneOff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QA for the one-off encoding (name + kcal + optional distance/reps). The column is a single
 * delimited string, so the round-trip and — critically — backward compatibility with the older
 * two-field (name + kcal) rows must hold, or existing logs would lose data on read.
 */
class OneOffEncodingTest {

    private val us = '' // unit separator (between a record's fields)
    private val rs = '' // record separator (between records)

    @Test
    fun roundTrip_preservesNameKcalDistanceAndReps() {
        val list = listOf(
            OneOff("Morning ride", kcal = 196, distanceMi = 5.0, reps = 0),
            OneOff("Overhead press + bike kicks", kcal = 32, distanceMi = 0.0, reps = 120),
            OneOff("Spin class", kcal = 400)
        )
        val decoded = WorkoutDayEntity.decodeOneOffs(WorkoutDayEntity.encodeOneOffs(list))
        assertEquals(list, decoded)
    }

    @Test
    fun decode_oldTwoFieldFormat_stillReadsWithZeroMetrics() {
        // Rows written by the previous version: name<US>kcal, two records.
        val legacy = "Marathon${us}2600${rs}Yoga${us}150"
        val decoded = WorkoutDayEntity.decodeOneOffs(legacy)
        assertEquals(2, decoded.size)
        assertEquals(OneOff("Marathon", 2600, 0.0, 0), decoded[0])
        assertEquals(OneOff("Yoga", 150, 0.0, 0), decoded[1])
    }

    @Test
    fun decode_emptyOrBlank_returnsEmptyList() {
        assertTrue(WorkoutDayEntity.decodeOneOffs("").isEmpty())
        assertTrue(WorkoutDayEntity.decodeOneOffs("   ").isEmpty())
    }

    @Test
    fun decode_dropsRecordsWithBlankName() {
        // A stray record separator / nameless record must not become a phantom entry.
        val encoded = "${us}500${rs}Real${us}120"
        val decoded = WorkoutDayEntity.decodeOneOffs(encoded)
        assertEquals(1, decoded.size)
        assertEquals("Real", decoded[0].name)
    }

    @Test
    fun encode_stripsDelimiterCharsFromName_soDecodeStaysUnambiguous() {
        val sneaky = OneOff("a${us}b${rs}c", kcal = 10, distanceMi = 1.0, reps = 5)
        val decoded = WorkoutDayEntity.decodeOneOffs(WorkoutDayEntity.encodeOneOffs(listOf(sneaky)))
        assertEquals(1, decoded.size)
        // Name is sanitized (delimiters replaced with spaces) but the metrics survive intact.
        assertTrue("delimiters removed from name", us !in decoded[0].name && rs !in decoded[0].name)
        assertEquals(10, decoded[0].kcal)
        assertEquals(1.0, decoded[0].distanceMi, 0.0001)
        assertEquals(5, decoded[0].reps)
    }

    @Test
    fun roundTrip_preservesActivityId_andOldRowsDecodeAsBlank() {
        // The 5th field remembers which activity a distance was logged as (walk/run/bike).
        val run = OneOff("Tempo run", kcal = 300, distanceMi = 3.0, activityId = "RUN")
        val decoded = WorkoutDayEntity.decodeOneOffs(WorkoutDayEntity.encodeOneOffs(listOf(run)))
        assertEquals("RUN", decoded[0].activityId)
        // Rows written before the field existed decode with a blank id (resolves to WALK).
        val legacy = "Ride${us}200${us}5.0${us}0"
        assertEquals("", WorkoutDayEntity.decodeOneOffs(legacy)[0].activityId)
        assertEquals(
            com.mhurston.ascendant.domain.DistanceActivity.WALK,
            com.mhurston.ascendant.domain.DistanceActivity.forId("")
        )
    }

    @Test
    fun roundTrip_preservesFractionalDistance() {
        val decoded = WorkoutDayEntity.decodeOneOffs(
            WorkoutDayEntity.encodeOneOffs(listOf(OneOff("Trail run", kcal = 250, distanceMi = 3.7)))
        )
        assertEquals(3.7, decoded[0].distanceMi, 0.0001)
    }

    @Test
    fun metricsLabel_showsOnlyRecordedMetrics() {
        assertEquals("120 reps", OneOff("Press", 32, reps = 120).metricsLabel())
        assertEquals("5.0 mi", OneOff("Ride", 196, distanceMi = 5.0).metricsLabel())
        assertEquals("60 reps · 2.5 mi", OneOff("Mixed", 80, distanceMi = 2.5, reps = 60).metricsLabel())
        assertEquals("", OneOff("Calories only", 300).metricsLabel())
    }

    @Test
    fun metricsLabel_convertsDistanceToMetricWhenRequested() {
        // 5 mi ≈ 8.0 km; reps are unit-agnostic.
        assertEquals(
            "60 reps · 8.0 km",
            OneOff("Ride", 196, distanceMi = 5.0, reps = 60)
                .metricsLabel(com.mhurston.ascendant.domain.UnitSystem.METRIC)
        )
    }
}
