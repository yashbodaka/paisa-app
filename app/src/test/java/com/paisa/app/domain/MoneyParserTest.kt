package com.paisa.app.domain

import com.paisa.app.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyParserTest {
    private val parser = MoneyParser()

    @Test
    fun parsesPlainExpense() {
        val result = parser.parse("200 food")

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(20_000, entry.amountPaise)
        assertEquals(TransactionType.EXPENSE, entry.type)
        assertEquals("food", entry.category)
    }

    @Test
    fun parsesSalaryAsIncome() {
        val result = parser.parse("5000 salary")

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(500_000, entry.amountPaise)
        assertEquals(TransactionType.INCOME, entry.type)
        assertEquals("salary", entry.category)
    }

    @Test
    fun parsesPlusAsIncome() {
        val result = parser.parse("+2000 freelance")

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(200_000, entry.amountPaise)
        assertEquals(TransactionType.INCOME, entry.type)
        assertEquals("freelance", entry.category)
    }

    @Test
    fun parsesLentMoney() {
        val result = parser.parse("300 to rahul")

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(30_000, entry.amountPaise)
        assertEquals(TransactionType.LENT, entry.type)
        assertEquals("Rahul", entry.personName)
    }

    @Test
    fun parsesBorrowedMoney() {
        val result = parser.parse("300 from aman")

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(30_000, entry.amountPaise)
        assertEquals(TransactionType.BORROWED, entry.type)
        assertEquals("Aman", entry.personName)
    }

    @Test
    fun rejectsTextWithoutAmount() {
        val result = parser.parse("just food")

        assertTrue(result is ParseResult.Error)
    }

    @Test
    fun parsesBankSmsWithConciseNote() {
        val sms = "Dear Customer, Rs. 240.00 Debited from A/c XX1234 via UPI/DR/123456789/SWIGGY/okaxis on 12-May. Avl Bal Rs. 1000"
        val result = parser.parse(sms)

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(24_000, entry.amountPaise)
        assertEquals(TransactionType.EXPENSE, entry.type)
        assertEquals("To Swiggy", entry.note)
        assertEquals(sms, entry.rawText)
    }

    @Test
    fun paidManualExpenseDoesNotBecomeIncome() {
        val result = parser.parse("paid 200 for movie")

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(20_000, entry.amountPaise)
        assertEquals(TransactionType.EXPENSE, entry.type)
        assertEquals("entertainment", entry.category)
    }

    @Test
    fun paidToMerchantDoesNotBecomeLent() {
        val result = parser.parse("paid 300 to starbucks", categorizer = FixedCategoryClassifier("food"))

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(30_000, entry.amountPaise)
        assertEquals(TransactionType.EXPENSE, entry.type)
        assertEquals("food", entry.category)
    }

    @Test
    fun modelFallbackCategoryIsNormalized() {
        val result = parser.parse("450 stationery", categorizer = FixedCategoryClassifier("Education"))

        assertTrue(result is ParseResult.Success)
        val entry = (result as ParseResult.Success).entry
        assertEquals(TransactionType.EXPENSE, entry.type)
        assertEquals("education", entry.category)
    }

    private class FixedCategoryClassifier(private val category: String) : CategoryClassifier {
        override fun categorize(text: String): String = category
    }
}
