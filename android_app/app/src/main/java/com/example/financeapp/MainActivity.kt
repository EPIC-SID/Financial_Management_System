package com.example.financeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.financeapp.ui.screens.onboarding.OnboardingRoute
import com.example.financeapp.ui.theme.FinanceAppTheme
import com.example.financeapp.ui.navigation.FinanceAppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FinanceAppTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var previewOnboarding by remember { mutableStateOf(BuildConfig.DEBUG) }
                    if (previewOnboarding) {
                        OnboardingRoute(
                            onExistingAccount = { previewOnboarding = false },
                            onComplete = { previewOnboarding = false }
                        )
                    } else {
                        FinanceAppNavGraph()
                    }
                }
            }
        }
    }
}
