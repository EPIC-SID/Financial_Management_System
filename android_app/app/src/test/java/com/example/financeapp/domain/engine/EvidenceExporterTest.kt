package com.example.financeapp.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class EvidenceExporterTest {

    @Test
    fun testExportEvidencePackGeneratesAllNineArtifacts() {
        val targetDir = File("build/evidence_test_output")

        val result = EvidenceExporter.exportEvidencePack(targetDir)

        assertEquals("Should export exactly 9 evidence artifacts", 9, result.totalFilesCount)
        assertEquals(9, result.exportedFiles.size)

        val expectedFiles = listOf(
            "README.md",
            "periods_plans.csv",
            "forecasts.csv",
            "outcomes.csv",
            "baselines.csv",
            "timeline.json",
            "conflicts.json",
            "accounts.json",
            "benchmark_results.json"
        )

        expectedFiles.forEach { filename ->
            val file = File(targetDir, filename)
            assertTrue("Artifact $filename must exist", file.exists())
            assertTrue("Artifact $filename must not be empty (length > 0)", file.length() > 0)
        }

        // Verify CSV header schemas match Section 17
        val periodsPlansCsv = File(targetDir, "periods_plans.csv").readText()
        assertTrue(periodsPlansCsv.startsWith("scenario,plan_id,period,opening,income"))

        val forecastsCsv = File(targetDir, "forecasts.csv").readText()
        assertTrue(forecastsCsv.startsWith("scenario,seed,period_index,metric,p10,p50,p90"))

        val outcomesCsv = File(targetDir, "outcomes.csv").readText()
        assertTrue(outcomesCsv.startsWith("scenario,plan_id,terminal_balance,terminal_debt"))

        val baselinesCsv = File(targetDir, "baselines.csv").readText()
        assertTrue(baselinesCsv.startsWith("model,mae,rmse,coverage_80,mean_width"))

        // Verify README contains reproduction instructions
        val readmeContent = File(targetDir, "README.md").readText()
        assertTrue(readmeContent.contains("ModelValidationBenchmarkTest"))
        assertTrue(readmeContent.contains("Component bootstrap"))

        // Cleanup test directory
        targetDir.deleteRecursively()
    }

    @Test
    fun testProductionBuildEvidencePack() {
        // Also run production export into build/evidence/ as required by Phase 4C Done definition
        val prodDir = File("build/evidence")
        val result = EvidenceExporter.exportEvidencePack(prodDir)

        assertEquals(9, result.exportedFiles.size)
        println("Successfully generated ${result.totalFilesCount} evidence submission artifacts in build/evidence/")
    }
}

