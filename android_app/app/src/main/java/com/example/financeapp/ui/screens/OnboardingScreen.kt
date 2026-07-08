package com.example.financeapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
fun OnboardingScreen(onNavigateToHome: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            // Logo Box
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .border(2.dp, BorderBlack, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = BronzePrimary)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "RupAI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Automated tracking for your Indian bank accounts.",
                fontSize = 16.sp,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary, contentColor = Color.White),
                shape = RectangleShape
            ) {
                Text("SIGN IN WITH GOOGLE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onNavigateToHome,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                border = BorderStroke(1.dp, BorderBlack),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack),
                shape = RectangleShape
            ) {
                Text("TRY WITH SAMPLE DATA", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
