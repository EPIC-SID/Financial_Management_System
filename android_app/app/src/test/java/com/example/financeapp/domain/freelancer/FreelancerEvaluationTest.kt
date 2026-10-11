package com.example.financeapp.domain.freelancer

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class FreelancerEvaluationTest {
    private val evaluator = FreelancerEvaluator()
    private val openingCash = 100_000_000L
    private fun history() = List(30) { i ->
        FinancialHistoryMonth(YearMonth.of(2024, 1).plusMonths(i.toLong()), incomePaise = 6_000_000,
            cashEssentialsPaise = 2_000_000, cashDebtServicePaise = 500_000,
            cashSipPaise = 300_000, cashDiscretionaryPaise = 1_000_000)
    }
    private fun evaluate(months: List<FinancialHistoryMonth> = history()) =
        evaluator.evaluate(months, openingCash, recurringIncomePaise = 6_000_000, seed = 42)

    @Test fun stableHistoryHasCorrectFlowAndBalanceAndBaselineComparison() {
        val report = evaluate()
        val point = report.predictions.first { it.model == ForecastModel.BOOTSTRAP &&
            it.target == ForecastTarget.NET_FLOW && it.step == 1 }
        assertEquals(22_000.0, point.band.p50, 0.000001)
        assertEquals(22_000.0, point.actual, 0.0)
        assertTrue(report.metrics.filter { it.model == ForecastModel.BOOTSTRAP }.all {
            it.metrics.mae == 0.0 && it.metrics.rmse == 0.0 && it.metrics.coverage == 1.0
        })
        val recurring = report.predictions.first { it.model == ForecastModel.RECURRING_ONLY &&
            it.target == ForecastTarget.NET_FLOW }
        assertEquals(32_000.0, recurring.band.p50, 0.0)
        assertEquals(0.0, recurring.band.p90 - recurring.band.p10, 0.0)
        val balance = report.predictions.first { it.model == ForecastModel.BOOTSTRAP &&
            it.target == ForecastTarget.ENDING_BALANCE && it.step == 1 }
        assertEquals(1_000_000.0 + 25 * 22_000.0, balance.actual, 0.000001)
    }

    @Test fun splitCountsAndHorizonDenominatorsAreExplicit() {
        val report = evaluate()
        assertEquals(126, report.predictions.size)
        assertEquals(42, report.metrics.size)
        assertEquals(listOf(21, 21), report.calibration.map { it.sampleSize })
        assertEquals(listOf(6, 5, 4, 3, 2, 1), report.metrics.filter {
            it.model == ForecastModel.BOOTSTRAP && it.target == ForecastTarget.NET_FLOW && it.step > 0
        }.map { it.metrics.sampleCount })
        assertTrue(report.predictions.all { it.historyMonths in 24..29 && it.step in 1..6 })
        assertTrue(report.predictions.all {
            history()[it.historyMonths - 1].month < it.originMonth
        })
    }

    @Test fun testOutcomesCannotChangeCalibrationOrFirstOriginForecasts() {
        val before = evaluate()
        val changed = history().mapIndexed { i, month ->
            if (i >= 24) month.copy(incomePaise = 0, cashEssentialsPaise = 4_000_000) else month
        }
        val after = evaluate(changed)
        assertEquals(before.calibration, after.calibration)
        val origin = history()[24].month
        assertEquals(before.predictions.filter { it.originMonth == origin }.map { it.band },
            after.predictions.filter { it.originMonth == origin }.map { it.band })
        assertNotEquals(before.predictions.map { it.actual }, after.predictions.map { it.actual })
        assertTrue(after.metrics.filter { it.model == ForecastModel.BOOTSTRAP }.any { it.metrics.mae > 0.0 })
    }

    @Test fun changingLaterTestMonthCannotChangeEarlierOriginPredictions() {
        val original = evaluate()
        val changed = evaluate(history().mapIndexed { i, month ->
            if (i == 29) month.copy(incomePaise = 1) else month
        })
        assertEquals(original.predictions.map { it.band }, changed.predictions.map { it.band })
        assertNotEquals(original.predictions.map { it.actual }, changed.predictions.map { it.actual })
    }

    @Test fun calibrationHandlesZeroWidthWithoutShrinkingAnyRawInterval() {
        val months = history().mapIndexed { i, month ->
            if (i in 18..23) month.copy(incomePaise = 12_000_000) else month
        }
        val report = evaluate(months)
        assertTrue(report.calibration.all { it.factor.isFinite() && it.factor >= 1.0 })
        assertTrue(report.calibration.any { it.factor > 1.0 })
        report.predictions.filter { it.model == ForecastModel.BOOTSTRAP }.forEach {
            assertTrue(it.band.p10 <= it.rawBand.p10)
            assertTrue(it.band.p90 >= it.rawBand.p90)
            assertEquals(it.rawBand.p50, it.band.p50, 0.0)
        }
    }

    @Test fun cumulativeIntervalsComeFromWholePathsNotSumOfMarginalQuantiles() {
        val months = history().mapIndexed { i, month ->
            month.copy(incomePaise = if (i % 2 == 0) 0 else 10_000_000)
        }
        val report = evaluate(months)
        val origin = months[24].month
        val flow = report.predictions.first { it.originMonth == origin && it.step == 1 &&
            it.model == ForecastModel.BOOTSTRAP && it.target == ForecastTarget.NET_FLOW }
        val balance = report.predictions.first { it.originMonth == origin && it.step == 2 &&
            it.model == ForecastModel.BOOTSTRAP && it.target == ForecastTarget.ENDING_BALANCE }
        assertTrue(flow.rawBand.p90 - flow.rawBand.p10 > 0.0)
        assertEquals(0.0, balance.rawBand.p90 - balance.rawBand.p10, 0.000001)
    }

    @Test fun metricsAgreeWithIndependentCalculationAndRunIsReproducible() {
        val report = evaluate(history().mapIndexed { i, month -> month.copy(incomePaise = (i % 4) * 3_000_000L) })
        val metric = report.metrics.first { it.model == ForecastModel.BOOTSTRAP &&
            it.target == ForecastTarget.NET_FLOW && it.step == 0 }.metrics
        val rows = report.predictions.filter { it.model == ForecastModel.BOOTSTRAP && it.target == ForecastTarget.NET_FLOW }
        val errors = rows.map { it.actual - it.band.p50 }
        assertEquals(errors.map { abs(it) }.average(), metric.mae, 0.000001)
        assertEquals(sqrt(errors.map { it * it }.average()), metric.rmse, 0.000001)
        assertEquals(rows.count { it.actual in it.band.p10..it.band.p90 }.toDouble() / rows.size,
            metric.coverage, 0.0)
        assertEquals(rows.map { it.band.p90 - it.band.p10 }.average(), metric.meanWidth, 0.000001)
        assertTrue(listOf(metric.pinballP10, metric.pinballP50, metric.pinballP90).all { it >= 0.0 })
        assertEquals(evaluate(), evaluate())
    }

    @Test fun rejectsWrongLengthGapsNegativeMoneyAndMissingCashWithoutInventingData() {
        for (data in listOf(history().dropLast(1), history().reversed(), history() + history().last(),
            history().mapIndexed { i, month -> if (i == 20) month.copy(cashRefundsPaise = -1) else month })) {
            assertThrows(IllegalArgumentException::class.java) { evaluate(data) }
        }
        assertThrows(IllegalArgumentException::class.java) { evaluator.evaluate(history(), -1, 0, 42) }
        assertThrows(IllegalArgumentException::class.java) { evaluator.evaluate(history(), openingCash, Long.MAX_VALUE, 42) }
    }

    private fun synthetic(kind: String, seed: Int): List<FinancialHistoryMonth> =
        history().mapIndexed { i, month ->
            // Independent streams per split; no future shock flags are passed to the forecaster.
            val splitSeed = seed + when { i < 18 -> 100; i < 24 -> 200; else -> 300 }
            val rng = Random(splitSeed + i * 17)
            val income = when (kind) {
                "stable" -> 6_000_000L
                "irregular" -> if (rng.nextInt(4) == 0) 0L else rng.nextLong(3_000_000, 10_000_001)
                "income_drop" -> if (i >= 24) 3_000_000L else 6_000_000L
                else -> 6_000_000L
            }
            month.copy(incomePaise = income,
                cashDiscretionaryPaise = rng.nextLong(500_000, 1_500_001),
                cashEssentialsPaise = if (kind == "expense_shock" && i == 26) 3_500_000 else 2_000_000)
        }

    @Test fun writesReproducibleSyntheticEvidenceWithoutClaimingRealWorldAccuracy() {
        fun observationJson(row: ForecastObservation) = buildJsonObject {
            put("model", row.model.name); put("target", row.target.name)
            put("origin", row.originMonth.toString()); put("historyMonths", row.historyMonths)
            put("step", row.step); put("actual", row.actual)
            put("p10", row.band.p10); put("p50", row.band.p50); put("p90", row.band.p90)
            put("rawP10", row.rawBand.p10); put("rawP90", row.rawBand.p90)
        }
        val json = buildJsonObject {
            put("data", "Synthetic monthly aggregates only; not real-world deployment accuracy")
            put("protocol", "Rolling origins; train 1-18, validation 19-24, test 25-30; prior-only refits")
            put("units", "INR; coverage is a fraction; step 0 means all held-out origin-step pairs")
            put("limitations", "Overlapping horizons are dependent; calibration is interval-only, not path-risk calibration; no universal coverage guarantee")
            putJsonArray("runs") {
                for (kind in listOf("stable", "irregular", "income_drop", "expense_shock")) {
                    for (seed in listOf(42, 73)) {
                        val months = synthetic(kind, seed)
                        val report = evaluator.evaluate(months, openingCash,
                            if (kind == "stable") 6_000_000 else 0, seed)
                        add(buildJsonObject {
                            put("scenario", kind); put("seed", seed); put("initialCashPaise", openingCash)
                            put("recurringIncomePaise", if (kind == "stable") 6_000_000 else 0)
                            putJsonArray("history") { months.forEach { add(buildJsonObject {
                                put("month", it.month.toString()); put("incomePaise", it.incomePaise)
                                put("essentialsPaise", it.cashEssentialsPaise); put("debtPaise", it.cashDebtServicePaise)
                                put("sipPaise", it.cashSipPaise); put("discretionaryPaise", it.cashDiscretionaryPaise)
                            }) } }
                            putJsonArray("calibration") { report.calibration.forEach { add(buildJsonObject {
                                put("target", it.target.name); put("factor", it.factor); put("n", it.sampleSize)
                                put("halfWidthFloorINR", it.halfWidthFloor)
                            }) } }
                            putJsonArray("metrics") { report.metrics.forEach { add(buildJsonObject {
                                put("model", it.model.name); put("target", it.target.name); put("step", it.step)
                                put("n", it.metrics.sampleCount); put("mae", it.metrics.mae); put("rmse", it.metrics.rmse)
                                put("coverage", it.metrics.coverage); put("width", it.metrics.meanWidth)
                                put("pinballP10", it.metrics.pinballP10); put("pinballP50", it.metrics.pinballP50)
                                put("pinballP90", it.metrics.pinballP90)
                            }) } }
                            putJsonArray("validationPredictions") { report.validationPredictions.forEach { add(observationJson(it)) } }
                            putJsonArray("predictions") { report.predictions.forEach { add(observationJson(it)) } }
                        })
                    }
                }
            }
        }
        val file = File("build/reports/freelancer/phase3a-evaluation.json")
        requireNotNull(file.parentFile).mkdirs()
        file.writeText(Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json))
        assertEquals(8, Json.parseToJsonElement(file.readText()).jsonObject.getValue("runs").jsonArray.size)
    }
}
