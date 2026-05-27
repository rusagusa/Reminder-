package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean,
    val isVibrate: Boolean = true,
    val daysOfWeek: String = "", // Comma-separated list of calendar API days: "2,3,4,5,6"
    val date: String? = null, // Optional calendar date: "YYYY-MM-DD" for one-off reminders
    val ringtoneStyle: String = "Melodic Rise" // Custom sound style
) {
    val formattedTime: String
        get() {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%02d:%02d %s", displayHour, minute, amPm)
        }

    fun getDaysDisplay(): String {
        if (!date.isNullOrEmpty()) {
            // It's a calendar reminder on a specific date
            return "Calendar Event: $date"
        }
        if (daysOfWeek.isEmpty()) return "Once"
        val days = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (days.size == 7) return "Every day"
        if (days.size == 5 && !days.contains(Calendar.SUNDAY) && !days.contains(Calendar.SATURDAY)) return "Weekdays"
        if (days.size == 2 && days.contains(Calendar.SUNDAY) && days.contains(Calendar.SATURDAY)) return "Weekends"
        
        val dayMap = mapOf(
            Calendar.SUNDAY to "Sun",
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat"
        )
        return days.sorted().mapNotNull { dayMap[it] }.joinToString(", ")
    }
}
