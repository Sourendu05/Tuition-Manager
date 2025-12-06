package com.example.tuitionmanager.model.data

/**
 * Represents the teacher (current user) information.
 * Data comes from Firebase Authentication.
 */
data class Teacher(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null
)




