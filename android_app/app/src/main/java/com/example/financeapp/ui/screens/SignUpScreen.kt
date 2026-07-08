package com.example.financeapp.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.data.UserRepository
import com.example.financeapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onNavigateToHome: () -> Unit,
    onNavigateBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullNameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var monthlyIncomeInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation states
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Password strength calculation
    val passwordStrength = remember(passwordInput) {
        calculatePasswordStrength(passwordInput)
    }

    fun validate(): Boolean {
        var isValid = true

        if (fullNameInput.isBlank()) {
            fullNameError = "Full name is required"
            isValid = false
        } else {
            fullNameError = null
        }

        if (emailInput.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            emailError = "Invalid email format"
            isValid = false
        } else {
            emailError = null
        }

        if (passwordInput.isBlank()) {
            passwordError = "Password is required"
            isValid = false
        } else if (passwordInput.length < 6) {
            passwordError = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordError = null
        }

        if (confirmPasswordInput.isBlank()) {
            confirmPasswordError = "Please confirm your password"
            isValid = false
        } else if (passwordInput != confirmPasswordInput) {
            confirmPasswordError = "Passwords do not match"
            isValid = false
        } else {
            confirmPasswordError = null
        }

        return isValid
    }

    Surface(modifier = Modifier.fillMaxSize(), color = BackgroundWhite) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Create Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Start tracking your finances today",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Full Name
            OutlinedTextField(
                value = fullNameInput,
                onValueChange = {
                    fullNameInput = it
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

            // Email
            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RectangleShape,
                isError = emailError != null,
                supportingText = emailError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BronzePrimary,
                    unfocusedBorderColor = BorderBlack,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number (optional)
            OutlinedTextField(
                value = phoneInput,
                onValueChange = { phoneInput = it },
                label = { Text("Phone Number (optional)") },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Monthly Income (optional)
            OutlinedTextField(
                value = monthlyIncomeInput,
                onValueChange = { newValue ->
                    // Only allow digits and decimal point
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        monthlyIncomeInput = newValue
                    }
                },
                label = { Text("Monthly Income (optional)") },
                placeholder = { Text("₹ 0.00", color = TextGray.copy(alpha = 0.5f)) },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Password with visibility toggle
            OutlinedTextField(
                value = passwordInput,
                onValueChange = {
                    passwordInput = it
                    if (passwordError != null) passwordError = null
                },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = TextGray
                        )
                    }
                },
                shape = RectangleShape,
                isError = passwordError != null,
                supportingText = passwordError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BronzePrimary,
                    unfocusedBorderColor = BorderBlack,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )

            // Password Strength Indicator
            if (passwordInput.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                PasswordStrengthIndicator(strength = passwordStrength)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Password with visibility toggle
            OutlinedTextField(
                value = confirmPasswordInput,
                onValueChange = {
                    confirmPasswordInput = it
                    if (confirmPasswordError != null) confirmPasswordError = null
                },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                            tint = TextGray
                        )
                    }
                },
                shape = RectangleShape,
                isError = confirmPasswordError != null,
                supportingText = confirmPasswordError?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BronzePrimary,
                    unfocusedBorderColor = BorderBlack,
                    errorBorderColor = MaterialTheme.colorScheme.error
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Create Account button
            Button(
                onClick = {
                    if (!validate()) return@Button

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val income = monthlyIncomeInput.toDoubleOrNull()
                            UserRepository.signUp(
                                email = emailInput.trim(),
                                password = passwordInput,
                                fullName = fullNameInput.trim(),
                                phoneNumber = phoneInput.trim().takeIf { it.isNotBlank() },
                                monthlyIncome = income
                            )
                            onNavigateToHome()
                        } catch (e: Exception) {
                            val message = when {
                                e.message?.contains("already registered", ignoreCase = true) == true ->
                                    "This email is already registered. Try logging in."
                                e.message?.contains("network", ignoreCase = true) == true ->
                                    "Network error. Check your internet connection."
                                e.message?.contains("weak password", ignoreCase = true) == true ->
                                    "Password is too weak. Add numbers and special characters."
                                else -> "Sign up failed: ${e.message}"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary, contentColor = Color.White),
                shape = RectangleShape,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("CREATE ACCOUNT", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Already have an account? ", color = TextGray)
                Text(
                    text = "Log in",
                    color = BronzePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateBackToLogin() }
                )
            }
        }
    }
}

// ── Password Strength ─────────────────────────────────────────

enum class PasswordStrength(val label: String, val color: Color, val progress: Float) {
    WEAK("Weak", Color(0xFFD32F2F), 0.25f),
    FAIR("Fair", Color(0xFFF57C00), 0.5f),
    GOOD("Good", Color(0xFF388E3C), 0.75f),
    STRONG("Strong", Color(0xFF1B5E20), 1.0f)
}

fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when {
        score >= 4 -> PasswordStrength.STRONG
        score >= 3 -> PasswordStrength.GOOD
        score >= 2 -> PasswordStrength.FAIR
        else -> PasswordStrength.WEAK
    }
}

@Composable
fun PasswordStrengthIndicator(strength: PasswordStrength) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Password strength:", fontSize = 12.sp, color = TextGray)
            Text(strength.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = strength.color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = strength.progress,
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = strength.color,
            trackColor = SurfaceGray
        )
    }
}
