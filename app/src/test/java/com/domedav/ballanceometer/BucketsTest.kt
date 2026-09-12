package com.domedav.ballanceometer

import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.ui.data.Period
import com.domedav.ballanceometer.ui.data.buildBuckets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/**
 * JVM unit tests for the Data screen bucket math (pure functions in Buckets.kt).
 */
class BucketsTest {

    private fun completion(date: LocalDate, value: Double) =
        Completion("c-${date}-$value", "s1", date.toString(), 0L, value)

    private fun spending(date: LocalDate, amount: Double) =
        Spending(
            id = "s-${date}-$amount",
            amount = amount,
            timestamp = LocalDateTime.of(date, LocalTime.NOON)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    @Test
    fun daily_returnsSevenBucketsEndingToday() {
        val buckets = buildBuckets(Period.DAILY, emptyList(), emptyList())
        assertEquals(7, buckets.size)
        assertEquals(LocalDate.now(), buckets.last().date)
        assertTrue(buckets.all { it.income == 0.0 && it.spending == 0.0 && it.net == 0.0 })
    }

    @Test
    fun daily_netIsIncomeMinusSpending() {
        val today = LocalDate.now()
        val buckets = buildBuckets(
            Period.DAILY,
            listOf(completion(today, 100.0), completion(today, 50.0)),
            listOf(spending(today, 30.0))
        )
        val last = buckets.last()
        assertEquals(150.0, last.income, 0.0)
        assertEquals(30.0, last.spending, 0.0)
        assertEquals(120.0, last.net, 0.0)
    }

    @Test
    fun daily_malformedCompletionDateIsSkipped() {
        val bad = Completion("bad", "s1", "not-a-date", 0L, 999.0)
        val buckets = buildBuckets(Period.DAILY, listOf(bad), emptyList())
        assertTrue(buckets.all { it.income == 0.0 })
    }

    @Test
    fun weekly_returnsFourMondayBuckets() {
        val buckets = buildBuckets(Period.WEEKLY, emptyList(), emptyList())
        assertEquals(4, buckets.size)
        assertTrue(buckets.all { it.date.dayOfWeek == DayOfWeek.MONDAY })
        for (i in 0 until 3) {
            assertEquals(buckets[i].date.plusWeeks(1), buckets[i + 1].date)
        }
    }

    @Test
    fun weekly_groupsIncomeIntoCorrectWeek() {
        val thisMonday = LocalDate.now().with(DayOfWeek.MONDAY)
        val prevSunday = thisMonday.minusDays(1)
        val buckets = buildBuckets(
            Period.WEEKLY,
            listOf(completion(thisMonday, 50.0), completion(prevSunday, 20.0)),
            emptyList()
        )
        assertEquals(50.0, buckets[3].income, 0.0)
        assertEquals(20.0, buckets[2].income, 0.0)
        assertEquals(0.0, buckets[0].income, 0.0)
    }

    @Test
    fun monthly_returnsSixBucketsEndingCurrentMonth() {
        val buckets = buildBuckets(Period.MONTHLY, emptyList(), emptyList())
        assertEquals(6, buckets.size)
        val current = YearMonth.now()
        assertEquals(current.atDay(1), buckets.last().date)
        for (i in 0 until 5) {
            assertEquals(
                YearMonth.from(buckets[i].date).plusMonths(1),
                YearMonth.from(buckets[i + 1].date)
            )
        }
    }

    @Test
    fun monthly_countsOnlyDaysInsideEachMonth() {
        val current = YearMonth.now()
        val firstOfMonth = current.atDay(1)
        val lastOfPrev = firstOfMonth.minusDays(1)
        val buckets = buildBuckets(
            Period.MONTHLY,
            listOf(completion(firstOfMonth, 70.0), completion(lastOfPrev, 40.0)),
            listOf(spending(firstOfMonth, 10.0))
        )
        val last = buckets.last()
        assertEquals(70.0, last.income, 0.0)
        assertEquals(10.0, last.spending, 0.0)
        assertEquals(60.0, last.net, 0.0)
        assertEquals(40.0, buckets[4].income, 0.0)
    }

    @Test
    fun oldDataOutsideWindows_isExcluded() {
        val ancient = LocalDate.now().minusYears(2)
        val buckets = buildBuckets(
            Period.DAILY,
            listOf(completion(ancient, 500.0)),
            listOf(spending(ancient, 500.0))
        )
        assertTrue(buckets.all { it.income == 0.0 && it.spending == 0.0 })
    }
}
