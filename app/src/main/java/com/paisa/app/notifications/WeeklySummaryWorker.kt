package com.paisa.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.app.data.MoneyRepository
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.domain.SummaryCalculator
import com.paisa.app.domain.formatInr
import kotlin.math.abs

class WeeklySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = MoneyRepository(PaisaDatabase.getDatabase(applicationContext).transactionDao())
        val summary = SummaryCalculator.buildSummary(repository.getTransactions())

        if (summary.weekSpendingPaise > 0) {
            val trend = when {
                summary.weeklyDeltaPaise > 0 -> "up by ${summary.weeklyDeltaPaise.formatInr()}"
                summary.weeklyDeltaPaise < 0 -> "down by ${abs(summary.weeklyDeltaPaise).formatInr()}"
                else -> "about the same as last week"
            }
            NotificationHelper.show(
                context = applicationContext,
                id = 1002,
                title = "Weekly Paisa summary",
                body = "You spent ${summary.weekSpendingPaise.formatInr()} this week, $trend. Top category: ${summary.topCategory}."
            )
        }

        return Result.success()
    }
}

