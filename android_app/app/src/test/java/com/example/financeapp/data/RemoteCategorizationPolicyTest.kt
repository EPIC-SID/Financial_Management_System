package com.example.financeapp.data

import org.junit.Assert.*
import org.junit.Test

class RemoteCategorizationPolicyTest {
    @Test fun rejectsPlaintextRemoteUrlsCredentialsAndMalformedUrls() {
        assertFalse(RemoteCategorizationPolicy.isAllowedUrl("http://example.com", true))
        assertFalse(RemoteCategorizationPolicy.isAllowedUrl("https://user:secret@example.com", true))
        assertFalse(RemoteCategorizationPolicy.isAllowedUrl("not a url", true))
        assertFalse(RemoteCategorizationPolicy.isAllowedUrl("http://localhost:8000", false))
        assertTrue(RemoteCategorizationPolicy.isAllowedUrl("http://10.0.2.2:8000", true))
        assertTrue(RemoteCategorizationPolicy.isAllowedUrl("https://example.com", false))
    }
    @Test fun rejectsLikelyIdentifiersAndUnboundedMerchantFields() {
        assertNull(RemoteCategorizationPolicy.merchant("person@upi"))
        assertNull(RemoteCategorizationPolicy.merchant("Account 123456789"))
        assertNull(RemoteCategorizationPolicy.merchant("Swiggy\nOTP"))
        assertNull(RemoteCategorizationPolicy.merchant("a".repeat(81)))
        assertEquals("Swiggy", RemoteCategorizationPolicy.merchant(" Swiggy "))
    }
}
