package com.example.financeapp.domain.anomaly

import com.example.financeapp.data.TransactionEntity
import org.junit.Assert.*
import org.junit.Test

class ExpenseAnomalyDetectorTest {

    @Test
    fun computeMedian_oddAndEvenSets_calculatesCorrectly() {
        val odd = listOf(100.0, 300.0, 200.0)
        assertEquals(200.0, ExpenseAnomalyDetector.computeMedian(odd), 0.001)

        val even = listOf(100.0, 400.0, 200.0, 300.0)
        assertEquals(250.0, ExpenseAnomalyDetector.computeMedian(even), 0.001)

        assertEquals(0.0, ExpenseAnomalyDetector.computeMedian(emptyList()), 0.001)
    }

    @Test
    fun computeMad_standardVariance_calculatesCorrectly() {
        // Data: [100, 100, 100, 100, 900]
        // Median = 100
        // Deviations: [|100-100|, |100-100|, |100-100|, |100-100|, |900-100|] = [0, 0, 0, 0, 800]
        // MAD = Median([0, 0, 0, 0, 800]) = 0.0
        val tightlyClustered = listOf(100.0, 100.0, 100.0, 100.0, 900.0)
        assertEquals(0.0, ExpenseAnomalyDetector.computeMad(tightlyClustered), 0.001)

        // Data: [10, 20, 30, 40, 50]
        // Median = 30
        // Deviations: [20, 10, 0, 10, 20] -> sorted [0, 10, 10, 20, 20]
        // MAD = 10.0
        val uniform = listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        assertEquals(10.0, ExpenseAnomalyDetector.computeMad(uniform), 0.001)
    }

    @Test
    fun evaluate_normalExpenseWithinRange_returnsNormal() {
        val history = listOf(
            createTxn("1", "Swiggy", -250.0, "FOOD", "2026-03-01T12:00:00"),
            createTxn("2", "Swiggy", -300.0, "FOOD", "2026-03-05T12:00:00"),
            createTxn("3", "Swiggy", -280.0, "FOOD", "2026-03-10T12:00:00")
        )
        val target = createTxn("4", "Swiggy", -320.0, "FOOD", "2026-03-15T12:00:00")

        val result = ExpenseAnomalyDetector.evaluate(target, history)

        assertEquals(AnomalyStatus.NORMAL, result.status)
        assertEquals(AnomalyScope.MERCHANT, result.scope)
        assertEquals(320.0, result.amount, 0.01)
        assertEquals(280.0, result.historicalMedian, 0.01)
        assertTrue(result.score < 3.0)
    }

    @Test
    fun evaluate_outlierExpense_returnsUnusualWithMerchantScope() {
        val history = listOf(
            createTxn("1", "Swiggy", -250.0, "FOOD", "2026-03-01T12:00:00"),
            createTxn("2", "Swiggy", -300.0, "FOOD", "2026-03-05T12:00:00"),
            createTxn("3", "Swiggy", -280.0, "FOOD", "2026-03-10T12:00:00"),
            createTxn("4", "Swiggy", -270.0, "FOOD", "2026-03-12T12:00:00")
        )
        // Median is ~275, MAD is ~15 (bounded by minAbsoluteDeviation of 50.0)
        // A ₹3,500 order on Swiggy is a massive outlier: (3500 - 275) / 50 = 64.5x MAD!
        val target = createTxn("5", "Swiggy", -3500.0, "FOOD", "2026-03-15T12:00:00")

        val result = ExpenseAnomalyDetector.evaluate(target, history)

        assertEquals(AnomalyStatus.UNUSUAL, result.status)
        assertEquals(AnomalyScope.MERCHANT, result.scope)
        assertEquals(3500.0, result.amount, 0.01)
        assertTrue("Score should be much higher than 3.0 MAD", result.score > 3.0)
        assertTrue(result.explanation.contains("Unusual spend"))
    }

    @Test
    fun evaluate_insufficientMerchantData_fallsBackToCategoryScope() {
        val history = listOf(
            createTxn("1", "Zomato", -250.0, "FOOD", "2026-03-01T12:00:00"),
            createTxn("2", "McDonalds", -300.0, "FOOD", "2026-03-05T12:00:00"),
            createTxn("3", "Burger King", -280.0, "FOOD", "2026-03-10T12:00:00")
        )
        // New merchant never seen before, but Category "FOOD" has 3 data points (median ~280)
        // Target is ₹5,000 at a new restaurant
        val target = createTxn("4", "Luxury Fine Dine", -5000.0, "FOOD", "2026-03-15T12:00:00")

        val result = ExpenseAnomalyDetector.evaluate(target, history)

        assertEquals(AnomalyStatus.UNUSUAL, result.status)
        assertEquals(AnomalyScope.CATEGORY, result.scope)
        assertEquals(5000.0, result.amount, 0.01)
        assertEquals(280.0, result.historicalMedian, 0.01)
    }

    @Test
    fun evaluate_priorOnlyObservations_futureSpikesDoNotLeakIntoPastEvaluation() {
        val t1 = createTxn("1", "Swiggy", -200.0, "FOOD", "2026-03-01T12:00:00")
        val t2 = createTxn("2", "Swiggy", -250.0, "FOOD", "2026-03-05T12:00:00")
        val t3 = createTxn("3", "Swiggy", -220.0, "FOOD", "2026-03-10T12:00:00")
        val targetNormal = createTxn("4", "Swiggy", -230.0, "FOOD", "2026-03-12T12:00:00")
        // Future transaction happening days later with huge amount
        val futureSpike = createTxn("5", "Swiggy", -10000.0, "FOOD", "2026-03-25T12:00:00")

        val allTransactions = listOf(t1, t2, t3, targetNormal, futureSpike)

        val result = ExpenseAnomalyDetector.evaluate(targetNormal, allTransactions)

        // Future spike must NOT affect targetNormal's historical median calculation
        assertEquals(AnomalyStatus.NORMAL, result.status)
        assertEquals(220.0, result.historicalMedian, 0.01)
    }

    @Test
    fun evaluate_excludedCategories_billsAndInvestments_returnsNormal() {
        val history = listOf(
            createTxn("1", "Rent", -15000.0, "BILLS", "2026-01-01T12:00:00"),
            createTxn("2", "Rent", -15000.0, "BILLS", "2026-02-01T12:00:00"),
            createTxn("3", "Rent", -15000.0, "BILLS", "2026-03-01T12:00:00")
        )
        // Rent of 15k is high, but BILLS are excluded from discretionary anomaly alerts
        val target = createTxn("4", "Rent", -15000.0, "BILLS", "2026-04-01T12:00:00")

        val result = ExpenseAnomalyDetector.evaluate(target, history)

        assertEquals(AnomalyStatus.NORMAL, result.status)
        assertTrue(result.explanation.contains("Excluded category"))
    }

    @Test
    fun evaluate_creditTransactions_returnsNormal() {
        val target = createTxn("1", "Salary", 85000.0, "INCOME", "2026-03-01T12:00:00")
        val result = ExpenseAnomalyDetector.evaluate(target, emptyList())

        assertEquals(AnomalyStatus.NORMAL, result.status)
        assertTrue(result.explanation.contains("Credits and zero amounts are excluded"))
    }

    @Test
    fun detectAll_runsChronologicalEvaluationOnFullStream() {
        val stream = listOf(
            createTxn("1", "Uber", -150.0, "TRAVEL", "2026-03-01T09:00:00"),
            createTxn("2", "Uber", -180.0, "TRAVEL", "2026-03-02T09:00:00"),
            createTxn("3", "Uber", -160.0, "TRAVEL", "2026-03-03T09:00:00"),
            createTxn("4", "Uber", -1500.0, "TRAVEL", "2026-03-04T09:00:00") // Anomaly!
        )

        val results = ExpenseAnomalyDetector.detectAll(stream)

        assertEquals(4, results.size)
        // Txn 1, 2, 3 have < 3 prior points -> Insufficient data / Normal
        assertEquals(AnomalyStatus.NORMAL, results[0].second.status)
        assertEquals(AnomalyStatus.NORMAL, results[1].second.status)
        assertEquals(AnomalyStatus.NORMAL, results[2].second.status)
        // Txn 4 has 3 prior points, ₹1500 is ~10x median -> Unusual
        assertEquals(AnomalyStatus.UNUSUAL, results[3].second.status)
        assertEquals(AnomalyScope.MERCHANT, results[3].second.scope)
    }

    private fun createTxn(
        id: String,
        merchant: String,
        amount: Double,
        category: String,
        date: String
    ): TransactionEntity {
        return TransactionEntity(
            smsId = id,
            merchant = merchant,
            amount = amount,
            category = category,
            description = "Test transaction",
            transactionDate = date,
            isCategorizedByAi = true,
            createdAt = 1000L
        )
    }
}

