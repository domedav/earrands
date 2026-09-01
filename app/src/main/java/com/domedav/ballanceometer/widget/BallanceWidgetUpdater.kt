package com.domedav.ballanceometer.widget

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object BallanceWidgetUpdater {
    private const val IMMEDIATE_WORK = "ballance_widget_immediate"
    private const val PERIODIC_WORK = "ballance_widget_periodic"

    fun enqueueImmediate(ctx: Context) {
        // Direct Glance update for instant feedback (same as widget toggle), plus WorkManager as fallback
        try {
            // launch direct update without blocking caller
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try { BallanceWidget().updateAll(ctx) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        val request = OneTimeWorkRequestBuilder<BallanceWidgetWorker>().build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueuePeriodic(ctx: Context) {
        val request = PeriodicWorkRequestBuilder<BallanceWidgetWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork(IMMEDIATE_WORK)
        wm.cancelUniqueWork(PERIODIC_WORK)
    }
}
