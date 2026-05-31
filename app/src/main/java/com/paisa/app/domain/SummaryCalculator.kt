package com.paisa.app.domain

import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.TransactionType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

data class MoneySummary(
    val todaySpendingPaise: Long = 0,
    val todayNetPaise: Long = 0,
    val weekSpendingPaise: Long = 0,
    val weekNetPaise: Long = 0,
    val monthIncomePaise: Long = 0,
    val monthExpensePaise: Long = 0,
    val totalBalancePaise: Long = 0,
    val topCategory: String = "none",
    val weeklyDeltaPaise: Long = 0
)

data class PersonBalance(
    val name: String,
    val netPaise: Long
) {
    val label: String
        get() = if (netPaise >= 0) "$name owes you" else "You owe $name"
}

object SummaryCalculator {
    fun buildSummary(
        transactions: List<MoneyTransaction>,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): MoneySummary {
        val today = LocalDate.ofInstant(now, zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val previousWeekStart = weekStart.minusDays(7)
        val monthStart = today.withDayOfMonth(1)

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }

        val todayTransactions = transactions.filter { it.localDate(zone) == today }
        val todayIncome = todayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val todayExpense = todayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }

        val weekTransactions = transactions.filter { it.localDate(zone) >= weekStart }
        val weekIncome = weekTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amountPaise }
        val weekExpense = weekTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amountPaise }

        val previousWeekSpending = transactions
            .filter {
                val date = it.localDate(zone)
                it.type == TransactionType.EXPENSE && date >= previousWeekStart && date < weekStart
            }
            .sumOf { it.amountPaise }

        val monthTransactions = transactions.filter { it.localDate(zone) >= monthStart }
        val monthIncome = monthTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountPaise }
        val monthExpense = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountPaise }

        val topCategory = monthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.amountPaise } }
            ?.key
            ?: "none"

        return MoneySummary(
            todaySpendingPaise = todayExpense,
            todayNetPaise = todayIncome - todayExpense,
            weekSpendingPaise = weekExpense,
            weekNetPaise = weekIncome - weekExpense,
            monthIncomePaise = monthIncome,
            monthExpensePaise = monthExpense,
            totalBalancePaise = totalIncome - totalExpense,
            topCategory = topCategory,
            weeklyDeltaPaise = weekExpense - previousWeekSpending
        )
    }

    fun peopleBalances(transactions: List<MoneyTransaction>): List<PersonBalance> {
        return transactions
            .filter { it.type == TransactionType.LENT || it.type == TransactionType.BORROWED }
            .groupBy { it.personName.orEmpty() }
            .filterKeys { it.isNotBlank() }
            .map { (name, entries) ->
                val net = entries.sumOf { entry ->
                    when (entry.type) {
                        TransactionType.LENT -> entry.amountPaise
                        TransactionType.BORROWED -> -entry.amountPaise
                        else -> 0
                    }
                }
                PersonBalance(name = name, netPaise = net)
            }
            .filter { it.netPaise != 0L }
            .sortedByDescending { abs(it.netPaise) }
    }

    private fun MoneyTransaction.localDate(zone: ZoneId): LocalDate {
        return LocalDate.ofInstant(Instant.ofEpochMilli(createdAt), zone)
    }
}

