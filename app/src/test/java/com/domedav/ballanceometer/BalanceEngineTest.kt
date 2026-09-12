package com.domedav.ballanceometer

import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup
import com.domedav.ballanceometer.domain.BalanceEngine
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM unit tests for the unlock math. No Android framework needed.
 */
class BalanceEngineTest {

    private fun group(id: String, weight: Float) =
        TaskGroup(id = id, name = id, weight = weight)

    private fun subtask(id: String, groupId: String, recurrence: String = "DAILY") =
        Subtask(id = id, groupId = groupId, title = id, recurrence = recurrence, createdAt = 0L)

    @Test
    fun zeroWeights_giveZeroValue() {
        val groups = listOf(group("g1", 0f), group("g2", 0f))
        val subs = listOf(subtask("s1", "g1"))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(0.0, value, 0.0)
    }

    @Test
    fun singleDailySubtask_splitsGroupShareAcrossMonthDays() {
        // January 2026 has 31 days; single group weight 100, earnable 10 000
        val groups = listOf(group("g1", 100f))
        val subs = listOf(subtask("s1", "g1", "DAILY"))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(10_000.0 / 31, value, 0.001)
    }

    @Test
    fun weights_splitEarnableProportionally() {
        // 75/25 split, one DAILY subtask per group, February 2026 (28 days)
        val groups = listOf(group("g1", 75f), group("g2", 25f))
        val subs = listOf(
            subtask("s1", "g1", "DAILY"),
            subtask("s2", "g2", "DAILY")
        )
        val v1 = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 2, earnable = 10_000.0
        )
        val v2 = BalanceEngine.valuePerInstance(
            group = groups[1], subtask = subs[1],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 2, earnable = 10_000.0
        )
        assertEquals(7_500.0 / 28, v1, 0.001)
        assertEquals(2_500.0 / 28, v2, 0.001)
    }

    @Test
    fun unlockedTotal_sumsValues() {
        val completions = listOf(
            Completion("c1", "s1", "2026-09-12", 0L, 100.0),
            Completion("c2", "s2", "2026-09-12", 0L, 250.5)
        )
        assertEquals(350.5, BalanceEngine.unlockedTotal(completions), 0.0)
        assertEquals(0.0, BalanceEngine.unlockedTotal(emptyList()), 0.0)
    }

    @Test
    fun available_isMinimalPlusUnlockedMinusSpent() {
        val spendings = listOf(
            Spending("s1", 1000.0, "", 0L),
            Spending("s2", 500.0, "", 0L)
        )
        assertEquals(8500.0, BalanceEngine.available(5000.0, 5000.0, spendings), 0.0)
        assertEquals(5000.0, BalanceEngine.available(5000.0, 0.0, emptyList()), 0.0)
    }

    @Test
    fun overspend_canDriveAvailableNegative() {
        // Documents current behavior: no floor at zero
        val spendings = listOf(Spending("s1", 8000.0, "", 0L))
        assertEquals(-2000.0, BalanceEngine.available(5000.0, 1000.0, spendings), 0.0)
    }

    @Test
    fun earnableZero_givesZeroValue() {
        val groups = listOf(group("g1", 100f))
        val subs = listOf(subtask("s1", "g1", "DAILY"))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 1, earnable = 0.0
        )
        assertEquals(0.0, value, 0.0)
    }

    @Test
    fun groupWithoutSubtasks_givesZeroValue() {
        val groups = listOf(group("g1", 100f))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subtask("s1", "g1", "DAILY"),
            allGroups = groups, allSubtasks = emptyList(),
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(0.0, value, 0.0)
    }

    @Test
    fun multipleSubtasks_shareGroupInstances() {
        // Two DAILY subtasks in January (31 days) -> 62 instances, each worth 10000/62
        val groups = listOf(group("g1", 100f))
        val subs = listOf(subtask("s1", "g1", "DAILY"), subtask("s2", "g1", "DAILY"))
        for (s in subs) {
            val value = BalanceEngine.valuePerInstance(
                group = groups[0], subtask = s,
                allGroups = groups, allSubtasks = subs,
                year = 2026, month = 1, earnable = 10_000.0
            )
            assertEquals(10_000.0 / 62, value, 0.001)
        }
    }

    @Test
    fun weeklyMultiplier_appliesSixTimes() {
        // January 2026: 31 days -> 5 weekly instances; 10000/5 base * 6
        val groups = listOf(group("g1", 100f))
        val subs = listOf(subtask("s1", "g1", "WEEKLY"))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(2000.0 * 6, value, 0.001)
    }

    @Test
    fun monthlyMultiplier_appliesTwentyFourTimes() {
        val groups = listOf(group("g1", 100f))
        val subs = listOf(subtask("s1", "g1", "MONTHLY"))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(10_000.0 * 24, value, 0.001)
    }

    @Test
    fun onceMultiplier_appliesBonus() {
        // ONCE: 85x * 2 bonus = 170x (intentional bonus, may exceed earnable)
        val groups = listOf(group("g1", 100f))
        val subs = listOf(subtask("s1", "g1", "ONCE"))
        val value = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = subs[0],
            allGroups = groups, allSubtasks = subs,
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(10_000.0 * 170, value, 0.001)
    }

    @Test
    fun unknownRecurrence_fallsBackToOnce() {
        val groups = listOf(group("g1", 100f))
        val bogus = subtask("s1", "g1", "BOGUS")
        val once = subtask("s1", "g1", "ONCE")
        val vBogus = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = bogus,
            allGroups = groups, allSubtasks = listOf(bogus),
            year = 2026, month = 1, earnable = 10_000.0
        )
        val vOnce = BalanceEngine.valuePerInstance(
            group = groups[0], subtask = once,
            allGroups = groups, allSubtasks = listOf(once),
            year = 2026, month = 1, earnable = 10_000.0
        )
        assertEquals(vOnce, vBogus, 0.0)
    }

    @Test
    fun daysInMonth_handlesLeapFebruary() {
        assertEquals(31, BalanceEngine.daysInMonth(2026, 1))
        assertEquals(28, BalanceEngine.daysInMonth(2026, 2))
        assertEquals(29, BalanceEngine.daysInMonth(2024, 2))
        assertEquals(30, BalanceEngine.daysInMonth(2026, 4))
    }
}
