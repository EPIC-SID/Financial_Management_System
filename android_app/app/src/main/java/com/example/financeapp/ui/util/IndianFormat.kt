package com.example.financeapp.ui.util

import kotlin.math.abs
import kotlin.math.roundToLong

private const val MINUS = "\u2212"
private const val RUPEE = "\u20B9"

/** 150000.0 -> "₹1,50,000" ; -4200.0 -> "−₹4,200" */
fun formatInr(amount: Double): String {
    val sign = if (amount < 0) MINUS else ""
    val whole = abs(amount).roundToLong().toString()
    val grouped = if (whole.length <= 3) {
        whole
    } else {
        val last3 = whole.takeLast(3)
        val rest = whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed()
        "$rest,$last3"
    }
    return "$sign$RUPEE$grouped"
}

/** 25000.0 -> "₹25K" ; 150000.0 -> "₹1.5L" ; 23000000.0 -> "₹2.3Cr" */
fun formatInrCompact(amount: Double): String {
    val sign = if (amount < 0) MINUS else ""
    val a = abs(amount)
    fun trim(v: Double) = String.format(java.util.Locale.US, "%.1f", v).removeSuffix(".0")
    return when {
        a >= 1e7 -> "$sign$RUPEE${trim(a / 1e7)}Cr"
        a >= 1e5 -> "$sign$RUPEE${trim(a / 1e5)}L"
        a >= 1e3 -> "$sign$RUPEE${trim(a / 1e3)}K"
        else -> "$sign$RUPEE${a.roundToLong()}"
    }
}
