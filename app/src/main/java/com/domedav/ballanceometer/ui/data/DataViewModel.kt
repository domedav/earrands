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
