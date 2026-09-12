package com.domedav.ballanceometer.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.domedav.ballanceometer.R
import com.domedav.ballanceometer.data.BankAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSheet(
    accounts: List<BankAccount>,
    currency: String,
    onDismiss: () -> Unit,
    onAdd: (name: String, balance: Double) -> Unit,
    onUpdate: (account: BankAccount) -> Unit,
    onDelete: (account: BankAccount) -> Unit,
    onSetMain: (accountId: String) -> Unit,
    onTopUp: (accountId: String, amount: Double) -> Unit
) {
    var showAccountDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<BankAccount?>(null) }
    var topUpAccount by remember { mutableStateOf<BankAccount?>(null) }
    var deleteTarget by remember { mutableStateOf<BankAccount?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.accounts_title),
                    style = MaterialTheme.typography.titleLarge
                )
                if (accounts.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            R.string.accounts_total_value,
                            accounts.sumOf { it.balance },
                            currency
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { editingAccount = null; showAccountDialog = true },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text(stringResource(R.string.add_account))
            }
            Spacer(Modifier.height(16.dp))

            if (accounts.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.no_accounts),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(accounts, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            currency = currency,
                            onSetMain = { onSetMain(account.id) },
                            onEdit = { editingAccount = account; showAccountDialog = true },
                            onDelete = { deleteTarget = account },
                            onTopUp = { topUpAccount = account }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }

    if (showAccountDialog) {
        AccountDialog(
            initial = editingAccount,
            currency = currency,
            onDismiss = { showAccountDialog = false; editingAccount = null },
            onConfirm = { name, balance ->
                if (editingAccount != null) {
                    onUpdate(editingAccount!!.copy(name = name, balance = balance))
                } else {
                    onAdd(name, balance)
                }
                showAccountDialog = false
                editingAccount = null
            }
        )
    }

    if (topUpAccount != null) {
        TopUpDialog(
            accountName = topUpAccount!!.name,
            currency = currency,
            onDismiss = { topUpAccount = null },
            onConfirm = { amount ->
                onTopUp(topUpAccount!!.id, amount)
                topUpAccount = null
            }
        )
    }

    if (deleteTarget != null) {
        val isLast = accounts.size <= 1
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_account_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (isLast) R.string.delete_last_account_confirm_msg
                        else R.string.delete_account_confirm_msg
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(deleteTarget!!); deleteTarget = null }) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
private fun AccountCard(
    account: BankAccount,
    currency: String,
    onSetMain: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTopUp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isMain)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (account.isMain) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = if (account.isMain) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (account.isMain) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.account_balance_value,
                            account.balance,
                            currency
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (account.isMain) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onSetMain,
                    enabled = !account.isMain,
                    label = { Text(stringResource(R.string.main_account)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (account.isMain) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (account.isMain)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                        labelColor = if (account.isMain)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.14f),
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = if (account.isMain) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_account_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Button(
                onClick = onTopUp,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Filled.Savings,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(stringResource(R.string.topup_action))
            }
        }
    }
}

@Composable
private fun AccountDialog(
    initial: BankAccount?,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balance: Double) -> Unit
) {
    var nameText by remember { mutableStateOf(initial?.name ?: "") }
    var balanceText by remember {
        mutableStateOf(
            if (initial != null && initial.balance != 0.0) initial.balance.toString() else ""
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    val amountErrorText = stringResource(R.string.amount_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial != null) R.string.edit_account_title else R.string.add_account
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text(stringResource(R.string.account_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = {
                        balanceText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                    },
                    label = { Text(stringResource(R.string.account_balance_label, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0") }
                )
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
            TextButton(onClick = {
                val balance = if (balanceText.isEmpty()) 0.0
                else balanceText.replace(',', '.').toDoubleOrNull()
                if (nameText.isBlank() || balance == null) {
                    error = amountErrorText
                    return@TextButton
                }
                onConfirm(nameText.trim(), balance)
            }) {
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

@Composable
private fun TopUpDialog(
    accountName: String,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val amountErrorText = stringResource(R.string.amount_error)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.topup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        error = null
                    },
                    label = { Text(stringResource(R.string.amount_with_currency, currency)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
            TextButton(onClick = {
                val amount = amountText.replace(',', '.').toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    error = amountErrorText
                    return@TextButton
                }
                onConfirm(amount)
            }) {
                Text(stringResource(R.string.topup_action))
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
