package com.domedav.ballanceometer.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.domedav.ballanceometer.BallanceometerApp
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class ToggleSubtaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val subtaskId = parameters[SUBTASK_ID_KEY] ?: return
        val app = context.applicationContext as? BallanceometerApp ?: return
        val today = LocalDate.now().toString()
        // toggle: if already completed today delete, else complete — mirrors ShowViewModel
        val existing = app.repository.completions.first().firstOrNull { it.subtaskId == subtaskId && it.date == today }
        if (existing != null) {
            app.repository.deleteCompletion(existing)
        } else {
            app.repository.completeSubtask(subtaskId, today)
        }
        BallanceWidget().updateAll(context)
    }

    companion object {
        val SUBTASK_ID_KEY = ActionParameters.Key<String>("subtask_id")
    }
}
