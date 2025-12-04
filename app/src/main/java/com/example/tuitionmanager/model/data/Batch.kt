package com.example.tuitionmanager.model.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Represents a batch/class that a teacher manages.
 * Maps to Firestore: teachers/{uid}/batches/{batchId}
 */
data class Batch(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val standard: String? = null, // Optional, e.g., "10th"
    val feeAmount: Double = 0.0,
    val schedule: List<Schedule> = emptyList(),
    val creationDate: Date = Date() // When the batch was created
) {
    /**
     * Get a summary of schedule days for display (e.g., "Mon, Wed, Fri").
     */
    fun getScheduleSummary(): String {
        return schedule
            .sortedBy { it.day }
            .map { Schedule.getDayName(it.day) }
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
            .sortedBy { Schedule.getSortableTimeKey(it.time) }
    }

    /**
     * Get formatted creation date string.
     */
    fun getFormattedCreationDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(creationDate)
    }

    /**
     * Get the month-year key for the creation date.
     * Format: "MM-yyyy"
     */
    fun getCreationMonthKey(): String {
        val sdf = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        return sdf.format(creationDate)
    }
}
