package com.paisa.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun transactionTypeToString(type: TransactionType): String = type.name

    @TypeConverter
    fun stringToTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

