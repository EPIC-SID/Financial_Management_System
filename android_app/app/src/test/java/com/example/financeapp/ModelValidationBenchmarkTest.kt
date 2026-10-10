package com.example.financeapp

import com.example.financeapp.domain.engine.Evaluator
import com.example.financeapp.domain.engine.ModelComparisonReport
import com.example.financeapp.domain.model.Scenario
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * Addition 2 & HackMatrix Page 2 Mandate: Reproducible CLI Evidence Package.
 *
 * Standalone JUnit benchmark runner callable via terminal:
 * `./gradlew testDebugUnitTest --tests "*ModelValidationBenchmarkTest*"`
 *
 * Evaluates the Probabilistic Forecast Engine against Historical-Average and Recurring-Only baselines
 * across multiple scenarios and seeds without requiring an emulator or Android Studio.
 * Prints an ASCII comparison table to stdout and writes benchmark_results.json.
 */
class ModelValidationBenchmarkTest {

    @Test
    fun runReproducibleBenchmarkSuite() {
        val scenarios = listOf(
            Scenario.STABLE_INCOME,
            Scenario.INCOME_REDUCTION,
            Scenario.UNEXPECTED_EXPENSE,
            Scenario.HIGH_UNCERTAINTY
        )
        val seeds = listOf(42L, 101L, 2024L)

        val allReports = mutableListOf<ModelComparisonReport>()

        for (scenario in scenarios) {
            for (seed in seeds) {
                val report = Evaluator.evaluateScenario(
                    scenario = scenario,
                    seed = seed,
                    calibrationScalingFactor = 1.15
                )
                allReports.add(report)
            }
        }

        assertTrue("Reports must not be empty", allReports.isNotEmpty())

        // Compute macro averages across all evaluation runs
        val avgOursMae = allReports.map { it.calibratedBootstrap.mae }.average()
        val avgOursRmse = allReports.map { it.calibratedBootstrap.rmse }.average()
        val avgOursCoverage = allReports.map { it.calibratedBootstrap.coverage80 }.average()
        val avgOursWidth = allReports.map { it.calibratedBootstrap.meanIntervalWidth }.average()

        val avgHistMae = allReports.map { it.historicalAverage.mae }.average()
        val avgHistRmse = allReports.map { it.historicalAverage.rmse }.average()
        val avgHistCoverage = allReports.map { it.historicalAverage.coverage80 }.average()
        val avgHistWidth = allReports.map { it.historicalAverage.meanIntervalWidth }.average()

        val avgRecurMae = allReports.map { it.recurringOnly.mae }.average()
        val avgRecurRmse = allReports.map { it.recurringOnly.rmse }.average()

        // 1. Format and print clean ASCII table for judges
        val asciiTable = buildString {
            appendLine()
            appendLine("========================================================================================")
            appendLine("HACKMATRIX 5.0 | FIN-02 REPRODUCIBLE EVALUATION BENCHMARK")
            appendLine("Train Period: M-6 to M-2 | Validation: M-1 | Test Horizon: M+1 to M+6 (Multi-Seed Runs: ${allReports.size})")
            appendLine("========================================================================================")
            appendLine(String.format("%-32s %-9s %-9s %-9s %-12s %-10s", "Model", "Horizon", "MAE (₹)", "RMSE (₹)", "80% PI Cov", "Mean Width"))
            appendLine("----------------------------------------------------------------------------------------")
            appendLine(
                String.format(
                    "%-32s %-9s ₹%-8.0f ₹%-8.0f %-12s ₹%-10.0f",
                    "Probabilistic Bootstrap (Ours)",
                    "6 Months",
                    avgOursMae,
                    avgOursRmse,
                    String.format("%.1f%%", avgOursCoverage * 100),
                    avgOursWidth
                )
            )
            appendLine(
                String.format(
                    "%-32s %-9s ₹%-8.0f ₹%-8.0f %-12s ₹%-10.0f",
                    "Historical Average Baseline",
                    "6 Months",
                    avgHistMae,
                    avgHistRmse,
                    String.format("%.1f%%", avgHistCoverage * 100),
                    avgHistWidth
                )
            )
            appendLine(
                String.format(
                    "%-32s %-9s ₹%-8.0f ₹%-8.0f %-12s %-10s",
                    "Recurring-Only Fixed Baseline",
                    "6 Months",
                    avgRecurMae,
                    avgRecurRmse,
                    "N/A",
                    "N/A"
                )
            )
            appendLine("========================================================================================")
        }

        print(asciiTable)

        // 2. Generate benchmark_results.json
        val jsonPayload = buildJsonBenchmarkReport(
            avgOursMae = avgOursMae,
            avgOursRmse = avgOursRmse,
            avgOursCoverage = avgOursCoverage,
            avgOursWidth = avgOursWidth,
            avgHistMae = avgHistMae,
            avgHistRmse = avgHistRmse,
            avgHistCoverage = avgHistCoverage,
            avgHistWidth = avgHistWidth,
            avgRecurMae = avgRecurMae,
            avgRecurRmse = avgRecurRmse,
            runsCount = allReports.size,
            scenarios = scenarios.map { it.name },
            seeds = seeds
        )

        // Write to both root directory and build directory for guaranteed submission access
        val targetLocations = listOf(
            File("benchmark_results.json"),
            File("../../benchmark_results.json"),
            File("../benchmark_results.json")
        )

        var writtenCount = 0
        targetLocations.forEach { file ->
            try {
                file.parentFile?.mkdirs()
                file.writeText(jsonPayload)
                writtenCount++
            } catch (_: Exception) {}
        }

        assertTrue("benchmark_results.json must be successfully written to at least one target location", writtenCount > 0)
        assertNotNull(jsonPayload)
    }

    private fun buildJsonBenchmarkReport(
        avgOursMae: Double,
        avgOursRmse: Double,
        avgOursCoverage: Double,
        avgOursWidth: Double,
        avgHistMae: Double,
        avgHistRmse: Double,
        avgHistCoverage: Double,
        avgHistWidth: Double,
        avgRecurMae: Double,
        avgRecurRmse: Double,
        runsCount: Int,
        scenarios: List<String>,
        seeds: List<Long>,
    ): String {
        return """
        {
          "benchmark": "HACKMATRIX 5.0 FIN-02 EVALUATION",
          "generatedAt": "${Instant.now()}",
          "protocol": {
            "trainWindow": "Month -6 to Month -2",
            "validationWindow": "Month -1",
            "testHorizon": "Month +1 to Month +6",
            "totalEvaluatedRuns": $runsCount,
            "scenarios": ${scenarios.map { "\"$it\"" }},
            "seeds": $seeds
          },
          "models": {
            "probabilisticBootstrapOurs": {
              "name": "Probabilistic Bootstrap with Conformal Calibration",
              "horizon": "6 Months",
              "mae": ${String.format("%.2f", avgOursMae)},
              "rmse": ${String.format("%.2f", avgOursRmse)},
              "coverage80Percent": ${String.format("%.2f", avgOursCoverage * 100)},
              "meanIntervalWidth": ${String.format("%.2f", avgOursWidth)}
            },
            "historicalAverageBaseline": {
              "name": "Trailing Historical Monthly Average (Normal Approximation ±1.28σ)",
              "horizon": "6 Months",
              "mae": ${String.format("%.2f", avgHistMae)},
              "rmse": ${String.format("%.2f", avgHistRmse)},
              "coverage80Percent": ${String.format("%.2f", avgHistCoverage * 100)},
              "meanIntervalWidth": ${String.format("%.2f", avgHistWidth)}
            },
            "recurringOnlyFixedBaseline": {
              "name": "Scheduled Recurring Obligations (Zero Variance Point Forecast)",
              "horizon": "6 Months",
              "mae": ${String.format("%.2f", avgRecurMae)},
              "rmse": ${String.format("%.2f", avgRecurRmse)},
              "coverage80Percent": null,
              "meanIntervalWidth": null
            }
          }
        }
        """.trimIndent()
    }
}

