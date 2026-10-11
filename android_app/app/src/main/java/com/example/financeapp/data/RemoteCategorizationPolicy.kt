package com.example.financeapp.data

import java.net.URI

object RemoteCategorizationPolicy {
    const val PREFS = "remote_categorization"
    const val ENABLED = "explicit_consent"

    fun isAllowedUrl(value: String, debug: Boolean): Boolean = try {
        val uri = URI(value)
        uri.userInfo == null && !uri.host.isNullOrBlank() &&
            (uri.scheme == "https" || (debug && uri.scheme == "http" &&
                uri.host in setOf("localhost", "127.0.0.1", "10.0.2.2")))
    } catch (_: Exception) { false }

    fun merchant(value: String): String? {
        val clean = value.trim()
        if (clean.length !in 1..80 || clean.any { it.isISOControl() } ||
            clean.contains('@') || Regex("[0-9]{4,}").containsMatchIn(clean)) return null
        return clean
    }
}
