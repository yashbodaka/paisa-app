package com.paisa.app.domain

import com.paisa.app.data.MoneyTransaction
import com.paisa.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SummaryCalculatorTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-04-19T12:00:00Z")

    @Test
    fun buildsDashboardSummary() {
        val transactions = listOf(
            transaction(20_000, TransactionType.EXPENSE, "food", "2026-04-19T06:00:00Z"),
            transaction(15_000, TransactionType.EXPENSE, "travel", "2026-04-18T06:00:00Z"),
            transaction(500_000, TransactionType.INCOME, "salary", "2026-04-05T06:00:00Z"),
            transaction(10_000, TransactionType.EXPENSE, "food", "2026-04-11T06:00:00Z")
        )

        val summary = SummaryCalculator.buildSummary(transactions, now, zone)

        assertEquals(20_000, summary.todaySpendingPaise)
        assertEquals(35_000, summary.weekSpendingPaise)
        assertEquals(500_000, summary.monthIncomePaise)
        assertEquals(45_000, summary.monthExpensePaise)
        assertEquals("food", summary.topCategory)
        assertEquals(25_000, summary.weeklyDeltaPaise)
    }

    @Test
    fun derivesPeopleBalances() {
        val balances = SummaryCalculator.peopleBalances(
            listOf(
                transaction(50_000, TransactionType.LENT, "people", "2026-04-19T06:00:00Z", person = "Rahul"),
                transaction(20_000, TransactionType.BORROWED, "people", "2026-04-19T06:00:00Z", person = "Aman"),
                transaction(10_000, TransactionType.LENT, "people", "2026-04-19T06:00:00Z", person = "Rahul")
            )
        )

        assertEquals(2, balances.size)
        assertEquals("Rahul", balances[0].name)
        assertEquals(60_000, balances[0].netPaise)
        assertEquals("Aman", balances[1].name)
        assertEquals(-20_000, balances[1].netPaise)
    }

    private fun transaction(
        amountPaise: Long,
        type: TransactionType,
        category: String,
        instant: String,
        person: String? = null
    ): MoneyTransaction {
        return MoneyTransaction(
            amountPaise = amountPaise,
            type = type,
            category = category,
            personName = person,
            rawText = "$amountPaise $category",
            createdAt = Instant.parse(instant).toEpochMilli()
        )
    }
}

