package com.example.tuitionmanager.view.navigation

import androidx.compose.runtime.Composable
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
import com.example.tuitionmanager.view.screens.StudentDetailsScreen
import com.example.tuitionmanager.viewmodel.TuitionViewModel

/**
 * Main navigation composable for the Tuition Manager app.
 * Sets up the NavHost with all screen routes.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: TuitionViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = DashboardRoute
    ) {
        // Dashboard (Home Screen)
        composable<DashboardRoute> {
            DashboardScreen(
                viewModel = viewModel,
                onBatchClick = { batchId ->
                    navController.navigate(BatchDetailsRoute(batchId))
                },
                onAddBatchClick = {
                    navController.navigate(AddBatchRoute)
                }
            )
        }

        // Add Batch Screen
        composable<AddBatchRoute> {
            AddBatchScreen(
                viewModel = viewModel,
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
                viewModel = viewModel,
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
                viewModel = viewModel,
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
                viewModel = viewModel,
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
                viewModel = viewModel,
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
                viewModel = viewModel,
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
