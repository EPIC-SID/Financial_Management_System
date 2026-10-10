package com.example.financeapp.data

import org.junit.Assert.*
import org.junit.Test

class CategorizationPolicyTest {

    @Test
    fun businessContext_medicalStore_resolvesToHealthNotShopping() {
        // "Medical Store" contains "store" which must not trigger generic SHOPPING
        val category1 = CategorizationPolicy.determineCategory("Medical Store", isCredit = false, amount = 450.0)
        assertEquals("HEALTH", category1)

        val category2 = CategorizationPolicy.determineCategory("City Chemist Store", isCredit = false, amount = 220.0)
        assertEquals("HEALTH", category2)

        val category3 = CategorizationPolicy.determineCategory("Apollo Pharmacy Store", isCredit = false, amount = 600.0)
        assertEquals("HEALTH", category3)
    }

    @Test
    fun businessContext_steam_resolvesToEntertainment() {
        val category1 = CategorizationPolicy.determineCategory("Steam", isCredit = false, amount = 999.0)
        assertEquals("ENTERTAINMENT", category1)

        val category2 = CategorizationPolicy.determineCategory("Steam Games", isCredit = false, amount = 1499.0)
        assertEquals("ENTERTAINMENT", category2)
    }

    @Test
    fun microDebitHeuristic_unclassifiedSmallDebits_resolvesToFood() {
        // Debits in range [₹10, ₹150] to unclassified merchants or individuals default to FOOD
        val cat10 = CategorizationPolicy.determineCategory("Ramu Tapri", isCredit = false, amount = 10.0)
        assertEquals("FOOD", cat10)

        val cat50 = CategorizationPolicy.determineCategory("Sunil Kumar", isCredit = false, amount = 50.0)
        assertEquals("FOOD", cat50)

        val cat150 = CategorizationPolicy.determineCategory("Local Vendor", isCredit = false, amount = 150.0)
        assertEquals("FOOD", cat150)
    }

    @Test
    fun microDebitHeuristic_outsideRange_remainsMiscellaneous() {
        // Below ₹10
        val cat5 = CategorizationPolicy.determineCategory("Sunil Kumar", isCredit = false, amount = 5.0)
        assertEquals("MISCELLANEOUS", cat5)

        // Above ₹150
        val cat151 = CategorizationPolicy.determineCategory("Sunil Kumar", isCredit = false, amount = 151.0)
        assertEquals("MISCELLANEOUS", cat151)

        val cat500 = CategorizationPolicy.determineCategory("Sunil Kumar", isCredit = false, amount = 500.0)
        assertEquals("MISCELLANEOUS", cat500)
    }

    @Test
    fun microDebitHeuristic_creditsAreNotTreatedAsFood() {
        // Credit of ₹50 from a peer is INCOME, never FOOD
        val cat = CategorizationPolicy.determineCategory("Sunil Kumar", isCredit = true, amount = 50.0)
        assertEquals("INCOME", cat)
    }

    @Test
    fun explicitCategories_atMicroAmounts_areNotOverriddenByFood() {
        // ₹50 petrol is TRAVEL, not FOOD
        val petrol = CategorizationPolicy.determineCategory("HPCL Petrol Pump", isCredit = false, amount = 50.0)
        assertEquals("TRAVEL", petrol)

        // ₹80 medicine is HEALTH, not FOOD
        val medicine = CategorizationPolicy.determineCategory("Apollo Pharmacy", isCredit = false, amount = 80.0)
        assertEquals("HEALTH", medicine)

        // ₹30 metro ticket is TRAVEL, not FOOD
        val metro = CategorizationPolicy.determineCategory("Metro Rail", isCredit = false, amount = 30.0)
        assertEquals("TRAVEL", metro)
    }

    @Test
    fun isCachableCategory_rejectsTransientFallbacksAndInvalidValues() {
        // Valid categories
        assertTrue(CategorizationPolicy.isCachableCategory("FOOD"))
        assertTrue(CategorizationPolicy.isCachableCategory("TRAVEL"))
        assertTrue(CategorizationPolicy.isCachableCategory("HEALTH"))
        assertTrue(CategorizationPolicy.isCachableCategory("ENTERTAINMENT"))
        assertTrue(CategorizationPolicy.isCachableCategory("SHOPPING"))
        assertTrue(CategorizationPolicy.isCachableCategory("BILLS"))
        assertTrue(CategorizationPolicy.isCachableCategory("INVESTMENTS"))
        assertTrue(CategorizationPolicy.isCachableCategory("EDUCATION"))
        assertTrue(CategorizationPolicy.isCachableCategory("INCOME"))

        // Transient / fallback categories that must NEVER poison the cache
        assertFalse(CategorizationPolicy.isCachableCategory("MISCELLANEOUS"))
        assertFalse(CategorizationPolicy.isCachableCategory("FALLBACK"))
        assertFalse(CategorizationPolicy.isCachableCategory("UNKNOWN"))
        assertFalse(CategorizationPolicy.isCachableCategory(null))
        assertFalse(CategorizationPolicy.isCachableCategory(""))
        assertFalse(CategorizationPolicy.isCachableCategory("   "))
    }
}

