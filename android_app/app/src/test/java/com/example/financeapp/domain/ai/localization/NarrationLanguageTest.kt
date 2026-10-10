package com.example.financeapp.domain.ai.localization

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationLanguageTest {

    @Test
    fun fourLanguagesHaveTheAgreedCodesAndLocales() {
        assertEquals(
            listOf("en" to "en-IN", "hi-Latn" to "hi-IN", "hi-Deva" to "hi-IN", "mr-Deva" to "mr-IN"),
            NarrationLanguage.entries.map { it.code to it.locale },
        )
        assertEquals(4, NarrationLanguage.entries.map { it.code }.toSet().size)
    }

    @Test
    fun fromCodeFindsALanguageAndRejectsUnknown() {
        assertEquals(NarrationLanguage.HINGLISH, NarrationLanguage.fromCode("hi-Latn"))
        assertEquals(NarrationLanguage.MARATHI, NarrationLanguage.fromCode("mr-Deva"))
        assertNull(NarrationLanguage.fromCode("fr"))
    }

    @Test
    fun everyLanguageHasCashIncomeAndShockExamples() {
        NarrationLanguage.entries.forEach { language ->
            val examples = language.examples()
            assertNotNull(examples)
            listOf(examples.cashOverview, examples.income, examples.shock).forEach { line ->
                assertTrue("${language.name} blank", line.isNotBlank())
                assertTrue("${language.name} missing rupee", line.contains("₹"))
            }
        }
    }
}
