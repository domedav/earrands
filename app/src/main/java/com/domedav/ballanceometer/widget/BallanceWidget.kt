package com.domedav.ballanceometer.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.domedav.ballanceometer.BallanceometerApp
import com.domedav.ballanceometer.MainActivity
import com.domedav.ballanceometer.R
import com.domedav.ballanceometer.domain.BalanceEngine
import com.domedav.ballanceometer.ui.theme.BallanceometerGlanceTheme
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

class BallanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? BallanceometerApp
        if (app == null) {
            provideContent {
                BallanceometerGlanceTheme {
                    WidgetContentError()
                }
            }
            return
        }
        // Snapshot via first() - not reactive collectAsState
        val config = app.repository.config.first()
        val groups = app.repository.groups.first()
        val subtasks = app.repository.subtasks.first()
        val completions = app.repository.completions.first()
        val spendings = app.repository.spendings.first()

        val minimal = config?.minimalSpend ?: 0.0
        val total = config?.totalBalance ?: 0.0
        val currency = config?.currency ?: "HUF"
        val earnable = if (config != null) maxOf(0.0, total - minimal) else 0.0
        val unlocked = BalanceEngine.unlockedTotal(completions)
        val available = BalanceEngine.available(minimal, unlocked, spendings)
        val progress = if (earnable > 0) (unlocked / earnable).coerceIn(0.0, 1.0) else 0.0

        val today = LocalDate.now().toString()
        val ym = YearMonth.now()
        val completedTodayIds = completions.filter { it.date == today }.map { it.subtaskId }.toSet()

        val todos = subtasks.map { st ->
            val group = groups.find { it.id == st.groupId }
            val value = if (group != null) {
                BalanceEngine.valuePerInstance(
                    group = group,
                    subtask = st,
                    allGroups = groups,
                    allSubtasks = subtasks,
                    year = ym.year,
                    month = ym.monthValue,
                    earnable = earnable
                )
            } else 0.0
            TodoEntry(
                id = st.id,
                title = st.title,
                isCompleted = completedTodayIds.contains(st.id),
                value = value
            )
        }.sortedWith(compareBy({ it.isCompleted }, { it.title }))

        provideContent {
            BallanceometerGlanceTheme {
                WidgetContent(
                    available = available,
                    currency = currency,
                    progress = progress,
                    unlocked = unlocked,
                    earnable = earnable,
                    todos = todos
                )
            }
        }
    }
}

@GlanceComposable
@Composable
private fun WidgetContentError() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(GlanceTheme.colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = context.getString(R.string.widget_error),
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        )
    }
}

@GlanceComposable
@Composable
private fun WidgetContent(
    available: Double,
    currency: String,
    progress: Double,
    unlocked: Double,
    earnable: Double,
    todos: List<TodoEntry>
) {
    val context = LocalContext.current
    val openMain = Intent(context, MainActivity::class.java)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(GlanceTheme.colors.surface)
            .padding(16.dp)
    ) {
        // Header with available balance
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.available_balance),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
                Text(
                    text = context.getString(R.string.available_value, available, currency),
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(20.dp)
                    .background(GlanceTheme.colors.primary)
                    .clickable(actionStartActivity(openMain)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_add),
                    contentDescription = context.getString(R.string.open_app),
                    modifier = GlanceModifier.size(22.dp),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary)
                )
            }
        }

        // Progress bar - accurate segmented meter (10% steps, Glance limitation)
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp).background(GlanceTheme.colors.surfaceVariant)
        ) {
            val blocks = 10
            val filledBlocks = (progress * blocks).toInt().coerceIn(0, blocks)
            val remainderFilled = ((progress * blocks * 10).toInt() % 10) >= 5 // half-block rounding visual not needed
            for (i in 0 until blocks) {
                Box(
                    modifier = GlanceModifier.defaultWeight().height(8.dp).background(
                        if (i < filledBlocks) GlanceTheme.colors.primary else GlanceTheme.colors.surfaceVariant
                    ).cornerRadius(4.dp)
                ) {}
            }
        }
        Text(
            text = context.getString(R.string.widget_progress, unlocked, earnable, currency, progress * 100),
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            ),
            modifier = GlanceModifier.padding(top = 4.dp, bottom = 8.dp)
        )

        if (todos.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight().padding(top = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.widget_empty),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            LazyColumn(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            ) {
                items(todos) { todo ->
                    TodoRow(todo = todo, isLast = todo.id == todos.lastOrNull()?.id)
                }
            }
        }
    }
}

private data class TodoEntry(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
    val value: Double
)



@GlanceComposable
@Composable
private fun TodoRow(todo: TodoEntry, isLast: Boolean) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight().padding(start = 4.dp)) {
                Text(
                    text = todo.title,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (todo.isCompleted) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
            }
            if (!todo.isCompleted) {
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primaryContainer)
                        .clickable(
                            actionRunCallback<ToggleSubtaskAction>(
                                actionParametersOf(ToggleSubtaskAction.SUBTASK_ID_KEY to todo.id)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check),
                        contentDescription = LocalContext.current.getString(R.string.widget_description),
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimaryContainer)
                    )
                }
            } else {
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_check),
                        contentDescription = null,
                        modifier = GlanceModifier.size(18.dp),
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant)
                    )
                }
            }
        }
        if (!isLast) {
            Box(
                modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(GlanceTheme.colors.surfaceVariant)
            ) {}
        }
    }
}
