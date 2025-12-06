package com.example.tuitionmanager.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tuitionmanager.model.Repo
import com.example.tuitionmanager.model.ResultState
import com.example.tuitionmanager.model.data.Teacher
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling authentication logic.
 * Manages Sign In, Sign Up, Google Sign In, Profile Updates, and Sign Out.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: Repo
) : ViewModel() {

    // ==================== State ====================

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentTeacher = MutableStateFlow<Teacher?>(null)
    val currentTeacher: StateFlow<Teacher?> = _currentTeacher.asStateFlow()

    // Separate loading states for email and Google buttons
    private val _isEmailLoading = MutableStateFlow(false)
    val isEmailLoading: StateFlow<Boolean> = _isEmailLoading.asStateFlow()
    
    private val _isGoogleLoading = MutableStateFlow(false)
    val isGoogleLoading: StateFlow<Boolean> = _isGoogleLoading.asStateFlow()
    
    // Combined loading state for disabling UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // For tracking verification email sent state
    private val _verificationEmailSent = MutableStateFlow(false)
    val verificationEmailSent: StateFlow<Boolean> = _verificationEmailSent.asStateFlow()
    
    private val _pendingVerificationEmail = MutableStateFlow<String?>(null)
    val pendingVerificationEmail: StateFlow<String?> = _pendingVerificationEmail.asStateFlow()
    
    // OTP verification state
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()
    
    private val _pendingOtpEmail = MutableStateFlow<String?>(null)
    val pendingOtpEmail: StateFlow<String?> = _pendingOtpEmail.asStateFlow()
    
    // Password reset state
    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()

    // ==================== Initialization ====================

    init {
        checkAuthState()
        loadCurrentTeacher()
    }

    private fun checkAuthState() {
        _authState.value = if (repo.isUserSignedIn()) {
            AuthState.Authenticated
        } else {
            AuthState.Unauthenticated
        }
    }

    private fun loadCurrentTeacher() {
        viewModelScope.launch {
            repo.getCurrentTeacher().collect { result ->
                when (result) {
                    is ResultState.Success -> {
                        _currentTeacher.value = result.data
                        if (result.data != null) {
                            // Only authenticate if teacher data is returned
                            // (Repo already verified email/Google status)
                            _authState.value = AuthState.Authenticated
                        } else {
                            // Null teacher means user is not verified or not signed in
                            _authState.value = AuthState.Unauthenticated
                        }
                    }
                    is ResultState.Error -> {
                        // On error, stay in current state
                    }
                    is ResultState.Loading -> { /* Loading handled separately */ }
                }
            }
        }
    }
    
    /**
     * Force refresh the teacher data from Firebase.
     */
    fun refreshTeacherData() {
        viewModelScope.launch {
            val teacher = repo.refreshCurrentTeacher()
            if (teacher != null) {
                _currentTeacher.value = teacher
            }
        }
    }

    // ==================== Error Mapping ====================
    
    /**
     * Maps Firebase exceptions to user-friendly error messages.
     * Messages are kept short and simple.
     */
    private fun mapAuthError(errorMessage: String): String {
        return when {
            errorMessage.contains("EMAIL_NOT_VERIFIED", ignoreCase = true) -> 
                "Please verify your email first"
            errorMessage.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            errorMessage.contains("invalid", ignoreCase = true) && errorMessage.contains("credential", ignoreCase = true) ->
                "Wrong email or password"
            errorMessage.contains("user-not-found", ignoreCase = true) ||
            errorMessage.contains("no user record", ignoreCase = true) ->
                "Account not found"
            errorMessage.contains("wrong-password", ignoreCase = true) ->
                "Wrong password"
            errorMessage.contains("email-already-in-use", ignoreCase = true) ||
            errorMessage.contains("already exists", ignoreCase = true) ->
                "Email already registered"
            errorMessage.contains("weak-password", ignoreCase = true) ->
                "Password too weak"
            errorMessage.contains("invalid-email", ignoreCase = true) ||
            errorMessage.contains("badly formatted", ignoreCase = true) ->
                "Invalid email format"
            errorMessage.contains("network", ignoreCase = true) ->
                "No internet connection"
            errorMessage.contains("too-many-requests", ignoreCase = true) ->
                "Too many attempts. Try later"
            errorMessage.contains("No credentials available", ignoreCase = true) ||
            errorMessage.contains("NoCredentialException", ignoreCase = true) ->
                "No Google accounts found"
            errorMessage.contains("canceled", ignoreCase = true) ||
            errorMessage.contains("cancelled", ignoreCase = true) ->
                "Sign in cancelled"
            errorMessage.contains("collision", ignoreCase = true) ||
            errorMessage.contains("account-exists-with-different-credential", ignoreCase = true) ->
                "Account exists with different sign-in method"
            else -> "Sign in failed. Try again"
        }
    }

    // ==================== Email/Password Auth ====================

    /**
     * Sign in with email and password.
     */
    fun signInWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }

        viewModelScope.launch {
            _isEmailLoading.value = true
            _isLoading.value = true
            _error.value = null

            when (val result = repo.signInWithEmail(email, password)) {
                is ResultState.Success -> {
                    _authState.value = AuthState.Authenticated
                    refreshTeacherData()
                }
                is ResultState.Error -> {
                    _error.value = mapAuthError(result.error)
                    // Check if it's email verification issue
                    if (result.error.contains("EMAIL_NOT_VERIFIED")) {
                        _pendingVerificationEmail.value = email
                    }
                }
                is ResultState.Loading -> { /* Already handled */ }
            }

            _isEmailLoading.value = false
            _isLoading.value = false
        }
    }

    /**
     * Create a new account with email and password.
     * Sends verification email - user must verify before signing in.
     */
    fun signUpWithEmail(name: String, email: String, password: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _error.value = "Please fill in all fields"
            return
        }

        if (password.length < 6) {
            _error.value = "Password needs at least 6 characters"
            return
        }
        
        if (!password.any { it.isUpperCase() }) {
            _error.value = "Password needs 1 uppercase letter"
            return
        }
        
        if (!password.any { it.isLowerCase() }) {
            _error.value = "Password needs 1 lowercase letter"
            return
        }
        
        if (!password.any { it.isDigit() }) {
            _error.value = "Password needs 1 number"
            return
        }

        viewModelScope.launch {
            _isEmailLoading.value = true
            _isLoading.value = true
            _error.value = null
            _verificationEmailSent.value = false

            when (val result = repo.signUpWithEmail(name, email, password)) {
                is ResultState.Success -> {
                    // Account created, verification email sent
                    _verificationEmailSent.value = true
                    _pendingVerificationEmail.value = email
                    // Stay unauthenticated until verified
                    _authState.value = AuthState.Unauthenticated
                }
                is ResultState.Error -> {
                    _error.value = mapAuthError(result.error)
                }
                is ResultState.Loading -> { /* Already handled */ }
            }

            _isEmailLoading.value = false
            _isLoading.value = false
        }
    }
    
    /**
     * Clear verification sent state.
     */
    fun clearVerificationState() {
        _verificationEmailSent.value = false
        _pendingVerificationEmail.value = null
        _otpSent.value = false
        _pendingOtpEmail.value = null
    }

    // ==================== Google Sign In ====================

    /**
     * Initiate Google Sign In using Credential Manager with Sign In With Google button flow.
     * This provides better UX and account selection on devices.
     */
    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            _isGoogleLoading.value = true
            _isLoading.value = true
            _error.value = null

            try {
                val credentialManager = CredentialManager.create(context)

                // Use GetSignInWithGoogleOption for better account picker experience
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                val response = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleGoogleSignInResponse(response)
            } catch (e: NoCredentialException) {
                // Fallback to standard Google ID option
                try {
                    val credentialManager = CredentialManager.create(context)
                    
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val response = credentialManager.getCredential(
                        request = request,
                        context = context
                    )

                    handleGoogleSignInResponse(response)
                } catch (fallbackError: NoCredentialException) {
                    _error.value = "No Google accounts found"
                    _isGoogleLoading.value = false
                    _isLoading.value = false
                } catch (fallbackError: GetCredentialCancellationException) {
                    _isGoogleLoading.value = false
                    _isLoading.value = false
                } catch (fallbackError: Exception) {
                    _error.value = mapAuthError(fallbackError.message ?: "Google sign in failed")
                    _isGoogleLoading.value = false
                    _isLoading.value = false
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled - don't show error
                _isGoogleLoading.value = false
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = mapAuthError(e.message ?: "Google sign in failed")
                _isGoogleLoading.value = false
                _isLoading.value = false
            }
        }
    }

    private suspend fun handleGoogleSignInResponse(response: GetCredentialResponse) {
        val credential = response.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        when (val result = repo.signInWithGoogle(googleIdTokenCredential.idToken)) {
                            is ResultState.Success -> {
                                _authState.value = AuthState.Authenticated
                                refreshTeacherData()
                            }
                            is ResultState.Error -> {
                                _error.value = mapAuthError(result.error)
                            }
                            is ResultState.Loading -> { /* Already handled */ }
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        _error.value = "Google sign in failed"
                    }
                } else {
                    _error.value = "Sign in failed. Try again"
                }
            }
            else -> {
                _error.value = "Sign in failed. Try again"
            }
        }

        _isGoogleLoading.value = false
        _isLoading.value = false
    }

    // ==================== Profile Management ====================

    /**
     * Update the user's display name.
     */
    fun updateProfile(name: String) {
        if (name.isBlank()) {
            _error.value = "Name cannot be empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            when (val result = repo.updateUserProfile(name)) {
                is ResultState.Success -> {
                    // Force refresh the teacher data
                    refreshTeacherData()
                }
                is ResultState.Error -> {
                    _error.value = mapAuthError(result.error)
                }
                is ResultState.Loading -> { /* Already handled */ }
            }

            _isLoading.value = false
        }
    }

    // ==================== Password Reset ====================
    
    /**
     * Send password reset email.
     */
    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _error.value = "Please enter your email"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _passwordResetSent.value = false
            
            when (val result = repo.sendPasswordResetEmail(email)) {
                is ResultState.Success -> {
                    _passwordResetSent.value = true
                }
                is ResultState.Error -> {
                    _error.value = mapAuthError(result.error)
                }
                is ResultState.Loading -> { /* Already handled */ }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Clear password reset sent state.
     */
    fun clearPasswordResetState() {
        _passwordResetSent.value = false
    }

    // ==================== Sign Out ====================

    /**
     * Sign out the current user.
     */
    fun signOut() {
        repo.signOut()
        _currentTeacher.value = null
        _authState.value = AuthState.Unauthenticated
        _verificationEmailSent.value = false
        _pendingVerificationEmail.value = null
        _otpSent.value = false
        _pendingOtpEmail.value = null
        _passwordResetSent.value = false
    }

    // ==================== Utility ====================

    /**
     * Clear any error state.
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Represents the authentication state of the app.
 */
sealed class AuthState {
    object Initial : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}
