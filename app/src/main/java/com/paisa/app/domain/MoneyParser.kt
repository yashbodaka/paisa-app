package com.paisa.app.domain

import com.paisa.app.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class MoneyParser {
    private val amountPattern = Regex("""^([+-]?\d+(?:\.\d{1,2})?)\s*(.*)$""")
    private val toPersonPattern = Regex("""\bto\s+([a-zA-Z][a-zA-Z ]{0,40})$""", RegexOption.IGNORE_CASE)
    private val fromPersonPattern = Regex("""\bfrom\s+([a-zA-Z][a-zA-Z ]{0,40})$""", RegexOption.IGNORE_CASE)

    private val incomeWords = setOf("salary", "freelance", "stipend", "bonus", "refund", "income", "paid")
    private val categoryKeywords = mapOf(
        "food" to setOf("food", "lunch", "dinner", "breakfast", "snack", "snacks", "coffee", "cafe", "restaurant"),
        "groceries" to setOf("grocery", "groceries", "milk", "vegetables", "fruit"),
        "travel" to setOf("travel", "taxi", "cab", "auto", "bus", "train", "fuel", "petrol", "diesel"),
        "rent" to setOf("rent", "house", "flat", "room"),
        "shopping" to setOf("shopping", "clothes", "shirt", "shoes", "amazon", "flipkart"),
        "bills" to setOf("bill", "bills", "electricity", "wifi", "internet", "phone", "recharge"),
        "health" to setOf("health", "doctor", "medicine", "pharmacy", "hospital"),
        "education" to setOf("education", "book", "books", "course", "class", "college"),
        "entertainment" to setOf("movie", "movies", "netflix", "game", "games", "party")
    )

    fun parse(input: String): ParseResult {
        val rawText = input.trim()
        if (rawText.isBlank()) {
            return ParseResult.Error("Type something like 200 food")
        }

        val match = amountPattern.matchEntire(rawText)
            ?: return ParseResult.Error("Start with an amount, for example 200 food")

        val amountText = match.groupValues[1]
        val details = match.groupValues[2].trim()
        val amountPaise = amountText.toPaiseOrNull()
            ?: return ParseResult.Error("That amount does not look right")

        if (amountPaise <= 0) {
            return ParseResult.Error("Amount should be more than zero")
        }

        val lowerDetails = details.lowercase(Locale.getDefault())
        val lentPerson = toPersonPattern.find(details)?.groupValues?.getOrNull(1)?.cleanPersonName()
        val borrowedPerson = fromPersonPattern.find(details)?.groupValues?.getOrNull(1)?.cleanPersonName()

        return when {
            lentPerson != null -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.LENT,
                    category = "people",
                    personName = lentPerson,
                    note = details,
                    rawText = rawText,
                    reason = "Detected 'to $lentPerson'"
                )
            )

            borrowedPerson != null -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.BORROWED,
                    category = "people",
                    personName = borrowedPerson,
                    note = details,
                    rawText = rawText,
                    reason = "Detected 'from $borrowedPerson'"
                )
            )

            amountText.startsWith("+") || lowerDetails.hasAny(incomeWords) -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.INCOME,
                    category = inferIncomeCategory(lowerDetails),
                    personName = null,
                    note = details,
                    rawText = rawText,
                    reason = "Detected income keyword or plus sign"
                )
            )

            else -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.EXPENSE,
                    category = inferExpenseCategory(lowerDetails),
                    personName = null,
                    note = details,
                    rawText = rawText,
                    reason = "Defaulted to expense"
                )
            )
        }
    }

    private fun inferIncomeCategory(details: String): String {
        return incomeWords.firstOrNull { details.containsWord(it) } ?: "income"
    }

    private fun inferExpenseCategory(details: String): String {
        if (details.isBlank()) return "other"
        categoryKeywords.forEach { (category, words) ->
            if (details.hasAny(words)) return category
        }
        return "other"
    }

    private fun String.hasAny(words: Set<String>): Boolean = words.any { containsWord(it) }

    private fun String.containsWord(word: String): Boolean {
        return Regex("""(^|\s)${Regex.escape(word)}($|\s)""").containsMatchIn(this)
    }

    private fun String.toPaiseOrNull(): Long? {
        return runCatching {
            BigDecimal(this.removePrefix("+"))
                .abs()
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }.getOrNull()
    }

    private fun String.cleanPersonName(): String {
        return trim()
            .split(Regex("""\s+"""))
            .joinToString(" ") { part ->
                part.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                    if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                }
            }
    }
}

