package com.example.financeapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.data.SupabaseClient
import com.example.financeapp.data.UserProfile
import com.example.financeapp.data.UserProfileUpdate
import com.example.financeapp.data.UserRepository
import com.example.financeapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoadingProfile by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    // Editable fields
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var monthlyIncome by remember { mutableStateOf("") }

    // Validation
    var fullNameError by remember { mutableStateOf<String?>(null) }

    // Load current profile
    LaunchedEffect(Unit) {
        try {
            val fetchedProfile = UserRepository.getProfile()
            profile = fetchedProfile
            if (fetchedProfile != null) {
                fullName = fetchedProfile.fullName
                phoneNumber = fetchedProfile.phoneNumber ?: ""
                monthlyIncome = if (fetchedProfile.monthlyIncome != null && fetchedProfile.monthlyIncome > 0) {
                    fetchedProfile.monthlyIncome.toLong().toString()
                } else {
                    ""
                }
            } else {
                // Fallback to auth metadata
                val user = SupabaseClient.auth.currentUserOrNull()
                fullName = user?.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: ""
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
        } finally {
            isLoadingProfile = false
        }
    }

    Scaffold(
        containerColor = BackgroundWhite,
        topBar = {
            TopAppBar(
                title = {
                    Text("Edit Profile", fontWeight = FontWeight.Bold, color = TextBlack, fontSize = 18.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextBlack)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWhite)
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
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // ── Avatar Preview ──────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(80.dp)
                        .border(2.dp, BorderBlack, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fullName.take(1).uppercase().ifEmpty { "?" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = BronzePrimary
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Section: Personal Information ───
                Text(
                    "PERSONAL INFORMATION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
                Divider(color = BorderBlack, thickness = 2.dp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = {
                        fullName = it
                        if (fullNameError != null) fullNameError = null
                    },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RectangleShape,
                    isError = fullNameError != null,
                    supportingText = fullNameError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BronzePrimary,
                        unfocusedBorderColor = BorderBlack,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email (read-only)
                OutlinedTextField(
                    value = profile?.email ?: SupabaseClient.auth.currentUserOrNull()?.email ?: "",
                    onValueChange = { },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = false,
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = SurfaceGray,
                        disabledTextColor = TextGray,
                        disabledLabelColor = TextGray
                    )
                )

                Text(
                    "Email cannot be changed",
                    fontSize = 11.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone Number
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+91 XXXXX XXXXX", color = TextGray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BronzePrimary,
                        unfocusedBorderColor = BorderBlack
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // ── Section: Financial Setup ────────
                Text(
                    "FINANCIAL SETUP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGray,
                    letterSpacing = 1.sp
                )
                Divider(color = BorderBlack, thickness = 2.dp, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))

                // Monthly Income
                OutlinedTextField(
                    value = monthlyIncome,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            monthlyIncome = newValue
                        }
                    },
                    label = { Text("Monthly Income") },
                    leadingIcon = { Text("₹", color = BronzePrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RectangleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BronzePrimary,
                        unfocusedBorderColor = BorderBlack
                    )
                )

                Text(
                    "Used for budgeting and savings calculations",
                    fontSize = 11.sp,
                    color = TextGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                // ── Save Button ─────────────────────
                Button(
                    onClick = {
                        // Validate
                        if (fullName.isBlank()) {
                            fullNameError = "Name cannot be empty"
                            return@Button
                        }

                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val update = UserProfileUpdate(
                                    fullName = fullName.trim(),
                                    phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() },
                                    monthlyIncome = monthlyIncome.toDoubleOrNull()
                                )
                                UserRepository.updateProfile(update)
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary, contentColor = Color.White),
                    shape = RectangleShape,
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("SAVE CHANGES", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Cancel Button ───────────────────
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, BorderBlack),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack),
                    shape = RectangleShape,
                    enabled = !isSaving
                ) {
                    Text("CANCEL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
