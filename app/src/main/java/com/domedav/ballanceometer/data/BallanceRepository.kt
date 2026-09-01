package com.domedav.ballanceometer.data

import com.domedav.ballanceometer.domain.BalanceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BallanceRepository(
    private val db: BallanceDatabase
) {
    private val appConfigDao = db.appConfigDao()
    private val taskGroupDao = db.taskGroupDao()
    private val subtaskDao = db.subtaskDao()
    private val completionDao = db.completionDao()
    private val spendingDao = db.spendingDao()

    val config: Flow<AppConfig?> = appConfigDao.getConfig()
    val groups: Flow<List<TaskGroup>> = taskGroupDao.getAll()
    val subtasks: Flow<List<Subtask>> = subtaskDao.getAll()
    val completions: Flow<List<Completion>> = completionDao.getAll()
    val spendings: Flow<List<Spending>> = spendingDao.getAll()

    fun subtasksByGroup(groupId: String): Flow<List<Subtask>> = subtaskDao.getByGroup(groupId)

    fun completionsByDate(date: String): Flow<List<Completion>> = completionDao.getByDate(date)

    suspend fun saveConfig(config: AppConfig) {
        appConfigDao.upsert(config)
    }

    suspend fun addGroup(group: TaskGroup) {
        taskGroupDao.insert(group)
    }

    suspend fun updateGroup(group: TaskGroup) {
        taskGroupDao.update(group)
    }

    suspend fun deleteGroup(group: TaskGroup) {
        taskGroupDao.delete(group)
    }

    suspend fun addSubtask(subtask: Subtask) {
        subtaskDao.insert(subtask)
    }

    suspend fun updateSubtask(subtask: Subtask) {
        subtaskDao.update(subtask)
    }

    suspend fun deleteSubtask(subtask: Subtask) {
        subtaskDao.delete(subtask)
    }

    /**
     * Creates a Completion for given subtask and date (yyyy-MM-dd).
     * Prevents duplicate per (subtaskId, date) by checking existing completions.
     * Computes valueUnlocked via BalanceEngine.valuePerInstance.
     * Returns the created Completion or null if already exists.
     */
    suspend fun completeSubtask(subtaskId: String, date: String): Completion? {
        // prevent duplicate
        val existing = completionDao.getByDate(date).first().any { it.subtaskId == subtaskId }
        if (existing) return null

        val allSubtasks = subtasks.first()
        val subtask = allSubtasks.find { it.id == subtaskId } ?: return null
        val allGroups = groups.first()
        val group = allGroups.find { it.id == subtask.groupId } ?: return null
        val cfg = config.first()

        val earnable = if (cfg != null) maxOf(0.0, cfg.totalBalance - cfg.minimalSpend) else 0.0

        val localDate = try {
            LocalDate.parse(date)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val year = localDate.year
        val month = localDate.monthValue

        val value = BalanceEngine.valuePerInstance(
            group = group,
            subtask = subtask,
            allGroups = allGroups,
            allSubtasks = allSubtasks,
            year = year,
            month = month,
            earnable = earnable
        )

        val completion = Completion(
            id = UUID.randomUUID().toString(),
            subtaskId = subtaskId,
            date = date,
            completedAt = System.currentTimeMillis(),
            valueUnlocked = value
        )
        completionDao.insert(completion)
        return completion
    }

    suspend fun deleteCompletion(completion: Completion) {
        completionDao.delete(completion)
    }

    suspend fun addSpending(spending: Spending) {
        spendingDao.insert(spending)
    }

    suspend fun deleteSpending(spending: Spending) {
        spendingDao.delete(spending)
    }
}
