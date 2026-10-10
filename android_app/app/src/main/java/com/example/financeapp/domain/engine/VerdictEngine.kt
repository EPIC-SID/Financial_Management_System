package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.FinancialSnapshot
import java.time.LocalDate
import java.util.Random

data class Verdict(
    val fineUntil: LocalDate?,
    val tightOn: LocalDate?,
    val shortfallChance: Double,
    val minP10: Double,
    val minP10Date: LocalDate,
    val summary: String,
)

/** 30-day daily P10 simulation (06-backend.md section 7.2). ForecastEngine is not used. */
object VerdictEngine {
    private const val DAYS = 30
    private const val PATHS = 200

    fun run(snapshot: FinancialSnapshot, seed: Long = 42L): Verdict {
        val reserve = snapshot.declared.minReserve
        val dailyVariable = (snapshot.history.lastOrNull()?.discretionary ?: 0.0) / 30.0
        val income = snapshot.declared.typicalIncome ?: 0.0
        val pay = snapshot.recurring.firstOrNull { it.kind == "INCOME" }
        val bills = snapshot.recurring.filter { it.kind != "INCOME" }
        val dates = (0 until DAYS).map { snapshot.asOf.plusDays(it.toLong()) }
        val rng = Random(seed)
        val paths = Array(PATHS) { DoubleArray(DAYS) }
        repeat(PATHS) { p ->
            var cash = snapshot.cash
            val payJitter = rng.nextInt(5) - 2
            dates.forEachIndexed { i, day ->
                bills.filter { dueOn(it.nextDue, day) }.forEach { cash -= it.amount }
                if (pay != null && dueOn(pay.nextDue.plusDays(payJitter.toLong()), day)) cash += income
                cash -= dailyVariable * (0.6 + rng.nextDouble())
                paths[p][i] = cash
            }
        }
        val p10 = DoubleArray(DAYS) { d -> quantile(paths.map { it[d] }, 0.10) }
        val minIdx = p10.indices.minBy { p10[it] }
        val tightIdx = p10.indices.firstOrNull { p10[it] < reserve }
        val chance = if (tightIdx == null) 0.0 else paths.count { it[tightIdx] < reserve }.toDouble() / PATHS
        val tightOn = tightIdx?.let { dates[it] }
        val fineUntil = tightIdx?.let { if (it == 0) null else dates[it - 1] }
        val summary = if (tightOn == null) {
            "You're fine for the next 30 days. Lowest P10 is on ${dates[minIdx]}."
        } else {
            "Tight on $tightOn" + (fineUntil?.let { ". Fine until $it" } ?: "") + "."
        }
        return Verdict(fineUntil, tightOn, chance, p10[minIdx], dates[minIdx], summary)
    }

    private fun dueOn(nextDue: java.time.LocalDate, day: LocalDate): Boolean {
        val cap = day.lengthOfMonth()
        return day.dayOfMonth == nextDue.dayOfMonth.coerceAtMost(cap)
    }

    private fun quantile(values: List<Double>, q: Double): Double {
        val s = values.sorted()
        val i = ((s.size - 1) * q).toInt().coerceIn(0, s.lastIndex)
        return s[i]
    }
}
