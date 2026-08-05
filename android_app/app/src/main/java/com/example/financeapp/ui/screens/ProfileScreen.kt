package com.example.financeapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.data.Resource
import com.example.financeapp.data.SupabaseClient
import com.example.financeapp.data.UserProfile
import com.example.financeapp.data.UserRepository
import com.example.financeapp.ui.components.AppBottomBar
import com.example.financeapp.ui.components.Tab
import com.example.financeapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ProfileScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var isSigningOut by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoadingProfile by remember { mutableStateOf(true) }

    // Fetch profile from Supabase on load
    LaunchedEffect(Unit) {
        try {
            val result = UserRepository.getProfile()
            if (result is Resource.Success) {
                profile = result.data
            } else if (result is Resource.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            // Fallback to auth metadata if profile fetch fails
        } finally {
            isLoadingProfile = false
        }
    }

    // Fallback values from auth metadata
    val user = SupabaseClient.auth.currentUserOrNull()
    val displayName = profile?.fullName
        ?: user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
        ?: "No Name"
    val displayEmail = profile?.email ?: user?.email ?: "Unknown"
    val displayPhone = profile?.phoneNumber ?: "Not provided"
    val displayIncome = profile?.monthlyIncome
    val displayCurrency = profile?.currency ?: "INR"
    val memberSince = profile?.createdAt?.take(10) ?: "Unknown"
    val lastUpdated = profile?.updatedAt?.take(10) ?: "Unknown"

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Profile", fontWeight = FontWeight.Bold, color = BronzePrimary, fontSize = 20.sp)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEditProfile) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = BronzePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
            )
        },
        bottomBar = {
            AppBottomBar(
                selectedTab = Tab.PROFILE,
                onTabSelected = { tab ->
                    when (tab) {
                        Tab.HOME -> onNavigateToHome()
                        Tab.TRANSACTIONS -> onNavigateToTransactions()
                        else -> { }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoadingProfile) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BronzePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Avatar ──────────────────────────────
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(2.dp, BorderBlack, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = BronzePrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = displayEmail, fontSize = 14.sp, color = TextGray)

                Spacer(modifier = Modifier.height(32.dp))

                // ── Account Info Section ────────────────
                ProfileSectionHeader("ACCOUNT INFORMATION")

                ProfileInfoRow("Full Name", displayName)
                ProfileDivider()
                ProfileInfoRow("Email", displayEmail)
                ProfileDivider()
                ProfileInfoRow("Phone", displayPhone)

                Spacer(modifier = Modifier.height(24.dp))

                // ── Financial Setup Section ─────────────
                ProfileSectionHeader("FINANCIAL SETUP")

                ProfileInfoRow(
                    label = "Monthly Income",
                    value = if (displayIncome != null && displayIncome > 0) {
                        "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(displayIncome)}"
                    } else {
                        "Not set"
                    }
                )
                ProfileDivider()
                ProfileInfoRow("Currency", displayCurrency)

                Spacer(modifier = Modifier.height(24.dp))

                // ── Account Details Section ─────────────
                ProfileSectionHeader("ACCOUNT DETAILS")

                ProfileInfoRow("Member Since", memberSince)
                ProfileDivider()
                ProfileInfoRow("Last Updated", lastUpdated)

                Spacer(modifier = Modifier.height(32.dp))

                // ── Edit Profile Button ─────────────────
                OutlinedButton(
                    onClick = onNavigateToEditProfile,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(2.dp, BronzePrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BronzePrimary),
                    shape = RectangleShape
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EDIT PROFILE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Reset Data & AI Cache Button ───────
                var isResetting by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = {
                        isResetting = true
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val db = com.example.financeapp.data.AppDatabase.getInstance(context)
                                db.transactionDao().deleteAll()
                                context.getSharedPreferences("GeminiCategorizerCache", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                context.getSharedPreferences("PlacesCategorizerCache", android.content.Context.MODE_PRIVATE).edit().clear().apply()
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "DB & AI Caches Cleared! Sync SMS to trigger Gemini.", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, "Reset failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isResetting = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(2.dp, Color(0xFFC05621)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC05621)),
                    shape = RectangleShape,
                    enabled = !isResetting
                ) {
                    if (isResetting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFFC05621))
                    } else {
                        Text("RESET DATA & AI CACHE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Sign Out Button ─────────────────────
                OutlinedButton(
                    onClick = {
                        isSigningOut = true
                        coroutineScope.launch {
                            try {
                                UserRepository.signOut()
                                onNavigateToLogin()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Sign out failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSigningOut = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    border = BorderStroke(2.dp, BorderBlack),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack),
                    shape = RectangleShape,
                    enabled = !isSigningOut
                ) {
                    if (isSigningOut) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextBlack)
                    } else {
                        Text("SIGN OUT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ── Profile UI Components ──────────────────────────────────────

@Composable
fun ProfileSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextGray,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Divider(color = BorderBlack, thickness = 2.dp)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = TextGray, fontWeight = FontWeight.Medium)
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextBlack,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ProfileDivider() {
    Divider(color = SurfaceGray, thickness = 1.dp)
}
