package com.example.financeapp.data

import android.util.Log
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Repository that encapsulates all user authentication and profile operations.
 * Uses Supabase Auth (GoTrue) for authentication and Postgrest for the user_profiles table.
 */
object UserRepository {

    private const val TAG = "UserRepository"

    // ──────────────────────────────────────────────
    //  Authentication
    // ──────────────────────────────────────────────

    /**
     * Sign up a new user with email/password.
     * The Supabase trigger `on_auth_user_created` auto-creates the profile row,
     * but we also upsert phone_number and monthly_income if provided during sign-up.
     */
    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String? = null,
        monthlyIncome: Double? = null
    ) {
        // 1. Create the auth user
        SupabaseClient.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("full_name", fullName)
            }
        }

        // 2. If phone or income were provided, update the auto-created profile row
        if (!phoneNumber.isNullOrBlank() || monthlyIncome != null) {
            val userId = SupabaseClient.auth.currentUserOrNull()?.id ?: return
            val update = UserProfileUpdate(
                phoneNumber = phoneNumber?.takeIf { it.isNotBlank() },
                monthlyIncome = monthlyIncome
            )
            SupabaseClient.client.postgrest["user_profiles"]
                .update(update) {
                    filter { eq("id", userId) }
                }
        }
    }

    /**
     * Sign in with email/password.
     */
    suspend fun signIn(email: String, password: String) {
        SupabaseClient.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    /**
     * Sign out the current user and clear the session.
     */
    suspend fun signOut() {
        SupabaseClient.auth.signOut()
    }

    /**
     * Send a password reset email to the given address.
     */
    suspend fun resetPassword(email: String) {
        SupabaseClient.auth.resetPasswordForEmail(email)
    }

    /**
     * Returns true if a user is currently authenticated.
     */
    fun isLoggedIn(): Boolean {
        return SupabaseClient.auth.currentUserOrNull() != null
    }

    // ──────────────────────────────────────────────
    //  Profile (user_profiles table)
    // ──────────────────────────────────────────────

    /**
     * Fetch the current user's profile from the `user_profiles` table.
     * Returns [Resource.Success] with the profile (or null if not found),
     * or [Resource.Error] on failure.
     */
    suspend fun getProfile(): Resource<UserProfile?> {
        val userId = SupabaseClient.auth.currentUserOrNull()?.id
            ?: return Resource.Error("User not authenticated")
        return try {
            val profile = SupabaseClient.client.postgrest["user_profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfile>()
            Resource.Success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch profile", e)
            Resource.Error("Failed to load profile: ${e.localizedMessage}", e)
        }
    }

    /**
     * Update the current user's profile with the given fields.
     * Only non-null fields in [update] will be sent to Supabase.
     * Returns [Resource.Success] on success, or [Resource.Error] on failure.
     */
    suspend fun updateProfile(update: UserProfileUpdate): Resource<Unit> {
        val userId = SupabaseClient.auth.currentUserOrNull()?.id
            ?: return Resource.Error("User not authenticated")
        return try {
            SupabaseClient.client.postgrest["user_profiles"]
                .update(update) {
                    filter { eq("id", userId) }
                }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update profile", e)
            Resource.Error("Failed to update profile: ${e.localizedMessage}", e)
        }
    }
}
