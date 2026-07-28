package com.example.financeapp.data

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SmsParser {
    // Regex to find amount: matches Rs, Rs., INR, ₹ followed by number (possibly with commas and decimals)
    private val amountRegex = Regex("(?i)(?:Rs\\.?|INR|₹)\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    
    // Keywords for debit
    private val debitKeywords = listOf("debited", "spent", "paid", "sent", "transferred", "payment of", "deducted", "withdrawn")
    // Keywords for credit
    private val creditKeywords = listOf("credited", "received", "added", "deposited", "refunded")

    fun parse(smsBody: String, smsId: String, timestamp: Long): TransactionData? {
        val body = smsBody.replace(",", "") // remove commas to make regex match simpler
        
        // 1. Find Amount
        val amountMatch = amountRegex.find(body) ?: return null
        val amountStr = amountMatch.groups[1]?.value ?: return null
        val baseAmount = amountStr.toDoubleOrNull() ?: return null

        // 2. Determine type (Debit or Credit)
        val isDebit = debitKeywords.any { body.contains(it, ignoreCase = true) }
        val isCredit = creditKeywords.any { body.contains(it, ignoreCase = true) }
        
        // If neither is found, it might not be a transaction SMS
        if (!isDebit && !isCredit) return null

        val finalAmount = if (isDebit) -baseAmount else baseAmount

        // 3. Extract Merchant
        var merchant = "Unknown"
        
        // Try to extract merchant from VPA or "to/towards/at"
        if (isDebit) {
            val toIndex = body.indexOf("to ", ignoreCase = true)
            val towardsIndex = body.indexOf("towards ", ignoreCase = true)
            val atIndex = body.indexOf("at ", ignoreCase = true)
            val forIndex = body.indexOf("for ", ignoreCase = true)
            
            val startIndex = when {
                towardsIndex != -1 -> towardsIndex + "towards ".length
                toIndex != -1 -> toIndex + "to ".length
                atIndex != -1 -> atIndex + "at ".length
                forIndex != -1 -> forIndex + "for ".length
                else -> -1
            }
            
            if (startIndex != -1) {
                val sub = body.substring(startIndex)
                val cleanSub = sub.split(Regex("[\\.\\n]"))[0].trim()
                val words = cleanSub.split(" ")
                val merchantName = words.take(2).joinToString(" ")
                merchant = cleanMerchantName(merchantName)
            }
        } else {
            val fromIndex = body.indexOf("from ", ignoreCase = true)
            val byIndex = body.indexOf("by ", ignoreCase = true)
            
            val startIndex = when {
                fromIndex != -1 -> fromIndex + "from ".length
                byIndex != -1 -> byIndex + "by ".length
                else -> -1
            }
            
            if (startIndex != -1) {
                val sub = body.substring(startIndex)
                val cleanSub = sub.split(Regex("[\\.\\n]"))[0].trim()
                val words = cleanSub.split(" ")
                merchant = cleanMerchantName(words.take(2).joinToString(" "))
            }
        }

        if (merchant.isBlank() || merchant == "Unknown") {
            merchant = if (isDebit) "Debit Transaction" else "Credit Transaction"
        }

        // 4. Determine Category
        val category = determineCategory(merchant, isCredit)

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

    /**
     * AI-enhanced version of [parse].
     * Runs the fast regex parser first. If the result is MISCELLANEOUS,
     * it calls [GeminiCategorizer] to get a smarter category using Gemini.
     * Results are cached so each unique merchant is only ever sent to the API once.
     */
    suspend fun parseWithAi(context: Context, smsBody: String, smsId: String, timestamp: Long): TransactionData? {
        val transaction = parse(smsBody, smsId, timestamp) ?: return null

        // Only call AI if the fast regex couldn't figure out the category
        return if (transaction.category == "MISCELLANEOUS") {
            val aiCategory = GeminiCategorizer.categorize(
                context = context,
                merchantName = transaction.merchant,
                smsSnippet = smsBody
            )
            transaction.copy(category = aiCategory)
        } else {
            transaction
        }
    }

    /**
     * Strips sensitive data (account numbers, UPI VPAs, reference numbers)
     * from SMS text before storing as description.
     */
    private fun sanitizeDescription(text: String): String {
        var sanitized = text
        // Mask bank account numbers (sequences of 8+ digits)
        sanitized = sanitized.replace(Regex("\\b\\d{8,}\\b"), "****")
        // Mask UPI VPAs (e.g., user@bank)
        sanitized = sanitized.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+"), "***@***")
        // Mask card numbers (4 groups of 4 digits or ending in XX1234)
        sanitized = sanitized.replace(Regex("\\b(?:\\d{4}[\\s-]?){3}\\d{4}\\b"), "****-****-****-****")
        sanitized = sanitized.replace(Regex("(?i)(?:XX|xx)\\d{4,}"), "XX****")
        return sanitized
    }

    private fun cleanMerchantName(name: String): String {
        var clean = name.trim()
        
        // Remove trailing "UPI" or VPA handles
        if (clean.contains("@")) {
            clean = clean.split("@")[0]
        }
        
        // Remove common words like "Ref", "using", "A/C", "No", etc.
        val ignoreWords = listOf("ref", "using", "acct", "a/c", "ac", "no", "on", "towards", "for", "via", "upi", "card", "vpa", "no.", "bank", "link")
        val words = clean.split(" ").filter { word ->
            val wLower = word.lowercase().trim('.', ',', ':', ';', '-')
            !ignoreWords.contains(wLower) && !wLower.matches(Regex("\\d+"))
        }
        
        return words.joinToString(" ").trim().ifEmpty { "Unknown Merchant" }
    }

    internal fun determineCategory(merchant: String, isCredit: Boolean): String {
        if (isCredit) return "INCOME"
        val m = merchant.lowercase()
        return when {
            m.contains("swiggy") || m.contains("zomato") || m.contains("starbucks") ||
            m.contains("restaurant") || m.contains("cafe") || m.contains("food") ||
            m.contains("eats") || m.contains("kitchen") || m.contains("dhaba") ||
            m.contains("mithai") || m.contains("sweet") || m.contains("bakery") ||
            m.contains("bakers") || m.contains("pizza") || m.contains("burger") ||
            m.contains("dominos") || m.contains("mcdonalds") || m.contains("kfc") ||
            m.contains("subway") || m.contains("haldiram") || m.contains("blinkit") ||
            m.contains("zepto") || m.contains("instamart") || m.contains("bigbasket") ||
            m.contains("grofers") || m.contains("chai") || m.contains("tea") ||
            m.contains("juice") || m.contains("vending") || m.contains("canteen") ||
            m.contains("tiffin") || m.contains("mess") || m.contains("snacks") -> "FOOD"

            m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
            m.contains("metro") || m.contains("petrol") || m.contains("fuel") ||
            m.contains("hpcl") || m.contains("bpcl") || m.contains("iocl") ||
            m.contains("cab") || m.contains("taxi") || m.contains("bus") ||
            m.contains("irctc") || m.contains("railway") || m.contains("train") ||
            m.contains("flight") || m.contains("airline") || m.contains("airport") ||
            m.contains("indigo") || m.contains("spicejet") || m.contains("parking") ||
            m.contains("fastag") || m.contains("toll") || m.contains("auto") -> "TRAVEL"

            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
            m.contains("nykaa") || m.contains("shopping") || m.contains("retail") ||
            m.contains("decathlon") || m.contains("store") || m.contains("supermarket") ||
            m.contains("grocery") || m.contains("meesho") || m.contains("ajio") ||
            m.contains("croma") || m.contains("dmart") || m.contains("ikea") -> "SHOPPING"

            m.contains("jio") || m.contains("airtel") || m.contains("vodafone") ||
            m.contains("bsnl") || m.contains("recharge") || m.contains("electricity") ||
            m.contains("bill") || m.contains("water") || m.contains("gas") ||
            m.contains("utility") || m.contains("bescom") || m.contains("msedcl") ||
            m.contains("broadband") || m.contains("wifi") || m.contains("rent") ||
            m.contains("emi") || m.contains("insurance") || m.contains("premium") -> "BILLS"

            m.contains("movie") || m.contains("netflix") || m.contains("prime") ||
            m.contains("hotstar") || m.contains("spotify") || m.contains("youtube") ||
            m.contains("entertainment") || m.contains("cinema") || m.contains("pvr") ||
            m.contains("inox") || m.contains("bookmyshow") || m.contains("gaming") ||
            m.contains("steam") || m.contains("playstation") -> "ENTERTAINMENT"

            m.contains("invest") || m.contains("groww") || m.contains("zerodha") ||
            m.contains("shares") || m.contains("stock") || m.contains("sip") ||
            m.contains("ppf") || m.contains("nps") || m.contains("upstox") ||
            m.contains("smallcase") -> "INVESTMENTS"

            m.contains("pharmacy") || m.contains("medical") || m.contains("hospital") ||
            m.contains("clinic") || m.contains("doctor") || m.contains("apollo") ||
            m.contains("medplus") || m.contains("1mg") || m.contains("pharmeasy") ||
            m.contains("netmeds") || m.contains("diagnostic") ||
            m.contains("health") || m.contains("chemist") || m.contains("medicine") -> "HEALTH"

            m.contains("school") || m.contains("college") || m.contains("university") ||
            m.contains("tuition") || m.contains("udemy") || m.contains("coursera") ||
            m.contains("byju") || m.contains("unacademy") || m.contains("stationery") ||
            m.contains("education") || m.contains("library") -> "EDUCATION"

            else -> "MISCELLANEOUS"
        }
    }
}
