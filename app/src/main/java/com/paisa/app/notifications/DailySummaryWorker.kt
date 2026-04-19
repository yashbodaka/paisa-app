package com.paisa.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.app.data.MoneyRepository
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.domain.SummaryCalculator
import com.paisa.app.domain.formatInr

class DailySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = MoneyRepository(PaisaDatabase.getDatabase(applicationContext).transactionDao())
        val summary = SummaryCalculator.buildSummary(repository.getTransactions())

        if (summary.todaySpendingPaise > 0) {
            val category = summary.topCategory.takeIf { it != "none" } ?: "daily spending"
            NotificationHelper.show(
                context = applicationContext,
                id = 1001,
                title = "Today in Paisa",
                body = "You spent ${summary.todaySpendingPaise.formatInr()} today. Most on $category."
            )
        }

        return Result.success()
    }
}

