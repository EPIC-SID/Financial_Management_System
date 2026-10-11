package com.example.financeapp.data

import android.content.Context
import android.net.Uri

/**
 * A raw SMS message read from the Android SMS inbox.
 */
data class RawSms(
    val id: String,
    val body: String,
    val date: Long,
    val address: String  // sender phone number / bank short-code
)

/**
 * SmsReader: extracts UPI/bank SMS messages from the device inbox.
 * Runs on Dispatchers.IO — never call from the main thread.
 */
object SmsReader {

    // Known Indian bank SMS sender IDs to filter relevant messages
    private val bankSenders = setOf(
        // SBI
        "SBIINB", "SBIPSG", "SBIUPI", "SBIBNK",
        // HDFC
        "HDFCBK", "HDFCBN", "HDFCSC",
        // ICICI
        "ICICIB", "ICICIBS",
        // Axis
        "AXISBK", "AXISBN",
        // Kotak
        "KOTAKB",
        // Yes Bank
        "YESBK",
        // PNB
        "PNBSMS",
        // BOI, BOB, Canara
        "BOIIND", "BOBTXN", "CANBKM",
        // PayTM, PhonePe, GPay
        "PAYTMB", "PYTMUPI", "PHNEPE", "GPAY",
        // IndusInd, Federal
        "INDBNK", "FEDBKM",
        // IDFC, AU
        "IDFCBK", "AUBCSB"
    )

    /**
     * Reads all SMS from inbox since [startTimeMs].
     * Optionally filters by known bank senders only.
     *
     * @param context Application context.
     * @param startTimeMs Epoch milliseconds. Pass 0 for all time.
     * @param filterByBankSenders If true, only returns SMS from known bank IDs.
     */
    fun read(
        context: Context,
        startTimeMs: Long,
        filterByBankSenders: Boolean = true
    ): List<RawSms> {
        val results = mutableListOf<RawSms>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "body", "date", "address")

        val selection = if (startTimeMs > 0L) "date >= ?" else null
        val selectionArgs = if (startTimeMs > 0L) arrayOf(startTimeMs.toString()) else null

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "date DESC"
        )

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow("_id")
            val bodyCol = c.getColumnIndexOrThrow("body")
            val dateCol = c.getColumnIndexOrThrow("date")
            val addressCol = c.getColumnIndexOrThrow("address")

            while (c.moveToNext()) {
                val address = c.getString(addressCol) ?: ""
                // If filtering, skip SMS not from known banks
                if (filterByBankSenders && !isBankSender(address)) continue

                results.add(
                    RawSms(
                        id = c.getString(idCol),
                        body = c.getString(bodyCol) ?: "",
                        date = c.getLong(dateCol),
                        address = address
                    )
                )
            }
        }
        return results
    }

    /**
     * Checks if the SMS sender address matches a known Indian bank sender ID.
     * Handles both short-codes (e.g. "HDFCBK") and long numbers.
     */
    fun isBankSender(address: String): Boolean {
        val upper = address.uppercase().trim()
        // Sender headers are a relevance filter, not cryptographic proof of a bank.
        val header = Regex("^(?:[A-Z]{2}-)?([A-Z0-9]{3,11})(?:-[A-Z])?$").matchEntire(upper)
            ?.groupValues?.get(1) ?: return false
        return header in bankSenders
    }
}
