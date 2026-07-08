package com.example.financeapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
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

data class Transaction(val id: Int, val merchant: String, val amount: String, val category: String, val description: String)

val sampleTransactions = listOf(
    Transaction(1, "Swiggy", "- ₹350.00", "Food", "Food Delivery • Today"),
    Transaction(2, "Zomato", "- ₹1,200.00", "Food", "Dining • Yesterday"),
    Transaction(3, "Uber", "- ₹180.00", "Transport", "Transport • Mar 12")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToTransactions: () -> Unit) {
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
            NavigationBar(containerColor = BackgroundWhite, modifier = Modifier.border(1.dp, SurfaceGray, RectangleShape)) {
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = BronzePrimary, selectedTextColor = BronzePrimary, indicatorColor = BackgroundWhite)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.List, contentDescription = "Transactions") },
                    label = { Text("Transactions") },
                    selected = false,
                    onClick = onNavigateToTransactions,
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextGray, unselectedTextColor = TextGray)
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = { },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = TextGray, unselectedTextColor = TextGray)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add manual txn */ },
                containerColor = BronzePrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
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
                Text("₹24,000.00", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { },
                    colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary, contentColor = Color.White),
                    shape = RectangleShape,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("ADD FUNDS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        Text("₹3,400", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TealPositive)
                    }
                }
                // Monthly Out
                Box(modifier = Modifier.weight(1f).border(1.dp, BorderBlack).padding(16.dp)) {
                    Column {
                        Text("↘", color = TextGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("MONTHLY OUT", fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Bold)
                        Text("₹1,730", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
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
                Text("VIEW ALL", color = BronzePrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Divider(color = BorderBlack, thickness = 2.dp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                items(sampleTransactions) { txn ->
                    TransactionRow(txn)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(txn: Transaction) {
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
                Text(txn.description, fontSize = 12.sp, color = TextGray)
            }
        }
        Text(txn.amount, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
    }
    Divider(color = SurfaceGray, thickness = 1.dp)
}
