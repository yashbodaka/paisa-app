package com.paisa.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.paisa.app.data.MoneyRepository
import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsCategoryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_SAVE_SMS_CATEGORY) return

        val category = intent.getStringExtra(NotificationHelper.EXTRA_CATEGORY) ?: return
        val amountPaise = intent.getLongExtra(NotificationHelper.EXTRA_AMOUNT_PAISE, 0L)
        if (amountPaise <= 0L) return

        val typeName = intent.getStringExtra(NotificationHelper.EXTRA_TRANSACTION_TYPE) ?: TransactionType.EXPENSE.name
        val type = runCatching { TransactionType.valueOf(typeName) }.getOrDefault(TransactionType.EXPENSE)
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, amountPaise.hashCode())
        val createdAt = intent.getLongExtra(NotificationHelper.EXTRA_CREATED_AT, System.currentTimeMillis())
        val note = intent.getStringExtra(NotificationHelper.EXTRA_NOTE).orEmpty()
        val rawText = intent.getStringExtra(NotificationHelper.EXTRA_RAW_TEXT).orEmpty()
        val personName = intent.getStringExtra(NotificationHelper.EXTRA_PERSON_NAME)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = MoneyRepository(PaisaDatabase.getDatabase(context).transactionDao())
                repository.addTransaction(
                    MoneyTransaction(
                        amountPaise = amountPaise,
                        type = type,
                        category = category,
                        personName = personName,
                        note = note,
                        rawText = rawText,
                        createdAt = createdAt
                    )
                )
                NotificationManagerCompat.from(context).cancel(notificationId)
                NotificationHelper.show(
                    context = context,
                    id = notificationId + 1,
                    title = "Payment saved",
                    body = "Saved as $category."
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
