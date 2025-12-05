package com.example.tuitionmanager

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.tuitionmanager.ui.theme.TuitionManagerTheme
import com.example.tuitionmanager.view.navigation.AppNavigation
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var firebaseAuth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TuitionManagerTheme {
                AuthenticationWrapper()
            }
        }
    }
    
    @Composable
    private fun AuthenticationWrapper() {
        var isAuthenticating by remember { mutableStateOf(true) }
        
        LaunchedEffect(Unit) {
            // Check if user is already signed in
            if (firebaseAuth.currentUser != null) {
                Log.d("MainActivity", "User already signed in: ${firebaseAuth.currentUser?.uid}")
                isAuthenticating = false
            } else {
                // Sign in anonymously
                Log.d("MainActivity", "Signing in anonymously...")
                firebaseAuth.signInAnonymously()
                    .addOnSuccessListener { authResult ->
                        Log.d("MainActivity", "Anonymous sign-in successful: ${authResult.user?.uid}")
                        isAuthenticating = false
                    }
                    .addOnFailureListener { e ->
                        Log.e("MainActivity", "Anonymous sign-in failed", e)
                        isAuthenticating = false
                    }
            }
        }
        
        if (isAuthenticating) {
            // Show loading indicator while authenticating
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Show app navigation once authenticated
            Box(modifier = Modifier.fillMaxSize()) {
                AppNavigation()
            }
        }
    }
}