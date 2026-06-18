package com.mhurston.ascendant.health

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mhurston.ascendant.data.Repository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Pulls passive steps + active calories from Health Connect and banks them into the day log.
 *
 * Triggered two ways: on app foreground (immediate, cheap) and by a periodic WorkManager job
 * (~every 2 h) so days keep closing out even if the app is never opened — that's what makes it
 * feel "passive." Each sync re-reads a short back-window and OVERWRITES those days' totals with
 * the authoritative aggregate, so re-syncing can never double count (wearables backfill late).
 */
object PassiveSync {
    private const val WORK_NAME = "ascendant_passive_sync"
    /** Re-read today + the previous days to absorb late wearable/app backfill. */
    private const val BACK_WINDOW_DAYS = 2L

    /** Run one sync now. Safe to call anytime: no-ops unless enabled, available, and permitted. */
    suspend fun sync(context: Context): Boolean {
        val repo = Repository.get(context)
        if (!repo.passiveSyncEnabled.first()) return false
        val client = HealthConnect.clientOrNull(context) ?: return false
        if (!HealthConnect.hasAllPermissions(client)) return false

        val today = LocalDate.now()
        val start = today.minusDays(BACK_WINDOW_DAYS)
        val daily = runCatching { HealthConnect.readDaily(client, start, today) }.getOrNull() ?: return false
        for (d in daily) {
            repo.bankPassive(d.date.toString(), d.steps, d.kcal)
        }
        repo.setLastPassiveSync(Instant.now().toString())
        return true
    }

    /** Enqueue the periodic background sync (~every 2 h). Idempotent. */
    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<PassiveSyncWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

class PassiveSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        PassiveSync.sync(applicationContext)
        return Result.success()
    }
}
