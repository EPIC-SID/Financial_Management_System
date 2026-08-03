package com.example.financeapp.data

import com.example.financeapp.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client — used for:
 * - Authentication (login/signup/session)
 * - User Profile data (user_profiles table — monthly income, name, etc.)
 *
 * Transaction data is stored locally in Room DB, not in Supabase.
 */
object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)  // Used by UserRepository for user_profiles table only
    }

    val auth: Auth
        get() = client.auth
}


