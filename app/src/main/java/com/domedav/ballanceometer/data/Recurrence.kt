package com.domedav.ballanceometer.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

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

    fun isDue(createdAt: Long, today: LocalDate, completionsForSubtask: List<String>): Boolean {
        return when (this) {
            DAILY -> true
            ONCE -> completionsForSubtask.isEmpty()
            WEEKLY -> {
                val createdDate = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                today.dayOfWeek == createdDate.dayOfWeek
            }
            MONTHLY -> {
                val createdDate = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                val todayDom = today.dayOfMonth
                val createdDom = createdDate.dayOfMonth
                val lastDom = today.lengthOfMonth()
                // ha hónap rövidebb, utolsó napon esedékes
                if (createdDom > lastDom) todayDom == lastDom else todayDom == createdDom
            }
        }
    }
}
