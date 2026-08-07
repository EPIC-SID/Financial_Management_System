package com.example.financeapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("AI Insights", fontWeight = FontWeight.Bold, color = TextBlack) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "🍧 WHERE IS MY MONEY GOING?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (expenseSummary.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).border(1.dp, SurfaceGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No spending this month yet 😊", color = TextGray)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PieChart(
                            data = expenseSummary,
                            modifier = Modifier.size(150.dp)
                        )
                        Spacer(modifier = Modifier.width(24.dp))
                        ChartLegend(data = expenseSummary)
                    }
                }
            }

            item {
                InsightSummaryCard(savingsRate, savings)
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🤖 TIPS JUST FOR YOU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGray,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    if (isLoadingSuggestions) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp).padding(top = 8.dp),
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
                style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun ChartLegend(data: Map<String, Double>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        data.keys.take(ChartColors.size).forEachIndexed { index, category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ChartColors[index % ChartColors.size], CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category,
                    fontSize = 12.sp,
                    color = TextBlack,
                    fontWeight = FontWeight.Medium
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
            .border(2.dp, BorderBlack)
            .background(if (isPositive) TealPositive.copy(alpha = 0.1f) else BronzePrimary.copy(alpha = 0.1f))
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) TealPositive else BronzePrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isPositive) "You're saving well! 😄" else "Let's save more this month 💪",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextBlack
            )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You've saved ${String.format("%.1f", savingsRate)}% of your income this month — that's ₹${format.format(savings)}!",
                fontSize = 14.sp,
                color = TextBlack
            )
        }
    }
}

@Composable
fun AiSuggestionCard(suggestion: AiSuggestion) {
    val icon = when(suggestion.type) {
        "SAVINGS" -> Icons.Default.TrendingUp
        "WARNING" -> Icons.Default.TrendingDown
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
            .border(1.dp, BorderBlack)
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(suggestion.description, fontSize = 13.sp, color = TextGray)
            }
        }
    }
}

@Composable
fun StaticSuggestionCard(suggestion: StaticSuggestion) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderBlack)
            .padding(16.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(suggestion.icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = TextBlack)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(suggestion.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text(suggestion.description, fontSize = 13.sp, color = TextGray)
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
            "💰 Set your monthly income",
            "Go to Profile and add your salary so we can track how much you're saving!",
            Icons.Default.AutoAwesome
        ))
    }

    val foodSpent = txns.filter { it.category.contains("Food", true) }.sumOf { kotlin.math.abs(it.amount) }
    if (foodSpent > income * 0.2 && income > 0) {
        list.add(StaticSuggestion(
            "🍕 You're spending a lot on food!",
            "Food is ${String.format("%.0f", (foodSpent/income)*100)}% of your income this month. Try cooking at home more!",
            Icons.Default.TrendingDown
        ))
    }

    list.add(StaticSuggestion(
        "🛡️ Build a Safety Net",
        "Try to save at least ₹50,000 as an emergency fund — it's like a financial seatbelt!",
        Icons.Default.AutoAwesome
    ))

    return list
}
