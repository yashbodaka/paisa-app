package com.paisa.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.paisa.app.domain.MoneyParser
import com.paisa.app.domain.ParseResult
import com.paisa.app.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val parser = MoneyParser()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (sms in messages) {
                    val body = sms.displayMessageBody ?: continue
                    val sender = sms.displayOriginatingAddress ?: continue

                    if (!isBankSms(sender, body)) continue

                    val result = parser.parse(body, null, null)
                    if (result is ParseResult.Success) {
                        val entry = result.entry
                        if (entry.type == TransactionType.EXPENSE) {
                            NotificationHelper.showSmsCategoryPrompt(context, entry, sms.timestampMillis)
                        }
                        Log.d("SMS_RECEIVER", "Prompted category for: ${entry.amountPaise}")
                    } else {
                        Log.w("SMS_RECEIVER", "Failed to parse: $body")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isBankSms(sender: String, body: String): Boolean {
        // More comprehensive bank SMS patterns
        val lowerBody = body.lowercase()
        val bankKeywords = listOf("dr inr", "cr inr", "debited", "credited", "spent", "received", "upi/", "txn", "transaction")
        val isBankLike = bankKeywords.any { lowerBody.contains(it) }
        
        // Also check if sender looks like a bank (usually 6-character alphanumeric like AD-HDFCBK)
        val senderIsBank = sender.matches(Regex("""[a-zA-Z]{2}-[a-zA-Z]{6}""")) || sender.length >= 5
        
        return isBankLike && senderIsBank
    }
}
