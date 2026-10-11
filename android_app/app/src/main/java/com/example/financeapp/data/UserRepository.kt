package com.example.financeapp.data

import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/**
 * Repository that encapsulates all user authentication and profile operations.
 * Uses Supabase Auth (GoTrue) for authentication and Postgrest for the user_profiles table.
 */
object UserRepository {


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
        requireSecureAuth(AuthAction.SIGN_UP, AuthInput(email, password, fullName))
        require(UserProfileUpdate(fullName = fullName, phoneNumber = phoneNumber,
            monthlyIncome = monthlyIncome).validationError() == null) { "Please check your profile details." }
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
        requireSecureAuth(AuthAction.SIGN_IN, AuthInput(email, password))
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
        requireSecureAuth(AuthAction.RESET_PASSWORD, AuthInput(email))
        SupabaseClient.auth.resetPasswordForEmail(email)
    }

    private fun requireSecureAuth(action: AuthAction, input: AuthInput) {
        require(com.example.financeapp.BuildConfig.SUPABASE_URL.startsWith("https://")) { "Secure sign-in is unavailable." }
        require(input.validationError(action) == null) { "Please check your account details." }
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
            val profile = withTimeout(15_000) { SupabaseClient.client.postgrest["user_profiles"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfile>() }
            if (SupabaseClient.auth.currentUserOrNull()?.id != userId) Resource.Error("Your account changed. Please retry.")
            else Resource.Success(profile)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Resource.Error("Profile request timed out. Please retry.")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Resource.Error("Could not load your profile. Please retry.")
        }
    }

    /**
     * Update the current user's profile with the given fields.
     * Empty phone/income fields explicitly clear the previously saved values.
     * Returns [Resource.Success] on success, or [Resource.Error] on failure.
     */
    suspend fun updateProfile(update: UserProfileUpdate): Resource<Unit> {
        update.validationError()?.let { return Resource.Error(it) }
        val userId = SupabaseClient.auth.currentUserOrNull()?.id
            ?: return Resource.Error("User not authenticated")
        return try {
            val rows = withTimeout(15_000) { SupabaseClient.client.postgrest["user_profiles"]
                .update(buildJsonObject {
                    update.fullName?.let { put("full_name", it) }
                    put("phone_number", update.phoneNumber?.let { kotlinx.serialization.json.JsonPrimitive(it) } ?: kotlinx.serialization.json.JsonNull)
                    put("monthly_income", update.monthlyIncome?.let { kotlinx.serialization.json.JsonPrimitive(it) } ?: kotlinx.serialization.json.JsonNull)
                    update.currency?.let { put("currency", it) }
                }) {
                    select()
                    filter { eq("id", userId) }
                }.decodeList<UserProfile>() }
            if (rows.singleOrNull()?.id != userId || SupabaseClient.auth.currentUserOrNull()?.id != userId)
                Resource.Error("Profile was not saved. Please retry after signing in.")
            else Resource.Success(Unit)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Resource.Error("Profile request timed out. Please retry.")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Resource.Error("Could not save your profile. Please retry.")
        }
    }
}
