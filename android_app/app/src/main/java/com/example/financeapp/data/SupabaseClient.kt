package com.example.financeapp.data

import android.content.Context
import com.example.financeapp.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.FlowType
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client — used for:
 * - Authentication (login/signup/session)
 * - User Profile data (user_profiles table — monthly income, name, etc.)
 *
 * Transaction data is stored locally in Room DB, not in Supabase.
 *
 * Session Persistence: The JWT access token + refresh token are stored in
 * Android SharedPreferences so the user stays logged in across app restarts.
 * Supabase automatically refreshes the access token using the refresh token
 * before it expires (access tokens expire in 1 hour by default).
 */
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Persist session to SharedPreferences so user stays logged in after app kill/restart
            flowType = FlowType.PKCE
            autoSaveToStorage = true   // Save JWT + refresh token to storage
            autoLoadFromStorage = true // Restore session from storage on launch
            alwaysAutoRefresh = true   // Auto-refresh access token before expiry
        }
        install(Postgrest)  // Used by UserRepository for user_profiles table only
    }

    val auth: Auth
        get() = client.auth
}


