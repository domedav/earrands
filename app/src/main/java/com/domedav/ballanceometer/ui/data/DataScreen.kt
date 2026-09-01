package com.domedav.ballanceometer.ui.data

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domedav.ballanceometer.R
import kotlinx.coroutines.launch

@Composable
fun DataScreen(
    viewModel: DataViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var period by remember { mutableStateOf(Period.DAILY) }

    val config by viewModel.config.collectAsState()
    val currency = config?.currency ?: "HUF"
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val totalNet by viewModel.totalNet.collectAsState()
    val buckets by viewModel.buckets(period).collectAsState()

    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var popupMessage by remember { mutableStateOf<String?>(null) }

    val importSuccessMsg = stringResource(R.string.import_success)
    val importFailedMsg = stringResource(R.string.widget_error)
    val exportSuccessMsg = stringResource(R.string.export_success)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = viewModel.exportToUri(context, uri)
                popupMessage = if (ok) exportSuccessMsg else importFailedMsg
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period selector
            item {
                PeriodSelector(
                    selected = period,
                    onSelected = { period = it }
                )
            }

            // Summary card
            item {
                SummaryCard(
                    income = totalIncome,
                    spending = totalSpending,
                    net = totalNet,
                    currency = currency
                )
            }

            // Chart card — compact
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val maxVal = buckets.maxOfOrNull { maxOf(it.income, it.spending) } ?: 0.0
                        if (buckets.isEmpty() || maxVal == 0.0) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_data),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            BarChart(
                                buckets = buckets,
                                period = period,
                                modifier = Modifier.fillMaxWidth().height(160.dp)
                            )
                        }
                    }
                }
            }

            // Buckets list rows
            items(buckets, key = { it.label + it.date.toString() }) { bucket ->
                BucketRow(
                    bucket = bucket,
                    currency = currency
                )
            }

            // Export / Import — compact
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { exportLauncher.launch("ballanceometer_backup.json") },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.export_action), style = MaterialTheme.typography.labelLarge)
                        }
                        FilledTonalButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.import_action), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    // Popup instead of Snackbar
    popupMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { popupMessage = null },
            title = { Text(text = msg) },
            confirmButton = {
                TextButton(onClick = { popupMessage = null }) { Text(stringResource(R.string.save)) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false; pendingImportUri = null },
            title = { Text(text = stringResource(R.string.import_confirm_title)) },
            text = { Text(text = stringResource(R.string.import_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImportUri
                    showImportDialog = false
                    pendingImportUri = null
                    if (uri != null) {
                        scope.launch {
                            val ok = viewModel.importFromUri(context, uri)
                            popupMessage = if (ok) importSuccessMsg else importFailedMsg
                        }
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false; pendingImportUri = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun PeriodSelector(
    selected: Period,
    onSelected: (Period) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = selected == Period.DAILY,
            onClick = { onSelected(Period.DAILY) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            label = { Text(stringResource(R.string.period_daily), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        )
        SegmentedButton(
            selected = selected == Period.WEEKLY,
            onClick = { onSelected(Period.WEEKLY) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            label = { Text(stringResource(R.string.period_weekly), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        )
        SegmentedButton(
            selected = selected == Period.MONTHLY,
            onClick = { onSelected(Period.MONTHLY) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            label = { Text(stringResource(R.string.period_monthly), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
        )
    }
}

@Composable
private fun SummaryCard(
    income: Double,
    spending: Double,
    net: Double,
    currency: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryColumn(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                label = stringResource(R.string.income),
                value = String.format("%.0f", income),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            SummaryColumn(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                label = stringResource(R.string.spending),
                value = String.format("%.0f", spending),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            SummaryColumn(
                icon = Icons.Filled.Wallet,
                label = stringResource(R.string.net),
                value = String.format("%.0f %s", net, currency),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BarChart(
    buckets: List<Bucket>,
    period: Period,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val tertiary = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Prepare X labels
    val xLabels = buckets.map { bucket ->
        when (period) {
            Period.DAILY -> bucket.label // already MM/dd, spec wants MM.dd - close enough, replace / with .
            Period.WEEKLY -> bucket.label
            Period.MONTHLY -> bucket.label
        }.replace("/", ".")
    }

    Canvas(modifier = modifier) {
        val maxValue = buckets.maxOfOrNull { maxOf(it.income, it.spending) } ?: 0.0
        if (maxValue == 0.0) return@Canvas

        val leftPadding = 28.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val topPadding = 12.dp.toPx()
        val bottomPadding = 28.dp.toPx()
        val labelAreaHeight = bottomPadding

        val chartLeft = leftPadding
        val chartRight = size.width - rightPadding
        val chartTop = topPadding
        val chartBottom = size.height - labelAreaHeight
        val chartHeight = chartBottom - chartTop
        val chartWidth = chartRight - chartLeft

        // Grid lines (4 horizontal)
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = chartTop + chartHeight * (1 - i.toFloat() / gridLines)
            drawLine(
                color = gridColor,
                start = Offset(chartLeft, y),
                end = Offset(chartRight, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val bucketCount = buckets.size
        if (bucketCount == 0) return@Canvas

        val gap = 6.dp.toPx()
        val bucketWidth = chartWidth / bucketCount
        val barWidth = ((bucketWidth - gap) / 2).coerceAtLeast(2.dp.toPx())
        val barCorner = 4.dp.toPx()

        buckets.forEachIndexed { index, bucket ->
            val bucketStart = chartLeft + index * bucketWidth
            val centerOffset = (bucketWidth - (barWidth * 2 + 4.dp.toPx())) / 2

            // Income bar
            val incomeHeight = (bucket.income / maxValue * chartHeight).toFloat()
            val incomeTop = chartBottom - incomeHeight
            val incomeLeft = bucketStart + centerOffset
            drawRoundRect(
                color = primary,
                topLeft = Offset(incomeLeft, incomeTop),
                size = Size(barWidth, incomeHeight.coerceAtLeast(0f)),
                cornerRadius = CornerRadius(barCorner, barCorner)
            )

            // Spending bar
            val spendingHeight = (bucket.spending / maxValue * chartHeight).toFloat()
            val spendingTop = chartBottom - spendingHeight
            val spendingLeft = incomeLeft + barWidth + 4.dp.toPx()
            drawRoundRect(
                color = error,
                topLeft = Offset(spendingLeft, spendingTop),
                size = Size(barWidth, spendingHeight.coerceAtLeast(0f)),
                cornerRadius = CornerRadius(barCorner, barCorner)
            )

            // Net dot (if net !=0, draw small circle on top of appropriate height interpolated)
            // Net could be negative; clamp to chart
            // We draw dot at net position relative to max, but only if within range
            // Use absolute? Just draw dot offset by net mapped to height, allow below zero as not drawn
            // Simpler: draw dot at (income - spending) normalized but shifted? We'll just place at max(income,spending) height with tertiary color if net !=0
            if (bucket.net != 0.0) {
                val netY = chartBottom - (bucket.net / maxValue * chartHeight).toFloat().coerceIn(-chartHeight.toFloat(), chartHeight.toFloat())
                // Only draw if within chart bounds
                if (netY in chartTop..chartBottom) {
                    val cx = bucketStart + bucketWidth / 2
                    drawCircle(
                        color = tertiary,
                        radius = 3.dp.toPx(),
                        center = Offset(cx, netY.coerceIn(chartTop, chartBottom))
                    )
                }
            }
        }

        // X labels: show subset to avoid crowding (e.g. every Nth)
        val labelStep = when {
            bucketCount <= 12 -> 1
            bucketCount <= 30 -> 5
            else -> 3
        }
        // Labels are drawn outside Canvas via overlay? We draw text manually using native? Keep simple: draw small rects as placeholder not needed
        // We skip text rendering in Canvas and rely on BucketRow list for full labels; but draw minimal ticks
        for (i in buckets.indices step labelStep) {
            val x = chartLeft + i * bucketWidth + bucketWidth / 2
            drawLine(
                color = onSurfaceVariant.copy(alpha = 0.4f),
                start = Offset(x, chartBottom),
                end = Offset(x, chartBottom + 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
    // X labels below canvas using Row
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 8.dp, top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val step = when {
            buckets.size <= 12 -> 1
            buckets.size <= 30 -> 5
            else -> 3
        }
        buckets.forEachIndexed { index, bucket ->
            if (index % step == 0 || index == buckets.lastIndex) {
                Text(
                    text = bucket.label.replace("/", "."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BucketRow(
    bucket: Bucket,
    currency: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = bucket.label.replace("/", "."),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+%.0f".format(bucket.income),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "-%.0f".format(bucket.spending),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "%+.0f %s".format(bucket.net, currency),
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        bucket.net > 0 -> MaterialTheme.colorScheme.primary
                        bucket.net < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.tertiary
                    }
                )
            }
        }
    }
}
