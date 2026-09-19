package com.domedav.ballanceometer.ui.show

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.domedav.ballanceometer.R
import com.domedav.ballanceometer.data.BankAccount
import com.domedav.ballanceometer.data.Spending
import com.domedav.ballanceometer.ui.accounts.AccountsSheet
import com.domedav.ballanceometer.ui.components.BalanceMeter

@Composable
fun ShowScreen(
    viewModel: ShowViewModel = viewModel()
) {
    val config by viewModel.config.collectAsState()
    val spendings by viewModel.spendings.collectAsState()
    val todayTodos by viewModel.todayTodos.collectAsState()
    val available by viewModel.available.collectAsState()
    val unlocked by viewModel.unlocked.collectAsState()
    val earnable by viewModel.earnable.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val bankAccounts by viewModel.bankAccounts.collectAsState()
    val mainAccount by viewModel.mainAccount.collectAsState()

    val minimal = config?.minimalSpend ?: 0.0
    val total = config?.totalBalance ?: 0.0
    val currency = config?.currency ?: "HUF"

    var showAddSpending by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val noGroupLabel = stringResource(R.string.no_subtasks)
    val grouped = remember(todayTodos) {
        todayTodos.groupBy { it.groupName ?: noGroupLabel }.toSortedMap()
    }
    val previewSpendings = remember(spendings) {
        spendings.sortedByDescending { it.timestamp }.take(8)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BalanceMeter(
                available = available,
                unlocked = unlocked,
                earnable = earnable,
                minimal = minimal,
                total = total,
                currency = currency,
                spent = spendings.sumOf { it.amount },
                hasAccounts = bankAccounts.isNotEmpty(),
                onAccountsClick = { showAccounts = true }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TaskAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.today_title),
                    style = MaterialTheme.typography.titleMedium
                )
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.counter_fraction, todayTodos.count { it.isCompletedToday }, todayTodos.size)) },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer)
                )
            }
        }

        if (todayTodos.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = stringResource(R.string.today_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            grouped.forEach { (groupName, todos) ->
                item {
                    Text(
                        text = "— $groupName —",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                items(todos, key = { it.subtask.id }) { todo ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (todo.isCompletedToday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (todo.isCompletedToday) 0.dp else 1.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.toggleSubtask(todo.subtask) }) {
                                Icon(
                                    imageVector = if (todo.isCompletedToday) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (todo.isCompletedToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(text = todo.subtask.title, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = stringResource(R.string.todo_value_positive, todo.value, currency),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.spendings_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                FilledTonalButton(
                    onClick = { showAddSpending = true },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text(text = stringResource(R.string.subtract_action))
                }
            }
        }

        if (spendings.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = stringResource(R.string.no_spendings),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(previewSpendings, key = { it.id }) { spending ->
                SpendingRow(
                    spending = spending,
                    currency = currency,
                    onDelete = { viewModel.deleteSpending(spending) }
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = { showHistory = true }) {
                        Text(text = stringResource(R.string.all_spendings_title))
                    }
                }
            }
        }
    }

    if (showAddSpending) {
        AddSpendingDialog(
            currency = currency,
            accounts = bankAccounts,
            mainAccountId = mainAccount?.id,
            onDismiss = { showAddSpending = false },
            onConfirm = { amount, note, accountId ->
                viewModel.addSpending(amount, note, accountId)
                showAddSpending = false
            }
        )
    }

    if (showAccounts) {
        AccountsSheet(
            accounts = bankAccounts,
            currency = currency,
            onDismiss = { showAccounts = false },
            onAdd = { name, balance -> viewModel.addAccount(name, balance) },
            onUpdate = { account -> viewModel.updateAccount(account) },
            onDelete = { account -> viewModel.deleteAccount(account) },
            onSetMain = { id -> viewModel.setMainAccount(id) },
            onTopUp = { id, amount -> viewModel.topUpAccount(id, amount) }
        )
    }

    if (showHistory) {
        SpendingHistorySheet(
            viewModel = viewModel,
            currency = currency,
            onDismiss = { showHistory = false }
        )
    }
}

@Composable
internal fun SpendingRow(
    spending: Spending,
    currency: String,
    onDelete: () -> Unit,
    accountName: String? = null
) {
    val dateTime = remember(spending.timestamp) {
        java.time.Instant.ofEpochMilli(spending.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("MM.dd HH:mm"))
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.spending_value_negative, spending.amount, currency),
                    style = MaterialTheme.typography.titleSmall
                )
                if (spending.note.isBlank()) {
                    Text(
                        text = dateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = spending.note + " • " + dateTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (accountName != null) {
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete_spending_desc),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSpendingDialog(
    currency: String,
    accounts: List<BankAccount>,
    mainAccountId: String?,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val amountErrorText = stringResource(R.string.amount_error)

    // Default to the main account; fall back to the first one
    var selectedAccountId by remember(accounts, mainAccountId) {
        mutableStateOf(mainAccountId ?: accounts.firstOrNull()?.id)
    }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.id == selectedAccountId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.add_spending_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text(stringResource(R.string.amount_with_currency, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(stringResource(R.string.note_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (accounts.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = accountMenuExpanded,
                        onExpandedChange = { accountMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedAccount?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.account_for_spending)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = accountMenuExpanded,
                            onDismissRequest = { accountMenuExpanded = false }
                        ) {
                            accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (account.isMain) "${account.name} ★"
                                            else account.name
                                        )
                                    },
                                    onClick = {
                                        selectedAccountId = account.id
                                        accountMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        error = amountErrorText
                        return@TextButton
                    }
                    onConfirm(amount, noteText.trim(), selectedAccountId)
                }
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}
