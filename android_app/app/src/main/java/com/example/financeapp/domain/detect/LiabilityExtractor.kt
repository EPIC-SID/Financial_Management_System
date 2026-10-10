package com.example.financeapp.domain.detect

import com.example.financeapp.domain.model.Liability
import com.example.financeapp.domain.model.RecurringItem
import com.example.financeapp.domain.model.Txn

data class LiabilityExtract(
    val liabilities: List<Liability>,
    val missing: List<String>,
)

object LiabilityExtractor {
    /** EMI rows from the recurring detector. Rate and outstanding stay unknown until the user declares them. */
    fun from(recurring: List<RecurringItem>, txns: List<Txn>): LiabilityExtract {
        val emis = recurring.filter { it.kind == "EMI" }.map { item ->
            Liability(
                id = item.id,
                kind = "EMI",
                outstanding = null,
                annualRate = null,
                emi = item.amount,
                monthsLeft = null,
                dueDay = item.nextDue.dayOfMonth,
                creditLimit = null,
                minDue = null,
                totalDue = null,
                unbilled = null,
                accountId = txns.firstOrNull { it.counterpartyHash in item.id }?.accountId,
            )
        }
        val bills = recurring.filter { it.kind == "CARD_BILL" }.map { item ->
            Liability(
                id = item.id, kind = "CARD", outstanding = null, annualRate = null, emi = null,
                monthsLeft = null, dueDay = item.nextDue.dayOfMonth, creditLimit = null,
                minDue = null, totalDue = item.amount, unbilled = null, accountId = null,
            )
        }
        val missing = buildList {
            if (emis.isNotEmpty()) {
                add("loan interest rate")
                add("loan balance")
            }
            if (bills.isNotEmpty()) add("card limit")
        }
        return LiabilityExtract(emis + bills, missing)
    }
}
