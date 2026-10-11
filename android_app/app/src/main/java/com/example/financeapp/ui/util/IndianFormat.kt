package com.example.financeapp.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MINUS = "\u2212"
private const val RUPEE = "\u20B9"
private val compactUnits = listOf(
    BigDecimal.ONE to "",
    BigDecimal("1000") to "K",
    BigDecimal("100000") to "L",
    BigDecimal("10000000") to "Cr"
)

private fun decimal(value: Double): BigDecimal {
    require(value.isFinite()) { "Value must be finite" }
    return BigDecimal.valueOf(value)
}

/** Whole rupees, Indian grouping, and symmetric half-up rounding. */
fun formatInr(amount: Double): String {
    val rounded = decimal(amount).setScale(0, RoundingMode.HALF_UP)
    val whole = rounded.abs().toPlainString()
    val grouped = if (whole.length <= 3) whole else {
        val leading = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$leading,${whole.takeLast(3)}"
    }
    return (if (rounded.signum() < 0) MINUS else "") + RUPEE + grouped
}

fun formatInrCompact(amount: Double): String {
    val magnitude = decimal(amount).abs()
    var unit = compactUnits.indexOfLast { magnitude >= it.first }.coerceAtLeast(0)
    fun rounded() = magnitude.divide(compactUnits[unit].first, if (unit == 0) 0 else 1, RoundingMode.HALF_UP)
    var value = rounded()
    if (unit < compactUnits.lastIndex && value * compactUnits[unit].first >= compactUnits[unit + 1].first) {
        unit++
        value = rounded()
    }
    if (unit == 0) return formatInr(amount)
    val sign = if (amount < 0) MINUS else ""
    return "$sign$RUPEE${value.stripTrailingZeros().toPlainString()}${compactUnits[unit].second}"
}

/** Input is a ratio: 0.38 becomes 38%. */
fun formatPercent(ratio: Double): String =
    decimal(ratio).movePointRight(2).setScale(0, RoundingMode.HALF_UP)
        .toPlainString().replace("-", MINUS) + "%"

/** Preserve nonzero simulation risks instead of rounding them to a reassuring zero. */
fun formatRiskPercent(ratio: Double): String {
    require(ratio.isFinite() && ratio in 0.0..1.0)
    if (ratio == 0.0) return "0%"
    if (ratio < 0.001) return "<0.1%"
    return decimal(ratio).movePointRight(2).setScale(1, RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString() + "%"
}

fun formatDate(date: LocalDate, today: LocalDate = LocalDate.now()): String =
    date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "d MMM" else "d MMM yyyy", Locale.ENGLISH))

/** firstMonth is the month represented by Month 1. */
fun formatPeriod(index: Int, firstMonth: YearMonth): String {
    require(index >= 1) { "Period index must be positive" }
    val month = firstMonth.plusMonths(index.toLong() - 1).format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
    return "Month $index ($month)"
}

fun formatAccountDigits(digits: String): String {
    require(digits.length >= 4 && digits.all { it in '0'..'9' }) { "Account digits must contain at least four digits" }
    return "•• ${digits.takeLast(4)}"
}
