package com.paisa.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.domain.SummaryCalculator
import com.paisa.app.domain.formatInr
import com.paisa.app.domain.formatSignedInr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PaisaAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        coroutineScope.launch {
            try {
                val database = PaisaDatabase.getDatabase(context)
                val transactions = database.transactionDao().getTransactions()
                val summary = SummaryCalculator.buildSummary(transactions)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.paisa_widget)

                    // 1. Update Wallet Balance
                    views.setTextViewText(
                        R.id.widget_wallet_balance,
                        "Wallet: ${summary.totalBalancePaise.formatSignedInr()}"
                    )
                    
                    val balanceColor = if (summary.totalBalancePaise >= 0) {
                        0xFF146C43.toInt() // Green
                    } else {
                        0xFF8C1D18.toInt() // Red
                    }
                    views.setTextColor(R.id.widget_wallet_balance, balanceColor)

                    // 2. Update Today's Summary
                    views.setTextViewText(
                        R.id.widget_today_status,
                        "Today: Spent ${summary.todaySpendingPaise.formatInr()}"
                    )

                    // 3. Update Recent Transaction
                    val recent = transactions.firstOrNull()
                    if (recent != null) {
                        views.setViewVisibility(R.id.widget_empty_state_text, View.GONE)
                        views.setViewVisibility(R.id.widget_recent_item_container, View.VISIBLE)

                        views.setTextViewText(R.id.widget_recent_category, recent.category.uppercase())
                        views.setTextViewText(
                            R.id.widget_recent_note,
                            if (recent.note.isNotBlank()) recent.note else recent.rawText
                        )
                        views.setTextViewText(R.id.widget_recent_amount, recent.amountPaise.formatInr())

                        val amountColor = if (recent.type == com.paisa.app.data.TransactionType.INCOME) {
                            0xFF146C43.toInt() // Green
                        } else {
                            0xFF8C1D18.toInt() // Red
                        }
                        views.setTextColor(R.id.widget_recent_amount, amountColor)

                        // Bind Edit Intent
                        val editIntent = Intent(context, MainActivity::class.java).apply {
                            action = MainActivity.ACTION_EDIT_TRANSACTION
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(MainActivity.EXTRA_EDIT_TRANSACTION_ID, recent.id)
                        }
                        val editPendingIntent = PendingIntent.getActivity(
                            context,
                            recent.id.toInt(),
                            editIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_recent_item_container, editPendingIntent)
                    } else {
                        views.setViewVisibility(R.id.widget_empty_state_text, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_recent_item_container, View.GONE)
                    }

                    // 4. Bind Quick Action Buttons
                    val logIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val logPendingIntent = PendingIntent.getActivity(
                        context,
                        100,
                        logIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_log, logPendingIntent)

                    val speakIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_START_VOICE_INPUT, true)
                    }
                    val speakPendingIntent = PendingIntent.getActivity(
                        context,
                        101,
                        speakIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_speak, speakPendingIntent)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, PaisaAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, PaisaAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
