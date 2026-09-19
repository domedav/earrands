package com.domedav.ballanceometer.ui.show

import com.domedav.ballanceometer.data.Spending
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure, framework-free spending-history helpers for the Show screen.
 * Lives in its own file (no Android imports) so plain JVM unit tests can cover it.
 */
data class SpendingDay(
    val date: LocalDate,
    val items: List<Spending>,
    val total: Double
)

fun groupSpendingsByDay(list: List<Spending>): Map<LocalDate, List<Spending>> {
    val zone = ZoneId.systemDefault()
    val grouped = mutableMapOf<LocalDate, MutableList<Spending>>()
    for (s in list) {
        val d = Instant.ofEpochMilli(s.timestamp).atZone(zone).toLocalDate()
        grouped.getOrPut(d) { mutableListOf() }.add(s)
    }
    return grouped
}

fun buildSpendingDays(list: List<Spending>, daysBack: Int = 30): List<SpendingDay> {
    if (list.isEmpty()) return emptyList()
    val grouped = groupSpendingsByDay(list)
    return grouped.map { (date, items) ->
        SpendingDay(date = date, items = items.toList(), total = items.sumOf { it.amount })
    }.sortedByDescending { it.date }
}

fun filterSpendings(list: List<Spending>, query: String): List<Spending> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return list
    val zone = ZoneId.systemDefault()
    return list.filter { s ->
        if (s.note.lowercase().contains(q)) return@filter true
        // Amount: raw ("1500") and rounded ("%.0f") forms
        if (s.amount.toString().contains(q)) return@filter true
        if ("%.0f".format(s.amount).contains(q)) return@filter true
        // Date: yyyy.MM.dd, yyyy-MM-dd, MM.dd
        val d = Instant.ofEpochMilli(s.timestamp).atZone(zone).toLocalDate()
        if (d.toString().lowercase().contains(q)) return@filter true
        if ("%04d.%02d.%02d".format(d.year, d.monthValue, d.dayOfMonth).contains(q)) return@filter true
        if ("%02d.%02d".format(d.monthValue, d.dayOfMonth).contains(q)) return@filter true
        false
    }
}
