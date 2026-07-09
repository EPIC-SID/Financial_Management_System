package com.example.financeapp.data

/**
 * A generic sealed class for wrapping API/database results.
 * Provides consistent error handling across all repository operations.
 */
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
