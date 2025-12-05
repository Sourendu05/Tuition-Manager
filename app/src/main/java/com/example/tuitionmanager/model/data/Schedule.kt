package com.example.tuitionmanager.model.data

import java.util.Locale

/**
 * Represents a single schedule entry for a batch.
 * A batch can have multiple schedule entries (e.g., Mon 5 PM, Wed 6 PM).
 * Fields are nullable for Firestore deserialization compatibility.
 */
data class Schedule(
    val day: Int? = 1, // 1=Monday, 2=Tuesday, ..., 7=Sunday
    val time: String? = "05:00 PM" // 12hr format with AM/PM (e.g., "05:30 PM")
) {
    companion object {
        /**
         * Get the day name from the day number.
         */
        fun getDayName(day: Int): String = when (day) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> "Unknown"
        }

        /**
         * Get the full day name from the day number.
         */
        fun getFullDayName(day: Int): String = when (day) {
            1 -> "Monday"
            2 -> "Tuesday"
            3 -> "Wednesday"
            4 -> "Thursday"
            5 -> "Friday"
            6 -> "Saturday"
            7 -> "Sunday"
            else -> "Unknown"
        }

        /**
         * Format time from 24-hour (hour, minute) to 12-hour format string.
         * @param hour 0-23
         * @param minute 0-59
         * @return Formatted string like "05:30 PM"
         */
        fun formatTime12Hour(hour: Int, minute: Int): String {
            val period = if (hour < 12) "AM" else "PM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, period)
        }

        /**
         * Parse a 12-hour format time string to (hour24, minute).
         * @param time Format: "05:30 PM" or "12:00 AM"
         * @return Pair of (hour in 24hr format, minute) or null if invalid
         */
        fun parseTime12Hour(time: String): Pair<Int, Int>? {
            return try {
                val cleanTime = time.trim().uppercase()
                val isPM = cleanTime.endsWith("PM")
                val isAM = cleanTime.endsWith("AM")
                if (!isPM && !isAM) return null

                val timePart = cleanTime.replace("AM", "").replace("PM", "").trim()
                val parts = timePart.split(":")
                if (parts.size != 2) return null

                var hour = parts[0].toInt()
                val minute = parts[1].toInt()

                // Convert to 24-hour format
                hour = when {
                    hour == 12 && isAM -> 0        // 12 AM = 0
                    hour == 12 && isPM -> 12       // 12 PM = 12
                    isPM -> hour + 12              // 1-11 PM = 13-23
                    else -> hour                   // 1-11 AM = 1-11
                }

                Pair(hour, minute)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Get a sortable key from a 12-hour time string.
         * Used for sorting schedules by time.
         */
        fun getSortableTimeKey(time: String): Int {
            val parsed = parseTime12Hour(time) ?: return 0
            return parsed.first * 60 + parsed.second
        }
    }

    /**
     * Get a short display string for this schedule entry.
     */
    fun getDisplayString(): String = "${day?.let { getDayName(it) } ?: "Unknown"} ${time ?: ""}"

    /**
     * Get a full display string for this schedule entry.
     */
    fun getFullDisplayString(): String = "${day?.let { getFullDayName(it) } ?: "Unknown"} at ${time ?: ""}"
}
