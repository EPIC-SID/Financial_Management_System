package com.example.financeapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.data.TransactionViewModel
import com.example.financeapp.data.formatTransactionDate
import com.example.financeapp.data.formatTransactionMonth
import com.example.financeapp.data.formatTransactionTime
import com.example.financeapp.ui.components.AppBottomBar
import com.example.financeapp.ui.components.Tab
import com.example.financeapp.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe live data from ViewModel (backed by Room DB)
    val transactions by transactionViewModel.transactions.collectAsState()
    val isSyncing by transactionViewModel.isSyncing.collectAsState()
    val syncMessage by transactionViewModel.syncMessage.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedPeriod by remember { mutableStateOf("Current Month") }
    var displayStartTime by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    var showDurationDialog by remember { mutableStateOf(false) }

    // Pre-load caches on first composition
    LaunchedEffect(Unit) {
        transactionViewModel.loadCaches(context)
    }

    // Show toast whenever a sync message arrives
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            transactionViewModel.clearSyncMessage()
        }
    }

    // SMS permission handling
    val permission = Manifest.permission.READ_SMS
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) showDurationDialog = true
            else Toast.makeText(context, "Permission denied. Cannot sync transactions.", Toast.LENGTH_LONG).show()
        }
    )

    fun startSyncFlow() {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            showDurationDialog = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Duration selection dialog
    if (showDurationDialog) {
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            shape = RectangleShape,
            containerColor = BackgroundWhite,
            title = { Text("Sync Transaction History", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Select how far back we should scan your SMS inbox:",
                        color = TextGray, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf("Last 7 Days", "Last 30 Days", "Current Month", "All Time").forEach { label ->
                        Button(
                            onClick = {
                                showDurationDialog = false
                                selectedPeriod = label
                                val startTime = when (label) {
                                    "Last 7 Days"   -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                                    "Last 30 Days"  -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
                                    "Current Month" -> {
                                        Calendar.getInstance().apply {
                                            set(Calendar.DAY_OF_MONTH, 1)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }
                                    else -> 0L // All Time
                                }
                                // Update both the view filter AND trigger the SMS sync
                                displayStartTime = startTime
                                transactionViewModel.syncSms(startTime)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary),
                            shape = RectangleShape
                        ) {
                            Text(label, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showDurationDialog = false },
                    border = BorderStroke(1.dp, BorderBlack),
                    shape = RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack)
                ) { Text("CANCEL") }
            }
        )
    }


    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.border(1.dp, BronzePrimary).padding(4.dp)) {
                            Text("₹", color = BronzePrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RupAI", fontWeight = FontWeight.Bold, color = BronzePrimary, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { startSyncFlow() }, enabled = !isSyncing) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = BronzePrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Sync SMS", tint = BronzePrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedTab = Tab.TRANSACTIONS,
                onTabSelected = { tab ->
                    when (tab) {
                        Tab.HOME    -> onNavigateBack()
                        Tab.PROFILE -> onNavigateToProfile()
                        else        -> {}
                    }
                }
            )
        }
    ) { paddingValues ->

        // Filter logic (pure in-memory, no DB call needed)
        val filteredTransactions = remember(transactions, selectedCategoryFilter, displayStartTime) {
            val byCategory = when (selectedCategoryFilter) {
                "All"           -> transactions
                "Income"        -> transactions.filter { it.amount > 0 }
                "Food"          -> transactions.filter { it.category == "FOOD" }
                "Travel"        -> transactions.filter { it.category == "TRAVEL" }
                "Shopping"      -> transactions.filter { it.category == "SHOPPING" }
                "Bills"         -> transactions.filter { it.category == "BILLS" }
                "Entertainment" -> transactions.filter { it.category == "ENTERTAINMENT" }
                "Health"        -> transactions.filter { it.category == "HEALTH" }
                "Education"     -> transactions.filter { it.category == "EDUCATION" }
                else            -> transactions
            }
            if (displayStartTime > 0L) {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                byCategory.filter {
                    try {
                        val date = format.parse(it.transactionDate)
                        date != null && date.time >= displayStartTime
                    } catch (e: Exception) { true }
                }
            } else byCategory
        }

        val groupedTransactions = remember(filteredTransactions, selectedPeriod) {
            filteredTransactions.groupBy {
                if (selectedPeriod == "All Time") formatTransactionMonth(it.transactionDate)
                else formatTransactionDate(it.transactionDate)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    // Clickable period badge — shows current time window, tap to change
                    Box(
                        modifier = Modifier
                            .border(1.dp, BronzePrimary)
                            .clickable { startSyncFlow() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = selectedPeriod,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BronzePrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Showing: $selectedPeriod", fontSize = 14.sp, color = TextGray)
                Spacer(modifier = Modifier.height(24.dp))

                // Category filter chips
                val filters = listOf("All", "Income", "Food", "Travel", "Shopping", "Bills", "Entertainment", "Health", "Education")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        CategoryFilterChip(
                            selected = selectedCategoryFilter == filter,
                            text = filter,
                            onClick = { selectedCategoryFilter = filter }
                        )
                    }
                }
            }


            if (transactions.isEmpty() && !isSyncing) {
                // Empty state
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No Transactions Found", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextBlack)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the refresh icon in the top right to sync transaction SMS messages from your device.",
                            color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isSyncing && transactions.isEmpty()) {
                // First-time loading indicator
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BronzePrimary)
                }
            } else {
                var selectedTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

                // Transaction detail dialog
                selectedTransaction?.let { txn ->
                    val amountFormatted = if (txn.amount > 0) {
                        "+ ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)}"
                    } else {
                        "- ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(kotlin.math.abs(txn.amount))}"
                    }
                    AlertDialog(
                        onDismissRequest = { selectedTransaction = null },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = BackgroundWhite,
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Transaction Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextBlack)
                                IconButton(onClick = { selectedTransaction = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                                }
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (txn.amount > 0) TealPositive.copy(alpha = 0.1f)
                                            else BronzePrimary.copy(alpha = 0.08f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(amountFormatted, fontWeight = FontWeight.Bold, fontSize = 28.sp,
                                        color = if (txn.amount > 0) TealPositive else TextBlack)
                                }
                                DetailRow("Merchant", txn.merchant)
                                DetailRow("Category", txn.category)
                                DetailRow("Date", formatTransactionDate(txn.transactionDate))
                                DetailRow("Time", formatTransactionTime(txn.transactionDate))
                                if (!txn.description.isNullOrBlank()) {
                                    Divider(color = SurfaceGray)
                                    Text("SMS Snippet", fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                                    Text(txn.description, fontSize = 12.sp, color = TextGray, lineHeight = 18.sp)
                                }
                            }
                        },
                        confirmButton = {}
                    )
                }

                if (groupedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No transactions found for \"$selectedCategoryFilter\".",
                            color = TextGray, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        groupedTransactions.forEach { (date, txnsForDate) ->
                            item { DateHeader(date.uppercase()) }
                            items(txnsForDate, key = { it.smsId }) { txn ->
                                TransactionEntityListItem(
                                    txn = txn,
                                    onClick = { selectedTransaction = txn }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionEntityListItem(txn: TransactionEntity, onClick: () -> Unit) {
    val isPositive = txn.amount > 0
    val amountStr = if (isPositive) {
        "+ ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)}"
    } else {
        "- ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(kotlin.math.abs(txn.amount))}"
    }

    val iconBg = when (txn.category) {
        "FOOD"          -> Color(0xFFE8F5E9)
        "TRAVEL"        -> Color(0xFFE3F2FD)
        "SHOPPING"      -> Color(0xFFF3E5F5)
        "BILLS"         -> Color(0xFFFFF8E1)
        "ENTERTAINMENT" -> Color(0xFFFCE4EC)
        "INVESTMENTS"   -> Color(0xFFE8F5E9)
        "HEALTH"        -> Color(0xFFE0F7FA)
        "EDUCATION"     -> Color(0xFFFFF3E0)
        "INCOME"        -> TealPositive.copy(alpha = 0.15f)
        else            -> SurfaceGray
    }
    val iconEmoji = when (txn.category) {
        "FOOD"          -> "🍽"; "TRAVEL"        -> "🚗"; "SHOPPING" -> "🛍"
        "BILLS"         -> "⚡"; "ENTERTAINMENT" -> "🎬"; "INVESTMENTS" -> "📈"
        "HEALTH"        -> "💊"; "EDUCATION"     -> "📚"; "INCOME" -> "💰"
        else            -> txn.merchant.take(1).uppercase()
    }
    val pillBg = when (txn.category) {
        "FOOD"          -> Color(0xFFE8F5E9); "TRAVEL"    -> Color(0xFFE3F2FD)
        "SHOPPING"      -> Color(0xFFF3E5F5); "BILLS"     -> Color(0xFFFFF8E1)
        "ENTERTAINMENT" -> Color(0xFFFCE4EC); "INVESTMENTS" -> Color(0xFFE8F5E9)
        "HEALTH"        -> Color(0xFFE0F7FA); "EDUCATION" -> Color(0xFFFFF3E0)
        "INCOME"        -> Color(0xFFE8F5E9); else        -> SurfaceGray
    }
    val pillText = when (txn.category) {
        "FOOD"          -> Color(0xFF2E7D32); "TRAVEL"    -> Color(0xFF1565C0)
        "SHOPPING"      -> Color(0xFF6A1B9A); "BILLS"     -> Color(0xFFF57F17)
        "ENTERTAINMENT" -> Color(0xFFC62828); "INVESTMENTS" -> Color(0xFF1B5E20)
        "HEALTH"        -> Color(0xFF006064); "EDUCATION" -> Color(0xFFE65100)
        "INCOME"        -> Color(0xFF2E7D32); else        -> TextGray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text(iconEmoji, fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(txn.merchant, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(pillBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text(txn.category, fontSize = 10.sp, color = pillText, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatTransactionTime(txn.transactionDate), fontSize = 12.sp, color = TextGray)
                }
            }
        }
        Text(amountStr, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            color = if (isPositive) TealPositive else TextBlack)
    }
    Divider(color = SurfaceGray, thickness = 1.dp)
}

@Composable
fun CategoryFilterChip(selected: Boolean, text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) BronzePrimary else BorderBlack, RectangleShape)
            .background(if (selected) BronzePrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else TextBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DateHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceGray)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGray)
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextGray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
    }
}
