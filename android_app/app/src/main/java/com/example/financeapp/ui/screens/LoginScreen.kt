package com.example.financeapp.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financeapp.data.UserRepository
import com.example.financeapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Validation states
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Forgot password dialog state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }

    fun validateEmail(): Boolean {
        return if (emailInput.isBlank()) {
            emailError = "Email is required"
            false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            emailError = "Invalid email format"
            false
        } else {
            emailError = null
            true
        }
    }

    fun validatePassword(): Boolean {
        return if (passwordInput.isBlank()) {
            passwordError = "Password is required"
            false
        } else if (passwordInput.length < 6) {
            passwordError = "Password must be at least 6 characters"
            false
        } else {
            passwordError = null
            true
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSendingReset) showForgotPasswordDialog = false
            },
            shape = RectangleShape,
            containerColor = BackgroundWhite,
            title = {
                Text(
                    "Reset Password",
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
            },
            text = {
                Column {
                    Text(
                        "Enter your email address and we'll send you a link to reset your password.",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BronzePrimary,
                            unfocusedBorderColor = BorderBlack
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotPasswordEmail.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(forgotPasswordEmail).matches()) {
                            Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSendingReset = true
                        coroutineScope.launch {
                            try {
                                UserRepository.resetPassword(forgotPasswordEmail)
                                Toast.makeText(context, "Password reset email sent! Check your inbox.", Toast.LENGTH_LONG).show()
                                showForgotPasswordDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to send reset email: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSendingReset = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BronzePrimary),
                    shape = RectangleShape,
                    enabled = !isSendingReset
                ) {
                    if (isSendingReset) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("SEND LINK", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showForgotPasswordDialog = false },
                    border = BorderStroke(1.dp, BorderBlack),
                    shape = RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextBlack),
                    enabled = !isSendingReset
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

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
                    .size(80.dp)
                    .border(2.dp, BorderBlack, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = BronzePrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to RupAI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )

            Text(
                text = "Sign in to manage your finances",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field with validation
            OutlinedTextField(
                value = emailInput,
                onValueChange = {
                    emailInput = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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

            // Password field with visibility toggle
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

            // Forgot Password link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    color = BronzePrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        forgotPasswordEmail = emailInput  // Pre-fill with current email
                        showForgotPasswordDialog = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login button
            Button(
                onClick = {
                    val isEmailValid = validateEmail()
                    val isPasswordValid = validatePassword()
                    if (!isEmailValid || !isPasswordValid) return@Button

                    isLoading = true
                    coroutineScope.launch {
                        try {
                            UserRepository.signIn(emailInput.trim(), passwordInput)
                            onNavigateToHome()
                        } catch (e: Exception) {
                            val message = when {
                                e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                                    "Incorrect email or password. Please try again."
                                e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                                    "Please verify your email before signing in."
                                e.message?.contains("network", ignoreCase = true) == true ->
                                    "Network error. Check your internet connection."
                                else -> "Login failed: ${e.message}"
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
                    Text("LOG IN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = TextGray)
                Text(
                    text = "Sign up",
                    color = BronzePrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }
    }
}
