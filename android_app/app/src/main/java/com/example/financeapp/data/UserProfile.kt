package com.example.financeapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("full_name")
    val fullName: String,
    val email: String,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("monthly_income")
    val monthlyIncome: Double? = null,
    val currency: String = "INR",
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Subset of fields that can be updated by the user.
 * Only includes columns the user is allowed to modify.
 */
@Serializable
data class UserProfileUpdate(
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("monthly_income")
    val monthlyIncome: Double? = null,
    val currency: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null
)
