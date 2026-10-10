package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.FinancialSnapshot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class HealthScoreBreakdown(
    val total: Int,
    val runway: Double,
    val debtLoad: Double,
    val buffer: Double,
    val stability: Double,
    val dti: Double,
    val monthlyObligations: Double,
)

/** Four components, each 0–25 (06-backend.md section 7.3). SIP is not scored. */
object HealthScore {
    fun of(snapshot: FinancialSnapshot): HealthScoreBreakdown {
        val last = snapshot.history.lastOrNull()
        val obligations = (last?.essentials ?: 0.0) + (last?.debtService ?: 0.0)
        val income = snapshot.declared.typicalIncome
            ?: snapshot.history.map { it.income }.filter { it > 0 }.average().takeIf { !it.isNaN() }
            ?: 0.0
        val emis = snapshot.liabilities.filter { it.kind != "CARD" }.sumOf { it.emi ?: 0.0 }
        val cardMins = snapshot.liabilities.filter { it.kind == "CARD" }.sumOf { it.minDue ?: 0.0 }
        val dti = if (income <= 0) 1.0 else (emis + cardMins) / income
        val nets = snapshot.history.map { it.income - it.essentials - it.debtService - it.discretionary - it.investments }
        val meanNet = if (nets.isEmpty()) 0.0 else nets.average()
        val sigma = if (nets.size < 2) 0.0 else {
            val v = nets.map { (it - meanNet) * (it - meanNet) }.average()
            sqrt(v)
        }
        val runway = 25.0 * clamp((if (obligations <= 0) 1.0 else snapshot.cash / obligations) / 6.0)
        val debtLoad = 25.0 * (1.0 - clamp(dti / 0.5))
        val buffer = 25.0 * clamp(if (snapshot.declared.minReserve <= 0) 1.0 else snapshot.cash / snapshot.declared.minReserve)
        val stability = 25.0 * (1.0 - clamp((if (income <= 0) 1.0 else sigma / income) / 0.3))
        return HealthScoreBreakdown(
            total = (runway + debtLoad + buffer + stability).toInt().coerceIn(0, 100),
            runway = runway, debtLoad = debtLoad, buffer = buffer, stability = stability,
            dti = dti, monthlyObligations = obligations,
        )
    }

    private fun clamp(x: Double) = min(1.0, max(0.0, x))
}
