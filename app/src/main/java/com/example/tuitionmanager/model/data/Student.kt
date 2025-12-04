package com.example.tuitionmanager.model.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a student enrolled in a batch.
 * Maps to Firestore: teachers/{uid}/students/{studentId}
 */
data class Student(
    val id: String = UUID.randomUUID().toString(),
    val batchId: String = "",
    val name: String = "",
    val phone: String = "",
    val joiningDate: Date = Date(),
    val feesPaid: Map<String, FeePayment> = emptyMap() // Key: "MM-YYYY", e.g., "12-2025"
) {
    companion object {
        /**
         * Generate the month key for a given date.
         * Format: "MM-YYYY" (e.g., "01-2025", "12-2025")
         */
        fun generateMonthKey(date: Date = Date()): String {
            val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            return sdf.format(date)
        }

        /**
         * Generate month key from month and year.
         */
        fun generateMonthKey(month: Int, year: Int): String {
            return String.format(Locale.getDefault(), "%02d-%04d", month, year)
        }

        /**
         * Parse a month key to get month and year.
         * @return Pair of (month, year) or null if invalid
         */
        fun parseMonthKey(monthKey: String): Pair<Int, Int>? {
            return try {
                val parts = monthKey.split("-")
                if (parts.size == 2) {
                    Pair(parts[0].toInt(), parts[1].toInt())
                } else null
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Get a display string for a month key.
         * e.g., "12-2025" -> "December 2025"
         */
        fun getMonthDisplayString(monthKey: String): String {
            val parsed = parseMonthKey(monthKey) ?: return monthKey
            val calendar = Calendar.getInstance().apply {
                set(Calendar.MONTH, parsed.first - 1)
                set(Calendar.YEAR, parsed.second)
            }
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            return sdf.format(calendar.time)
        }
    }

    /**
     * Check if fee is paid for a specific month.
     * @param monthKey Format: "MM-YYYY"
     */
    fun isFeePaidFor(monthKey: String): Boolean {
        return feesPaid.containsKey(monthKey)
    }

    /**
     * Check if fee is paid for the current month.
     */
    fun isCurrentMonthFeePaid(): Boolean {
        return isFeePaidFor(generateMonthKey())
    }

    /**
     * Get formatted joining date string.
     */
    fun getFormattedJoiningDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(joiningDate)
    }
}

