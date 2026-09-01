package com.domedav.ballanceometer.ui.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.domedav.ballanceometer.data.AppConfig
import com.domedav.ballanceometer.data.BallanceDatabase
import com.domedav.ballanceometer.data.BallanceRepository
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup
import com.domedav.ballanceometer.widget.BallanceWidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BallanceRepository by lazy {
        // Try to obtain repository from BallanceometerApp if it exists, otherwise create directly
        try {
            val clazz = Class.forName("com.domedav.ballanceometer.BallanceometerApp")
            if (clazz.isInstance(application)) {
                val method = clazz.methods.find { it.name == "getBallanceRepository" || it.name == "getRepository" }
                if (method != null) {
                    val result = method.invoke(application)
                    if (result is BallanceRepository) return@lazy result
                }
                // try field
                val fieldNames = listOf("ballanceRepository", "repository", "ballanceometerRepository")
                for (name in fieldNames) {
                    try {
                        val field = clazz.getDeclaredField(name)
                        field.isAccessible = true
                        val result = field.get(application)
                        if (result is BallanceRepository) return@lazy result
                    } catch (_: Exception) { }
                }
            }
        } catch (_: Exception) { }
        // fallback: try MinitasklistApp pattern or direct DB
        BallanceRepository(BallanceDatabase.getInstance(application))
    }

    val config: Flow<AppConfig?> = repository.config

    val groups: StateFlow<List<TaskGroup>> = repository.groups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtasks: StateFlow<List<Subtask>> = repository.subtasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveConfig(totalBalance: Double, minimalSpend: Double, currency: String) {
        viewModelScope.launch {
            repository.saveConfig(AppConfig(id = 1, totalBalance = totalBalance, minimalSpend = minimalSpend, currency = currency))
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun addGroup(name: String, weight: Float) {
        viewModelScope.launch {
            repository.addGroup(TaskGroup(name = name, weight = weight.coerceIn(0f, 100f)))
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun updateGroup(group: TaskGroup) {
        viewModelScope.launch {
            repository.updateGroup(group)
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun deleteGroup(group: TaskGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun addSubtask(groupId: String, title: String, recurrence: String) {
        viewModelScope.launch {
            repository.addSubtask(
                Subtask(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    title = title,
                    recurrence = recurrence,
                    createdAt = System.currentTimeMillis()
                )
            )
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun updateSubtask(subtask: Subtask) {
        viewModelScope.launch {
            repository.updateSubtask(subtask)
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }

    fun deleteSubtask(subtask: Subtask) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
            BallanceWidgetUpdater.enqueueImmediate(getApplication())
        }
    }
}
