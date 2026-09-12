package com.domedav.ballanceometer

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.domedav.ballanceometer.data.AppConfig
import com.domedav.ballanceometer.data.BankAccount
import com.domedav.ballanceometer.data.ExportImportHelper
import com.domedav.ballanceometer.data.Spending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for JSON export/import (org.json needs the Android runtime).
 */
@RunWith(AndroidJUnit4::class)
class ExportImportHelperTest {

    @Test
    fun exportImport_roundTripsBankAccountsAndSpendingLinks() {
        val accounts = listOf(
            BankAccount(id = "a1", name = "OTP", balance = 95_000.0, isMain = true, createdAt = 1L),
            BankAccount(id = "a2", name = "Revolut", balance = 45_000.0, isMain = false, createdAt = 2L)
        )
        val spendings = listOf(
            Spending(id = "s1", amount = 5_000.0, note = "food", timestamp = 10L, accountId = "a1"),
            Spending(id = "s2", amount = 1_000.0, note = "", timestamp = 11L, accountId = null)
        )
        val config = AppConfig(id = 1, totalBalance = 400_000.0, minimalSpend = 200_000.0, currency = "HUF")

        val json = ExportImportHelper.buildExportJson(
            config, emptyList(), emptyList(), emptyList(), spendings, accounts
        )
        val parsed = ExportImportHelper.parseImportJson(json.toString())

        assertEquals(2, parsed.bankAccounts.size)
        assertEquals("a1", parsed.bankAccounts.first { it.isMain }.id)
        assertEquals(95_000.0, parsed.bankAccounts.first { it.id == "a1" }.balance, 0.0)
        assertEquals("a1", parsed.spendings.first { it.id == "s1" }.accountId)
        assertNull(parsed.spendings.first { it.id == "s2" }.accountId)
        assertEquals(400_000.0, parsed.config!!.totalBalance, 0.0)
    }

    @Test
    fun parseV1Export_defaultsToEmptyAccountsAndNullAccountId() {
        // v1 format: no bankAccounts array, no accountId on spendings
        val json = """
            {
              "version": 1,
              "appConfig": {"id": 1, "totalBalance": 100.0, "minimalSpend": 50.0, "currency": "HUF"},
              "taskGroups": [],
              "subtasks": [],
              "completions": [],
              "spendings": [{"id": "s1", "amount": 10.0, "note": "", "timestamp": 5}]
            }
        """.trimIndent()

        val parsed = ExportImportHelper.parseImportJson(json)

        assertTrue(parsed.bankAccounts.isEmpty())
        assertEquals(1, parsed.spendings.size)
        assertNull(parsed.spendings[0].accountId)
    }
}
