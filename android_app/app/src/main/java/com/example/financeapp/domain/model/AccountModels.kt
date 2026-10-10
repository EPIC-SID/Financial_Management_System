package com.example.financeapp.domain.model

import java.time.LocalDate

enum class AccountType { SAVINGS, CARD, LOAN, INVESTMENT, WALLET }

data class Account(
    val id: String,
    val type: AccountType,
    /** Shown on device only; never sent to the proxy or Gemini. */
    val institution: String,
    /** Shown as "•• 4091" on device only. */
    val last4: String?,
    val latestBalance: Double?,
    val balanceAsOf: LocalDate?,
)

data class Liability(
    val id: String,
    /** HOME_LOAN, VEHICLE_LOAN, CARD, GADGET_EMI ... */
    val kind: String,
    val outstanding: Double?,
    val annualRate: Double?,
    val emi: Double?,
    val monthsLeft: Int?,
    val dueDay: Int?,
    val creditLimit: Double?,
    val minDue: Double?,
    val totalDue: Double?,
    /** Card: spend since the last statement. */
    val unbilled: Double?,
    val accountId: String?,
)

data class Investment(
    val id: String,
    /** On device only. */
    val platform: String,
    val monthlyAmount: Double,
    val debitDay: Int,
    /** MUTUAL_FUND, STOCKS, PPF */
    val assetType: String,
    val accountId: String?,
    val confidence: Double,
)

/** The five streams in one object. Built by ConsolidatedViewBuilder from a FinancialSnapshot; derived, never stored. */
data class ConsolidatedView(
    /** Stream 1: sum of latest SAVINGS balances. */
    val liquidCash: Double,
    /** Stream 2: sum of total due, or unbilled when no statement yet. Never subtracted from cash. */
    val cardDues: Double,
    /** Stream 3: sum of loan EMIs. */
    val monthlyEmis: Double,
    /** Stream 4: sum of detected or declared SIPs. */
    val monthlySip: Double,
    /** Stream 5: trailing 6-month mean of discretionary spend. */
    val avgDiscretionary: Double,
    /** Balances older than 7 days. */
    val staleAccountIds: List<String>,
)
