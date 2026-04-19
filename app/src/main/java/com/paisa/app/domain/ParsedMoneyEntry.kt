package com.paisa.app.domain

import com.paisa.app.data.TransactionType

data class ParsedMoneyEntry(
    val amountPaise: Long,
    val type: TransactionType,
    val category: String,
    val personName: String?,
    val note: String,
    val rawText: String,
    val reason: String
)

sealed interface ParseResult {
    data class Success(val entry: ParsedMoneyEntry) : ParseResult
    data class Error(val message: String) : ParseResult
}

