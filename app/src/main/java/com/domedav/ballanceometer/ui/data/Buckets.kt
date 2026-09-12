package com.domedav.ballanceometer.ui.data

import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.Spending
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Pure, framework-free bucket math for the Data screen.
 * Lives in its own file (no Android imports) so plain JVM unit tests can cover it.
 */
enum class Period {
    DAILY, WEEKLY, MONTHLY
}

data class Bucket(
    val label: String,
    val date: LocalDate,
    val income: Double,
    val spending: Double,
    val net: Double
)
internal fun buildBuckets(
    period: Period,
    compList: List<Completion>,
    spendList: List<Spending>
): List<Bucket> {
    // Group income by LocalDate.parse(completion.date)
    val incomeByDate = mutableMapOf<LocalDate, Double>()
    for (c in compList) {
        val d = try {
            LocalDate.parse(c.date)
        } catch (_: Exception) {
            continue
        }
        incomeByDate[d] = (incomeByDate[d] ?: 0.0) + c.valueUnlocked
    }
    // Group spending by Instant.ofEpochMilli(timestamp).atZone(systemDefault).toLocalDate()
    val spendingByDate = mutableMapOf<LocalDate, Double>()
    val zone = ZoneId.systemDefault()
    for (s in spendList) {
        val d = Instant.ofEpochMilli(s.timestamp).atZone(zone).toLocalDate()
        spendingByDate[d] = (spendingByDate[d] ?: 0.0) + s.amount
    }

    return when (period) {
        Period.DAILY -> buildDailyBuckets(incomeByDate, spendingByDate)
        Period.WEEKLY -> buildWeeklyBuckets(incomeByDate, spendingByDate)
        Period.MONTHLY -> buildMonthlyBuckets(incomeByDate, spendingByDate)
    }
}

internal fun buildDailyBuckets(
    incomeByDate: Map<LocalDate, Double>,
    spendingByDate: Map<LocalDate, Double>
): List<Bucket> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MM.dd")
    val result = mutableListOf<Bucket>()
    for (i in 6 downTo 0) {
        val date = today.minusDays(i.toLong())
        val income = incomeByDate[date] ?: 0.0
        val spending = spendingByDate[date] ?: 0.0
        val label = date.format(formatter)
        result.add(Bucket(label = label, date = date, income = income, spending = spending, net = income - spending))
    }
    return result
}

internal fun buildWeeklyBuckets(
    incomeByDate: Map<LocalDate, Double>,
    spendingByDate: Map<LocalDate, Double>
): List<Bucket> {
    val today = LocalDate.now()
    val latestMonday = today.with(DayOfWeek.MONDAY)
    val startMonday = latestMonday.minusWeeks(3)
    val formatter = DateTimeFormatter.ofPattern("MM.dd")
    val result = mutableListOf<Bucket>()
    for (w in 0 until 4) {
        val weekStart = startMonday.plusWeeks(w.toLong())
        val weekEnd = weekStart.plusDays(6)
        var income = 0.0
        var spending = 0.0
        var d = weekStart
        while (!d.isAfter(weekEnd)) {
            income += incomeByDate[d] ?: 0.0
            spending += spendingByDate[d] ?: 0.0
            d = d.plusDays(1)
        }
        val label = weekStart.format(formatter)
        result.add(Bucket(label = label, date = weekStart, income = income, spending = spending, net = income - spending))
    }
    return result
}

internal fun buildMonthlyBuckets(
    incomeByDate: Map<LocalDate, Double>,
    spendingByDate: Map<LocalDate, Double>
): List<Bucket> {
    val current = YearMonth.now()
    val labelFormatter = DateTimeFormatter.ofPattern("yy.MM")
    val result = mutableListOf<Bucket>()
    for (i in 5 downTo 0) {
        val ym = current.minusMonths(i.toLong())
        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()
        var income = 0.0
        var spending = 0.0
        var d = start
        while (!d.isAfter(end)) {
            income += incomeByDate[d] ?: 0.0
            spending += spendingByDate[d] ?: 0.0
            d = d.plusDays(1)
        }
        val label = ym.format(labelFormatter)
        result.add(Bucket(label = label, date = start, income = income, spending = spending, net = income - spending))
    }
    return result
}
