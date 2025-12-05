package com.example.tuitionmanager.model.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Represents a fee payment record for a specific month.
 * Used as the value in the Student's feesPaid map.
 */
data class FeePayment(
    val status: String = "PAID", // Always "PAID" when present
    @ServerTimestamp
    val paidAt: Timestamp? = null // Server timestamp when payment was recorded
) {
    // Convert Timestamp to Date for compatibility
    fun getPaidAtAsDate(): Date = paidAt?.toDate() ?: Date()
}


