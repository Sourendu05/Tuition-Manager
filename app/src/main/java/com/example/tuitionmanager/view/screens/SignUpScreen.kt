package com.example.tuitionmanager.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tuitionmanager.R
import com.example.tuitionmanager.view.components.GoogleSignInButton
import com.example.tuitionmanager.viewmodel.AuthState
import com.example.tuitionmanager.viewmodel.AuthViewModel
import androidx.compose.material.icons.filled.HelpOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val isEmailLoading by authViewModel.isEmailLoading.collectAsState()
    val isGoogleLoading by authViewModel.isGoogleLoading.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val verificationEmailSent by authViewModel.verificationEmailSent.collectAsState()
    val pendingEmail by authViewModel.pendingVerificationEmail.collectAsState()

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val webClientId = stringResource(R.string.default_web_client_id)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var showPasswordHint by remember { mutableStateOf(false) }

    // Navigate on successful authentication (for Google Sign-In only)
    // Email sign-up shows verification dialog first, so don't navigate then
    LaunchedEffect(authState, verificationEmailSent) {
        if (authState is AuthState.Authenticated && !verificationEmailSent) {
            authViewModel.clearVerificationState()
            onSignUpSuccess()
        }
    }

    // Clear error when user starts typing
    LaunchedEffect(name, email, password) {
        authViewModel.clearError()
        localError = null
    }

    // Verification Email Sent Dialog
    if (verificationEmailSent && pendingEmail != null) {
        AlertDialog(
            onDismissRequest = { 
                authViewModel.clearVerificationState()
                onNavigateBack()
            },
            title = {
                Text(
                    text = "Verify Your Email",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text(
                        text = "We've sent a verification link to:"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = pendingEmail ?: "",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Click the link in the email to verify your account, then sign in."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Check Spam/Junk folder if not found in inbox.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.clearVerificationState()
                        onNavigateBack()
                    }
                ) {
                    Text("Go to Sign In")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
    
    // Password Requirements Dialog
    if (showPasswordHint) {
        AlertDialog(
            onDismissRequest = { showPasswordHint = false },
            title = {
                Text(
                    text = "Password Requirements",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    Text("• Minimum 6 characters")
                    Text("• At least 1 uppercase letter (A-Z)")
                    Text("• At least 1 lowercase letter (a-z)")
                    Text("• At least 1 number (0-9)")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPasswordHint = false }) {
                    Text("Got it")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF334155)
                    )
                )
            )
            .systemBarsPadding()
    ) {
        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section - Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 56.dp)
            ) {
                Text(
                    text = "Create Account",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Start managing your tuition batches",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Middle Section - Form
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = Color(0xFF10B981),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = Color(0xFF10B981),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (password.length >= 6 && name.isNotBlank() && email.isNotBlank()) {
                                authViewModel.signUpWithEmail(name, email, password)
                            } else if (password.length < 6) {
                                localError = "Password must be at least 6 characters"
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.6f),
                        cursorColor = Color(0xFF10B981),
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Password Requirements Hint
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showPasswordHint = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Password requirements",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Min 6 chars, 1 uppercase, 1 lowercase, 1 number",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                // Error Message
                val displayError = localError ?: error
                if (displayError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayError,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up Button
                Button(
                    onClick = {
                        if (password.length < 6) {
                            localError = "Password must be at least 6 characters"
                        } else {
                            authViewModel.signUpWithEmail(name, email, password)
                        }
                    },
                    enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF10B981).copy(alpha = 0.4f)
                    )
                ) {
                    if (isEmailLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "  or  ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign In Button - with its own loading state
                GoogleSignInButton(
                    onClick = { authViewModel.signInWithGoogle(context, webClientId) },
                    enabled = !isLoading,
                    isLoading = isGoogleLoading
                )
            }

            // Bottom Section - Sign In Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Text(
                    text = "Sign In",
                    color = Color(0xFF34D399),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(enabled = !isLoading) { onNavigateBack() }
                )
            }
        }
    }
}
