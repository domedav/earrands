package com.domedav.ballanceometer.ui.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.domedav.ballanceometer.BallanceometerApp
import com.domedav.ballanceometer.data.AppConfig
import com.domedav.ballanceometer.data.BallanceDatabase
import com.domedav.ballanceometer.data.Completion
import com.domedav.ballanceometer.data.ExportImportHelper
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.widget.BallanceWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class Period {
    DAILY, WEEKLY, MONTHLY
}

data class Bucket(
    val label: String,
    val date: LocalDate,
    val income: Double,
    val spending: Double,
    val net: Double
)

private val Context.dataStore by preferencesDataStore(name = "ballanceometer_prefs")

class DataViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as BallanceometerApp).repository
    private val db: BallanceDatabase = BallanceDatabase.getInstance(application)
    private val dataStore = application.dataStore
    private val periodKey = stringPreferencesKey("data_period")

    val savedPeriod: StateFlow<Period> = dataStore.data
        .map { prefs -> try { Period.valueOf(prefs[periodKey] ?: Period.DAILY.name) } catch (_: Exception) { Period.DAILY } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Period.DAILY)

    fun savePeriod(period: Period) {
        viewModelScope.launch { dataStore.edit { it[periodKey] = period.name } }
    }

    val config: StateFlow<AppConfig?> = repository.config
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val completions: StateFlow<List<Completion>> = repository.completions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spendings: StateFlow<List<Spending>> = repository.spendings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalIncome: StateFlow<Double> = completions
        .map { list -> list.sumOf { it.valueUnlocked } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalSpending: StateFlow<Double> = spendings
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalNet: StateFlow<Double> = combine(totalIncome, totalSpending) { inc, spend -> inc - spend }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun buckets(period: Period): StateFlow<List<Bucket>> {
        return combine(completions, spendings) { compList, spendList ->
            buildBuckets(period, compList, spendList)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    private fun buildBuckets(
        period: Period,
        compList: List<Completion>,
        spendList: List<Spending>
    ): List<Bucket> {
        // Group income by LocalDate.parse(completion.date)
        val incomeByDate = mutableMapOf<LocalDate, Double>()
        for (c in compList) {
            val d = try {
                LocalDate.parse(c.date)
            } catch (_: Exception) {
                continue
            }
            incomeByDate[d] = (incomeByDate[d] ?: 0.0) + c.valueUnlocked
        }
        // Group spending by Instant.ofEpochMilli(timestamp).atZone(systemDefault).toLocalDate()
        val spendingByDate = mutableMapOf<LocalDate, Double>()
        val zone = ZoneId.systemDefault()
        for (s in spendList) {
            val d = Instant.ofEpochMilli(s.timestamp).atZone(zone).toLocalDate()
            spendingByDate[d] = (spendingByDate[d] ?: 0.0) + s.amount
        }

        return when (period) {
            Period.DAILY -> buildDailyBuckets(incomeByDate, spendingByDate)
            Period.WEEKLY -> buildWeeklyBuckets(incomeByDate, spendingByDate)
            Period.MONTHLY -> buildMonthlyBuckets(incomeByDate, spendingByDate)
        }
    }

    private fun buildDailyBuckets(
        incomeByDate: Map<LocalDate, Double>,
        spendingByDate: Map<LocalDate, Double>
    ): List<Bucket> {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM.dd")
        val result = mutableListOf<Bucket>()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val income = incomeByDate[date] ?: 0.0
            val spending = spendingByDate[date] ?: 0.0
            val label = date.format(formatter)
            result.add(Bucket(label = label, date = date, income = income, spending = spending, net = income - spending))
        }
        return result
    }

    private fun buildWeeklyBuckets(
        incomeByDate: Map<LocalDate, Double>,
        spendingByDate: Map<LocalDate, Double>
    ): List<Bucket> {
        val today = LocalDate.now()
        val latestMonday = today.with(DayOfWeek.MONDAY)
        val startMonday = latestMonday.minusWeeks(3)
        val formatter = DateTimeFormatter.ofPattern("MM.dd")
        val result = mutableListOf<Bucket>()
        for (w in 0 until 4) {
            val weekStart = startMonday.plusWeeks(w.toLong())
            val weekEnd = weekStart.plusDays(6)
            var income = 0.0
            var spending = 0.0
            var d = weekStart
            while (!d.isAfter(weekEnd)) {
                income += incomeByDate[d] ?: 0.0
                spending += spendingByDate[d] ?: 0.0
                d = d.plusDays(1)
            }
            val label = weekStart.format(formatter)
            result.add(Bucket(label = label, date = weekStart, income = income, spending = spending, net = income - spending))
        }
        return result
    }

    private fun buildMonthlyBuckets(
        incomeByDate: Map<LocalDate, Double>,
        spendingByDate: Map<LocalDate, Double>
    ): List<Bucket> {
        val current = YearMonth.now()
        val labelFormatter = DateTimeFormatter.ofPattern("yy.MM")
        val result = mutableListOf<Bucket>()
        for (i in 5 downTo 0) {
            val ym = current.minusMonths(i.toLong())
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            var income = 0.0
            var spending = 0.0
            var d = start
            while (!d.isAfter(end)) {
                income += incomeByDate[d] ?: 0.0
                spending += spendingByDate[d] ?: 0.0
                d = d.plusDays(1)
            }
            val label = ym.format(labelFormatter)
            result.add(Bucket(label = label, date = start, income = income, spending = spending, net = income - spending))
        }
        return result
    }

    suspend fun exportToUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val cfg = repository.config.first()
            val groups = repository.groups.first()
            val subtasks = repository.subtasks.first()
            val completionsList = repository.completions.first()
            val spendingsList = repository.spendings.first()
            val accountsList = repository.bankAccounts.first()

            val json = ExportImportHelper.buildExportJson(cfg, groups, subtasks, completionsList, spendingsList, accountsList)
            val jsonString = json.toString(2)

            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return@withContext false

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return@withContext false

            val parsed = ExportImportHelper.parseImportJson(jsonString)

            // Use db from repository to clear and repopulate inside transaction
            // Simplest: clearAllTables then insert
            // clearAllTables must not be called on main thread - we are on IO
            db.clearAllTables()

            // Insert in order respecting FKs: config, groups, subtasks, completions, spendings
            if (parsed.config != null) {
                db.appConfigDao().upsert(parsed.config)
            }
            for (g in parsed.groups) {
                db.taskGroupDao().insert(g)
            }
            for (s in parsed.subtasks) {
                db.subtaskDao().insert(s)
            }
            for (c in parsed.completions) {
                db.completionDao().insert(c)
            }
            for (s in parsed.spendings) {
                db.spendingDao().insert(s)
            }
            for (a in parsed.bankAccounts) {
                db.bankAccountDao().insert(a)
            }

            BallanceWidgetUpdater.enqueueImmediate(context.applicationContext)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
