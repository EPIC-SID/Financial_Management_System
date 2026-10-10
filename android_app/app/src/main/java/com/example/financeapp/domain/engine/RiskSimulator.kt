package com.example.financeapp.domain.engine

import com.example.financeapp.domain.model.FinancialSnapshot
import com.example.financeapp.domain.model.PlanningPeriod
import com.example.financeapp.domain.model.Scenario
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Parameters governing a multi-period state-dependent financial policy.
 *
 * @param cutFraction Discretionary spend cut fraction c (e.g. 0.30 for 30% reduction). Hard cap at 0.60.
 * @param prepayShare Share of available surplus p routed to early debt prepayment (e.g. 0.80 for 80%).
 * @param bufferMultiple Multiplier m of min reserve R required before surplus is unlocked (e.g. 1.0x vs 2.5x).
 */
data class PolicyParameters(
    val name: String,
    val cutFraction: Double,
    val prepayShare: Double,
    val bufferMultiple: Double,
)

/**
 * Result of Monte Carlo risk simulation for a specific candidate policy.
 */
data class PolicyRiskSimulationResult(
    val policyName: String,
    val shortfallRisk: Double,
    val periodShortfallProbabilities: List<Double>,
    val meanTerminalBalance: Double,
    val pathsEvaluated: Int,
)

/**
 * Addition 1 / Phase 2B: Monte Carlo Shortfall Risk Simulator for Candidate Policies.
 *
 * Simulates a state-dependent financial policy over 1,000–2,000 empirical bootstrap paths to calculate:
 * - Total horizon shortfall risk: count(paths where min_t Balance_t < R) / totalPaths.
 * - Period-by-period breach probabilities.
 *
 * Pure Kotlin JVM — zero Android dependencies.
 */
object RiskSimulator {

    private const val DEFAULT_NUM_PATHS = 1500
    private const val DEFAULT_SEED = 42L

    /**
     * Evaluates shortfall risk for a policy across Monte Carlo simulation paths.
     */
    fun evaluatePolicyRisk(
        snapshot: FinancialSnapshot,
        policy: PolicyParameters,
        numPaths: Int = DEFAULT_NUM_PATHS,
        seed: Long = DEFAULT_SEED,
    ): PolicyRiskSimulationResult {
        val rng = Random(seed)
        val periods = snapshot.planningPeriods
        val numPeriods = periods.size
        val minReserve = snapshot.minReserve
        val totalOutstandingDebt = snapshot.debts.sumOf { it.outstandingPrincipal }

        var breachingPathsCount = 0
        val periodBreachCounts = IntArray(numPeriods)
        val terminalBalances = DoubleArray(numPaths)

        // Symmetrized discretionary variance pool
        val discretionaryPool = buildDiscretionaryVariancePool(snapshot)

        for (k in 0 until numPaths) {
            var currentBalance = snapshot.currentBalance
            var remainingDebt = totalOutstandingDebt
            var pathBreached = false

            for (t in 0 until numPeriods) {
                val period = periods[t]

                // 1. Simulate period income with scenario shocks/jitter
                val simIncome = simulateIncomeForPath(snapshot.activeScenario, period.projectedIncome, rng)

                // 2. Scheduled fixed commitments
                val essential = period.essentialExpenses
                val debtService = period.debtObligations

                // 3. Bootstrapped discretionary spending adjusted by policy cut c
                val sampledResidual = discretionaryPool[rng.nextInt(discretionaryPool.size)]
                val baselineDiscretionary = max(0.0, period.projectedDiscretionary + sampledResidual)
                val policyDiscretionary = baselineDiscretionary * (1.0 - policy.cutFraction.coerceIn(0.0, 0.60))

                // 4. State-dependent balance & surplus calculation
                val available = currentBalance + simIncome - essential - debtService - policyDiscretionary
                val requiredBuffer = policy.bufferMultiple * minReserve
                val surplus = max(0.0, available - requiredBuffer)

                // 5. Debt prepayment capped by remaining debt
                val prepay = min(policy.prepayShare * surplus, remainingDebt)
                currentBalance = available - prepay
                remainingDebt = max(0.0, remainingDebt - prepay)

                // Check breach: Balance_t < minReserve
                if (currentBalance < minReserve) {
                    pathBreached = true
                    periodBreachCounts[t]++
                }
            }

            if (pathBreached) {
                breachingPathsCount++
            }
            terminalBalances[k] = currentBalance
        }

        val totalShortfallRisk = breachingPathsCount.toDouble() / numPaths
        val periodProbs = periodBreachCounts.map { it.toDouble() / numPaths }
        val meanTerminal = terminalBalances.average()

        return PolicyRiskSimulationResult(
            policyName = policy.name,
            shortfallRisk = totalShortfallRisk,
            periodShortfallProbabilities = periodProbs,
            meanTerminalBalance = meanTerminal,
            pathsEvaluated = numPaths
        )
    }

    private fun simulateIncomeForPath(scenario: Scenario, baseIncome: Double, rng: Random): Double {
        return when (scenario) {
            Scenario.HIGH_UNCERTAINTY -> {
                val noise = (rng.nextDouble() * 0.80) - 0.40 // ±40%
                max(0.0, baseIncome * (1.0 + noise))
            }
            Scenario.STABLE_INCOME -> {
                val jitter = (rng.nextDouble() * 0.04) - 0.02 // ±2%
                baseIncome * (1.0 + jitter)
            }
            else -> {
                val variation = (rng.nextDouble() * 0.10) - 0.05 // ±5%
                baseIncome * (1.0 + variation)
            }
        }
    }

    private fun buildDiscretionaryVariancePool(snapshot: FinancialSnapshot): List<Double> {
        val stdDev = if (snapshot.activeScenario == Scenario.HIGH_UNCERTAINTY) 4500.0 else 1800.0
        return listOf(
            0.0,
            -stdDev,
            stdDev,
            -stdDev * 1.5,
            stdDev * 1.5,
            -stdDev * 0.5,
            stdDev * 0.5
        )
    }
}

