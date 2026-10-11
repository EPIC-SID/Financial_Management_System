package com.example.financeapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financeapp.ui.theme.NumberCompact
import com.example.financeapp.ui.theme.Spacing
import com.example.financeapp.ui.viewmodel.AccountSummaryUi

@Composable
fun AccountsRow(accounts: List<AccountSummaryUi>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("Accounts", style = MaterialTheme.typography.titleLarge)
            TypeTag(StatementType.FACT)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            accounts.forEach { account ->
                Surface(onClick = onClick, modifier = Modifier.widthIn(min = 112.dp).heightIn(min = 72.dp), shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(Modifier.padding(Spacing.sm), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        Text(account.label, style = MaterialTheme.typography.bodyMedium)
                        Text(account.amount ?: "Add", style = NumberCompact)
                    }
                }
            }
        }
    }
}
