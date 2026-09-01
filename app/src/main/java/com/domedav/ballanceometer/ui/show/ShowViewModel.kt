package com.domedav.ballanceometer.ui.show

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.domedav.ballanceometer.BallanceometerApp
import com.domedav.ballanceometer.data.AppConfig
import com.domedav.ballanceometer.data.BallanceRepository
import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup
import com.domedav.ballanceometer.domain.BalanceEngine
import com.domedav.ballanceometer.widget.BallanceWidgetUpdater
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayTodo(
    val subtask: Subtask,
    val isCompletedToday: Boolean,
    val value: Double,
    val groupName: String?
)

class ShowViewModel(application: Application) : AndroidViewModel(application) {

    // Injected repository from BallanceometerApp
    private val repository: BallanceRepository = (getApplication() as BallanceometerApp).repository

    val config: StateFlow<AppConfig?> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val groups: StateFlow<List<TaskGroup>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtasks: StateFlow<List<Subtask>> = repository.subtasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completions: StateFlow<List<Completion>> = repository.completions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendings: StateFlow<List<Spending>> = repository.spendings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived: earnable = max(0, totalBalance - minimalSpend)
    val earnable: StateFlow<Double> = config
        .map { cfg -> if (cfg != null) maxOf(0.0, cfg.totalBalance - cfg.minimalSpend) else 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unlocked: StateFlow<Double> = completions
        .map { list -> BalanceEngine.unlockedTotal(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val available: StateFlow<Double> = combine(config, unlocked, spendings) { cfg, ul, sps ->
        val minimal = cfg?.minimalSpend ?: 0.0
        BalanceEngine.available(minimal, ul, sps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val remainingLocked: StateFlow<Double> = combine(earnable, unlocked) { e, u ->
        maxOf(0.0, e - u)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val progress: StateFlow<Float> = combine(unlocked, earnable) { u, e ->
        if (e > 0) (u / e).coerceIn(0.0, 1.0).toFloat() else 0f
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val todayTodos: StateFlow<List<TodayTodo>> = combine(
        groups, subtasks, completions, config
    ) { grps, subs, comps, cfg ->
        val today = LocalDate.now().toString()
        val ym = YearMonth.now()
        val earnableVal = if (cfg != null) maxOf(0.0, cfg.totalBalance - cfg.minimalSpend) else 0.0
        val completionsTodayIds = comps.filter { it.date == today }.map { it.subtaskId }.toSet()
        val groupNameById = grps.associate { it.id to it.name }
        subs.map { st ->
            val isCompleted = completionsTodayIds.contains(st.id)
            val group = grps.find { it.id == st.groupId }
            val value = if (group != null) {
                BalanceEngine.valuePerInstance(
                    group = group,
                    subtask = st,
                    allGroups = grps,
                    allSubtasks = subs,
                    year = ym.year,
                    month = ym.monthValue,
                    earnable = earnableVal
                )
            } else 0.0
            TodayTodo(
                subtask = st,
                isCompletedToday = isCompleted,
                value = value,
                groupName = groupNameById[st.groupId]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleSubtask(subtask: Subtask) {
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            val isCompleted = completions.value.any { it.subtaskId == subtask.id && it.date == today }
            if (isCompleted) {
                val completion = completions.value.firstOrNull { it.subtaskId == subtask.id && it.date == today }
                if (completion != null) {
                    repository.deleteCompletion(completion)
                }
            } else {
                repository.completeSubtask(subtask.id, today)
            }
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun addSpending(amount: Double, note: String) {
        if (amount <= 0) return
        viewModelScope.launch {
            val spending = Spending(
                id = UUID.randomUUID().toString(),
                amount = amount,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            repository.addSpending(spending)
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun deleteSpending(spending: Spending) {
        viewModelScope.launch {
            repository.deleteSpending(spending)
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }
}
