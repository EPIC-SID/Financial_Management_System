package com.example.financeapp.ui.viewmodel

import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.data.ingest.SyntheticDataset
import com.example.financeapp.domain.model.Direction
import java.time.*

enum class AppDataMode(val label: String) { DEMO("Demo"), LIVE("My Transactions") }
enum class ActivityPeriod(val label: String) { MONTH("This month"), LAST_30("Last 30 days"), ALL("All dates") }

data class ActivityEntry(val id: String, val merchant: String, val amount: Double, val category: String, val date: LocalDate?)
data class ActivitySummary(val credits: Double, val debits: Double, val categories: Map<String, Double>, val unclassified: Int)

object ActivityData {
    private val zone = ZoneId.of("Asia/Kolkata")
    fun live(rows: List<TransactionEntity>): List<ActivityEntry> = rows.filter { it.amount.isFinite() }.map {
        ActivityEntry(it.smsId, it.merchant, it.amount, it.category, date(it.transactionDate))
    }
    fun demo(dataset: SyntheticDataset): List<ActivityEntry> = dataset.transactions.filter { it.date < dataset.demoDate }.map {
        ActivityEntry(it.id, "Demo " + it.category.lowercase().replace('_', ' '),
            if (it.direction == Direction.DEBIT) -it.amount else it.amount, it.category, it.date)
    }
    fun filter(entries: List<ActivityEntry>, period: ActivityPeriod, today: LocalDate): List<ActivityEntry> {
        val start = when (period) {
            ActivityPeriod.MONTH -> today.withDayOfMonth(1)
            ActivityPeriod.LAST_30 -> today.minusDays(29)
            ActivityPeriod.ALL -> LocalDate.MIN
        }
        return entries.filter { entry ->
            entry.date?.let { it >= start && it <= today } ?: (period == ActivityPeriod.ALL)
        }.sortedByDescending { it.date }
    }
    fun summary(entries: List<ActivityEntry>): ActivitySummary {
        val debits = entries.filter { it.amount < 0 }
        return ActivitySummary(entries.filter { it.amount > 0 }.sumOf { it.amount },
            debits.sumOf { -it.amount }, debits.groupBy { it.category }.mapValues { (_, rows) -> rows.sumOf { -it.amount } },
            debits.count { it.category == "MISCELLANEOUS" })
    }
    private fun date(value: String): LocalDate? {
        return try { OffsetDateTime.parse(value).atZoneSameInstant(zone).toLocalDate() }
        catch (_: DateTimeException) {
            try { LocalDateTime.parse(value).atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDate() }
            catch (_: DateTimeException) { try { LocalDate.parse(value) } catch (_: DateTimeException) { null } }
        }
    }
}
