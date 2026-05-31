package com.paisa.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val DAILY_WORK = "daily_spending_summary"
    private const val WEEKLY_WORK = "weekly_spending_summary"
    private const val MONTHLY_WORK = "monthly_spending_summary"

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        NotificationHelper.ensureChannel(context)

        // Daily at 9:00 PM
        val dailyRequest = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(LocalTime.of(21, 0)), TimeUnit.MILLISECONDS)
            .build()

        // Weekly (Sunday) at 9:00 PM
        val weeklyRequest = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntilSunday(LocalTime.of(21, 0)), TimeUnit.MILLISECONDS)
            .build()

        // Monthly at 9:00 PM (runs every 30 days as an approximation, or we can use 1 day check)
        // For precision, we'll run a daily check that only notifies on last day, 
        // OR a 30-day periodic with initial delay to end of month.
        val monthlyRequest = PeriodicWorkRequestBuilder<MonthlySummaryWorker>(30, TimeUnit.DAYS)
            .setInitialDelay(delayUntilEndOfMonth(LocalTime.of(21, 30)), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.UPDATE, dailyRequest)
        workManager.enqueueUniquePeriodicWork(WEEKLY_WORK, ExistingPeriodicWorkPolicy.UPDATE, weeklyRequest)
        workManager.enqueueUniquePeriodicWork(MONTHLY_WORK, ExistingPeriodicWorkPolicy.UPDATE, monthlyRequest)
    }

    private fun delayUntilSunday(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(time)
        while (target.dayOfWeek != DayOfWeek.SUNDAY || target.isBefore(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target).toMillis()
    }

    private fun delayUntilEndOfMonth(time: LocalTime): Long {
        val now = LocalDateTime.now()
        val lastDay = now.toLocalDate().with(TemporalAdjusters.lastDayOfMonth())
        var target = lastDay.atTime(time)
        if (target.isBefore(now)) {
            target = now.toLocalDate().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()).atTime(time)
        }
        return Duration.between(now, target).toMillis()
    }

    private fun delayUntil(time: LocalTime): Long {
        val now = LocalDateTime.now()
        val targetToday = now.toLocalDate().atTime(time)
        val target = if (targetToday.isAfter(now)) targetToday else targetToday.plusDays(1)
        return Duration.between(now, target).toMillis()
    }
}
