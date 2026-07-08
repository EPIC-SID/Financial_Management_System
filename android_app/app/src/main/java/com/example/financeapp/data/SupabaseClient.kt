package com.example.financeapp.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

object SupabaseClient {
    private const val SUPABASE_URL = "https://lsudxyorjmmoagkyzxzn.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxzdWR4eW9yam1tb2Fna3l6eHpuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODM1MjE2NjgsImV4cCI6MjA5OTA5NzY2OH0.x4PLWP6lI4M5eCYloo5Hr1Z106KuoHH27EWBX9o2amw"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }

    val auth: Auth
        get() = client.auth

    val postgrest: Postgrest
        get() = client.postgrest
}
