package com.mhurston.ascendant.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.Period
import kotlin.math.roundToInt

/** One day's passive totals read from Health Connect. */
data class PassiveDay(val date: LocalDate, val steps: Int, val kcal: Int)

/**
 * Thin wrapper over the Health Connect client. We only ever READ aggregated daily totals
 * (steps + active calories) — no GPS, no foreground service, no writing. Battery cost ≈ 0.
 * Everything degrades gracefully when Health Connect is unavailable (older devices, or the
 * provider app not installed): callers just hide the feature.
 */
object HealthConnect {

    /** The read permissions we request. Distance is intentionally omitted — passive burn comes
     *  from active-calorie records (or a step estimate), and passive distance is never used. */
    val PERMISSIONS: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    )

    /** True when Health Connect is installed/available and ready to use on this device. */
    fun isAvailable(context: Context): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    fun clientOrNull(context: Context): HealthConnectClient? =
        if (isAvailable(context)) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        else null

    /** Contract for requesting the read permissions from a Compose launcher. */
    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(client: HealthConnectClient): Boolean =
        client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)

    /**
     * Aggregate steps + active calories per local day over [start]..[end] (inclusive).
     * Days with no records simply come back as 0/0. Buckets are one calendar day each.
     */
    suspend fun readDaily(
        client: HealthConnectClient,
        start: LocalDate,
        end: LocalDate
    ): List<PassiveDay> {
        val request = AggregateGroupByPeriodRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
            ),
            timeRangeSlicer = Period.ofDays(1)
        )
        return client.aggregateGroupByPeriod(request).map { bucket ->
            val steps = (bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
            val kcal = bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories?.roundToInt() ?: 0
            PassiveDay(bucket.startTime.toLocalDate(), steps, kcal)
        }
    }
}
