package com.example.financeapp.domain.model

import java.time.LocalDate
import java.time.YearMonth

enum class Direction { DEBIT, CREDIT }

enum class Source { SMS, CSV, MANUAL, SYNTHETIC }

data class Txn(
    /** smsId, csv row hash, or uuid. */
    val id: String,
    val date: LocalDate,
    /** Always positive; sign comes from [direction]. */
    val amount: Double,
    val direction: Direction,
    val category: String,
    /** Salted hash; never the raw VPA or name. */
    val counterpartyHash: String,
    val accountId: String?,
    /** Own-account transfer or card-bill payment. */
    val isTransfer: Boolean,
    val recurringId: String?,
    val source: Source,
)

data class RecurringItem(
    val id: String,
    val label: String,
    val amount: Double,
    val intervalDays: Int,
    val nextDue: LocalDate,
    val confidence: Double,
    /** RENT, EMI, CARD_BILL, SUBSCRIPTION, UTILITY, INSURANCE, OTHER */
    val kind: String,
)

data class MonthlyAggregate(
    val month: YearMonth,
    val income: Double,
    val essentials: Double,
    val debtService: Double,
    val discretionary: Double,
    val investments: Double,
)

/** The single snapshot contract. The health score is computed from it, never stored; the reserve is [DeclaredInputs.minReserve]. */
data class FinancialSnapshot(
    val asOf: LocalDate,
    val cash: Double,
    val accounts: List<Account>,
    val liabilities: List<Liability>,
    val investments: List<Investment>,
    val recurring: List<RecurringItem>,
    /** Income, essentials, variable, per month. */
    val history: List<MonthlyAggregate>,
    val declared: DeclaredInputs,
    /** 0..1. */
    val completeness: Double,
    /** e.g. ["loan balance"] */
    val missing: List<String>,
)
