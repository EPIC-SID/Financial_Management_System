package com.example.financeapp.data

import com.example.financeapp.BuildConfig
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout

/** Uses the public client and signed-in user's JWT; authorization is enforced by RLS. */
object AccountSetupRepository {
    private const val TABLE = "account_setup"

    private suspend fun <T> request(action: suspend (String) -> T): Resource<T> {
        if (!BuildConfig.SUPABASE_URL.startsWith("https://")) return Resource.Error("Secure account storage is unavailable.")
        val owner = SupabaseClient.auth.currentUserOrNull()?.id ?: return Resource.Error("Sign in to manage your setup.")
        return try {
            val result = withTimeout(15_000) { action(owner) }
            if (SupabaseClient.auth.currentUserOrNull()?.id != owner) Resource.Error("Your account changed. Please try again.")
            else Resource.Success(result)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Resource.Error("Account storage timed out. Please retry.")
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) {
            Resource.Error("Account storage is unavailable. Check your connection and try again.")
        }
    }

    suspend fun load(): Resource<AccountSetup?> = request { owner ->
        SupabaseClient.client.postgrest[TABLE].select { filter { eq("user_id", owner) } }.decodeSingleOrNull<AccountSetup>()
    }

    suspend fun save(setup: AccountSetup): Resource<Unit> {
        setup.validationError()?.let { return Resource.Error(it) }
        return request { owner ->
            require(setup.userId == owner)
            val stored = SupabaseClient.client.postgrest[TABLE].upsert(setup.storagePayload(), onConflict = "user_id", defaultToNull = false) {
                select()
            }.decodeSingle<AccountSetup>()
            require(stored == setup)
            Unit
        }
    }

    suspend fun delete(): Resource<Unit> = request { owner ->
        SupabaseClient.client.postgrest[TABLE].delete { filter { eq("user_id", owner) } }
        Unit
    }
}
