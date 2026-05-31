package com.paisa.app.notifications

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
import com.paisa.app.MainActivity
import androidx.core.content.ContextCompat
import com.paisa.app.R
import com.paisa.app.data.TransactionType
import com.paisa.app.domain.ParsedMoneyEntry
import com.paisa.app.domain.formatInr

object NotificationHelper {
    private const val CHANNEL_ID = "money_awareness"
    private const val CHANNEL_NAME = "Money awareness"
    const val ACTION_SAVE_SMS_CATEGORY = "com.paisa.app.action.SAVE_SMS_CATEGORY"
    const val EXTRA_CATEGORY = "com.paisa.app.extra.CATEGORY"
    const val EXTRA_AMOUNT_PAISE = "com.paisa.app.extra.AMOUNT_PAISE"
    const val EXTRA_TRANSACTION_TYPE = "com.paisa.app.extra.TRANSACTION_TYPE"
    const val EXTRA_CREATED_AT = "com.paisa.app.extra.CREATED_AT"
    const val EXTRA_NOTE = "com.paisa.app.extra.NOTE"
    const val EXTRA_RAW_TEXT = "com.paisa.app.extra.RAW_TEXT"
    const val EXTRA_PERSON_NAME = "com.paisa.app.extra.PERSON_NAME"
    const val EXTRA_NOTIFICATION_ID = "com.paisa.app.extra.NOTIFICATION_ID"
    private val smsCategories = listOf("food", "travel", "needs", "football")

    fun show(context: Context, id: Int, title: String, body: String) {
        show(context, id, title, body, null)
    }

    fun showDetectedExpense(context: Context, transactionId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_EDIT_TRANSACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_EDIT_TRANSACTION_ID, transactionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            transactionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        show(
            context = context,
            id = transactionId.hashCode(),
            title = "Expense added from SMS",
            body = "We detected and added an expense. Tap to edit.",
            contentIntent = pendingIntent
        )
    }

    fun showSmsCategoryPrompt(context: Context, entry: ParsedMoneyEntry, smsTimestampMillis: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val notificationId = "${entry.rawText}|$smsTimestampMillis".hashCode()
        val title = if (entry.type == TransactionType.INCOME) {
            "Money received"
        } else {
            "Payment detected"
        }
        val body = "${entry.amountPaise.formatInr()} ${entry.note.ifBlank { "from SMS" }}. Choose a category to save."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(false)

        smsCategories.forEach { category ->
            builder.addAction(
                R.drawable.ic_launcher_foreground,
                category.replaceFirstChar { it.titlecase() },
                categoryPendingIntent(context, entry, smsTimestampMillis, notificationId, category)
            )
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    private fun categoryPendingIntent(
        context: Context,
        entry: ParsedMoneyEntry,
        smsTimestampMillis: Long,
        notificationId: Int,
        category: String
    ): PendingIntent {
        val intent = Intent(context, SmsCategoryReceiver::class.java).apply {
            action = ACTION_SAVE_SMS_CATEGORY
            putExtra(EXTRA_CATEGORY, category)
            putExtra(EXTRA_AMOUNT_PAISE, entry.amountPaise)
            putExtra(EXTRA_TRANSACTION_TYPE, entry.type.name)
            putExtra(EXTRA_CREATED_AT, smsTimestampMillis)
            putExtra(EXTRA_NOTE, entry.note)
            putExtra(EXTRA_RAW_TEXT, entry.rawText)
            putExtra(EXTRA_PERSON_NAME, entry.personName)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(
            context,
            "$notificationId:$category".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun show(context: Context, id: Int, title: String, body: String, contentIntent: PendingIntent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Daily, weekly and monthly financial summaries"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
