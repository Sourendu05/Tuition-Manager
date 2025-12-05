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
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // For tracking verification email sent state
    private val _verificationEmailSent = MutableStateFlow(false)
    val verificationEmailSent: StateFlow<Boolean> = _verificationEmailSent.asStateFlow()
    
    private val _pendingVerificationEmail = MutableStateFlow<String?>(null)
    val pendingVerificationEmail: StateFlow<String?> = _pendingVerificationEmail.asStateFlow()

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
                            _authState.value = AuthState.Authenticated
                        }
                    }
                    is ResultState.Error -> {
                        // Don't show error for teacher loading
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
     */
    private fun mapAuthError(errorMessage: String): String {
        return when {
            errorMessage.contains("EMAIL_NOT_VERIFIED", ignoreCase = true) -> 
                "Please verify your email first"
            errorMessage.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
            errorMessage.contains("invalid", ignoreCase = true) && errorMessage.contains("credential", ignoreCase = true) ->
                "Incorrect email or password"
            errorMessage.contains("user-not-found", ignoreCase = true) ||
            errorMessage.contains("no user record", ignoreCase = true) ->
                "No account found with this email"
            errorMessage.contains("wrong-password", ignoreCase = true) ->
                "Incorrect password"
            errorMessage.contains("email-already-in-use", ignoreCase = true) ||
            errorMessage.contains("already exists", ignoreCase = true) ->
                "An account with this email already exists"
            errorMessage.contains("weak-password", ignoreCase = true) ->
                "Password is too weak"
            errorMessage.contains("invalid-email", ignoreCase = true) ||
            errorMessage.contains("badly formatted", ignoreCase = true) ->
                "Please enter a valid email address"
            errorMessage.contains("network", ignoreCase = true) ->
                "No internet connection"
            errorMessage.contains("too-many-requests", ignoreCase = true) ->
                "Too many attempts. Please try again later"
            errorMessage.contains("No credentials available", ignoreCase = true) ||
            errorMessage.contains("NoCredentialException", ignoreCase = true) ->
                "No Google accounts found on this device"
            errorMessage.contains("canceled", ignoreCase = true) ||
            errorMessage.contains("cancelled", ignoreCase = true) ->
                "Sign in was cancelled"
            else -> "Something went wrong. Please try again"
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
            _error.value = "Password must be at least 6 characters"
            return
        }

        viewModelScope.launch {
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

            _isLoading.value = false
        }
    }
    
    /**
     * Clear verification sent state.
     */
    fun clearVerificationState() {
        _verificationEmailSent.value = false
        _pendingVerificationEmail.value = null
    }

    // ==================== Google Sign In ====================

    /**
     * Initiate Google Sign In using Credential Manager.
     * This should be called from a Composable with access to Context.
     */
    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

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
            } catch (e: NoCredentialException) {
                _error.value = "No Google accounts found on this device"
                _isLoading.value = false
            } catch (e: GetCredentialCancellationException) {
                // User cancelled - don't show error
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = mapAuthError(e.message ?: "Google sign in failed")
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
                        _error.value = "Failed to sign in with Google"
                    }
                } else {
                    _error.value = "Sign in failed. Please try again"
                }
            }
            else -> {
                _error.value = "Sign in failed. Please try again"
            }
        }

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
