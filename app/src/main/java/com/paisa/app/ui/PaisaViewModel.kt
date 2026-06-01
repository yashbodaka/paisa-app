package com.paisa.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paisa.app.data.MoneyRepository
import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.data.TransactionType
import com.paisa.app.domain.MoneyParser
import com.paisa.app.domain.NlpCategorizer
import com.paisa.app.domain.NlpManager
import com.paisa.app.domain.ParseResult
import com.paisa.app.domain.SummaryCalculator
import com.paisa.app.domain.formatInr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaisaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MoneyRepository(PaisaDatabase.getDatabase(application).transactionDao())
    private val prefs = application.getSharedPreferences("paisa_prefs", android.content.Context.MODE_PRIVATE)
    private val savingsPercentageFlow = MutableStateFlow(prefs.getInt("savings_percentage", 20))

    fun getSavingsPercentage(): Int = savingsPercentageFlow.value

    fun updateSavingsPercentage(value: Int) {
        prefs.edit().putInt("savings_percentage", value).apply()
        savingsPercentageFlow.value = value
    }
    private val parser = MoneyParser()
    private val draft = MutableStateFlow("")
    private val isListening = MutableStateFlow(false)
    private val isTranscribing = MutableStateFlow(false)
    private val nlpManager = NlpManager(application)
    private val nlpCategorizer = NlpCategorizer(application)
    private val isLoading = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            nlpCategorizer.init()
            kotlinx.coroutines.delay(1200)
            isLoading.value = false
        }
    }
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PaisaUiState> = combine(
        combine(repository.observeTransactions(), draft, isListening) { txns, d, l -> Triple(txns, d, l) },
        combine(isTranscribing, message, isLoading) { t, m, i -> Triple(t, m, i) },
        savingsPercentageFlow
    ) { (txns, currentDraft, listening), (transcribing, currentMessage, loading), percent ->
        val deposits = txns.filter { it.type == TransactionType.SAVINGS_DEPOSIT }.sumOf { it.amountPaise }
        val withdraws = txns.filter { it.type == TransactionType.SAVINGS_WITHDRAW }.sumOf { it.amountPaise }
        val totalSavings = (deposits - withdraws).coerceAtLeast(0L)

        PaisaUiState(
            draft = currentDraft,
            isListening = listening,
            isTranscribing = transcribing,
            isLoading = loading,
            transactions = txns,
            summary = SummaryCalculator.buildSummary(txns),
            people = SummaryCalculator.peopleBalances(txns),
            suggestions = buildSuggestions(txns),
            message = currentMessage,
            savingsPercentage = percent,
            totalSavingsPaise = totalSavings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaisaUiState()
    )

    fun updateDraft(value: String) {
        draft.value = value
    }

    fun toggleListening() {
        isListening.value = !isListening.value
    }

    fun setListening(active: Boolean) {
        isListening.value = active
    }

    fun setTranscribing(active: Boolean) {
        isTranscribing.value = active
    }

    /** Called by PaisaRoute when Vosk returns a transcription. */
    fun onTranscribed(text: String) {
        isTranscribing.value = false
        if (text.isNotBlank()) {
            draft.value = text
            viewModelScope.launch {
                val nlpAmount = nlpManager.findAmountPaise(text)
                submitText(text, nlpAmount)
            }
        } else {
            message.value = "Couldn't understand — please try again"
        }
    }

    fun submitDraft() {
        submitText(draft.value)
    }

    fun submitText(text: String, nlpAmount: Long? = null) {
        when (val result = parser.parse(text, nlpAmount, nlpCategorizer)) {
            is ParseResult.Error -> message.value = result.message
            is ParseResult.Success -> viewModelScope.launch {
                insertAndAutoSave(result.entry)
                draft.value = ""
                message.value = "Logged ${result.entry.amountPaise.formatInr()}"
            }
        }
    }

    fun delete(transaction: MoneyTransaction) {
        viewModelScope.launch {
            repository.delete(transaction)
            message.value = "Deleted entry"
        }
    }

    fun syncSms() {
        viewModelScope.launch {
            if (androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                message.value = "Scanning recent SMS..."
                try {
                    // Run the DB query on IO dispatcher
                    val found = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        var count = 0
                        val cursor = getApplication<Application>().contentResolver.query(
                            android.provider.Telephony.Sms.Inbox.CONTENT_URI,
                            arrayOf(android.provider.Telephony.Sms.Inbox.BODY, android.provider.Telephony.Sms.Inbox.ADDRESS),
                            null,
                            null,
                            "${android.provider.Telephony.Sms.Inbox.DATE} DESC LIMIT 50"
                        )
                        cursor?.use { c ->
                            val bodyIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.Inbox.BODY)
                            val addrIdx = c.getColumnIndexOrThrow(android.provider.Telephony.Sms.Inbox.ADDRESS)
                            while (c.moveToNext()) {
                                val body = c.getString(bodyIdx) ?: continue
                                val lowerBody = body.lowercase()
                                val bankPattern = Regex("(dr|cr)\\s+inr", RegexOption.IGNORE_CASE)
                                val upiPattern = Regex("upi/", RegexOption.IGNORE_CASE)
                                val isBankSms = bankPattern.containsMatchIn(lowerBody) || upiPattern.containsMatchIn(lowerBody)
                                if (isBankSms) {
                                    val result = parser.parse(body, null, nlpCategorizer)
                                    if (result is ParseResult.Success) {
                                        // Simple deduplication
                                        val existing = repository.getTransactions()
                                        val already = existing.any { t -> t.amountPaise == result.entry.amountPaise && t.rawText == result.entry.rawText }
                                        if (!already) {
                                            insertAndAutoSave(result.entry)
                                            count++
                                        }
                                    }
                                }
                            }
                        }
                        count
                    }
                    message.value = if (found > 0) "Synced $found new transaction(s) from SMS" else "No new transactions found in recent SMS"
                } catch (e: Exception) {
                    message.value = "Failed to sync SMS: ${e.message}"
                }
            } else {
                message.value = "SMS permission not granted. Please allow in settings."
            }
        }
    }

    fun showMessage(value: String) {
        message.value = value
    }

    private fun buildSuggestions(transactions: List<MoneyTransaction>): List<String> {
        val categories = transactions
            .asSequence()
            .filter { it.type == TransactionType.EXPENSE }
            .map { it.category }
            .filter { it != "other" }
            .distinct()
            .take(4)
            .map { "200 $it" }

        val people = transactions
            .asSequence()
            .mapNotNull { it.personName }
            .distinct()
            .take(3)
            .map { "300 to $it" }

        return (categories + people).take(6).toList()
    }

    fun updateTransaction(transaction: MoneyTransaction) {
        viewModelScope.launch {
            repository.update(transaction)
            message.value = "Updated entry"
        }
    }

    private suspend fun insertAndAutoSave(entry: com.paisa.app.domain.ParsedMoneyEntry): Long {
        val id = repository.add(entry)
        if (entry.type == TransactionType.INCOME) {
            val savingsPercent = getSavingsPercentage()
            if (savingsPercent > 0) {
                val savingsAmount = entry.amountPaise * savingsPercent / 100
                if (savingsAmount > 0L) {
                    repository.addTransaction(
                        MoneyTransaction(
                            amountPaise = savingsAmount,
                            type = TransactionType.SAVINGS_DEPOSIT,
                            category = "Savings",
                            note = "Auto-save $savingsPercent% from Income",
                            rawText = "Auto-saved from: ${entry.rawText}"
                        )
                    )
                }
            }
        }
        return id
    }

    fun depositToSavings(amountPaise: Long, note: String) {
        viewModelScope.launch {
            repository.addTransaction(
                MoneyTransaction(
                    amountPaise = amountPaise,
                    type = TransactionType.SAVINGS_DEPOSIT,
                    category = "Savings",
                    note = note.ifBlank { "Manual Deposit" },
                    rawText = "Manual savings deposit"
                )
            )
            message.value = "Deposited ${amountPaise.formatInr()} to Savings"
        }
    }

    fun withdrawFromSavings(amountPaise: Long, note: String) {
        viewModelScope.launch {
            repository.addTransaction(
                MoneyTransaction(
                    amountPaise = amountPaise,
                    type = TransactionType.SAVINGS_WITHDRAW,
                    category = "Savings",
                    note = note.ifBlank { "Manual Withdrawal" },
                    rawText = "Manual savings withdrawal"
                )
            )
            message.value = "Withdrew ${amountPaise.formatInr()} from Savings"
        }
    }

    override fun onCleared() {
        super.onCleared()
        nlpManager.close()
        nlpCategorizer.close()
    }
}
