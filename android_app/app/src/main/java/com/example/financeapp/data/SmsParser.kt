package com.example.financeapp.data

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
            description = smsBody.take(150), // save first 150 chars of SMS as description
            transactionDate = dateStr,
            smsId = smsId
        )
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

    private fun determineCategory(merchant: String, isCredit: Boolean): String {
        if (isCredit) return "INCOME"
        
        val mLower = merchant.lowercase()
        return when {
            mLower.contains("swiggy") || mLower.contains("zomato") || mLower.contains("starbucks") || 
            mLower.contains("restaurant") || mLower.contains("cafe") || mLower.contains("food") || mLower.contains("eats") -> "FOOD"
            
            mLower.contains("uber") || mLower.contains("ola") || mLower.contains("rapido") || 
            mLower.contains("metro") || mLower.contains("petrol") || mLower.contains("fuel") || 
            mLower.contains("hpcl") || mLower.contains("bpcl") || mLower.contains("cab") -> "TRAVEL"
            
            mLower.contains("amazon") || mLower.contains("flipkart") || mLower.contains("myntra") || 
            mLower.contains("nykaa") || mLower.contains("shopping") || mLower.contains("retail") || 
            mLower.contains("decathlon") || mLower.contains("store") || mLower.contains("supermarket") || mLower.contains("grocery") -> "SHOPPING"
            
            mLower.contains("jio") || mLower.contains("airtel") || mLower.contains("vi") || 
            mLower.contains("recharge") || mLower.contains("electricity") || mLower.contains("bill") || 
            mLower.contains("water") || mLower.contains("gas") || mLower.contains("utility") || mLower.contains("bescom") -> "BILLS"
            
            mLower.contains("movie") || mLower.contains("netflix") || mLower.contains("prime") || 
            mLower.contains("hotstar") || mLower.contains("spotify") || mLower.contains("entertainment") || mLower.contains("cinema") -> "ENTERTAINMENT"
            
            mLower.contains("invest") || mLower.contains("mutual") || mLower.contains("groww") || 
            mLower.contains("zerodha") || mLower.contains("shares") || mLower.contains("stock") || mLower.contains("fund") -> "INVESTMENTS"
            
            else -> "MISCELLANEOUS"
        }
    }
}
