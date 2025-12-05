package com.example.tuitionmanager.view.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the Tuition Manager app.
 * Uses Kotlin Serialization for type-safe navigation with Jetpack Navigation.
 */

// ==================== Authentication Routes ====================

@Serializable
object SignInRoute

@Serializable
object SignUpRoute

@Serializable
object ProfileRoute

// ==================== Main App Routes ====================

@Serializable
object DashboardRoute

@Serializable
data class BatchDetailsRoute(val batchId: String)

@Serializable
data class StudentDetailsRoute(val studentId: String)

@Serializable
object AddBatchRoute

@Serializable
data class EditBatchRoute(val batchId: String)

@Serializable
data class AddStudentRoute(val batchId: String? = null)

@Serializable
data class EditStudentRoute(val studentId: String)
