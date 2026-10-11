package com.example.financeapp.data

import org.junit.Assert.*
import org.junit.Test

class UserProfileValidationTest {
    @Test fun validatesNamesAndPhoneNumbers() {
        assertNull(UserProfileUpdate(fullName = "Siddhant", phoneNumber = "+919876543210").validationError())
        assertNotNull(UserProfileUpdate(fullName = " ").validationError())
        assertNotNull(UserProfileUpdate(fullName = "x".repeat(101)).validationError())
        assertNotNull(UserProfileUpdate(fullName = "A\nB").validationError())
        assertNotNull(UserProfileUpdate(phoneNumber = "not a phone").validationError())
    }
    @Test fun rejectsNonfiniteAndNegativeMoney() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 1_000_000_000.0).forEach {
            assertNotNull(UserProfileUpdate(monthlyIncome = it).validationError())
        }
        assertNull(UserProfileUpdate(monthlyIncome = 0.0).validationError())
    }
}
