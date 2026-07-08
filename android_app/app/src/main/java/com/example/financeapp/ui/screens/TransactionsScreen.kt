package com.example.financeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(onNavigateBack: () -> Unit) {
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
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.Search, contentDescription = "Search", tint = BronzePrimary)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Filled.MailOutline, contentDescription = "Notifications", tint = BronzePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = BackgroundWhite, modifier = Modifier.border(1.dp, SurfaceGray, RectangleShape)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = false,
                    onClick = onNavigateBack,
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextGray, unselectedTextColor = TextGray)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Transactions") },
                    label = { Text("Transactions") },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = BronzePrimary, selectedTextColor = BronzePrimary, indicatorColor = BackgroundWhite)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextGray, unselectedTextColor = TextGray)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Recent Transactions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Review your latest spending and income.", fontSize = 14.sp, color = TextGray)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Filter tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = true, text = "All")
                        FilterChip(selected = false, text = "Income")
                        FilterChip(selected = false, text = "Expenses")
                        FilterChip(selected = false, text = "Food")
                    }
                }
            }
            
            item {
                DateHeader("TODAY, OCT 24")
            }
            item { TransactionListItem("Amazon.in", "- ₹2,500.00", "SHOPPING", "14:32", isPositive = false) }
            item { TransactionListItem("Jio Recharge", "- ₹666.00", "BILLS", "10:15", isPositive = false) }
            item { TransactionListItem("Starbucks", "- ₹450.00", "FOOD", "08:45", isPositive = false) }
            
            item {
                DateHeader("YESTERDAY, OCT 23")
            }
            item { TransactionListItem("Salary Credit", "+ ₹85,000.00", "INCOME", "09:00", isPositive = true) }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(48.dp),
                    border = BorderStroke(1.dp, BorderBlack),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack),
                    shape = RectangleShape
                ) {
                    Text("LOAD MORE ˅", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FilterChip(selected: Boolean, text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, if (selected) BronzePrimary else BorderBlack, RectangleShape)
            .background(if (selected) BronzePrimary else Color.Transparent)
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
