package com.example.financeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.core.content.ContextCompat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.SupabaseClient
import com.example.financeapp.data.TransactionEntity
import com.example.financeapp.data.TransactionViewModel
import com.example.financeapp.data.UserProfile
import com.example.financeapp.data.UserRepository
import com.example.financeapp.data.Resource
import com.example.financeapp.data.formatTransactionDate
import com.example.financeapp.data.formatTransactionTime
import com.example.financeapp.ui.components.AppBottomBar
import com.example.financeapp.ui.components.Tab
import com.example.financeapp.ui.theme.*
import io.github.jan.supabase.gotrue.SessionStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe live transactions from Room DB via ViewModel (same instance as TransactionsScreen)
    val transactions by transactionViewModel.transactions.collectAsState()
    val isSyncing by transactionViewModel.isSyncing.collectAsState()
    val syncMessage by transactionViewModel.syncMessage.collectAsState()
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }
    var showDurationDialog by remember { mutableStateOf(false) }

    // Show toast whenever a sync message arrives
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            transactionViewModel.clearSyncMessage()
        }
    }

    // SMS permission + sync flow (mirrors TransactionsScreen logic)
    val permission = Manifest.permission.READ_SMS
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) showDurationDialog = true
            else Toast.makeText(context, "We need SMS permission to find your transactions. Please allow it!", Toast.LENGTH_LONG).show()
        }
    )

    fun startSyncFlow() {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            showDurationDialog = true
        } else {
            permissionLauncher.launch(permission)
        }
    }

    // Duration picker dialog
    if (showDurationDialog) {
        AlertDialog(
            onDismissRequest = { showDurationDialog = false },
            shape = androidx.compose.ui.graphics.RectangleShape,
            containerColor = com.example.financeapp.ui.theme.BackgroundWhite,
            title = { Text("How far back should we look? 🔍", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "We'll scan your SMS messages to find bank transactions.",
                        color = com.example.financeapp.ui.theme.TextGray, fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    listOf(
                        "Last 7 Days" to "Just the past week",
                        "Last 30 Days" to "Past month",
                        "Current Month" to "Since 1st of this month",
                        "All Time" to "Everything ever"
                    ).forEach { (label, subtitle) ->
                        Button(
                            onClick = {
                                showDurationDialog = false
                                val startTime = when (label) {
                                    "Last 7 Days" -> System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                                    "Last 30 Days" -> System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
                                    "Current Month" -> {
                                        java.util.Calendar.getInstance().apply {
                                            set(java.util.Calendar.DAY_OF_MONTH, 1)
                                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                                            set(java.util.Calendar.MINUTE, 0)
                                            set(java.util.Calendar.SECOND, 0)
                                            set(java.util.Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                    }
                                    else -> 0L
                                }
                                transactionViewModel.syncSms(startTime)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = com.example.financeapp.ui.theme.BronzePrimary),
                            shape = androidx.compose.ui.graphics.RectangleShape
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(label, fontWeight = FontWeight.Bold)
                                Text(subtitle, fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(
                    onClick = { showDurationDialog = false },
                    border = BorderStroke(1.dp, com.example.financeapp.ui.theme.BorderBlack),
                    shape = androidx.compose.ui.graphics.RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.financeapp.ui.theme.TextBlack)
                ) { Text("Not now") }
            }
        )
    }

    // Wait for Supabase session to be fully authenticated before fetching profile.
    // Avoids a race condition where getProfile() fires while session is still
    // being restored from SharedPreferences (LoadingFromStorage state).
    val sessionStatus by SupabaseClient.auth.sessionStatus.collectAsState()
    LaunchedEffect(sessionStatus) {
        if (sessionStatus is SessionStatus.Authenticated) {
            isLoadingProfile = true
            try {
                val profileResult = UserRepository.getProfile()
                if (profileResult is Resource.Success) {
                    profile = profileResult.data
                }
            } catch (e: Exception) {
                // Profile load failed silently — transactions still show
            } finally {
                isLoadingProfile = false
            }
        }
    }

    // ── Calculations ────────────────────────────────────────
    val monthlyIncomeValue = profile?.monthlyIncome ?: 0.0

    // Current month transactions only
    val currentMonthTxns = transactions.filter { isCurrentMonth(it.transactionDate) }
    val monthlyIn  = currentMonthTxns.filter { it.amount > 0 }.sumOf { it.amount }
    val monthlyOut = currentMonthTxns.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

    // Total Balance = Income this month - Spent this month
    // (Previously used all-time transaction sum which caused wildly negative balances
    //  when syncing months of SMS history against just one month of income.)
    val totalBalance = monthlyIncomeValue - monthlyOut

    // Savings insight
    val isIncomeSet = monthlyIncomeValue > 0.0
    val isOverBudget = totalBalance < 0.0
    val savingsPct = if (isIncomeSet && !isOverBudget) ((totalBalance / monthlyIncomeValue) * 100).toInt() else 0

    // Recent activity: 3 newest transactions from the current month
    // (Falls back to newest 3 overall if no current-month transactions exist yet)
    val recentTxns = currentMonthTxns.take(3).ifEmpty { transactions.take(3) }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("RupAI", fontWeight = FontWeight.Bold, color = BronzePrimary, fontSize = 20.sp)
                    }
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp).border(1.dp, BronzePrimary).padding(4.dp)) {
                        Text("₹", color = BronzePrimary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedTab = Tab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        Tab.TRANSACTIONS -> onNavigateToTransactions()
                        Tab.PROFILE -> onNavigateToProfile()
                        else -> { }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { startSyncFlow() },
                containerColor = BronzePrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Sync SMS transactions")
                }
            }
        }
    ) { paddingValues ->
        if (isLoadingProfile && transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BronzePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // ── This Month's Net Balance ─────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "💰 MONEY LEFT THIS MONTH",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isLoadingProfile) {
                        // Show placeholder while income is still being fetched from Supabase.
                        // Without this, balance = 0 - monthlyOut = negative red flash for a split second.
                        Text(
                            text = "—",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Loading...",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalBalance)}",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverBudget) Color(0xFFB91C1C) else TextBlack
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Savings indicator / over-budget warning
                        when {
                            !isIncomeSet -> {
                                Text(
                                    text = "👆 Tap here to set your monthly income!",
                                    color = BronzePrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { onNavigateToProfile() }
                                )
                            }
                            isOverBudget -> {
                                val overBy = kotlin.math.abs(totalBalance)
                                Text(
                                    text = "😬 You've spent ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(overBy)} more than you earned!",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            else -> {
                                Text(
                                    text = "🎉 Great job! You've saved $savingsPct% of your income this month",
                                    color = TealPositive,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }


                Spacer(modifier = Modifier.height(32.dp))

                // AI Insights Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BronzePrimary)
                        .clickable { onNavigateToAiInsights() }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = BronzePrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("✨ See My Money Insights", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                            Text("Find out where your money is going!", fontSize = 12.sp, color = TextGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Monthly In / Out Cards ───────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Money In
                    Box(modifier = Modifier.weight(1f).border(1.dp, BorderBlack).padding(16.dp)) {
                        Column {
                            Text("↗", color = TealPositive)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("💵 MONEY IN", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(monthlyIn)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPositive
                            )
                        }
                    }
                    // Money Out
                    Box(modifier = Modifier.weight(1f).border(1.dp, BorderBlack).padding(16.dp)) {
                        Column {
                            Text("↘", color = Color(0xFFB91C1C))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("🛒 MONEY OUT", fontSize = 9.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(monthlyOut)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Recent Activity ──────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("🕐 RECENT ACTIVITY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                    Text(
                        text = "SEE ALL →",
                        color = BronzePrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToTransactions() }
                    )
                }
                Divider(color = BorderBlack, thickness = 2.dp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))

                if (recentTxns.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nothing here yet! 👇\nTap the orange button below to import your bank messages.",
                            color = TextGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        items(recentTxns, key = { it.smsId }) { txn ->
                            HomeTransactionRow(txn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeTransactionRow(txn: TransactionEntity) {
    val isPositive = txn.amount > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(txn.category.take(1), fontWeight = FontWeight.Bold, color = TextGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(txn.merchant, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                Text(
                    text = "${txn.category} • ${formatTransactionDate(txn.transactionDate)}",
                    fontSize = 12.sp,
                    color = TextGray
                )
            }
        }
        Text(
            text = if (isPositive) {
                "+ ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(txn.amount)}"
            } else {
                "- ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(kotlin.math.abs(txn.amount))}"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isPositive) TealPositive else TextBlack
        )
    }
    Divider(color = SurfaceGray, thickness = 1.dp)
}

// Check if transaction happened in current month/year
fun isCurrentMonth(dateStr: String): Boolean {
    return try {
        val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        isoParser.timeZone = TimeZone.getTimeZone("UTC")
        val txnDate = isoParser.parse(dateStr) ?: return false
        
        val calTxn = Calendar.getInstance()
        calTxn.time = txnDate
        
        val calNow = Calendar.getInstance()
        
        calTxn.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
        calTxn.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
    } catch (e: Exception) {
        false
    }
}
