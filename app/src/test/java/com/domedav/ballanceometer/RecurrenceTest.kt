package com.domedav.ballanceometer

import com.domedav.ballanceometer.data.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * JVM unit tests for recurrence instance counting and due-date logic.
 */
class RecurrenceTest {

    @Test
    fun monthlyInstances_onceAndMonthlyAlwaysOne() {
        for ((y, m) in listOf(2026 to 1, 2026 to 2, 2024 to 2)) {
            assertEquals(1, Recurrence.ONCE.monthlyInstances(y, m))
            assertEquals(1, Recurrence.MONTHLY.monthlyInstances(y, m))
        }
    }

    @Test
    fun monthlyInstances_dailyEqualsDaysInMonth() {
        assertEquals(31, Recurrence.DAILY.monthlyInstances(2026, 1))
        assertEquals(28, Recurrence.DAILY.monthlyInstances(2026, 2))
        assertEquals(29, Recurrence.DAILY.monthlyInstances(2024, 2))
    }

    @Test
    fun monthlyInstances_weeklyRoundsUp() {
        assertEquals(5, Recurrence.WEEKLY.monthlyInstances(2026, 1)) // 31 days
        assertEquals(4, Recurrence.WEEKLY.monthlyInstances(2026, 2)) // 28 days
        assertEquals(5, Recurrence.WEEKLY.monthlyInstances(2026, 4)) // 30 days
    }

    @Test
    fun isDue_dailyAlwaysTrue() {
        val today = LocalDate.of(2026, 9, 12)
        assertTrue(Recurrence.DAILY.isDue(0L, today, emptyList()))
        assertTrue(Recurrence.DAILY.isDue(0L, today, listOf("2026-09-12")))
    }

    @Test
    fun isDue_onceOnlyBeforeFirstCompletion() {
        val today = LocalDate.of(2026, 9, 12)
        assertTrue(Recurrence.ONCE.isDue(0L, today, emptyList()))
        assertFalse(Recurrence.ONCE.isDue(0L, today, listOf("2026-09-01")))
    }

    private fun millisOf(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun isDue_weeklyOnlyOnCreationWeekday() {
        // 2026-09-07 is a Monday
        val monday = LocalDate.of(2026, 9, 7)
        val createdAt = millisOf(monday)
        assertTrue(Recurrence.WEEKLY.isDue(createdAt, monday, emptyList()))
        assertTrue(Recurrence.WEEKLY.isDue(createdAt, monday.plusWeeks(2), emptyList()))
        assertFalse(Recurrence.WEEKLY.isDue(createdAt, monday.plusDays(1), emptyList()))
        assertFalse(Recurrence.WEEKLY.isDue(createdAt, monday.minusDays(1), emptyList()))
    }

    @Test
    fun isDue_monthlyOnlyOnCreationDay() {
        val createdAt = millisOf(LocalDate.of(2026, 1, 5))
        assertTrue(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 9, 5), emptyList()))
        assertTrue(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 2, 5), emptyList()))
        assertFalse(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 9, 6), emptyList()))
        assertFalse(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 9, 4), emptyList()))
    }

    @Test
    fun isDue_monthlyCreatedOn31st_fallsBackToLastDayOfShortMonth() {
        val createdAt = millisOf(LocalDate.of(2026, 1, 31))
        // February 2026 has 28 days: due on the 28th, not before
        assertTrue(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 2, 28), emptyList()))
        assertFalse(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 2, 27), emptyList()))
        // Full months keep the 31st
        assertTrue(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 3, 31), emptyList()))
        assertFalse(Recurrence.MONTHLY.isDue(createdAt, LocalDate.of(2026, 3, 30), emptyList()))
    }
}
