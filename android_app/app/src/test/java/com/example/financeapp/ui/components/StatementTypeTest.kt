package com.example.financeapp.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class StatementTypeTest {
    @Test fun labelsMatchProductLanguage() {
        assertEquals(
            listOf("Observed", "Predicted", "Recommended"),
            StatementType.values().map { it.label }
        )
    }
}
