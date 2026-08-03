package com.example.financeapp.data

/**
 * Layer 1 of the categorization pipeline.
 * Resolves a category from the UPI VPA (Virtual Payment Address) handle alone.
 * Runs in 0ms — no API call, no I/O.
 *
 * Many "unknown" merchants are actually identifiable payment gateways or
 * known services from just the @handle part of their UPI VPA.
 *
 * Returns null if the VPA is a personal bank account handle or unknown,
 * which signals the pipeline to proceed to Layer 2 (regex) and beyond.
 */
object VpaResolver {

    // Payment gateway handles that indicate online shopping / e-commerce
    private val shoppingGateways = listOf(
        "razorpay", "easebuzz", "cashfree", "payu", "ccavenue",
        "instamojo", "payumoney", "billpay", "juspay", "stripe"
    )

    // Handles that indicate utility / bill payments
    private val billGateways = listOf(
        "billdesk", "bbps", "npci", "utility", "electricity",
        "bsnlpay", "tatapay", "adanipay"
    )

    // Handles that indicate travel payments
    private val travelGateways = listOf(
        "irctc", "fastag", "nhai", "redbus", "makemytrip", "goibibo",
        "yatra", "ixigo", "cleartrip"
    )

    // Handles that indicate food delivery
    private val foodGateways = listOf(
        "swiggy", "zomato", "dunzo"
    )

    // Personal bank handles — these are person-to-person transfers, return null
    // so the pipeline can check the merchant name for context
    private val personalBankHandles = listOf(
        "oksbi", "okicici", "okaxis", "okhdfcbank", "ybl",
        "ibl", "axl", "apl", "barodampay", "pnb", "upi"
    )

    /**
     * Extracts the UPI VPA from an SMS body, then resolves a category.
     *
     * @param smsBody Full SMS body text.
     * @return A category string (e.g. "SHOPPING") or null if unresolvable from VPA.
     */
    fun resolveFromSms(smsBody: String): String? {
        val vpa = extractVpa(smsBody) ?: return null
        return resolveFromVpa(vpa)
    }

    /**
     * Resolves a category directly from a known VPA string.
     */
    fun resolveFromVpa(vpa: String): String? {
        val lower = vpa.lowercase()
        val handle = vpa.substringAfter("@").lowercase()

        // Don't resolve personal bank handles — let merchant name decide
        if (personalBankHandles.any { handle.contains(it) }) return null

        return when {
            shoppingGateways.any { lower.contains(it) } -> "SHOPPING"
            billGateways.any { lower.contains(it) }     -> "BILLS"
            travelGateways.any { lower.contains(it) }   -> "TRAVEL"
            foodGateways.any { lower.contains(it) }     -> "FOOD"
            else                                         -> null
        }
    }

    /**
     * Extracts a UPI VPA (e.g. "merchant@razorpay" or "user@oksbi") from SMS body.
     * Looks for email-like patterns that are UPI VPAs.
     */
    private fun extractVpa(smsBody: String): String? {
        // UPI VPA pattern: word@word (but not email TLDs like .com/.in)
        val vpaRegex = Regex("""[a-zA-Z0-9.\-_]+@[a-zA-Z]+""")
        return vpaRegex.find(smsBody)?.value
    }
}
