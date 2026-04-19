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
        val result = parser.parse("food 200")

        assertTrue(result is ParseResult.Error)
    }
}

