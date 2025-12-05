package com.example.tuitionmanager.model.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a batch/class that a teacher manages.
 * Maps to Firestore: teachers/{uid}/batches/{batchId}
 */
data class Batch(
    @DocumentId
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val standard: String? = null, // Optional, e.g., "10th"
    val feeAmount: Double = 0.0,
    val schedule: List<Schedule> = emptyList(),
    @ServerTimestamp
    val creationDate: Timestamp? = null // When the batch was created
) {
    // Convert Timestamp to Date for compatibility with existing code
    fun getCreationDateAsDate(): Date = creationDate?.toDate() ?: Date()

    /**
     * Get a summary of schedule days for display (e.g., "Mon, Wed, Fri").
     */
    fun getScheduleSummary(): String {
        return schedule
            .sortedBy { it.day }
            .mapNotNull { it.day?.let { day -> Schedule.getDayName(day) } }
            .distinct()
            .joinToString(", ")
    }

    /**
     * Check if this batch has a class on the given day.
     * @param dayOfWeek 1=Monday, 7=Sunday
     */
    fun hasClassOnDay(dayOfWeek: Int): Boolean {
        return schedule.any { it.day == dayOfWeek }
    }

    /**
     * Get all schedule entries for a specific day, sorted by time.
     */
    fun getScheduleForDay(dayOfWeek: Int): List<Schedule> {
        return schedule
            .filter { it.day == dayOfWeek }
            .sortedBy { it.time?.let { time -> Schedule.getSortableTimeKey(time) } ?: 0 }
    }

    /**
     * Get formatted creation date string.
     */
    fun getFormattedCreationDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(getCreationDateAsDate())
    }

    /**
     * Get the month-year key for the creation date.
     * Format: "MM-yyyy"
     */
    fun getCreationMonthKey(): String {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        return sdf.format(getCreationDateAsDate())
    }
}
