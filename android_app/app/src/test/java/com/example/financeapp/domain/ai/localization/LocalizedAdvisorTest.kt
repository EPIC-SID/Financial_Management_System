package com.example.financeapp.domain.ai.localization

import com.example.financeapp.domain.engine.ConsolidatedViewBuilder
import com.example.financeapp.domain.engine.VerdictEngine
import com.example.financeapp.domain.model.Account
import com.example.financeapp.domain.model.AccountType
import com.example.financeapp.domain.model.DeclaredInputs
import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.Investment
import com.example.financeapp.domain.model.Liability
import com.example.financeapp.domain.model.MonthlyAggregate
import com.example.financeapp.domain.model.RecurringItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class LocalizedAdvisorTest {

    private val asOf = LocalDate.of(2026, 10, 1)
    private val snapshot = snapshot()
    private val view = ConsolidatedViewBuilder.from(snapshot)
    private val verdict = VerdictEngine.run(snapshot)
    private val shock = 8_000.0

    @Test
    fun everyLanguageAndIntentReturnsThreeNonBlankSections() {
        NarrationLanguage.entries.forEach { language ->
            CopilotIntent.entries.forEach { intent ->
                val advice = LocalizedAdvisor.advise(language, intent, snapshot, view, verdict, shock)
                val r = advice.response
                assertEquals(1, r.observed.size)
                assertEquals(1, r.predicted.size)
                assertEquals(1, r.recommended.size)
                assertTrue("$language $intent observed", r.observed[0].text.isNotBlank())
                assertTrue("$language $intent predicted", r.predicted[0].text.isNotBlank())
                assertTrue("$language $intent recommended", r.recommended[0].text.isNotBlank())
                assertTrue("$language $intent rupee", r.observed[0].text.contains("₹") || r.predicted[0].text.contains("₹"))
            }
        }
    }

    @Test
    fun metadataDoesNotChangeWhenLanguageChanges() {
        val english = LocalizedAdvisor.advise(NarrationLanguage.ENGLISH, CopilotIntent.SHOCK, snapshot, view, verdict, shock)
        NarrationLanguage.entries.forEach { language ->
            val other = LocalizedAdvisor.advise(language, CopilotIntent.SHOCK, snapshot, view, verdict, shock)
            assertEquals(english.response.observed[0].refs, other.response.observed[0].refs)
            assertEquals(english.response.predicted[0].range, other.response.predicted[0].range)
            assertEquals(english.response.predicted[0].confidenceLabel, other.response.predicted[0].confidenceLabel)
            assertEquals(english.response.predicted[0].horizon, other.response.predicted[0].horizon)
            assertEquals(english.response.recommended[0].impact, other.response.recommended[0].impact)
        }
    }

    @Test
    fun cashAdviceKeepsCardDuesSeparateAndShockRemainderIsComputed() {
        val cash = LocalizedAdvisor.advise(NarrationLanguage.ENGLISH, CopilotIntent.CASH_OVERVIEW, snapshot, view, verdict, shock)
        assertTrue(cash.response.observed[0].text.contains(inr(view.liquidCash)))
        assertTrue(cash.response.observed[0].text.contains(inr(view.cardDues)))
        assertEquals(listOf("cash", "card_dues"), cash.response.observed[0].refs)
        assertFalse(cash.response.observed[0].text.contains(inr(view.liquidCash - view.cardDues)))

        val after = view.liquidCash - shock
        val shockAdvice = LocalizedAdvisor.advise(NarrationLanguage.HINGLISH, CopilotIntent.SHOCK, snapshot, view, verdict, shock)
        assertTrue(shockAdvice.response.observed[0].text.contains(inr(after)))
        assertTrue(shockAdvice.response.observed[0].text.contains(inr(shock)))
        assertEquals(view.liquidCash - shock, after, 0.0)
    }

    @Test
    fun missingGoalAndIncomeStillReturnAdvice() {
        val bare = snapshot.copy(
            declared = snapshot.declared.copy(typicalIncome = null, goal = null),
            recurring = emptyList(),
        )
        val bareView = ConsolidatedViewBuilder.from(bare)
        val bareVerdict = VerdictEngine.run(bare)
        NarrationLanguage.entries.forEach { language ->
            val income = LocalizedAdvisor.advise(language, CopilotIntent.INCOME, bare, bareView, bareVerdict, shock)
            val goal = LocalizedAdvisor.advise(language, CopilotIntent.GOAL, bare, bareView, bareVerdict, shock)
            assertTrue(income.response.observed[0].text.isNotBlank())
            assertTrue(goal.response.observed[0].text.isNotBlank())
            assertEquals(listOf("income"), income.response.observed[0].refs)
            assertEquals(listOf("goal", "surplus"), goal.response.observed[0].refs)
        }
    }

    @Test
    fun predictedRangeUsesEngineP10AndSnapshotCash() {
        val advice = LocalizedAdvisor.advise(NarrationLanguage.MARATHI, CopilotIntent.RUNWAY, snapshot, view, verdict, shock)
        val range = advice.response.predicted[0].range
        assertEquals(verdict.minP10, range.p10, 0.0)
        assertEquals(snapshot.cash, range.p50, 0.0)
        assertEquals(snapshot.cash + snapshot.declared.typicalIncome!!, range.p90, 0.0)
        assertTrue(advice.response.predicted[0].text.contains(inr(verdict.minP10)))
    }

    private fun snapshot() = FinancialSnapshot(
        asOf = asOf,
        cash = 45_200.0,
        accounts = listOf(Account("bank", AccountType.SAVINGS, "HDFC", "4091", 45_200.0, asOf)),
        liabilities = listOf(
            Liability("card", "CARD", 12_000.0, null, null, null, 5, 150_000.0, 2_000.0, 12_000.0, 0.0, "bank"),
            Liability("home", "HOME_LOAN", 500_000.0, 0.08, 18_500.0, 120, 5, null, null, null, null, null),
        ),
        investments = listOf(Investment("sip", "Groww", 10_000.0, 7, "MUTUAL_FUND", "bank", 0.9)),
        recurring = listOf(RecurringItem("inc", "Salary", 85_000.0, 30, LocalDate.of(2026, 11, 1), 0.95, "INCOME")),
        history = listOf(MonthlyAggregate(YearMonth.of(2026, 9), 85_000.0, 20_000.0, 18_500.0, 15_000.0, 10_000.0)),
        declared = DeclaredInputs("FIXED", 85_000.0, 25_000.0, Goal("Emergency fund", 100_000.0, 6), "25-34"),
        completeness = 0.9,
        missing = emptyList(),
    )
}
