package com.example.financeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.ui.theme.*

@Composable
fun AiReviewScreen(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, BorderBlack, RectangleShape)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextBlack)
                        }
                    }
                    
                    Text("Zepto", fontSize = 18.sp, color = TextGray)
                    Text("₹540", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        "We guessed this was Groceries with 80% confidence",
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = TextBlack
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { /* Edit */ },
                            modifier = Modifier.weight(1f).height(48.dp),
                            border = BorderStroke(1.dp, BorderBlack),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack),
                            shape = RectangleShape
                        ) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary, contentColor = Color.White),
                            shape = RectangleShape
                        ) {
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
