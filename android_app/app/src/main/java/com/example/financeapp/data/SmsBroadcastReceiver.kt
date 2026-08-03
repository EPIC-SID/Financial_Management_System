package com.example.financeapp.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Real-time SMS listener for incoming UPI/bank transaction messages.
 *
 * When a new SMS arrives:
 * 1. Filters for known bank senders only (ignores spam)
 * 2. Parses the SMS using Layers 1+2 (VpaResolver + Regex) — instant, 0ms
 * 3. Inserts into Room DB (smsId PK guarantees no duplicates)
 * 4. Room Flow notifies the UI — transaction appears in the list immediately
 * 5. Enqueues AiCategorizationWorker if the category is MISCELLANEOUS
 *
 * This receiver is registered in AndroidManifest.xml with:
 *   action: android.provider.Telephony.SMS_RECEIVED
 *   permission: android.permission.RECEIVE_SMS
 *
 * No user interaction needed — new transactions appear automatically.
 */
class SmsBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }

    // A supervisor scope so individual failures don't kill the whole receiver
    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        for (smsMessage in messages) {
            val sender = smsMessage.originatingAddress ?: continue
            val body = smsMessage.messageBody ?: continue
            val date = smsMessage.timestampMillis

            // Only process SMS from known bank senders — ignore personal/spam SMS
            if (!SmsReader.isBankSender(sender)) {
                Log.v(TAG, "Ignoring non-bank SMS from: $sender")
                continue
            }

            Log.d(TAG, "Incoming bank SMS from $sender — processing")

            // Use goAsync() to safely run coroutine work while receiver is active
            val pendingResult = goAsync()

            receiverScope.launch {
                try {
                    processSms(context, body, smsMessage.indexOnIcc.toString(), date)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process incoming SMS: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun processSms(context: Context, body: String, smsId: String, date: Long) {
        val dao = AppDatabase.getInstance(context).transactionDao()

        // Layer 1+2: Fast parse (VPA + regex) — runs in <1ms
        val parsed = SmsParser.parse(body, smsId, date) ?: run {
            Log.d(TAG, "SMS did not parse as transaction — skipping")
            return
        }

        // Apply Layer 1 VPA resolution
        val vpaCategory = VpaResolver.resolveFromSms(body)
        val finalEntity = if (vpaCategory != null && parsed.category == "MISCELLANEOUS") {
            parsed.copy(category = vpaCategory)
        } else {
            parsed
        }.let { txnData ->
            with(SmsParser) { txnData.toEntity() }
        }

        // Insert to Room (IGNORE if smsId already exists — no-op for duplicates)
        dao.insertAll(listOf(finalEntity))
        Log.d(TAG, "Inserted real-time transaction: ${finalEntity.merchant} ${finalEntity.amount}")

        // If MISCELLANEOUS, enqueue background AI worker (layers 3+4)
        if (finalEntity.category == "MISCELLANEOUS") {
            AiCategorizationWorker.enqueue(context)
        }
    }
}
