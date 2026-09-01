package com.domedav.ballanceometer.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domedav.ballanceometer.R
import com.domedav.ballanceometer.data.Recurrence
import com.domedav.ballanceometer.data.Subtask
import com.domedav.ballanceometer.data.TaskGroup

@Composable
fun ConfigScreen(viewModel: ConfigViewModel = viewModel()) {
    val config by viewModel.config.collectAsStateWithLifecycle(initialValue = null)
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val subtasks by viewModel.subtasks.collectAsStateWithLifecycle()

    var showAddGroup by remember { mutableStateOf(false) }
    var expandedGroupId by remember { mutableStateOf<String?>(null) }
    var subtaskDialogGroupId by remember { mutableStateOf<String?>(null) }
    var editingSubtask by remember { mutableStateOf<Subtask?>(null) }

    // config editing states — autosave, no Save button
    var totalBalanceText by remember { mutableStateOf("") }
    var minimalSpendText by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }

    if (config != null && !initialized) {
        totalBalanceText = if (config!!.totalBalance == 0.0) "" else config!!.totalBalance.toString()
        minimalSpendText = if (config!!.minimalSpend == 0.0) "" else config!!.minimalSpend.toString()
        initialized = true
    }

    fun parseOrNull(s: String): Double? {
        if (s.isEmpty()) return 0.0
        return s.replace(',', '.').toDoubleOrNull()
    }

    // Periodikus save: minden másodpercben ha változott és nem ír a user, + ellépéskor save
    LaunchedEffect(totalBalanceText, minimalSpendText) {
        if (!initialized) return@LaunchedEffect
        delay(1000)
        val t = parseOrNull(totalBalanceText) ?: return@LaunchedEffect
        val m = parseOrNull(minimalSpendText) ?: return@LaunchedEffect
        if (t != config?.totalBalance || m != config?.minimalSpend) {
            viewModel.saveConfig(t, m, config?.currency ?: "HUF")
        }
    }

    DisposableEffect(totalBalanceText, minimalSpendText) {
        onDispose {
            val t = parseOrNull(totalBalanceText)
            val m = parseOrNull(minimalSpendText)
            if (t != null && m != null && (t != config?.totalBalance || m != config?.minimalSpend)) {
                viewModel.saveConfig(t, m, config?.currency ?: "HUF")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.balance_config_title), style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = totalBalanceText,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            totalBalanceText = filtered
                        },
                        label = { Text(stringResource(R.string.total_balance_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0") }
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = minimalSpendText,
                        onValueChange = {
                            val filtered = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            minimalSpendText = filtered
                        },
                        label = { Text(stringResource(R.string.minimal_spend_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("0") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.currency_label, config?.currency ?: "HUF"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.groups_title), style = MaterialTheme.typography.titleMedium)
                }
                FilledTonalButton(onClick = { showAddGroup = true }, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(stringResource(R.string.add_group))
                }
            }
        }

        items(groups, key = { it.id }) { group ->
            val isExpanded = expandedGroupId == group.id
            val groupSubtasks = subtasks.filter { it.groupId == group.id }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(String.format("%.1f", group.weight), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        IconButton(onClick = { expandedGroupId = if (isExpanded) null else group.id }) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
                        Slider(
                            value = group.weight,
                            onValueChange = { newVal ->
                                viewModel.updateGroup(group.copy(weight = newVal.coerceIn(0f, 100f)))
                            },
                            valueRange = 0f..100f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { subtaskDialogGroupId = group.id }, shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text(stringResource(R.string.subtask_title))
                        }
                        OutlinedButton(
                            onClick = { viewModel.deleteGroup(group) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 4.dp), tint = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (isExpanded) {
                        Spacer(Modifier.height(12.dp))
                        if (groupSubtasks.isEmpty()) {
                            Text(stringResource(R.string.no_subtasks), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                groupSubtasks.forEach { subtask ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(subtask.title, style = MaterialTheme.typography.bodyMedium)
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(recurrenceDisplayName(subtask.recurrence)) }
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { editingSubtask = subtask }) {
                                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary)
                                            }
                                            IconButton(onClick = { viewModel.deleteSubtask(subtask) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGroup) {
        AddGroupDialog(
            onDismiss = { showAddGroup = false },
            onConfirm = { name, weight ->
                viewModel.addGroup(name, weight)
                showAddGroup = false
            }
        )
    }

    subtaskDialogGroupId?.let { gid ->
        AddSubtaskDialog(
            onDismiss = { subtaskDialogGroupId = null },
            onConfirm = { title, recurrence ->
                viewModel.addSubtask(gid, title, recurrence)
                subtaskDialogGroupId = null
            }
        )
    }

    editingSubtask?.let { st ->
        EditSubtaskDialog(
            subtask = st,
            onDismiss = { editingSubtask = null },
            onConfirm = { newTitle, newRecurrence ->
                viewModel.updateSubtask(st.copy(title = newTitle, recurrence = newRecurrence))
                editingSubtask = null
            }
        )
    }
}

@Composable
private fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(50f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_group)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.weight_label), style = MaterialTheme.typography.bodyMedium)
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                            Text(String.format("%.1f", weight), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Slider(
                        value = weight,
                        onValueChange = { weight = it.coerceIn(0f, 100f) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name.trim(), weight)
                }
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(Recurrence.ONCE.name) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_subtask)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                RecurrenceDropdown(
                    selected = recurrence,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onSelect = { recurrence = it; expanded = false }
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onConfirm(title.trim(), recurrence) }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun EditSubtaskDialog(
    subtask: Subtask,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(subtask.title) }
    var recurrence by remember { mutableStateOf(subtask.recurrence) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_subtask_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                RecurrenceDropdown(
                    selected = recurrence,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    onSelect = { recurrence = it; expanded = false }
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onConfirm(title.trim(), recurrence) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun recurrenceDisplayName(key: String): String {
    return when (key) {
        Recurrence.ONCE.name -> stringResource(R.string.recurrence_once)
        Recurrence.DAILY.name -> stringResource(R.string.recurrence_daily)
        Recurrence.WEEKLY.name -> stringResource(R.string.recurrence_weekly)
        Recurrence.MONTHLY.name -> stringResource(R.string.recurrence_monthly)
        else -> key
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceDropdown(
    selected: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(Recurrence.ONCE.name, Recurrence.DAILY.name, Recurrence.WEEKLY.name, Recurrence.MONTHLY.name)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = recurrenceDisplayName(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.recurrence_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(recurrenceDisplayName(opt)) },
                    onClick = { onSelect(opt) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
