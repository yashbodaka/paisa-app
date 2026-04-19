package com.paisa.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class MoneyTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amountPaise: Long,
    val type: TransactionType,
    val category: String,
    val personName: String? = null,
    val note: String = "",
    val rawText: String,
    val createdAt: Long = System.currentTimeMillis()
)

