package com.example.financeapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.encodeToJsonElement

/** User declarations, never inferred balances or synthetic scenario values. */
@Serializable
data class AccountSetup(
    @SerialName("user_id") val userId: String,
    @SerialName("income_pattern") val incomePattern: String? = null,
    val obligations: List<String> = emptyList(),
    @SerialName("minimum_reserve") val minimumReserve: Long? = null,
    @SerialName("goal_name") val goalName: String? = null,
    @SerialName("goal_amount") val goalAmount: Long? = null,
    @SerialName("consent_version") val consentVersion: Int = 1
) {
    // Explicit nulls let users clear a previously saved goal or reserve.
    fun storagePayload(): JsonObject = STORAGE_JSON.encodeToJsonElement(this).jsonObject

    fun validationError(): String? = when {
        !UUID_PATTERN.matches(userId) -> "Sign in to save your setup."
        incomePattern != null && incomePattern !in INCOME_PATTERNS -> "Choose a supported income pattern."
        obligations.size > OBLIGATIONS.size || obligations.distinct().size != obligations.size ||
            obligations.any { it !in OBLIGATIONS } -> "Choose supported payment types."
        minimumReserve != null && minimumReserve !in 0L..999_999_999L -> "Enter a valid cash cushion."
        goalName != null && (goalName.isBlank() || goalName.length > 80 || goalName.any(Char::isISOControl)) -> "Goal names must contain 1 to 80 characters."
        (goalName == null) != (goalAmount == null) -> "Enter both a goal name and amount, or leave both empty."
        goalAmount != null && goalAmount !in 1L..999_999_999L -> "Enter a valid positive goal amount."
        consentVersion != 1 -> "Review the current cloud storage notice."
        else -> null
    }

    companion object {
        private val STORAGE_JSON = Json { encodeDefaults = true }
        val INCOME_PATTERNS = listOf("Fixed salary", "Varies month to month", "A mix")
        val OBLIGATIONS = listOf("Rent", "Home loan", "Vehicle EMI", "Credit card", "Gadget EMI", "Insurance", "Subscriptions", "Mutual fund SIP")
        private val UUID_PATTERN = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    }
}
