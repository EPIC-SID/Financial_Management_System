package com.example.financeapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
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
import com.example.financeapp.data.Resource
import com.example.financeapp.data.TransactionData
import com.example.financeapp.data.TransactionRepository
import com.example.financeapp.data.UserProfile
import com.example.financeapp.data.UserRepository
import com.example.financeapp.ui.components.AppBottomBar
import com.example.financeapp.ui.components.Tab
import com.example.financeapp.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTransactions: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current

    var transactions by remember { mutableStateOf<List<TransactionData>>(emptyList()) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch transactions & profile on load
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val profileResult = UserRepository.getProfile()
            if (profileResult is Resource.Success) {
                profile = profileResult.data
            }
            val txnResult = TransactionRepository.getTransactions()
            if (txnResult is Resource.Success) {
                transactions = txnResult.data
            } else if (txnResult is Resource.Error) {
                Toast.makeText(context, txnResult.message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load dashboard data", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    // Calculations based on real data
    val monthlyIncomeValue = profile?.monthlyIncome ?: 0.0
    val totalNetTxnFlow = transactions.sumOf { it.amount }
    val totalBalance = monthlyIncomeValue + totalNetTxnFlow

    // Current month calculations
    val currentMonthTxns = transactions.filter { isCurrentMonth(it.transactionDate) }
    val monthlyIn = currentMonthTxns.filter { it.amount > 0 }.sumOf { it.amount }
    val monthlyOut = currentMonthTxns.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

    // Recent activity: top 3 transactions
    val recentTxns = transactions.take(3)

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
                onClick = onNavigateToTransactions, // Navigate to transaction sync
                containerColor = BronzePrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.List, contentDescription = "Sync/View")
            }
        }
    ) { paddingValues ->
        if (isLoading) {
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
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("TOTAL BALANCE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalBalance)}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlack
                    )
                    
                    // Show a warning/link if monthly income is not set
                    if (profile?.monthlyIncome == null || profile?.monthlyIncome == 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Monthly income not set. Tap here to set it.",
                            color = BronzePrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onNavigateToProfile() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Monthly In
                    Box(modifier = Modifier.weight(1f).border(1.dp, BorderBlack).padding(16.dp)) {
                        Column {
                            Text("↗", color = TextGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("MONTHLY IN", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(monthlyIn)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TealPositive
                            )
                        }
                    }
                    // Monthly Out
                    Box(modifier = Modifier.weight(1f).border(1.dp, BorderBlack).padding(16.dp)) {
                        Column {
                            Text("↘", color = TextGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("MONTHLY OUT", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(monthlyOut)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("RECENT ACTIVITY", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                    Text(
                        text = "VIEW ALL",
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
                            text = "No recent transactions.\nGo to Transactions screen and sync.",
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
                        items(recentTxns) { txn ->
                            TransactionRow(txn)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionRow(txn: TransactionData) {
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
