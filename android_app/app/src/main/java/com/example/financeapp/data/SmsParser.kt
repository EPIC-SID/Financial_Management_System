package com.example.financeapp.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SmsParser {
    // Pattern 1: Action verb + optional preposition + optional currency + amount
    private val actionAmountRegex = Regex("(?i)(?:debited|credited|spent|paid|sent|transferred|deducted|withdrawn|deposited|payment of)\\s*(?:by|for|of|with|in)?\\s*(?:Rs\\.?|INR|₹)?\\s*([\\d]+(?:\\.\\d{1,2})?)")
    
    // Pattern 2: Currency followed by amount
    private val currencyAmountRegex = Regex("(?i)(?:Rs\\.?|INR|₹)\\s*([\\d]+(?:\\.\\d{1,2})?)")
    
    // Keywords for debit
    private val debitKeywords = listOf("debited", "spent", "paid", "sent", "transferred", "payment of", "deducted", "withdrawn")
    // Keywords for credit
    private val creditKeywords = listOf("credited", "received", "added", "deposited", "refunded")

    private val stopDelimiters = Regex("(?i)\\b(?:ref|on|via|using|acct|a/c|ac|dated|val|avl|bal|upi|link|txn|txnid|order)\\b|[\\.\\n,;]")

    private fun extractAmount(body: String): Double? {
        // 1. Try action-associated amount first (e.g. "debited by 450.00", "paid Rs 200")
        val actionMatch = actionAmountRegex.find(body)
        if (actionMatch != null) {
            val amt = actionMatch.groupValues.getOrNull(1)?.toDoubleOrNull()
            if (amt != null && amt > 0.0) return amt
        }

        // 2. Try currency amounts that are NOT balance mentions
        val currencyMatches = currencyAmountRegex.findAll(body)
        for (m in currencyMatches) {
            val prefix = body.substring(0, m.range.first).lowercase().takeLast(25)
            val isBalance = prefix.contains("bal") || prefix.contains("balance")
            if (!isBalance) {
                val amt = m.groupValues.getOrNull(1)?.toDoubleOrNull()
                if (amt != null && amt > 0.0) return amt
            }
        }

        // 3. Fallback
        val fallbackMatch = currencyAmountRegex.find(body)
        return fallbackMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()
    }

    private fun extractMerchant(body: String, isDebit: Boolean): String {
        // 1. UPI P2M pattern: UPI/P2M/<num>/<Merchant>/... or UPI/<num>/<Merchant>/...
        val upiP2mMatch = Regex("(?i)UPI/(?:P2M/)?\\w+/([^/\\n\\r]+)").find(body)
        if (upiP2mMatch != null) {
            val candidate = cleanMerchantName(upiP2mMatch.groupValues[1])
            if (candidate != "Unknown Merchant") return candidate
        }

        // 2. Parenthesized merchant name, e.g., to user@bank (Medical Store)
        val parenMatch = Regex("(?i)(?:to|at)\\s+[^@\\s]+@[^\\s]+\\s*\\(([^)]+)\\)").find(body)
        if (parenMatch != null) {
            val candidate = cleanMerchantName(parenMatch.groupValues[1])
            if (candidate != "Unknown Merchant") return candidate
        }

        // 3. Keyword-based extraction
        val startIndex = if (isDebit) {
            val toIndex = body.indexOf("to ", ignoreCase = true)
            val towardsIndex = body.indexOf("towards ", ignoreCase = true)
            val atIndex = body.indexOf("at ", ignoreCase = true)
            val forIndex = body.indexOf("for ", ignoreCase = true)
            when {
                towardsIndex != -1 -> towardsIndex + "towards ".length
                toIndex != -1 -> toIndex + "to ".length
                atIndex != -1 -> atIndex + "at ".length
                forIndex != -1 -> forIndex + "for ".length
                else -> -1
            }
        } else {
            val fromIndex = body.indexOf("from ", ignoreCase = true)
            val byIndex = body.indexOf("by ", ignoreCase = true)
            when {
                fromIndex != -1 -> fromIndex + "from ".length
                byIndex != -1 -> byIndex + "by ".length
                else -> -1
            }
        }

        if (startIndex != -1) {
            val sub = body.substring(startIndex)
            val cleanSub = sub.split(stopDelimiters)[0].trim()
            val candidate = cleanMerchantName(cleanSub)
            if (candidate != "Unknown Merchant") return candidate
        }

        return if (isDebit) "Debit Transaction" else "Credit Transaction"
    }

    private fun cleanMerchantName(name: String): String {
        var clean = name.trim()
        if (clean.contains("@")) {
            clean = clean.split("@")[0]
        }
        val ignoreWords = setOf(
            "ref", "using", "acct", "a/c", "ac", "no", "on", "towards", "for", "via",
            "upi", "card", "vpa", "no.", "bank", "link", "txn", "txnid", "account",
            "inr", "rs", "rs.", "amount", "is", "of"
        )
        val words = clean.split(Regex("\\s+")).filter { word ->
            val wLower = word.lowercase().trim('.', ',', ':', ';', '-', '(', ')')
            wLower.isNotEmpty() && !ignoreWords.contains(wLower) && !wLower.matches(Regex("\\d+"))
        }
        val result = words.take(4).joinToString(" ").trim()
        return result.ifEmpty { "Unknown Merchant" }
    }

    internal fun determineCategory(merchant: String, isCredit: Boolean, amount: Double = 0.0): String {
        return CategorizationPolicy.determineCategory(merchant, isCredit, amount)
    }

    internal fun determineCategory(merchant: String, isCredit: Boolean): String {
        return determineCategory(merchant, isCredit, 0.0)
    }

    fun parse(smsBody: String, smsId: String, timestamp: Long): TransactionData? {
        val body = smsBody.replace(",", "") // remove commas to make regex match simpler
        
        // 1. Find Amount
        val baseAmount = extractAmount(body) ?: return null

        // 2. Determine type (Debit or Credit)
        val isDebit = debitKeywords.any { body.contains(it, ignoreCase = true) }
        val isCredit = creditKeywords.any { body.contains(it, ignoreCase = true) }
        
        // If neither is found, it might not be a transaction SMS
        if (!isDebit && !isCredit) return null

        val finalAmount = if (isDebit) -baseAmount else baseAmount

        // 3. Extract Merchant
        val merchant = extractMerchant(body, isDebit)

        // 4. Determine Category (incorporating micro-debit heuristics & business context)
        val category = determineCategory(merchant, isCredit, baseAmount)

        // Convert timestamp to ISO 8601 string
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val dateStr = sdf.format(Date(timestamp))

        return TransactionData(
            merchant = merchant,
            amount = finalAmount,
            category = category,
            description = sanitizeDescription(smsBody.take(150)),
            transactionDate = dateStr,
            smsId = smsId
        )
    }

    suspend fun parseWithAi(context: Context, smsBody: String, smsId: String, timestamp: Long): TransactionData? {
        val transaction = parse(smsBody, smsId, timestamp) ?: return null

        // If the regex already resolved a non-MISCELLANEOUS category, we're done (Layer 2 hit)
        if (transaction.category != "MISCELLANEOUS") return transaction

        // Layer 1: Try to resolve from VPA pattern in the SMS body
        val vpaCategory = VpaResolver.resolveFromSms(smsBody)
        if (vpaCategory != null) {
            return transaction.copy(category = vpaCategory)
        }

        // Layer 3: Google Places API — identifies local merchants by name
        val placesCategory = PlacesCategorizer.categorize(
            context = context,
            merchantName = transaction.merchant
        )
        if (placesCategory != null) {
            return transaction.copy(category = placesCategory)
        }

        // Layer 4: Gemini LLM fallback — last resort
        val aiCategory = GeminiCategorizer.categorize(
            context = context,
            merchantName = transaction.merchant,
            smsSnippet = smsBody
        )
        return transaction.copy(category = aiCategory)
    }

    fun TransactionData.toEntity(): TransactionEntity {
        return TransactionEntity(
            smsId = this.smsId ?: error("smsId must not be null when converting to entity"),
            merchant = this.merchant,
            amount = this.amount,
            category = this.category,
            description = this.description,
            transactionDate = this.transactionDate,
            isCategorizedByAi = false
        )
    }

    private fun sanitizeDescription(text: String): String {
        var sanitized = text
        sanitized = sanitized.replace(Regex("\\b\\d{8,}\\b"), "****")
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+"), "***@***")
        sanitized = sanitized.replace(Regex("\\b(?:\\d{4}[\\s-]?){3}\\d{4}\\b"), "****-****-****-****")
        sanitized = sanitized.replace(Regex("(?i)(?:XX|xx)\\d{4,}"), "XX****")
        return sanitized
    }
}
