package com.example.financeapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.AccountDetailUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsSheet(details: List<AccountDetailUi>, cashAfterBills: String, onDismiss: () -> Unit, onEdit: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text("Accounts", style = MaterialTheme.typography.headlineMedium)
            details.forEach { detail ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(detail.heading.substringBefore(" ·"), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        TypeTag(StatementType.FACT)
                        TextButton(onClick = onEdit) { Text("Edit") }
                    }
                    if (detail.isEmpty) Text("Nothing added. Add an account to include it.", style = MaterialTheme.typography.bodyMedium)
                    detail.lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (detail.isStale) Text("Update this account to refresh the forecast.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("Cash after card bills · $cashAfterBills", style = MaterialTheme.typography.titleMedium)
        }
    }
}
