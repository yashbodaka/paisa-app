package com.paisa.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paisa.app.data.MoneyRepository
import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.PaisaDatabase
import com.paisa.app.data.TransactionType
import com.paisa.app.domain.MoneyParser
import com.paisa.app.domain.ParseResult
import com.paisa.app.domain.SummaryCalculator
import com.paisa.app.domain.formatInr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PaisaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MoneyRepository(PaisaDatabase.getDatabase(application).transactionDao())
    private val parser = MoneyParser()
    private val draft = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PaisaUiState> = combine(
        repository.observeTransactions(),
        draft,
        message
    ) { transactions, currentDraft, currentMessage ->
        PaisaUiState(
            draft = currentDraft,
            transactions = transactions,
            summary = SummaryCalculator.buildSummary(transactions),
            people = SummaryCalculator.peopleBalances(transactions),
            suggestions = buildSuggestions(transactions),
            message = currentMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PaisaUiState()
    )

    fun updateDraft(value: String) {
        draft.value = value
    }

    fun submitDraft() {
        submitText(draft.value)
    }

    fun submitText(text: String) {
        when (val result = parser.parse(text)) {
            is ParseResult.Error -> message.value = result.message
            is ParseResult.Success -> viewModelScope.launch {
                repository.add(result.entry)
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
}

