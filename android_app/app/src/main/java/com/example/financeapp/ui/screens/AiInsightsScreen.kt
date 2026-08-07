package com.example.financeapp.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financeapp.data.*
import com.example.financeapp.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

private val ChartColors = listOf(
    Color(0xFF965C19), // Bronze
    Color(0xFF009688), // Teal
    Color(0xFFE91E63), // Pink
    Color(0xFF2196F3), // Blue
    Color(0xFFFF9800), // Orange
    Color(0xFF9C27B0), // Purple
    Color(0xFF4CAF50), // Green
    Color(0xFF607D8B)  // Blue Gray
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiInsightsScreen(
    onNavigateBack: () -> Unit,
    transactionViewModel: TransactionViewModel = viewModel()
) {
    val context = LocalContext.current
    val transactions by transactionViewModel.transactions.collectAsState()
    var monthlyIncome by remember { mutableStateOf(0.0) }
    
    // Budget preferences
    val prefs = remember { context.getSharedPreferences("RupAiBudgetPrefs", Context.MODE_PRIVATE) }
    var storedTargetBudget by remember { mutableStateOf(prefs.getFloat("monthly_budget_target", 0f).toDouble()) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInputText by remember { mutableStateOf("") }

    var aiSuggestions by remember { mutableStateOf<List<AiSuggestion>>(emptyList()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }
    
    val currentMonthTxns = transactions.filter { isCurrentMonth(it.transactionDate) }
    val expenseSummary = currentMonthTxns
        .filter { it.amount < 0 }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }
    
    val monthlyOut = expenseSummary.values.sum()
    val savings = monthlyIncome - monthlyOut
    val savingsRate = if (monthlyIncome > 0) (savings / monthlyIncome) * 100 else 0.0

    // Calculate effective target budget: user custom target OR 80% of income OR default 15k
    val effectiveTargetBudget = when {
        storedTargetBudget > 0.0 -> storedTargetBudget
        monthlyIncome > 0.0      -> monthlyIncome * 0.8
        else                     -> 15000.0
    }

    val sortedSummary = remember(expenseSummary) {
        expenseSummary.entries
            .sortedByDescending { it.value }
            .associate { it.key to it.value }
    }

    LaunchedEffect(Unit) {
        val profile = UserRepository.getProfile()
        if (profile is Resource.Success) {
            monthlyIncome = profile.data?.monthlyIncome ?: 0.0
        }
    }

    LaunchedEffect(monthlyIncome, effectiveTargetBudget, currentMonthTxns) {
        if (currentMonthTxns.isNotEmpty()) {
            isLoadingSuggestions = true
            try {
                aiSuggestions = GeminiAdvisor.getSuggestions(monthlyIncome, effectiveTargetBudget, currentMonthTxns)
            } catch (e: Exception) {
                // Fallback or silent error
            } finally {
                isLoadingSuggestions = false
            }
        }
    }

    // Budget Edit Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = BackgroundWhite,
            title = { Text("Set Monthly Budget Limit", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextBlack) },
            text = {
                Column {
                    Text(
                        "Set your maximum spending goal for this month. We will alert you when approaching or exceeding this limit.",
                        fontSize = 13.sp, color = TextGray, lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = it },
                        label = { Text("Monthly Limit (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newLimit = budgetInputText.toDoubleOrNull() ?: 0.0
                        if (newLimit > 0) {
                            prefs.edit().putFloat("monthly_budget_target", newLimit.toFloat()).apply()
                            storedTargetBudget = newLimit
                            Toast.makeText(context, "Monthly spending limit updated to ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(newLimit.toInt())}", Toast.LENGTH_SHORT).show()
                        }
                        showBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary)
                ) { Text("Save Limit", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text("Cancel", color = TextGray) }
            }
        )
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = { Text("AI Financial Insights", fontWeight = FontWeight.Bold, color = TextBlack) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Budget Proximity Card ─────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                BudgetTrackerCard(
                    spent = monthlyOut,
                    targetBudget = effectiveTargetBudget,
                    isCustomTarget = storedTargetBudget > 0.0,
                    onEditClick = {
                        budgetInputText = effectiveTargetBudget.toInt().toString()
                        showBudgetDialog = true
                    }
                )
            }

            // ── Expense Breakdown Donut ───────────────────
            item {
                Text(
                    "EXPENSE BREAKDOWN BY CATEGORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (sortedSummary.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, SurfaceGray, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No spending data available for current month", color = TextGray, fontSize = 14.sp)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                PieChart(
                                    data = sortedSummary,
                                    modifier = Modifier.size(140.dp)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(monthlyOut.toInt())}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextBlack
                                    )
                                    Text(
                                        text = "Spent",
                                        fontSize = 10.sp,
                                        color = TextGray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            ChartLegend(data = sortedSummary, total = monthlyOut)
                        }
                    }
                }
            }

            item {
                InsightSummaryCard(savingsRate, savings)
            }

            // ── AI Recommendations Header ─────────────────
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = BronzePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "AI RECOMMENDATIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray
                    )
                    if (isLoadingSuggestions) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = BronzePrimary
                        )
                    }
                }
            }

            if (aiSuggestions.isEmpty() && !isLoadingSuggestions) {
                val fallback = generateStaticSuggestions(monthlyIncome, effectiveTargetBudget, monthlyOut, currentMonthTxns)
                items(fallback) { suggestion ->
                    StaticSuggestionCard(suggestion)
                }
            } else {
                items(aiSuggestions) { suggestion ->
                    AiSuggestionCard(suggestion)
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun BudgetTrackerCard(
    spent: Double,
    targetBudget: Double,
    isCustomTarget: Boolean,
    onEditClick: () -> Unit
) {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    val usageRatio = (spent / targetBudget).coerceIn(0.0, 1.0).toFloat()
    val percentage = ((spent / targetBudget) * 100).toInt()
    val isOverBudget = spent >= targetBudget
    val isNearBudget = spent >= targetBudget * 0.8 && !isOverBudget

    val statusColor = when {
        isOverBudget -> Color(0xFFB91C1C) // Red
        isNearBudget -> Color(0xFFD97706) // Amber/Orange
        else         -> TealPositive      // Green
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderBlack),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MONTHLY SPENDING LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextGray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "₹${format.format(spent.toInt())} / ₹${format.format(targetBudget.toInt())}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextBlack
                    )
                }
                TextButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = BronzePrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isCustomTarget) "Edit Limit" else "Set Custom", fontSize = 12.sp, color = BronzePrimary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { usageRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = statusColor,
                trackColor = SurfaceGray,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isOverBudget -> "Exceeded target limit by ₹${format.format((spent - targetBudget).toInt())}"
                        isNearBudget -> "Used $percentage% of target limit (₹${format.format((targetBudget - spent).toInt())} remaining)"
                        else         -> "On track ($percentage% used — ₹${format.format((targetBudget - spent).toInt())} remaining)"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }
        }
    }
}

@Composable
fun PieChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    val proportions = data.values.map { (it / total).toFloat() }
    val sweepAngles = proportions.map { it * 360f }
    
    Canvas(modifier = modifier) {
        var startAngle = -90f
        sweepAngles.forEachIndexed { index, sweepAngle ->
            drawArc(
                color = ChartColors[index % ChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun ChartLegend(data: Map<String, Double>, total: Double) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.widthIn(max = 160.dp)
    ) {
        data.entries.take(5).forEachIndexed { index, entry ->
            val percentage = if (total > 0) (entry.value / total) * 100 else 0.0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(ChartColors[index % ChartColors.size], CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.key,
                        fontSize = 11.sp,
                        color = TextBlack,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${percentage.toInt()}%",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun InsightSummaryCard(savingsRate: Double, savings: Double) {
    val isPositive = savingsRate >= 20.0
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderBlack, RoundedCornerShape(8.dp))
            .background(if (isPositive) TealPositive.copy(alpha = 0.08f) else BronzePrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) TealPositive else BronzePrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPositive) "Healthy Savings Rate" else "Low Savings Performance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextBlack
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Current monthly savings rate is ${String.format("%.1f", savingsRate)}%. Total net savings: ₹${format.format(savings.toInt())}.",
                fontSize = 13.sp,
                color = TextBlack,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun AiSuggestionCard(suggestion: AiSuggestion) {
    val icon = when(suggestion.type) {
        "SAVINGS" -> Icons.Default.TrendingUp
        "WARNING" -> Icons.Default.WarningAmber
        else -> Icons.Default.AutoAwesome
    }
    
    val color = when(suggestion.type) {
        "SAVINGS" -> TealPositive
        "WARNING" -> BronzePrimary
        else -> TextBlack
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderBlack, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SurfaceGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(suggestion.description, fontSize = 13.sp, color = TextGray, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
fun StaticSuggestionCard(suggestion: StaticSuggestion) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderBlack, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SurfaceGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(suggestion.icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextBlack)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text(suggestion.description, fontSize = 13.sp, color = TextGray, lineHeight = 18.sp)
            }
        }
    }
}

data class StaticSuggestion(
    val title: String,
    val description: String,
    val icon: ImageVector
)

fun generateStaticSuggestions(
    income: Double,
    targetBudget: Double,
    expense: Double,
    txns: List<TransactionEntity>
): List<StaticSuggestion> {
    val list = mutableListOf<StaticSuggestion>()
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))

    // Dynamic Alert 1: Budget Proximity Alert
    if (expense >= targetBudget) {
        val overBy = expense - targetBudget
        list.add(StaticSuggestion(
            "Monthly Spending Limit Exceeded",
            "You have spent ₹${format.format(overBy.toInt())} over your ₹${format.format(targetBudget.toInt())} monthly budget target.",
            Icons.Default.WarningAmber
        ))
    } else if (expense >= targetBudget * 0.8) {
        val percentage = ((expense / targetBudget) * 100).toInt()
        val remaining = targetBudget - expense
        list.add(StaticSuggestion(
            "Approaching Spending Limit",
            "You have used $percentage% of your ₹${format.format(targetBudget.toInt())} spending limit. ₹${format.format(remaining.toInt())} remaining.",
            Icons.Default.WarningAmber
        ))
    } else {
        val percentage = if (targetBudget > 0) ((expense / targetBudget) * 100).toInt() else 0
        val remaining = targetBudget - expense
        list.add(StaticSuggestion(
            "Monthly Budget On Track",
            "You have utilized $percentage% of your ₹${format.format(targetBudget.toInt())} target limit. ₹${format.format(remaining.toInt())} remaining.",
            Icons.Default.TrendingUp
        ))
    }

    // Dynamic Alert 2: High Category Expenditure
    val topCategoryEntry = txns.filter { it.amount < 0 }
        .groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { kotlin.math.abs(it.amount) } }
        .maxByOrNull { it.value }

    if (topCategoryEntry != null && targetBudget > 0 && topCategoryEntry.value > targetBudget * 0.25) {
        val catPct = ((topCategoryEntry.value / targetBudget) * 100).toInt()
        list.add(StaticSuggestion(
            "High ${topCategoryEntry.key} Expenditure",
            "${topCategoryEntry.key} accounts for $catPct% (₹${format.format(topCategoryEntry.value.toInt())}) of your total spending limit.",
            Icons.Default.TrendingDown
        ))
    }

    // Dynamic Alert 3: Emergency Reserve Advice
    list.add(StaticSuggestion(
        "Emergency Reserve Allocation",
        "Maintain an emergency reserve of at least ₹50,000 (3-6 months essential expenses).",
        Icons.Default.Savings
    ))

    return list
}
