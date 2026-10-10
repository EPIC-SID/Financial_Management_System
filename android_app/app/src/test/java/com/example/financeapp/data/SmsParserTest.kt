package com.example.financeapp.data

import org.junit.Assert.*
import org.junit.Test

class SmsParserTest {

    @Test
    fun parse_hdfcDebitSms_extractsDetailsCorrectly() {
        val sms = "Dear UPI user, A/C **1234 debited by 450.00 on 10-Oct-26 to Swiggy UPI:swiggy@icici Ref 123456789. Avl Bal Rs 25,000.00"
        val txn = SmsParser.parse(sms, "sms-101", 1700000000000L)

        assertNotNull(txn)
        assertEquals(-450.0, txn!!.amount, 0.001)
        assertTrue(txn.merchant.contains("Swiggy", ignoreCase = true))
        assertEquals("FOOD", txn.category)
        assertFalse(txn.description!!.contains("swiggy@icici"))
    }

    @Test
    fun parse_medicalStore_preservesBusinessContextAsHealth() {
        val sms = "Sent Rs.550.00 from Kotak Bank AC X9988 to Medical Store on 11-10-26 ref 9988776655. Bal Rs 12000"
        val txn = SmsParser.parse(sms, "sms-102", 1700000000000L)

        assertNotNull(txn)
        assertEquals(-550.0, txn!!.amount, 0.001)
        assertTrue(txn.merchant.contains("Medical Store", ignoreCase = true))
        assertEquals("HEALTH", txn.category)
    }

    @Test
    fun parse_steamPurchase_categorizesAsEntertainment() {
        val sms = "Paid INR 1,499.00 at Steam Games on 12-Oct-2026 using Card ending 4321. Ref txn9988"
        val txn = SmsParser.parse(sms, "sms-103", 1700000000000L)

        assertNotNull(txn)
        assertEquals(-1499.0, txn!!.amount, 0.001)
        assertTrue(txn.merchant.contains("Steam Games", ignoreCase = true))
        assertEquals("ENTERTAINMENT", txn.category)
    }

    @Test
    fun parse_upiP2mPattern_extractsMerchantAndCategory() {
        val sms = "Debited Rs. 320 for txn UPI/P2M/987654/Apollo Pharmacy/HDFC Bank. Balance Rs 5400"
        val txn = SmsParser.parse(sms, "sms-104", 1700000000000L)

        assertNotNull(txn)
        assertEquals(-320.0, txn!!.amount, 0.001)
        assertTrue(txn.merchant.contains("Apollo Pharmacy", ignoreCase = true))
        assertEquals("HEALTH", txn.category)
    }

    @Test
    fun parse_parenthesizedVpaMerchant_extractsCleanMerchant() {
        val sms = "Paid Rs 200 to merchant@okaxis (DMart Ready) ref 554433 on 15-Oct-26"
        val txn = SmsParser.parse(sms, "sms-105", 1700000000000L)

        assertNotNull(txn)
        assertEquals(-200.0, txn!!.amount, 0.001)
        assertTrue(txn.merchant.contains("DMart Ready", ignoreCase = true))
        assertEquals("SHOPPING", txn.category)
    }

    @Test
    fun parse_creditSms_categorizesAsIncome() {
        val sms = "INR 45,000.00 credited to A/C 9876543210 on 01-Oct-2026 from Tech Innovations Pvt Ltd. Avl Bal INR 85,000"
        val txn = SmsParser.parse(sms, "sms-106", 1700000000000L)

        assertNotNull(txn)
        assertEquals(45000.0, txn!!.amount, 0.001)
        assertEquals("INCOME", txn.category)
        assertTrue(txn.merchant.contains("Tech Innovations", ignoreCase = true))
    }

    @Test
    fun parse_sanitizesSensitiveAccountAndCardData() {
        val sms = "Paid Rs 1,200.00 using card 4111-2222-3333-4444 from A/C 1234567890 to Amazon India"
        val txn = SmsParser.parse(sms, "sms-107", 1700000000000L)

        assertNotNull(txn)
        val desc = txn!!.description ?: ""
        assertFalse("Account number must be masked", desc.contains("1234567890"))
        assertFalse("Card number must be masked", desc.contains("4111-2222-3333-4444"))
    }

    @Test
    fun parse_nonTransactionSms_returnsNull() {
        val otpSms = "Your OTP for login to RupAI is 492011. Do not share this with anyone."
        val txn = SmsParser.parse(otpSms, "sms-108", 1700000000000L)
        assertNull(txn)
    }
}

