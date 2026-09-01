package com.domedav.ballanceometer.domain

import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.Recurrence
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup
import java.time.YearMonth

object BalanceEngine {

    fun daysInMonth(year: Int, month: Int): Int {
        return YearMonth.of(year, month).lengthOfMonth()
    }

    fun valuePerInstance(
        group: TaskGroup,
        subtask: Subtask,
        allGroups: List<TaskGroup>,
        allSubtasks: List<Subtask>,
        year: Int,
        month: Int,
        earnable: Double
    ): Double {
        val sumWeights = allGroups.sumOf { it.weight.toDouble() }
        if (sumWeights == 0.0) return 0.0
        val groupShare = earnable * (group.weight.toDouble() / sumWeights)
        val subtasksInGroup = allSubtasks.filter { it.groupId == group.id }
        if (subtasksInGroup.isEmpty()) return 0.0
        val totalInstances = subtasksInGroup.sumOf { st ->
            val rec = try {
                Recurrence.valueOf(st.recurrence)
            } catch (_: IllegalArgumentException) {
                Recurrence.ONCE
            }
            rec.monthlyInstances(year, month)
        }
        if (totalInstances == 0) return 0.0
        return groupShare / totalInstances.toDouble()
    }

    fun unlockedTotal(completions: List<Completion>): Double {
        return completions.sumOf { it.valueUnlocked }
    }

    fun available(minimal: Double, unlocked: Double, spendings: List<Spending>): Double {
        val spent = spendings.sumOf { it.amount }
        return minimal + unlocked - spent
    }
}
