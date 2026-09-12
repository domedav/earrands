package com.domedav.ballanceometer

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domedav.ballanceometer.data.AppConfig
import com.domedav.ballanceometer.data.BallanceDatabase
import com.domedav.ballanceometer.data.BallanceRepository
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the core task/unlock flow:
 * config, groups, subtasks, completions (incl. FK cascades and dedup).
 */
@RunWith(AndroidJUnit4::class)
class TaskFlowRepositoryTest {

    private lateinit var db: BallanceDatabase
    private lateinit var repo: BallanceRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            BallanceDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = BallanceRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun config_saveAndRead() = runTest {
        assertNull(repo.config.first())
        repo.saveConfig(AppConfig(id = 1, totalBalance = 400_000.0, minimalSpend = 200_000.0, currency = "HUF"))
        val cfg = repo.config.first()
        assertNotNull(cfg)
        assertEquals(400_000.0, cfg!!.totalBalance, 0.0)
        assertEquals(200_000.0, cfg.minimalSpend, 0.0)
        assertEquals("HUF", cfg.currency)
    }

    @Test
    fun group_crud() = runTest {
        val group = TaskGroup(id = "g1", name = "Health", weight = 50f)
        repo.addGroup(group)
        assertEquals(listOf(group), repo.groups.first())

        repo.updateGroup(group.copy(weight = 75f))
        assertEquals(75f, repo.groups.first()[0].weight)

        repo.deleteGroup(group.copy(weight = 75f))
        assertTrue(repo.groups.first().isEmpty())
    }

    @Test
    fun subtask_crud() = runTest {
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 50f))
        val sub = Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L)
        repo.addSubtask(sub)
        assertEquals(listOf(sub), repo.subtasks.first())
        assertEquals(listOf(sub), repo.subtasksByGroup("g1").first())
        assertTrue(repo.subtasksByGroup("other").first().isEmpty())

        repo.updateSubtask(sub.copy(title = "Swim"))
        assertEquals("Swim", repo.subtasks.first()[0].title)

        repo.deleteSubtask(sub.copy(title = "Swim"))
        assertTrue(repo.subtasks.first().isEmpty())
    }

    @Test
    fun completeSubtask_createsCompletionWithEngineValue() = runTest {
        repo.saveConfig(AppConfig(id = 1, totalBalance = 310_000.0, minimalSpend = 0.0, currency = "HUF"))
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 100f))
        // January has 31 days; DAILY single subtask -> earnable/31
        repo.addSubtask(Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L))

        val completion = repo.completeSubtask("s1", "2026-01-15")
        assertNotNull(completion)
        assertEquals(310_000.0 / 31, completion!!.valueUnlocked, 0.001)
        assertEquals(1, repo.completions.first().size)
        assertEquals(1, repo.completionsByDate("2026-01-15").first().size)
        assertTrue(repo.completionsByDate("2026-01-16").first().isEmpty())
    }

    @Test
    fun completeSubtask_duplicateSameDayReturnsNull() = runTest {
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 100f))
        repo.addSubtask(Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L))

        assertNotNull(repo.completeSubtask("s1", "2026-09-12"))
        assertNull(repo.completeSubtask("s1", "2026-09-12"))
        assertEquals(1, repo.completions.first().size)

        // ...but a different day is a new instance
        assertNotNull(repo.completeSubtask("s1", "2026-09-13"))
        assertEquals(2, repo.completions.first().size)
    }

    @Test
    fun completeSubtask_unknownIdsReturnNull() = runTest {
        assertNull(repo.completeSubtask("nope", "2026-09-12"))
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 100f))
        assertNull(repo.completeSubtask("nope", "2026-09-12"))
    }

    @Test
    fun completeSubtask_withoutConfig_unlocksZero() = runTest {
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 100f))
        repo.addSubtask(Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L))
        val completion = repo.completeSubtask("s1", "2026-09-12")
        assertNotNull(completion)
        assertEquals(0.0, completion!!.valueUnlocked, 0.0)
    }

    @Test
    fun deleteSubtask_cascadesCompletions() = runTest {
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 100f))
        val sub = Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L)
        repo.addSubtask(sub)
        repo.completeSubtask("s1", "2026-09-12")
        assertEquals(1, repo.completions.first().size)

        repo.deleteSubtask(sub)
        assertTrue(repo.completions.first().isEmpty())
    }

    @Test
    fun deleteGroup_cascadesSubtasksAndCompletions() = runTest {
        val group = TaskGroup(id = "g1", name = "Health", weight = 100f)
        repo.addGroup(group)
        repo.addSubtask(Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L))
        repo.completeSubtask("s1", "2026-09-12")

        repo.deleteGroup(group)
        assertTrue(repo.subtasks.first().isEmpty())
        assertTrue(repo.completions.first().isEmpty())
    }

    @Test
    fun deleteCompletion_removesUnlock() = runTest {
        repo.addGroup(TaskGroup(id = "g1", name = "Health", weight = 100f))
        repo.addSubtask(Subtask(id = "s1", groupId = "g1", title = "Run", recurrence = "DAILY", createdAt = 0L))
        repo.completeSubtask("s1", "2026-09-12")
        assertEquals(1, repo.completions.first().size)

        repo.deleteCompletion(repo.completions.first()[0])
        assertTrue(repo.completions.first().isEmpty())
    }
}
