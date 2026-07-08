package com.example.financeapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionData(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val merchant: String,
    val amount: Double,
    val category: String,
    val description: String? = null,
    @SerialName("transaction_date")
    val transactionDate: String,
    @SerialName("sms_id")
    val smsId: String? = null
)
