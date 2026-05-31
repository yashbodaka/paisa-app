package com.paisa.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.paisa.app.data.MoneyRepository
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.domain.SummaryCalculator
import com.paisa.app.domain.formatInr
import java.time.LocalDate

class MonthlySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // We only want to notify on the last day of the month or 1st day (summarizing previous)
        // For simplicity, we schedule it monthly and it runs at the end.
        
        val repository = MoneyRepository(PaisaDatabase.getDatabase(applicationContext).transactionDao())
        val summary = SummaryCalculator.buildSummary(repository.getTransactions())

        if (summary.monthExpensePaise > 0) {
            val savings = (summary.monthIncomePaise - summary.monthExpensePaise).takeIf { it > 0 }
            val savingMsg = if (savings != null) " You saved ${savings.formatInr()}!" else ""
            
            NotificationHelper.show(
                context = applicationContext,
                id = 1003,
                title = "Monthly Spend Summary",
                body = "This month you spent ${summary.monthExpensePaise.formatInr()} and earned ${summary.monthIncomePaise.formatInr()}.$savingMsg"
            )
        }

        return Result.success()
    }
}
