package com.paisa.app.data

import com.paisa.app.domain.ParsedMoneyEntry
import kotlinx.coroutines.flow.Flow

class MoneyRepository(
    private val dao: TransactionDao
) {
    fun observeTransactions(): Flow<List<MoneyTransaction>> = dao.observeTransactions()

    suspend fun getTransactions(): List<MoneyTransaction> = dao.getTransactions()

    suspend fun add(parsed: ParsedMoneyEntry) {
        dao.insert(
            MoneyTransaction(
                amountPaise = parsed.amountPaise,
                type = parsed.type,
                category = parsed.category,
                personName = parsed.personName,
                note = parsed.note,
                rawText = parsed.rawText
            )
        )
    }

    suspend fun delete(transaction: MoneyTransaction) {
        dao.delete(transaction)
    }
}

