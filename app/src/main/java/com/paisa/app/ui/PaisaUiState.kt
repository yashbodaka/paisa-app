package com.paisa.app.ui

import com.paisa.app.data.MoneyTransaction
import com.paisa.app.domain.MoneySummary
import com.paisa.app.domain.PersonBalance

data class PaisaUiState(
    val draft: String = "",
    val transactions: List<MoneyTransaction> = emptyList(),
    val summary: MoneySummary = MoneySummary(),
    val people: List<PersonBalance> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val message: String? = null
)

