package com.domedav.ballanceometer.domain

import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.Recurrence
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup
import java.time.YearMonth

object BalanceEngine {

    const val DAILY_MULTIPLIER = 1.0
    const val WEEKLY_MULTIPLIER = 6.0
    const val MONTHLY_MULTIPLIER = 24.0
    const val ONCE_MULTIPLIER = 85.0
    const val ONCE_BONUS = 2.0
    const val SUBTASK_WEIGHT_MIN = 0f
    const val SUBTASK_WEIGHT_MAX = 100f
    const val SUBTASK_WEIGHT_DEFAULT = 50f
    const val SUBTASK_WEIGHT_NEUTRAL = 50.0

    fun daysInMonth(year: Int, month: Int): Int {
        return YearMonth.of(year, month).lengthOfMonth()
    }

    private fun multiplier(rec: Recurrence): Double = when (rec) {
        Recurrence.DAILY -> DAILY_MULTIPLIER
        Recurrence.WEEKLY -> WEEKLY_MULTIPLIER
        Recurrence.MONTHLY -> MONTHLY_MULTIPLIER
        Recurrence.ONCE -> ONCE_MULTIPLIER
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
        val base = groupShare / totalInstances.toDouble()
        val rec = try { Recurrence.valueOf(subtask.recurrence) } catch (_: Exception) { Recurrence.ONCE }
        val mult = multiplier(rec)
        val onceDouble = if (rec == Recurrence.ONCE) ONCE_BONUS else 1.0
        // Egyszeri súly: ugyanaz a 0-100 logika mint a csoportsúlynál,
        // 50 = normál (1.0 faktor), csak ONCE feladatokra hat.
        val weightFactor = if (rec == Recurrence.ONCE) {
            val w = subtask.weight.toDouble()
            if (!w.isFinite() || w <= 0.0) 0.0 else w / SUBTASK_WEIGHT_NEUTRAL
        } else 1.0
        // ONCE 85x *2 =170x — nem tartja magát a büdzséhez, bonus
        return base * mult * onceDouble * weightFactor
    }

    fun unlockedTotal(completions: List<Completion>): Double {
        return completions.sumOf { it.valueUnlocked }
    }

    fun available(minimal: Double, unlocked: Double, spendings: List<Spending>): Double {
        val spent = spendings.sumOf { it.amount }
        return minimal + unlocked - spent
    }
}
