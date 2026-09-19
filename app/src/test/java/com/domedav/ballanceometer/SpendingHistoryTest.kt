package com.domedav.ballanceometer

import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.ui.show.SpendingDay
import com.domedav.ballanceometer.ui.show.buildSpendingDays
import com.domedav.ballanceometer.ui.show.filterSpendings
import com.domedav.ballanceometer.ui.show.groupSpendingsByDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * JVM unit tests for the Show screen spending-history helpers (pure functions in SpendingHistory.kt).
 */
class SpendingHistoryTest {

    private fun spending(date: LocalDate, amount: Double, note: String = "") =
        Spending(
            id = "s-${date}-$amount-$note",
            amount = amount,
            note = note,
            timestamp = LocalDateTime.of(date, LocalTime.NOON)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    @Test
    fun grouping_groupsByLocalDate() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val list = listOf(
            spending(today, 10.0),
            spending(today, 20.0),
            spending(yesterday, 5.0)
        )
        val grouped = groupSpendingsByDay(list)
        assertEquals(2, grouped.size)
        assertEquals(2, grouped[today]!!.size)
        assertEquals(1, grouped[yesterday]!!.size)
    }

    @Test
    fun buildSpendingDays_sortedDescendingWithTotals() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val days = buildSpendingDays(
            listOf(
                spending(yesterday, 5.0),
                spending(today, 10.0),
                spending(today, 20.0)
            )
        )
        assertEquals(2, days.size)
        assertEquals(today, days[0].date)
        assertEquals(30.0, days[0].total, 0.0)
        assertEquals(yesterday, days[1].date)
        assertEquals(5.0, days[1].total, 0.0)
    }

    @Test
    fun buildSpendingDays_emptyReturnsEmpty() {
        assertEquals(emptyList<SpendingDay>(), buildSpendingDays(emptyList()))
    }

    @Test
    fun filterSpendings_matchesNoteCaseInsensitiveTrimmed() {
        val list = listOf(
            spending(LocalDate.now(), 10.0, "Coffee"),
            spending(LocalDate.now(), 5.0, "Bus ticket"),
            spending(LocalDate.now(), 7.0, "COFFEE beans")
        )
        val result = filterSpendings(list, "  coffee ")
        assertEquals(2, result.size)
        assertTrue(result.all { it.note.lowercase().contains("coffee") })
    }

    @Test
    fun filterSpendings_blankQueryReturnsAll() {
        val list = listOf(spending(LocalDate.now(), 10.0, "Coffee"))
        assertEquals(list, filterSpendings(list, "   "))
    }

    @Test
    fun filterSpendings_matchesAmount() {
        val list = listOf(
            spending(LocalDate.now(), 1500.0, "Coffee"),
            spending(LocalDate.now(), 250.0, "Bus")
        )
        assertEquals(1, filterSpendings(list, "1500").size)
        assertEquals(1500.0, filterSpendings(list, "1500")[0].amount, 0.0)
    }

    @Test
    fun filterSpendings_matchesDate() {
        val date = LocalDate.now().minusDays(2)
        val other = LocalDate.now().minusDays(5)
        val list = listOf(spending(date, 10.0, ""), spending(other, 10.0, ""))
        val mmdd = "%02d.%02d".format(date.monthValue, date.dayOfMonth)
        val result = filterSpendings(list, mmdd)
        assertEquals(1, result.size)
        assertEquals(date, groupSpendingsByDay(result).keys.single())
    }

    @Test
    fun noonTimestamp_isDstSafe() {
        // NOON + systemDefault avoids midnight DST edge cases: round-trips to the same date.
        val zone = ZoneId.systemDefault()
        val dates = listOf(
            LocalDate.now(),
            LocalDate.now().minusDays(1),
            LocalDate.of(2024, 3, 31),
            LocalDate.of(2024, 10, 27)
        )
        for (date in dates) {
            val s = spending(date, 1.0)
            val grouped = groupSpendingsByDay(listOf(s))
            assertTrue(grouped.containsKey(date))
        }
    }
}
