package com.example.tuitionmanager.model.data

import java.util.Date

/**
 * Represents a fee payment record for a specific month.
 * Used as the value in the Student's feesPaid map.
 */
data class FeePayment(
    val status: String = "PAID", // Always "PAID" when present
    val paidAt: Date = Date()    // Server timestamp when payment was recorded
)

