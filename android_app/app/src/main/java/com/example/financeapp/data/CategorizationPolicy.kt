package com.example.financeapp.data

import kotlin.math.abs

/**
 * CategorizationPolicy — Deterministic on-device category assignment and cache validation policy.
 *
 * Enforces:
 * 1. Business Context Preservation: Domain-specific categories (HEALTH, EDUCATION, ENTERTAINMENT,
 *    BILLS, INVESTMENTS, TRAVEL, FOOD) take strict precedence over generic retail words
 *    (e.g., "Medical Store" -> HEALTH, "Steam Games" -> ENTERTAINMENT).
 * 2. Micro-debit Heuristic: Unclassified Indian debits between ₹10 and ₹150 default to FOOD (snacks/chai/local eats).
 * 3. Bounded Caching: Transient fallbacks ("FALLBACK", "MISCELLANEOUS", unknown) are never cached.
 */
object CategorizationPolicy {

    val VALID_CATEGORIES = setOf(
        "FOOD",
        "TRAVEL",
        "SHOPPING",
        "BILLS",
        "ENTERTAINMENT",
        "INVESTMENTS",
        "HEALTH",
        "EDUCATION",
        "INCOME"
    )

    private val KNOWN_PERSONS = listOf(
        "atharva", "rushikesh", "roshan", "okkhanjojdkar", "shashank", "ramdas"
    )

    /**
     * Determines transaction category from merchant, credit/debit direction, and amount.
     */
    fun determineCategory(merchant: String, isCredit: Boolean, amount: Double = 0.0): String {
        if (isCredit) return "INCOME"

        val m = merchant.lowercase().trim()
        val absAmount = abs(amount)

        // 1. If explicit known person, default to unclassified (MISCELLANEOUS candidate)
        val isKnownPerson = KNOWN_PERSONS.any { m.contains(it) }

        val explicitCategory = if (isKnownPerson) {
            null
        } else {
            resolveExplicitCategory(m)
        }

        if (explicitCategory != null) {
            return explicitCategory
        }

        // 2. Micro-debit heuristic: ₹10 – ₹150 for unclassified / local peer debits -> FOOD
        if (absAmount in 10.0..150.0) {
            return "FOOD"
        }

        return "MISCELLANEOUS"
    }

    private fun resolveExplicitCategory(m: String): String? {
        return when {
            // --- HEALTH (Takes precedence over generic retail/store keywords) ---
            m.contains("pharmacy") || m.contains("medical") || m.contains("hospital") ||
            m.contains("clinic") || m.contains("doctor") || m.contains("apollo") ||
            m.contains("medplus") || m.contains("1mg") || m.contains("pharmeasy") ||
            m.contains("netmeds") || m.contains("diagnostic") || m.contains("chemist") ||
            m.contains("medicine") || m.contains("health") || m.contains("dental") ||
            m.contains("pathology") -> "HEALTH"

            // --- EDUCATION ---
            m.contains("school") || m.contains("college") || m.contains("university") ||
            m.contains("tuition") || m.contains("udemy") || m.contains("coursera") ||
            m.contains("byju") || m.contains("unacademy") || m.contains("stationery") ||
            m.contains("education") || m.contains("library") || m.contains("academy") ||
            m.contains("institute") -> "EDUCATION"

            // --- ENTERTAINMENT (Steam, Netflix, Games, PVR) ---
            m.contains("steam") || m.contains("movie") || m.contains("netflix") ||
            m.contains("prime") || m.contains("hotstar") || m.contains("spotify") ||
            m.contains("youtube") || m.contains("entertainment") || m.contains("cinema") ||
            m.contains("pvr") || m.contains("inox") || m.contains("bookmyshow") ||
            m.contains("gaming") || m.contains("playstation") || m.contains("xbox") ||
            m.contains("theatre") || m.contains("theater") -> "ENTERTAINMENT"

            // --- BILLS & UTILITIES ---
            m.contains("jio") || m.contains("airtel") || m.contains("vodafone") ||
            m.contains("bsnl") || m.contains("recharge") || m.contains("electricity") ||
            m.contains("bill") || m.contains("water") || m.contains("gas") ||
            m.contains("utility") || m.contains("bescom") || m.contains("msedcl") ||
            m.contains("broadband") || m.contains("wifi") || m.contains("rent") ||
            m.contains("emi") || m.contains("insurance") || m.contains("premium") ||
            m.contains("tataplay") || m.contains("tata sky") || m.contains("dishtv") -> "BILLS"

            // --- INVESTMENTS ---
            m.contains("invest") || m.contains("groww") || m.contains("zerodha") ||
            m.contains("shares") || m.contains("stock") || m.contains("sip") ||
            m.contains("ppf") || m.contains("nps") || m.contains("upstox") ||
            m.contains("smallcase") || m.contains("mutual fund") || m.contains("kuvera") -> "INVESTMENTS"

            // --- TRAVEL & TRANSIT ---
            m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
            m.contains("metro") || m.contains("petrol") || m.contains("fuel") ||
            m.contains("hpcl") || m.contains("bpcl") || m.contains("iocl") ||
            m.contains("shell") || m.contains("cab") || m.contains("taxi") ||
            m.contains("bus") || m.contains("irctc") || m.contains("railway") ||
            m.contains("train") || m.contains("flight") || m.contains("airline") ||
            m.contains("airport") || m.contains("indigo") || m.contains("spicejet") ||
            m.contains("parking") || m.contains("fastag") || m.contains("toll") ||
            m.contains("auto") -> "TRAVEL"

            // --- FOOD & DINING ---
            m.contains("swiggy") || m.contains("zomato") || m.contains("starbucks") ||
            m.contains("restaurant") || m.contains("cafe") || m.contains("food") ||
            m.contains("eats") || m.contains("kitchen") || m.contains("dhaba") ||
            m.contains("mithai") || m.contains("sweet") || m.contains("bakery") ||
            m.contains("bakers") || m.contains("pizza") || m.contains("burger") ||
            m.contains("dominos") || m.contains("mcdonalds") || m.contains("kfc") ||
            m.contains("subway") || m.contains("haldiram") || m.contains("blinkit") ||
            m.contains("zepto") || m.contains("instamart") || m.contains("bigbasket") ||
            m.contains("grofers") || m.contains("chai") || m.contains("tea") ||
            m.contains("coffee") || m.contains("juice") || m.contains("vending") ||
            m.contains("canteen") || m.contains("tiffin") || m.contains("mess") ||
            m.contains("snacks") || m.contains("hotel") || m.contains("shankar") ||
            m.contains("dairy") || m.contains("daba") || m.contains("eatery") ||
            m.contains("thali") || m.contains("paratha") || m.contains("dosa") ||
            m.contains("biryani") || m.contains("vaishnav") || m.contains("trimurti") ||
            m.contains("chaiwala") || m.contains("bhojanalay") || m.contains("udupi") -> "FOOD"

            // --- SHOPPING (Evaluated AFTER specific services) ---
            m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
            m.contains("nykaa") || m.contains("shopping") || m.contains("retail") ||
            m.contains("decathlon") || m.contains("supermarket") || m.contains("grocery") ||
            m.contains("meesho") || m.contains("ajio") || m.contains("croma") ||
            m.contains("dmart") || m.contains("ikea") || m.contains("super") ||
            m.contains("mart") || m.contains("bazar") || m.contains("bazaar") ||
            m.contains("traders") || m.contains("kirana") || m.contains("provision") ||
            m.contains("provisions") || m.contains("general") || m.contains("enterprises") ||
            m.contains("footwear") || m.contains("fashion") || m.contains("textile") ||
            m.contains("textiles") || m.contains("cloth") || m.contains("garments") ||
            m.contains("jewellers") || m.contains("hardware") || m.contains("electronics") ||
            m.contains("mobile") || m.contains("store") || m.contains("opticals") -> "SHOPPING"

            else -> null
        }
    }

    /**
     * Cache validation gate.
     * Prevents transient failure states and uncertain fallbacks from poisoning SharedPreferences cache.
     */
    fun isCachableCategory(category: String?): Boolean {
        if (category.isNullOrBlank()) return false
        val clean = category.trim().uppercase()
        if (clean == "FALLBACK" || clean == "MISCELLANEOUS" || clean == "UNKNOWN") return false
        return VALID_CATEGORIES.contains(clean)
    }
}

