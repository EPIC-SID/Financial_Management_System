package com.example.financeapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.example.financeapp.data.Resource
import com.example.financeapp.data.SmsParser
import com.example.financeapp.data.TransactionData
import com.example.financeapp.data.TransactionRepository
import com.example.financeapp.ui.components.AppBottomBar
import com.example.financeapp.ui.components.Tab
import com.example.financeapp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var transactions by remember { mutableStateOf<List<TransactionData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSyncing by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
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
    var selectedPeriod by remember { mutableStateOf("Current Month") }

    // Dialog state
    var showDurationDialog by remember { mutableStateOf(false) }

    // SharedPreferences to track sync state
    val sharedPrefs = remember { context.getSharedPreferences("RupAiPrefs", Context.MODE_PRIVATE) }

    // Check SMS permissions
    val permission = Manifest.permission.READ_SMS
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Load transactions from database on start
    LaunchedEffect(Unit) {
        isLoading = true
        val result = TransactionRepository.getTransactions()
        when (result) {
            is Resource.Success -> transactions = result.data
            is Resource.Error -> Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            else -> { }
        }
        isLoading = false
    }

    // Helper: Perform SMS inbox query and parsing
    fun performSmsSync(startTime: Long) {
        isSyncing = true
        coroutineScope.launch {
            try {
                val parsedTxns = withContext(Dispatchers.IO) {
                    val list = mutableListOf<TransactionData>()
                    val uri = Uri.parse("content://sms/inbox")
                    val projection = arrayOf("_id", "body", "date", "address")
                    val selection = "date >= ?"
                    val selectionArgs = arrayOf(startTime.toString())
                    
                    val cursor = context.contentResolver.query(
                        uri,
                        projection,
                        selection,
                        selectionArgs,
                        "date DESC"
                    )
                    
                    cursor?.use { c ->
                        val idCol = c.getColumnIndexOrThrow("_id")
                        val bodyCol = c.getColumnIndexOrThrow("body")
                        val dateCol = c.getColumnIndexOrThrow("date")
                        val addressCol = c.getColumnIndexOrThrow("address")
                        
                        while (c.moveToNext()) {
                            val id = c.getString(idCol)
                            val body = c.getString(bodyCol)
                            val date = c.getLong(dateCol)
                            
                            val parsed = SmsParser.parse(body, id, date)
                            if (parsed != null) {
                                list.add(parsed)
                            }
                        }
                    }
                    list
                }

                if (parsedTxns.isNotEmpty()) {
                    val insertResult = TransactionRepository.insertTransactions(parsedTxns)
                    if (insertResult is Resource.Error) {
                        Toast.makeText(context, insertResult.message, Toast.LENGTH_LONG).show()
                    }
                    // Refresh from database
                    val refreshResult = TransactionRepository.getTransactions()
                    if (refreshResult is Resource.Success) {
                        transactions = refreshResult.data
                    }
                    Toast.makeText(context, "Synced ${parsedTxns.size} transactions!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No new transaction messages found.", Toast.LENGTH_SHORT).show()
                }
                
                // Save last sync time
                sharedPrefs.edit().putLong("last_sms_sync_time", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isSyncing = false
            }
        }
    }

    // Launcher for runtime permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                showDurationDialog = true
            } else {
                Toast.makeText(context, "Permission denied. Cannot sync transactions.", Toast.LENGTH_LONG).show()
            }
        }
    )

    fun startSyncFlow() {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            showDurationDialog = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Dynamic scan duration setup dialog
    if (showDurationDialog) {
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            shape = RectangleShape,
            containerColor = BackgroundWhite,
            title = { Text("Sync Transaction History", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select how far back we should scan your SMS inbox for transaction messages:", color = TextGray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val options = listOf(
                        "Last 7 Days",
                        "Last 30 Days",
                        "Current Month",
                        "All Time"
                    )
                    
                    options.forEach { label ->
                        Button(
                            onClick = {
                                showDurationDialog = false
                                selectedPeriod = label
                                val startTime = when (label) {
                                    "Last 7 Days" -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                                    "Last 30 Days" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
                                    "Current Month" -> {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)
                                        cal.timeInMillis
                                    }
                                    else -> 0L // All Time
                                }
                                displayStartTime = startTime
                                performSmsSync(startTime)
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
                ) {
                    Text("CANCEL")
                }
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
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = BronzePrimary, strokeWidth = 2.dp)
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
                        Tab.HOME -> onNavigateBack()
                        Tab.PROFILE -> onNavigateToProfile()
                        else -> { }
                    }
                }
            )
        }
    ) { paddingValues ->
        val filteredTransactions = remember(transactions, selectedCategoryFilter, displayStartTime) {
            val byCategory = when (selectedCategoryFilter) {
                "All" -> transactions
                "Income" -> transactions.filter { it.amount > 0 }
                "Expenses" -> transactions.filter { it.amount < 0 }
                "Food" -> transactions.filter { it.category == "FOOD" }
                else -> transactions
            }
            if (displayStartTime > 0L) {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                byCategory.filter {
                    try {
                        val date = format.parse(it.transactionDate)
                        date != null && date.time >= displayStartTime
                    } catch (e: Exception) {
                        true
                    }
                }
            } else {
                byCategory
            }
        }

        val groupedTransactions = remember(filteredTransactions, selectedPeriod) {
            filteredTransactions.groupBy { 
                if (selectedPeriod == "All Time") {
                    formatTransactionMonth(it.transactionDate)
                } else {
                    formatTransactionDate(it.transactionDate)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Section
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Recent Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Review your latest spending and income.", fontSize = 14.sp, color = TextGray)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Filter tabs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filters = listOf("All", "Income", "Expenses", "Food")
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedCategoryFilter == filter,
                            text = filter,
                            onClick = { selectedCategoryFilter = filter }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BronzePrimary)
                }
            } else if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No Transactions Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextBlack
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap the refresh icon in the top right to sync transaction SMS messages from your device.",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    groupedTransactions.forEach { (date, txnsForDate) ->
                        item {
                            DateHeader(date.uppercase())
                        }
                        items(txnsForDate) { txn ->
                            TransactionListItem(
                                merchant = txn.merchant,
                                amount = if (txn.amount > 0) {
                                    "+ ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)}"
                                } else {
                                    "- ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(kotlin.math.abs(txn.amount))}"
                                },
                                category = txn.category,
                                time = formatTransactionTime(txn.transactionDate),
                                isPositive = txn.amount > 0
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper formatting functions
fun formatTransactionDate(dateStr: String): String {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr) ?: return dateStr
        
        val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        displayFormat.format(date)
    } catch (e: Exception) {
        dateStr.take(10)
    }
}

fun formatTransactionMonth(dateStr: String): String {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr) ?: return dateStr
        
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
        displayFormat.format(date)
    } catch (e: Exception) {
        dateStr.take(7)
    }
}

fun formatTransactionTime(dateStr: String): String {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoParser.parse(dateStr) ?: return ""
        
        val displayFormat = SimpleDateFormat("hh:mm a", Locale.US)
        displayFormat.format(date)
    } catch (e: Exception) {
        ""
    }
}

@Composable
fun FilterChip(selected: Boolean, text: String, onClick: () -> Unit) {
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
fun TransactionListItem(merchant: String, amount: String, category: String, time: String, isPositive: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (isPositive) TealPositive.copy(alpha = 0.2f) else SurfaceGray, RectangleShape)
                    .border(1.dp, if (isPositive) TealPositive else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(category.take(1), fontWeight = FontWeight.Bold, color = if (isPositive) TealPositive else TextGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(merchant, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.border(1.dp, SurfaceGray).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text(category, fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(time, fontSize = 12.sp, color = TextGray)
                }
            }
        }
        Text(amount, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (isPositive) TealPositive else TextBlack)
    }
    Divider(color = SurfaceGray, thickness = 1.dp)
}
