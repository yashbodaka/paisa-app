package com.paisa.app.domain

import com.paisa.app.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

class MoneyParser {
    private val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
    private val currencyWords = setOf("rupees", "rs", "inr", "bucks", "buck", "rupai", "rupaye", "paisa", "rupay", "r")
    
    private val incomeKeywords = setOf("salary", "freelance", "stipend", "bonus", "refund", "income", "got", "received", "mile", "mila", "mili", "paya", "prapt", "aay", "credited")
    private val expenseKeywords = setOf("paid", "gave", "spent", "buy", "bought", "kharch", "diye", "diya", "bhugtan", "debited")
    
    private val toPersonPattern = Regex("""\b(to|ko)\s+([a-zA-Z][a-zA-Z ]*)""", RegexOption.IGNORE_CASE)
    private val toPersonSuffixPattern = Regex("""\b([a-zA-Z][a-zA-Z ]*)\s+ko\b""", RegexOption.IGNORE_CASE)
    private val fromPersonPattern = Regex("""\b(from|se)\s+([a-zA-Z][a-zA-Z ]*)""", RegexOption.IGNORE_CASE)
    private val fromPersonSuffixPattern = Regex("""\b([a-zA-Z][a-zA-Z ]*)\s+se\b""", RegexOption.IGNORE_CASE)

    private val categoryKeywords = mapOf(
        "food" to setOf("food", "lunch", "dinner", "breakfast", "snack", "snacks", "coffee", "cafe", "restaurant", "pani puri", "chaat", "zomato", "swiggy", "biryani", "pizza", "burger", "tea", "chai", "nashta", "khana", "dhabha"),
        "groceries" to setOf("grocery", "groceries", "milk", "vegetables", "fruit", "egg", "eggs", "bread", "butter", "blinkit", "zepto", "instamart", "rashan", "doodh", "sabji"),
        "travel" to setOf("travel", "taxi", "cab", "auto", "bus", "train", "fuel", "petrol", "diesel", "ola", "uber", "rapido", "bhada", "kiraya", "petrol"),
        "rent" to setOf("rent", "house", "flat", "room", "kiraya"),
        "shopping" to setOf("shopping", "clothes", "shirt", "shoes", "amazon", "flipkart", "myntra", "ajio", "kapde"),
        "bills" to setOf("bill", "bills", "electricity", "wifi", "internet", "phone", "recharge", "gas", "water", "bijli"),
        "health" to setOf("health", "doctor", "medicine", "pharmacy", "hospital", "gym", "dawai"),
        "education" to setOf("education", "book", "books", "course", "class", "college", "fees", "kitab"),
        "entertainment" to setOf("movie", "movies", "netflix", "game", "games", "party", "club", "drinks", "masti", "ghumna")
    )

    fun parse(input: String, nlpAmountPaise: Long? = null, categorizer: CategoryClassifier? = null): ParseResult {
        val rawText = input.trim()
        if (rawText.isBlank()) {
            return ParseResult.Error("Type something like `pani puri 20` or `200 from Rahul`")
        }

        // --- Specialized Bank SMS Parsing (Check this first) ---
        // Handles: "Dr INR 1.00", "Debited INR 250.00", "Amt Debited: INR 30.00", etc.
        // More flexible regex to allow words/symbols between verb and INR
        val bankMatch = findBankAmount(rawText)
        
        if (bankMatch != null) {
            val verb = bankMatch.verb.lowercase()
            val amountStr = bankMatch.amount.replace(",", "")
            val amountPaise = amountStr.toPaiseOrNull() ?: 0L

            if (amountPaise > 0) {
                val isIncome = verb == "cr" || verb == "credited" || verb == "received"
                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                
                // Extract person/merchant from UPI line if present (e.g. UPI/DR/1229.../NAME)
                var personName: String? = null
                val upiLine = rawText.lines().find { it.contains("UPI/", ignoreCase = true) }
                if (upiLine != null) {
                    val parts = upiLine.split("/")
                    if (parts.size >= 4) {
                        // UPI refs often look like UPI/DR/<numeric-ref>/<merchant>.
                        val nameCandidate = listOfNotNull(parts.getOrNull(3), parts.getOrNull(4))
                            .map { it.trim().substringBefore("\n").substringBefore("/") }
                            .firstOrNull { candidate ->
                                candidate.length > 2 && candidate.any { it.isLetter() }
                            }
                        if (nameCandidate != null && nameCandidate.length > 2) {
                            personName = nameCandidate
                        }
                    }
                }
                
                // If not found in UPI, try searching for "at [Merchant]" or "to [Name]"
                if (personName == null) {
                    val merchantRegex = Regex("""\b(?:at|to|info)\s+([A-Z0-9\s*]{3,20})\b""", RegexOption.IGNORE_CASE)
                    personName = merchantRegex.find(rawText)?.groupValues?.get(1)?.trim()
                }
                
                // Create a concise 3-4 word note/summary
                val cleanName = personName?.cleanPersonName()
                val summaryNote = when {
                    isIncome && cleanName != null -> "From $cleanName"
                    isIncome -> "Bank Credit"
                    !isIncome && cleanName != null -> "To $cleanName"
                    else -> if (isIncome) "Income" else "Expense"
                }

                val category = inferExpenseCategory(lowerDetails = rawText.lowercase(), categorizer = categorizer)

                return ParseResult.Success(
                    ParsedMoneyEntry(
                        amountPaise = amountPaise,
                        type = type,
                        category = category,
                        personName = cleanName,
                        note = summaryNote,
                        rawText = rawText,
                        reason = "Parsed specialized bank SMS"
                    )
                )
            }
        }
        // --- End Bank Parsing ---

        // 1. Find the amount anywhere (Check NLP first, then Generic regex)
        val regexAmountPaise = amountRegex.find(rawText)?.groupValues?.get(1)?.toPaiseOrNull()
        val amountPaise = nlpAmountPaise ?: regexAmountPaise
            ?: return ParseResult.Error("I couldn't find a number in your message. Try something like `100 food`.")

        if (amountPaise <= 0) {
            return ParseResult.Error("Amount should be more than zero")
        }

        // 2. Extract details (remove the amount text carefully)
        val amountTextToRemove = amountRegex.find(rawText)?.groupValues?.get(1) ?: ""
        var details = rawText
        if (amountTextToRemove.isNotEmpty()) {
            val firstOccurrence = rawText.indexOf(amountTextToRemove)
            if (firstOccurrence != -1) {
                details = rawText.removeRange(firstOccurrence, firstOccurrence + amountTextToRemove.length).trim()
            }
        }
        
        currencyWords.forEach { word ->
            details = details.replace(Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE), "").trim()
        }
        details = details.replace(Regex("""\s+"""), " ") // clean extra spaces

        // Truncate details if they are too long (e.g. from a long SMS that didn't match bank regex)
        if (details.length > 50) {
            details = details.take(47) + "..."
        }


        val lowerDetails = details.lowercase(Locale.getDefault())
        // Log removed to avoid exceptions in plain JVM unit tests
        
        // 3. Detect person (support both prefix like 'to Rahul' and suffix like 'Rahul ko')
        val lentPerson = (toPersonPattern.find(details)?.groupValues?.getOrNull(2) 
            ?: toPersonSuffixPattern.find(details)?.groupValues?.getOrNull(1))?.cleanPersonName()
            
        val borrowedPerson = (fromPersonPattern.find(details)?.groupValues?.getOrNull(2)
            ?: fromPersonSuffixPattern.find(details)?.groupValues?.getOrNull(1))?.cleanPersonName()

        // 4. Intelligent Classification
        return when {
            // Case: Udhaar (Debt) - very common in Hindi
            lowerDetails.containsWord("udhaar") -> {
                when {
                    lowerDetails.containsWord("diye") || lowerDetails.containsWord("diya") || lentPerson != null -> ParseResult.Success(
                        ParsedMoneyEntry(amountPaise, TransactionType.LENT, "people", lentPerson, details, rawText, "Detected debt lent")
                    )
                    else -> ParseResult.Success(
                        ParsedMoneyEntry(amountPaise, TransactionType.BORROWED, "people", borrowedPerson, details, rawText, "Detected debt borrowed")
                    )
                }
            }

            // Case: Lent money to someone (e.g., "500 to Rahul" or "Rahul ko 500")
            lentPerson != null && !lowerDetails.hasAny(expenseKeywords) -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.LENT,
                    category = "people",
                    personName = lentPerson,
                    note = details,
                    rawText = rawText,
                    reason = "Detected person receiving money"
                )
            )

            // Case: Income/Borrowed from someone (e.g., "200 from suju" or "suju se 200")
            borrowedPerson != null -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.BORROWED,
                    category = "people",
                    personName = borrowedPerson,
                    note = details,
                    rawText = rawText,
                    reason = "Detected money received from person"
                )
            )

            // Case: Explicit Income keywords
            (lowerDetails.hasAny(incomeKeywords) && !lowerDetails.hasAny(expenseKeywords)) || rawText.startsWith("+") -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.INCOME,
                    category = inferIncomeCategory(lowerDetails),
                    personName = null,
                    note = details,
                    rawText = rawText,
                    reason = "Detected income keyword"
                )
            )

            // Case: Default to Expense
            else -> ParseResult.Success(
                ParsedMoneyEntry(
                    amountPaise = amountPaise,
                    type = TransactionType.EXPENSE,
                    category = inferExpenseCategory(lowerDetails, categorizer),
                    personName = null,
                    note = details,
                    rawText = rawText,
                    reason = "Classified as expense"
                )
            )
        }
    }

    private fun inferIncomeCategory(details: String): String {
        return incomeKeywords.firstOrNull { details.containsWord(it) } ?: "income"
    }

    private fun findBankAmount(rawText: String): BankAmountMatch? {
        val amountBeforeVerb = Regex(
            """\b(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?).{0,80}?\b(Dr|Cr|Credited|Debited|Spent|Received)\b""",
            RegexOption.IGNORE_CASE
        )
        amountBeforeVerb.find(rawText)?.let { match ->
            return BankAmountMatch(
                verb = match.groupValues[2],
                amount = match.groupValues[1]
            )
        }

        val verbBeforeAmount = Regex(
            """\b(Dr|Cr|Credited|Debited|Spent|Received)\b.{0,80}?\b(?:INR|Rs\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        verbBeforeAmount.find(rawText)?.let { match ->
            return BankAmountMatch(
                verb = match.groupValues[1],
                amount = match.groupValues[2]
            )
        }

        return null
    }

    private fun inferExpenseCategory(lowerDetails: String, categorizer: CategoryClassifier? = null): String {
        if (lowerDetails.isBlank()) return "other"
        
        // 1. Keyword matching (Fastest)
        categoryKeywords.forEach { (category, words) ->
            if (lowerDetails.hasAny(words)) return category
        }
        
        // 2. NLP Semantic matching (Fallback)
        return categorizer?.categorize(lowerDetails)?.lowercase(Locale.getDefault()) ?: "other"
    }

    private fun String.hasAny(words: Set<String>): Boolean = words.any { containsWord(it) }

    private fun String.containsWord(word: String): Boolean {
        return Regex("""\b${Regex.escape(word)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(this)
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

    private data class BankAmountMatch(
        val verb: String,
        val amount: String
    )
}
