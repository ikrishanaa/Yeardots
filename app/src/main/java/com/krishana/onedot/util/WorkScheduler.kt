package com.krishana.onedot.util

import android.content.Context
import androidx.work.*
import com.krishana.onedot.worker.WallpaperWorker
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import java.time.Duration

/**
 * Utility object for scheduling WorkManager tasks.
 */
object WorkScheduler {

    private const val WALLPAPER_WORK_NAME = "wallpaper_daily_update"

    /**
     * Schedules a daily periodic work to update the wallpaper.
     */
    fun scheduleDailyWallpaperUpdate(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // Allow even on low battery
            .setRequiresStorageNotLow(false)
            .build()

        // Calculate initial delay to ~00:01 tomorrow
        val now = LocalDateTime.now()
        val tomorrow = now.plusDays(1).withHour(0).withMinute(1).withSecond(0).withNano(0)
        val initialDelayMinutes = Duration.between(now, tomorrow).toMinutes()

        // CRITICAL FIX #5: Increase flex interval from 15 to 60 minutes for better reliability
        // Android WorkManager recommends at least 30+ minutes flex for daily periodic work
        // A 15-minute window is too restrictive and may cause execution failures
        val dailyWorkRequest = PeriodicWorkRequestBuilder<WallpaperWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 60,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WALLPAPER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Don't duplicate if already scheduled
            dailyWorkRequest
        )
    }

    /**
     * Triggers an immediate one-time wallpaper update (for "Apply Now" button).
     */
    fun triggerImmediateUpdate(context: Context) {
        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<WallpaperWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
    }

    /**
     * Cancels all scheduled wallpaper updates.
     */
    fun cancelScheduledUpdates(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WALLPAPER_WORK_NAME)
    }
}
