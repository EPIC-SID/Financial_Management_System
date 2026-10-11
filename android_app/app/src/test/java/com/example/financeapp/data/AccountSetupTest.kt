package com.example.financeapp.data

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull

class AccountSetupTest {
    private val setup = AccountSetup("00000000-0000-0000-0000-000000000001")

    @Test fun optionalDeclarationsCanRemainUnknown() { assertNull(setup.validationError()) }
    @Test fun clearedOptionalFieldsAreExplicitNullsInCloudPayload() {
        assertEquals(JsonNull, setup.storagePayload()["goal_name"])
        assertEquals(JsonNull, setup.storagePayload()["goal_amount"])
        assertEquals(JsonNull, setup.storagePayload()["minimum_reserve"])
        assertTrue(setup.storagePayload().containsKey("consent_version"))
    }
    @Test fun rejectsInvalidOwnerAndUnsupportedChoices() {
        assertNotNull(setup.copy(userId = "someone else").validationError())
        assertNotNull(setup.copy(incomePattern = "invented").validationError())
        assertNotNull(setup.copy(obligations = listOf("Rent", "Rent")).validationError())
        assertNotNull(setup.copy(obligations = listOf("Bank password")).validationError())
    }
    @Test fun boundsReserveAndGoalValues() {
        assertNull(setup.copy(minimumReserve = 0).validationError())
        assertNotNull(setup.copy(minimumReserve = -1).validationError())
        assertNotNull(setup.copy(minimumReserve = 1_000_000_000).validationError())
        assertNotNull(setup.copy(goalAmount = 5000).validationError())
        assertNotNull(setup.copy(goalName = "Trip", goalAmount = 0).validationError())
        assertNotNull(setup.copy(goalName = "x".repeat(81), goalAmount = 1).validationError())
        assertNotNull(setup.copy(goalName = "Trip\n", goalAmount = 1).validationError())
        assertNull(setup.copy(goalName = "Trip", goalAmount = 5000).validationError())
    }
    @Test fun payloadContainsNoSmsMerchantOrDemographicFields() {
        val json = Json.encodeToString(setup.copy(incomePattern = "A mix", minimumReserve = 40000))
        listOf("sms", "merchant", "age", "account_number", "phone", "email", "demo").forEach {
            assertFalse(json.contains(it))
        }
    }
}
