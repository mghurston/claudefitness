package com.mhurston.ascendant.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.mhurston.ascendant.R
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

/** Local, offline daily reminders. Day-aware: stronger nudge on Wed/Fri/Sat (the cliffs). */
object Reminders {
    const val CHANNEL_ID = "ascendant_reminders"
    private const val WORK_NAME = "ascendant_daily_reminder"
    private const val NOTIF_ID = 4242

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Training reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily nudge to keep your streak alive" }
            mgr.createNotificationChannel(channel)
        }
    }

    /** Fires once a day at 00:00 UTC (≈ 5 PM Pacific Daylight Time). The day's rollover is
     *  the same wall-clock instant everywhere, so the nudge lands as the global day flips. */
    fun schedule(context: Context) {
        ensureChannel(context)
        val now = Instant.now()
        val nextUtcMidnight = LocalDate.now(ZoneOffset.UTC)
            .plusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
        val delay = Duration.between(now, nextUtcMidnight).toMillis().coerceAtLeast(0)
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    @android.annotation.SuppressLint("MissingPermission") // guarded by hasPermission() below
    fun postReminder(context: Context) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val msg = when (LocalDate.now().dayOfWeek) {
            DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY ->
                "Boss day — these are where streaks die. 20 reps keeps it alive."
            else -> "Don't break the chain. Log today's training to keep your streak."
        }
        // Tapping the notification must open the app — without a contentIntent it does nothing.
        val launchIntent = Intent(context, com.mhurston.ascendant.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ASCENDANT")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Reminders.postReminder(applicationContext)
        return Result.success()
    }
}
