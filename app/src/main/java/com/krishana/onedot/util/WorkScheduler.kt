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

        // Calculate initial delay to ~01:00 tomorrow.
        // Combined with a 60-minute flex, the execution window becomes [00:00, 01:00] —
        // entirely AFTER midnight, guaranteeing LocalDate.now() returns the new date.
        // (Previously targeting 00:01 put the window at [23:01, 00:01], causing
        //  the wallpaper to be generated with yesterday's date.)
        val now = LocalDateTime.now()
        val target = now.plusDays(1).withHour(1).withMinute(0).withSecond(0).withNano(0)
        val initialDelayMinutes = Duration.between(now, target).toMinutes()

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
