package com.paisa.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val DAILY_WORK = "daily_spending_summary"
    private const val WEEKLY_WORK = "weekly_spending_summary"

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val dailyRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(LocalTime.of(21, 0)), TimeUnit.MILLISECONDS)
            .build()

        val weeklyRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(LocalTime.of(18, 0)).coerceAtLeast(TimeUnit.HOURS.toMillis(1)), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DAILY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
        workManager.enqueueUniquePeriodicWork(
            WEEKLY_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            weeklyRequest
        )
    }

    private fun delayUntil(time: LocalTime): Long {
        val now = LocalDateTime.now()
        val targetToday = now.toLocalDate().atTime(time)
        val target = if (targetToday.isAfter(now)) targetToday else targetToday.plusDays(1)
        return Duration.between(now, target).toMillis()
    }
}

