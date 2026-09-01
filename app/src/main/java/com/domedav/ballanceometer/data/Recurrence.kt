package com.domedav.ballanceometer.data

import java.time.YearMonth

enum class Recurrence {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY;

    fun monthlyInstances(year: Int, month: Int): Int {
        val days = YearMonth.of(year, month).lengthOfMonth()
        return when (this) {
            ONCE -> 1
            DAILY -> days
            WEEKLY -> (days + 6) / 7
            MONTHLY -> 1
        }
    }
}
