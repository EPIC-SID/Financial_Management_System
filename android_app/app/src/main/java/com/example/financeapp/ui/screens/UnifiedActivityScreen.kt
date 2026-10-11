package com.example.financeapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.CategorizationPolicy
import com.example.financeapp.data.RemoteCategorizationPolicy
import com.example.financeapp.data.TransactionViewModel
import com.example.financeapp.data.ingest.SyntheticDataset
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.util.formatInr
import com.example.financeapp.ui.viewmodel.*
import java.time.*

@Composable
fun UnifiedActivityRoute(mode: AppDataMode, dataset: SyntheticDataset?, showHeader: Boolean = true) {
    val context = LocalContext.current
    val live: TransactionViewModel? = if (mode == AppDataMode.LIVE) viewModel() else null
    val rows = live?.transactions?.collectAsState()?.value.orEmpty()
    val syncing = live?.isSyncing?.collectAsState()?.value ?: false
    val message = live?.syncMessage?.collectAsState()?.value
    val entries = remember(mode, rows, dataset) {
        if (mode == AppDataMode.LIVE) ActivityData.live(rows) else dataset?.let(ActivityData::demo).orEmpty()
    }
    val today = if (mode == AppDataMode.DEMO) dataset?.demoDate?.minusDays(1) ?: LocalDate.now()
        else LocalDate.now(ZoneId.of("Asia/Kolkata"))
    val prefs = remember { context.getSharedPreferences(RemoteCategorizationPolicy.PREFS, Context.MODE_PRIVATE) }
    var remote by remember { mutableStateOf(prefs.getBoolean(RemoteCategorizationPolicy.ENABLED, false)) }
    var settings by remember { mutableStateOf(false) }
    var consent by remember { mutableStateOf(false) }
    var syncRange by remember { mutableStateOf(false) }
    var permissionNotice by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) syncRange = true else permissionNotice = true
    }
    fun requestSync() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) syncRange = true
        else permissionNotice = true
    }
    UnifiedActivityScreen(mode, entries, today, syncing, message,
        onSync = ::requestSync, onSettings = { settings = true },
        onCorrect = { merchant, category -> live?.updateCategoryForMerchant(merchant, category) }, showHeader = showHeader)

    if (permissionNotice) AlertDialog(onDismissRequest = { permissionNotice = false },
        title = { Text("Read bank transaction SMS?") },
        text = { Text("Android SMS access lets RupAI scan the selected date range for bank alerts and store parsed transactions on this phone. Cloud categorization has separate consent. Personal messages and raw SMS are not sent to the AI provider.") },
        confirmButton = { TextButton(onClick = { permissionNotice = false; permission.launch(Manifest.permission.READ_SMS) }) { Text("Continue") } },
        dismissButton = { TextButton(onClick = { permissionNotice = false }) { Text("Not now") } })
    if (syncRange) AlertDialog(onDismissRequest = { syncRange = false }, title = { Text("Sync SMS") },
        text = { Column {
            listOf("Last 7 days" to 7L, "Last 30 days" to 30L, "All history" to null).forEach { (label, days) ->
                TextButton(onClick = {
                    syncRange = false
                    live?.syncSms(days?.let { System.currentTimeMillis() - it * 86_400_000L } ?: 0L)
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(label) }
            }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { syncRange = false }) { Text("Cancel") } })
    if (settings && live != null) AlertDialog(onDismissRequest = { settings = false }, title = { Text("Activity settings") },
        text = { Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cloud categorization", Modifier.weight(1f))
                Switch(remote, onCheckedChange = {
                    if (it) consent = true else { remote = false; prefs.edit().putBoolean(RemoteCategorizationPolicy.ENABLED, false).apply() }
                })
            }
            Text("Local categorization stays available when cloud sharing is off.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { live.recheckLocalCategories(); settings = false }) { Text("Recheck categories locally") }
            if (remote) TextButton(onClick = { live.retryPendingCategorization(); settings = false }) { Text("Retry cloud categorization") }
        } }, confirmButton = { TextButton(onClick = { settings = false }) { Text("Done") } })
    if (consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("Share merchant labels?") },
        text = { Text("Merchant labels will be sent to the authenticated backend and its AI provider. Raw SMS, transaction amounts and account numbers are not included. You can turn this off in Activity settings.") },
        confirmButton = { TextButton(onClick = {
            prefs.edit().putBoolean(RemoteCategorizationPolicy.ENABLED, true).apply()
            remote = true; consent = false; live?.retryPendingCategorization()
        }) { Text("Enable cloud sharing") } },
        dismissButton = { TextButton(onClick = { consent = false }) { Text("Keep local") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedActivityScreen(mode: AppDataMode, entries: List<ActivityEntry>, today: LocalDate,
    syncing: Boolean = false, message: String? = null, onSync: () -> Unit = {},
    onSettings: () -> Unit = {}, onCorrect: (String, String) -> Unit = { _, _ -> }, showHeader: Boolean = true) {
    var period by remember { mutableStateOf(ActivityPeriod.MONTH) }
    var category by remember { mutableStateOf("All") }
    var periodMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ActivityEntry?>(null) }
    var correctedCategory by remember { mutableStateOf<String?>(null) }
    var allCategories by remember { mutableStateOf(false) }
    val window = remember(entries, period, today) { ActivityData.filter(entries, period, today) }
    val summary = remember(window) { ActivityData.summary(window) }
    val categories = remember(window) { window.map { it.category }.distinct().sorted() }
    LaunchedEffect(categories) { if (category != "All" && category !in categories) category = "All" }
    val filtered = remember(window, category) { window.filter { category == "All" || it.category == category } }
    Scaffold(contentWindowInsets = if (showHeader) ScaffoldDefaults.contentWindowInsets else WindowInsets(0, 0, 0, 0),
        topBar = { if (showHeader) TopAppBar(title = { Text("Transactions") }, actions = {
        if (mode == AppDataMode.LIVE) {
            IconButton(onClick = onSync, enabled = !syncing) {
                if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh, "Sync SMS")
            }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Activity settings") }
        }
    }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (mode == AppDataMode.DEMO) "Sample transactions through $today" else "SMS transactions saved on this phone", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    if (!showHeader && mode == AppDataMode.LIVE) {
                        IconButton(onClick = onSync, enabled = !syncing) {
                            if (syncing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, "Import bank SMS")
                        }
                        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Transaction settings") }
                    }
                }
                Box {
                    TextButton(onClick = { periodMenu = true }) { Text(if (period == ActivityPeriod.MONTH) today.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${today.year}" else period.label) }
                    DropdownMenu(periodMenu, { periodMenu = false }) {
                        ActivityPeriod.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { period = option; periodMenu = false }) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) { Text("Money in", style = MaterialTheme.typography.labelMedium); Text(formatInr(summary.credits), style = MaterialTheme.typography.titleLarge) }
                    Column(Modifier.weight(1f)) { Text("Money out", style = MaterialTheme.typography.labelMedium); Text(formatInr(summary.debits), style = MaterialTheme.typography.titleLarge) }
                }
                Text("These totals include transfers, refunds and card bill payments. Money out is not the same as total spending.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (summary.categories.isNotEmpty()) item {
                Text("Money out by category", style = MaterialTheme.typography.titleMedium)
                val ranked = summary.categories.entries.sortedByDescending { it.value }
                (if (allCategories) ranked else ranked.take(3)).forEach { (label, amount) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(categoryLabel(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(formatInr(amount), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (ranked.size > 3) TextButton(onClick = { allCategories = !allCategories }) {
                    Text(if (allCategories) "Fewer categories" else "All categories")
                }
                if (summary.unclassified > 0) Text("${summary.unclassified} payments need a category", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            message?.let { item { Text(it, style = MaterialTheme.typography.bodySmall) } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("All") + categories).forEach { label -> FilterChip(selected = category == label,
                    onClick = { category = label }, label = { Text(categoryLabel(label)) }) }
            } }
            if (filtered.isEmpty()) item {
                Text(if (entries.isEmpty()) "No transactions yet" else "No transactions match these filters", style = MaterialTheme.typography.titleMedium)
                if (mode == AppDataMode.LIVE && entries.isEmpty()) Button(onClick = onSync, enabled = !syncing) { Text("Sync bank SMS") }
            }
            filtered.groupBy { it.date }.forEach { (date, group) ->
                item { Text(date?.toString() ?: "Date unavailable", style = MaterialTheme.typography.labelLarge) }
                items(group, key = { it.id }) { entry ->
                    Row(Modifier.fillMaxWidth().clickable { selected = entry; correctedCategory = null }.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.merchant, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                            Text(categoryLabel(entry.category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text((if (entry.amount > 0) "+" else "") + formatInr(entry.amount), style = MaterialTheme.typography.titleSmall)
                    }
                    Divider()
                }
            }
        }
    }
    selected?.let { entry -> AlertDialog(onDismissRequest = { selected = null },
        title = { Text(entry.merchant) }, text = { Column {
            Text(formatInr(entry.amount)); Text(entry.date?.toString() ?: "Date unavailable")
            Text(categoryLabel(entry.category))
            if (mode == AppDataMode.LIVE) {
                Text("Category for this merchant", style = MaterialTheme.typography.labelLarge)
                Box {
                    var open by remember { mutableStateOf(false) }
                    TextButton(onClick = { open = true }) { Text(categoryLabel(correctedCategory ?: entry.category)) }
                    DropdownMenu(open, { open = false }) { CategorizationPolicy.VALID_CATEGORIES.sorted().forEach { label ->
                        DropdownMenuItem(text = { Text(categoryLabel(label)) }, onClick = { correctedCategory = label; open = false })
                    } }
                }
                Text("Applies locally to all saved transactions with this merchant label.", style = MaterialTheme.typography.bodySmall)
            }
        } }, confirmButton = { TextButton(onClick = {
            correctedCategory?.let { onCorrect(entry.merchant, it) }; selected = null
        }) { Text(if (correctedCategory == null) "Done" else "Save category") } },
        dismissButton = { if (correctedCategory != null) TextButton(onClick = { selected = null }) { Text("Cancel") } }) }
}

private fun categoryLabel(value: String) = if (value == "MISCELLANEOUS") "Unclassified" else value.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ActivityPreview() { FinanceAppTheme { UnifiedActivityScreen(AppDataMode.LIVE,
    listOf(ActivityEntry("sample", "Example shop", -264.0, "MISCELLANEOUS", LocalDate.of(2026, 10, 10))), LocalDate.of(2026, 10, 11)) } }
