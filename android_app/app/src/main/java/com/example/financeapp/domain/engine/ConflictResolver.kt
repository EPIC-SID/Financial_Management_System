package com.example.financeapp.domain.engine

data class SavingsGoal(val targetAmount: Double, val byPeriod: Int) {
    init {
        require(targetAmount.isFinite() && targetAmount >= 0)
        require(byPeriod in 1..24)
    }
}

enum class RelaxationKind { LOWER_TARGET, EXTEND_TIMELINE, REDUCE_RESERVE, REDUCE_SIP }

data class Relaxation(
    val kind: RelaxationKind,
    val value: Double,
    val delta: Double,
    val riskAfter: Double,
    val feasible: Boolean,
    val description: String,
    val notRecommended: Boolean = false
)

data class GoalConflict(
    val shortfall: Double,
    val maximumAchievable: Double,
    val firstViolatedPeriod: Int,
    val relaxations: List<Relaxation>
)

class ConflictResolver {
    fun resolve(inputs: SimulationInputs, goal: SavingsGoal): GoalConflict? {
        require(goal.byPeriod <= inputs.periods.size)
        fun feasible(target: Double, horizon: Int, reserve: Double = inputs.minReserve, sipCut: Double = 0.0) =
            firstViolation(inputs.cash, inputs.periods, target, horizon, reserve, sipCut) == null
        if (feasible(goal.targetAmount, goal.byPeriod)) return null

        // Goals are funded in equal period transfers, outside spendable cash and the SIP.
        val canSave = feasible(0.0, goal.byPeriod)
        val maximum = if (canSave) lastFeasible(0.0, goal.targetAmount) { feasible(it, goal.byPeriod) } else 0.0
        val options = ArrayList<Relaxation>()
        fun add(kind: RelaxationKind, value: Double, delta: Double, target: Double = goal.targetAmount,
                horizon: Int = goal.byPeriod, reserve: Double = inputs.minReserve, sipCut: Double = 0.0,
                description: String, notRecommended: Boolean = false) {
            val risk = inputs.sampledPaths.count {
                firstViolation(inputs.cash, it, target, horizon, reserve, sipCut) != null
            }.toDouble() / inputs.sampledPaths.size
            options.add(Relaxation(kind, value, delta, risk, feasible(target, horizon, reserve, sipCut), description, notRecommended))
        }
        add(RelaxationKind.LOWER_TARGET, maximum, goal.targetAmount - maximum, target = maximum,
            description = if (canSave) "Lower the goal amount" else "Even a zero target cannot preserve the reserve")

        // Future obligations can make a later horizon worse; search a monotone prefix predicate.
        val maxHorizon = inputs.periods.size
        fun extensionBy(end: Int) = (goal.byPeriod + 1..end).any { feasible(goal.targetAmount, it) }
        val extensionWorks = extensionBy(maxHorizon)
        var low = goal.byPeriod
        var high = maxHorizon
        if (extensionWorks) {
            while (high - low > 1) {
                val mid = (low + high) / 2
                if (extensionBy(mid)) high = mid else low = mid
            }
        }
        add(RelaxationKind.EXTEND_TIMELINE, high.toDouble(), (high - goal.byPeriod).toDouble(), horizon = high,
            description = if (extensionWorks) "Extend the goal timeline" else "No feasible extension within the supplied forecast (at most 24 periods)")

        val reserveWorks = feasible(goal.targetAmount, goal.byPeriod, 0.0)
        val reduction = if (reserveWorks) firstFeasible(0.0, inputs.minReserve) {
            feasible(goal.targetAmount, goal.byPeriod, inputs.minReserve - it)
        } else inputs.minReserve
        val reserve = inputs.minReserve - reduction
        val unsafe = reserve < inputs.periods.take(goal.byPeriod).map { it.essentials }.average()
        add(RelaxationKind.REDUCE_RESERVE, reserve, reduction, reserve = reserve,
            description = if (!reserveWorks) "Even a zero reserve cannot fund the goal" else if (unsafe) "Not recommended: reserve below one month of essentials" else "Reduce the minimum cash reserve",
            notRecommended = unsafe)

        val sip = inputs.periods.take(goal.byPeriod).maxOf { it.sip }
        if (sip > 0.0) {
            val sipWorks = feasible(goal.targetAmount, goal.byPeriod, sipCut = sip)
            val cut = if (sipWorks) firstFeasible(0.0, sip) {
                feasible(goal.targetAmount, goal.byPeriod, sipCut = it)
            } else sip
            add(RelaxationKind.REDUCE_SIP, (sip - cut).coerceAtLeast(0.0), cut, sipCut = cut,
                description = if (sipWorks) "Reduce or pause SIP; trades off long-term saving" else "Even pausing SIP cannot fund the goal",
                notRecommended = true)
        }
        return GoalConflict(goal.targetAmount - maximum, maximum,
            firstViolation(inputs.cash, inputs.periods, goal.targetAmount, goal.byPeriod, inputs.minReserve, 0.0)!!, options)
    }

    private fun firstViolation(cash: Double, path: List<CashFlowPeriod>, target: Double, horizon: Int,
                               reserve: Double, sipCut: Double): Int? {
        if (cash < reserve) return 0
        var balance = cash
        for (i in 0 until horizon) {
            val p = path[i]
            balance += p.income - p.essentials - p.debtService - p.discretionary * 0.4 -
                (p.sip - sipCut).coerceAtLeast(0.0) - target / horizon
            if (balance < reserve - 1e-7) return i + 1
        }
        return null
    }

    private fun lastFeasible(low: Double, high: Double, feasible: (Double) -> Boolean): Double {
        var left = low
        var right = high
        repeat(64) {
            if (right - left <= 0.01) return left
            val mid = left + (right - left) / 2
            if (feasible(mid)) left = mid else right = mid
        }
        return left
    }

    private fun firstFeasible(low: Double, high: Double, feasible: (Double) -> Boolean): Double {
        var left = low
        var right = high
        repeat(64) {
            if (right - left <= 100.0) return right
            val mid = left + (right - left) / 2
            if (feasible(mid)) right = mid else left = mid
        }
        return right
    }
}
