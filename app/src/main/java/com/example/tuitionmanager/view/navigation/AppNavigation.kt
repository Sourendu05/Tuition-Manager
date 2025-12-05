package com.example.tuitionmanager.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.tuitionmanager.view.screens.AddBatchScreen
import com.example.tuitionmanager.view.screens.AddStudentScreen
import com.example.tuitionmanager.view.screens.BatchDetailsScreen
import com.example.tuitionmanager.view.screens.DashboardScreen
import com.example.tuitionmanager.view.screens.EditBatchScreen
import com.example.tuitionmanager.view.screens.EditStudentScreen
import com.example.tuitionmanager.view.screens.ProfileScreen
import com.example.tuitionmanager.view.screens.SignInScreen
import com.example.tuitionmanager.view.screens.SignUpScreen
import com.example.tuitionmanager.view.screens.StudentDetailsScreen
import com.example.tuitionmanager.viewmodel.AuthState
import com.example.tuitionmanager.viewmodel.AuthViewModel
import com.example.tuitionmanager.viewmodel.TuitionViewModel

/**
 * Main navigation composable for the Tuition Manager app.
 * Sets up the NavHost with all screen routes.
 * Handles authentication state to determine start destination.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val tuitionViewModel: TuitionViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val authState by authViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination: Any = when (authState) {
        is AuthState.Authenticated -> DashboardRoute
        is AuthState.Unauthenticated -> SignInRoute
        is AuthState.Initial -> SignInRoute // Default to sign in while checking
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ==================== Authentication Routes ====================

        // Sign In Screen
        composable<SignInRoute> {
            SignInScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate(SignUpRoute)
                },
                onSignInSuccess = {
                    // Clear back stack and navigate to Dashboard
                    navController.navigate(DashboardRoute) {
                        popUpTo(SignInRoute) { inclusive = true }
                    }
                }
            )
        }

        // Sign Up Screen
        composable<SignUpRoute> {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignUpSuccess = {
                    // Clear back stack and navigate to Dashboard
                    navController.navigate(DashboardRoute) {
                        popUpTo(SignInRoute) { inclusive = true }
                    }
                }
            )
        }

        // Profile Screen
        composable<ProfileRoute> {
            ProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSignOut = {
                    // Clear entire back stack and navigate to Sign In
                    navController.navigate(SignInRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ==================== Main App Routes ====================

        // Dashboard (Home Screen)
        composable<DashboardRoute> {
            DashboardScreen(
                viewModel = tuitionViewModel,
                onBatchClick = { batchId ->
                    navController.navigate(BatchDetailsRoute(batchId))
                },
                onAddBatchClick = {
                    navController.navigate(AddBatchRoute)
                },
                onProfileClick = {
                    navController.navigate(ProfileRoute)
                }
            )
        }

        // Add Batch Screen
        composable<AddBatchRoute> {
            AddBatchScreen(
                viewModel = tuitionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Batch Screen
        composable<EditBatchRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditBatchRoute>()
            EditBatchScreen(
                batchId = route.batchId,
                viewModel = tuitionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Batch Details Screen
        composable<BatchDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<BatchDetailsRoute>()
            BatchDetailsScreen(
                batchId = route.batchId,
                viewModel = tuitionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onStudentClick = { studentId ->
                    navController.navigate(StudentDetailsRoute(studentId))
                },
                onAddStudentClick = {
                    navController.navigate(AddStudentRoute(route.batchId))
                },
                onEditBatchClick = {
                    navController.navigate(EditBatchRoute(route.batchId))
                }
            )
        }

        // Add Student Screen
        composable<AddStudentRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<AddStudentRoute>()
            AddStudentScreen(
                batchId = route.batchId,
                viewModel = tuitionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Edit Student Screen
        composable<EditStudentRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<EditStudentRoute>()
            EditStudentScreen(
                studentId = route.studentId,
                viewModel = tuitionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Student Details Screen
        composable<StudentDetailsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<StudentDetailsRoute>()
            StudentDetailsScreen(
                studentId = route.studentId,
                viewModel = tuitionViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onEditClick = {
                    navController.navigate(EditStudentRoute(route.studentId))
                }
            )
        }
    }
}
