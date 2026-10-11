package com.example.financeapp.ui.viewmodel

import com.example.financeapp.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class OnboardingViewModelTest {
    private val owner = "00000000-0000-0000-0000-000000000001"
    @Test fun sourceIsRequiredAndCsvCannotPretendToImport() {
        val vm = OnboardingViewModel()
        vm.next(); vm.next()
        assertEquals(OnboardingPage.DATA_SOURCE, vm.uiState.value.page)
        vm.selectSource(OnboardingDataSource.CSV); vm.next()
        assertEquals(OnboardingPage.DATA_SOURCE, vm.uiState.value.page)
    }
    @Test fun invalidMoneyIsNotSilentlyConvertedToPositiveMoney() {
        val vm = OnboardingViewModel()
        vm.setCushion("15000"); vm.setCushion("-8000")
        vm.setGoalAmount("5000"); vm.setGoalAmount("not 9000")
        assertEquals("15000", vm.uiState.value.cushionDigits)
        assertEquals("5000", vm.uiState.value.goalDigits)
    }
    @Test fun localCompletionDoesNotCallCloudOrFabricateAccounts() = runBlocking {
        val vm = OnboardingViewModel()
        vm.selectSource(OnboardingDataSource.SMS)
        vm.setMoneyArrival("A mix"); vm.setCushion("40000")
        vm.finish(null, false) { error("No cloud consent") }
        assertEquals(OnboardingPage.BUILDING, vm.uiState.value.page)
        assertEquals("40000", vm.uiState.value.cushionDigits)
        assertEquals(OnboardingDataSource.SMS, vm.uiState.value.selectedSource)
        assertTrue(vm.uiState.value.ingestRows.isEmpty())
    }
    @Test fun cloudSaveUsesActualDeclarationsAndOwner() = runBlocking {
        val vm = OnboardingViewModel()
        vm.setMoneyArrival("Varies month to month"); vm.toggleObligation("Rent")
        vm.setCushion("40000"); vm.setGoalName("Trip"); vm.setGoalAmount("8000")
        var saved: AccountSetup? = null
        vm.finish(owner, true) { saved = it; Resource.Success(Unit) }
        assertEquals(owner, saved!!.userId)
        assertEquals(40000L, saved!!.minimumReserve)
        assertEquals(8000L, saved!!.goalAmount)
        assertEquals(listOf("Rent"), saved!!.obligations)
        assertNotNull(vm.uiState.value.completeness)
    }
    @Test fun failedSavePreservesDraftAndDoesNotComplete() = runBlocking {
        val vm = OnboardingViewModel()
        vm.setCushion("15000")
        vm.finish(owner, true) { Resource.Error("Offline") }
        assertEquals("15000", vm.uiState.value.cushionDigits)
        assertEquals("Offline", vm.uiState.value.error)
        assertNull(vm.uiState.value.completeness)
        assertFalse(vm.uiState.value.busy)
    }
    @Test fun invalidGoalAndMissingLoginCannotSave() = runBlocking {
        val vm = OnboardingViewModel()
        vm.setGoalName("Trip")
        vm.finish(owner, true) { error("Invalid input must not be sent") }
        assertNotNull(vm.uiState.value.error)
        vm.setGoalName("")
        vm.finish(null, true) { error("Unauthenticated input must not be sent") }
        assertNull(vm.uiState.value.completeness)
    }
    @Test fun noObligationsIsExclusiveAndLoadedSetupIsPreserved() {
        val vm = OnboardingViewModel()
        vm.toggleObligation("Rent"); vm.toggleObligation("None")
        assertEquals(setOf("None"), vm.uiState.value.obligations)
        vm.toggleObligation("Credit card")
        assertEquals(setOf("Credit card"), vm.uiState.value.obligations)
        vm.load(AccountSetup(owner, minimumReserve = 0, goalName = "Trip", goalAmount = 100))
        assertEquals("0", vm.uiState.value.cushionDigits)
        assertEquals("Trip", vm.uiState.value.goalName)
    }
    @Test fun busyDraftCannotChangeDuringSave() = runBlocking {
        val vm = OnboardingViewModel()
        vm.setCushion("15000")
        vm.finish(owner, true) {
            vm.setCushion("99999"); vm.toggleObligation("Rent"); vm.next(); vm.back()
            assertEquals("15000", vm.uiState.value.cushionDigits)
            assertTrue(vm.uiState.value.obligations.isEmpty())
            Resource.Success(Unit)
        }
    }
    @Test fun moneyQuestionsFinishAfterTwoAnswers() {
        val vm = OnboardingViewModel()
        vm.next()
        vm.selectSource(OnboardingDataSource.SAMPLE)
        vm.next()
        assertEquals(OnboardingPage.ABOUT_MONEY, vm.uiState.value.page)
        vm.next()
        assertEquals(0, vm.uiState.value.moneyStep)
        vm.setMoneyArrival("A mix")
        vm.next()
        assertEquals(1, vm.uiState.value.moneyStep)
        vm.toggleObligation("None")
        vm.next()
        assertEquals(OnboardingPage.SAFETY_NET, vm.uiState.value.page)
    }
    @Test fun safetyNetNeedsExplicitConfirmationChoice() {
        val vm = OnboardingViewModel()
        assertFalse(vm.uiState.value.cushionSelected)
        vm.setCushion("")
        assertTrue(vm.uiState.value.cushionSelected)
        assertEquals("", vm.uiState.value.cushionDigits)
    }
}
