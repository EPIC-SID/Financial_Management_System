package com.example.financeapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val transactions by transactionViewModel.transactions.collectAsState()
    var monthlyIncome by remember { mutableStateOf(0.0) }
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

    LaunchedEffect(Unit) {
        val profile = UserRepository.getProfile()
        if (profile is Resource.Success) {
            monthlyIncome = profile.data?.monthlyIncome ?: 0.0
        }
    }

    LaunchedEffect(monthlyIncome, currentMonthTxns) {
        if (monthlyIncome > 0 && currentMonthTxns.isNotEmpty()) {
            isLoadingSuggestions = true
            try {
                aiSuggestions = GeminiAdvisor.getSuggestions(monthlyIncome, currentMonthTxns)
            } catch (e: Exception) {
                // Fallback or silent error
            } finally {
                isLoadingSuggestions = false
            }
        }
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
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "EXPENSE BREAKDOWN BY CATEGORY",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (expenseSummary.isEmpty()) {
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
                                    data = expenseSummary,
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
                            ChartLegend(data = expenseSummary, total = monthlyOut)
                        }
                    }
                }
            }

            item {
                InsightSummaryCard(savingsRate, savings)
            }

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
                val fallback = generateStaticSuggestions(monthlyIncome, monthlyOut, currentMonthTxns)
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
        data.entries.sortedByDescending { it.value }.take(5).forEachIndexed { index, entry ->
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
                text = "Current monthly savings rate is ${String.format("%.1f", savingsRate)}%. Total net savings: ₹${format.format(savings)}.",
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

fun generateStaticSuggestions(income: Double, expense: Double, txns: List<TransactionEntity>): List<StaticSuggestion> {
    val list = mutableListOf<StaticSuggestion>()
    
    if (income <= 0) {
        list.add(StaticSuggestion(
            "Set Monthly Income Target",
            "Update your profile with your monthly income to unlock precise savings analytics.",
            Icons.Default.Savings
        ))
    }

    val foodSpent = txns.filter { it.category.contains("Food", true) }.sumOf { kotlin.math.abs(it.amount) }
    if (foodSpent > income * 0.2 && income > 0) {
        list.add(StaticSuggestion(
            "High Food Expenditure",
            "Food expenditure accounts for ${String.format("%.0f", (foodSpent/income)*100)}% of monthly income. Consider optimizing dining budgets.",
            Icons.Default.TrendingDown
        ))
    }

    list.add(StaticSuggestion(
        "Emergency Reserve Allocation",
        "Maintain an emergency reserve of at least ₹50,000 (3-6 months essential expenses).",
        Icons.Default.AutoAwesome
    ))

    return list
}
