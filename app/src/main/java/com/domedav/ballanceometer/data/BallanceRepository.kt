package com.domedav.ballanceometer.data

import androidx.room.withTransaction
import com.domedav.ballanceometer.domain.BalanceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BallanceRepository(
    val db: BallanceDatabase
) {
    private val appConfigDao = db.appConfigDao()
    private val taskGroupDao = db.taskGroupDao()
    private val subtaskDao = db.subtaskDao()
    private val completionDao = db.completionDao()
    private val spendingDao = db.spendingDao()
    private val bankAccountDao = db.bankAccountDao()

    val config: Flow<AppConfig?> = appConfigDao.getConfig()
    val groups: Flow<List<TaskGroup>> = taskGroupDao.getAll()
    val subtasks: Flow<List<Subtask>> = subtaskDao.getAll()
    val completions: Flow<List<Completion>> = completionDao.getAll()
    val spendings: Flow<List<Spending>> = spendingDao.getAll()
    val bankAccounts: Flow<List<BankAccount>> = bankAccountDao.getAll()

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

    /**
     * Inserts a spending. If [spending.accountId] is null, falls back to the main
     * account. The effective account balance is decreased in the same transaction.
     * If there are no accounts, the spending is stored with null accountId.
     */
    suspend fun addSpending(spending: Spending) {
        db.withTransaction {
            val effectiveId = spending.accountId ?: bankAccountDao.getMain()?.id
            spendingDao.insert(spending.copy(accountId = effectiveId))
            if (effectiveId != null) {
                bankAccountDao.adjustBalance(effectiveId, -spending.amount)
            }
        }
    }

    /**
     * Deletes a spending and refunds the linked account balance.
     * Spendings without an account (none exist) only disappear.
     */
    suspend fun deleteSpending(spending: Spending) {
        db.withTransaction {
            spendingDao.delete(spending)
            if (spending.accountId != null) {
                bankAccountDao.adjustBalance(spending.accountId, spending.amount)
            }
        }
    }

    // ---- Bank accounts ----

    suspend fun addAccount(account: BankAccount) {
        db.withTransaction {
            val existing = bankAccounts.first()
            if (existing.isEmpty()) {
                // First account automatically becomes the main one
                bankAccountDao.insert(account.copy(isMain = true))
            } else {
                if (account.isMain) bankAccountDao.clearMain()
                bankAccountDao.insert(account)
            }
        }
    }

    suspend fun updateAccount(account: BankAccount) {
        db.withTransaction {
            if (account.isMain) bankAccountDao.clearMain()
            bankAccountDao.update(account)
        }
    }

    suspend fun setMainAccount(accountId: String) {
        db.withTransaction {
            val target = bankAccountDao.getById(accountId) ?: return@withTransaction
            bankAccountDao.clearMain()
            bankAccountDao.update(target.copy(isMain = true))
        }
    }

    /**
     * Manual top-up (e.g. monthly salary to the main account).
     * Increases the account balance; does not touch spending history.
     */
    suspend fun topUpAccount(accountId: String, amount: Double) {
        if (amount <= 0) return
        bankAccountDao.adjustBalance(accountId, amount)
    }

    /**
     * Deletes an account and reassigns its spendings to the main account
     * (or, if the deleted one was main, to the oldest remaining account).
     * If it was the last account, spendings keep a null accountId and
     * their balance effect is lost — there is nothing to track against.
     */
    suspend fun deleteAccount(account: BankAccount) {
        db.withTransaction {
            val others = bankAccounts.first().filter { it.id != account.id }
            val target = others.find { it.isMain } ?: others.firstOrNull()
            if (account.isMain && target != null) {
                bankAccountDao.clearMain()
                bankAccountDao.update(target.copy(isMain = true))
            }
            spendingDao.reassignAccount(account.id, target?.id)
            bankAccountDao.delete(account)
        }
    }
}
