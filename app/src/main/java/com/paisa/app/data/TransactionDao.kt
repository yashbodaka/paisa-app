package com.paisa.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun observeTransactions(): Flow<List<MoneyTransaction>>

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    suspend fun getTransactions(): List<MoneyTransaction>

    @Insert
    suspend fun insert(transaction: MoneyTransaction): Long

    @androidx.room.Update
    suspend fun update(transaction: MoneyTransaction)

    @Delete
    suspend fun delete(transaction: MoneyTransaction)
}
