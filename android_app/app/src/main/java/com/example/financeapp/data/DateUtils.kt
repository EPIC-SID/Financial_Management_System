package com.example.financeapp.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatTransactionDate(dateStr: String): String {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr) ?: return dateStr
        
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        displayFormat.format(date)
    } catch (e: Exception) {
        dateStr.take(10)
    }
}

fun formatTransactionMonth(dateStr: String): String {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr) ?: return dateStr
        
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        displayFormat.format(date)
    } catch (e: Exception) {
        dateStr.take(7)
    }
}

fun formatTransactionTime(dateStr: String): String {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr) ?: return ""
        
        val displayFormat = SimpleDateFormat("hh:mm a", Locale.US)
        displayFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}
