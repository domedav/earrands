package com.domedav.ballanceometer

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domedav.ballanceometer.data.BallanceDatabase
import com.domedav.ballanceometer.data.BallanceRepository
import com.domedav.ballanceometer.data.BankAccount
import com.domedav.ballanceometer.data.Spending
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
import java.util.UUID

/**
 * Instrumented tests for multi bank account tracking.
 * Runs on device/emulator with an in-memory Room database.
 */
@RunWith(AndroidJUnit4::class)
class BankAccountsRepositoryTest {

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

    private suspend fun accounts() = repo.bankAccounts.first()
    private suspend fun balanceOf(id: String) = accounts().first { it.id == id }.balance

    @Test
    fun firstAccount_becomesMainAutomatically() = runTest {
        repo.addAccount(BankAccount(name = "OTP", balance = 100_000.0))
        val all = accounts()
        assertEquals(1, all.size)
        assertTrue(all[0].isMain)
    }

    @Test
    fun secondAccount_isNotMainUntilSet() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0))
        repo.addAccount(BankAccount(id = "a2", name = "Revolut", balance = 50_000.0))
        assertTrue(accounts().first { it.id == "a1" }.isMain)

        repo.setMainAccount("a2")
        val all = accounts()
        assertTrue(all.first { it.id == "a2" }.isMain)
        assertTrue(all.none { it.id == "a1" && it.isMain })
    }

    @Test
    fun spending_withoutAccount_fallsBackToMainAndDecreasesIt() = runTest {
        repo.addAccount(BankAccount(id = "main", name = "OTP", balance = 100_000.0))
        repo.addSpending(Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L))

        val spendings = repo.spendings.first()
        assertEquals(1, spendings.size)
        assertEquals("main", spendings[0].accountId)
        assertEquals(95_000.0, balanceOf("main"), 0.0)
    }

    @Test
    fun spending_withExplicitAccount_onlyDecreasesThatOne() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0))
        repo.addAccount(BankAccount(id = "a2", name = "Revolut", balance = 50_000.0))
        repo.addSpending(
            Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L, accountId = "a2")
        )

        assertEquals(100_000.0, balanceOf("a1"), 0.0)
        assertEquals(45_000.0, balanceOf("a2"), 0.0)
    }

    @Test
    fun deleteSpending_refundsAccountBalance() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0))
        val spending = Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L, accountId = "a1")
        repo.addSpending(spending)
        assertEquals(95_000.0, balanceOf("a1"), 0.0)

        // deleteSpending reads the stored copy (with resolved accountId)
        repo.deleteSpending(repo.spendings.first()[0])
        assertEquals(100_000.0, balanceOf("a1"), 0.0)
        assertTrue(repo.spendings.first().isEmpty())
    }

    @Test
    fun topUp_increasesBalance() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0))
        repo.topUpAccount("a1", 300_000.0)
        assertEquals(400_000.0, balanceOf("a1"), 0.0)
    }

    @Test
    fun deleteNonMainAccount_reassignsSpendingsToMain() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0))
        repo.addAccount(BankAccount(id = "a2", name = "Revolut", balance = 50_000.0))
        repo.addSpending(
            Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L, accountId = "a2")
        )

        repo.deleteAccount(accounts().first { it.id == "a2" })

        val remaining = accounts()
        assertEquals(1, remaining.size)
        assertEquals("a1", remaining[0].id)
        val spendings = repo.spendings.first()
        assertEquals(1, spendings.size)
        assertEquals("a1", spendings[0].accountId)
    }

    @Test
    fun deleteMainAccount_promotesOldestRemainingToMain() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0, createdAt = 1L))
        repo.addAccount(BankAccount(id = "a2", name = "Revolut", balance = 50_000.0, createdAt = 2L))
        repo.addSpending(
            Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L)
        )

        repo.deleteAccount(accounts().first { it.id == "a1" })

        val remaining = accounts()
        assertEquals(1, remaining.size)
        assertEquals("a2", remaining[0].id)
        assertTrue(remaining[0].isMain)
        assertEquals("a2", repo.spendings.first()[0].accountId)
    }

    @Test
    fun deleteLastAccount_spendingsSurviveWithoutAccount() = runTest {
        repo.addAccount(BankAccount(id = "a1", name = "OTP", balance = 100_000.0))
        repo.addSpending(
            Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L)
        )

        repo.deleteAccount(accounts()[0])

        assertTrue(accounts().isEmpty())
        val spendings = repo.spendings.first()
        assertEquals(1, spendings.size)
        assertNull(spendings[0].accountId)
    }

    @Test
    fun spending_withoutAnyAccount_isStoredWithNullAccount() = runTest {
        repo.addSpending(Spending(id = UUID.randomUUID().toString(), amount = 5_000.0, timestamp = 0L))
        val spendings = repo.spendings.first()
        assertEquals(1, spendings.size)
        assertNull(spendings[0].accountId)
        assertNotNull(spendings[0].id)
    }
}
