package com.example.financeapp.domain.freelancer

import com.example.financeapp.domain.engine.CashFlowPeriod
import com.example.financeapp.domain.engine.SimulationInputs
import com.example.financeapp.domain.engine.SimulationLoan

/** Future cash obligations in paise, not historical consumption; debt includes EMIs/card bills. */
data class MonthlyCommitments(
    val essentialsPaise: Long,
    val debtServicePaise: Long,
    val sipPaise: Long
) {
    init {
        require(listOf(essentialsPaise, debtServicePaise, sipPaise).all { it in 0..MAX_FORECAST_PAISE })
    }
}

class ForecastPathAdapter {
    /** No re-sampling or cash debit: cash is already a snapshot. Engine amounts are INR. */
    fun adapt(
        forecast: FreelancerProjection,
        commitments: List<MonthlyCommitments>,
        minReservePaise: Long,
        loans: List<SimulationLoan> = emptyList()
    ): SimulationInputs {
        require(commitments.size == 6)
        require(minReservePaise in 0..MAX_FORECAST_PAISE)
        fun convert(path: List<ForecastComponents>) = path.mapIndexed { index, component ->
            val fixed = commitments[index]
            CashFlowPeriod(component.incomePaise / 100.0, fixed.essentialsPaise / 100.0,
                fixed.debtServicePaise / 100.0, component.discretionaryPaise / 100.0,
                fixed.sipPaise / 100.0)
        }
        return SimulationInputs(forecast.cashAnchor.amountPaise / 100.0, minReservePaise / 100.0,
            convert(forecast.p50), forecast.sampledPaths.map { convert(it) }, loans.toList())
    }
}
