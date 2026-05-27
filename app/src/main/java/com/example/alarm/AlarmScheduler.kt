package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import com.example.data.Alarm
import java.util.Calendar
import java.util.Date

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Cannot schedule exact alarms! Permission not granted.")
            }
        }

        val triggerTimeMs = calculateNextTriggerTime(alarm.hour, alarm.minute, alarm.daysOfWeek, alarm.date)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ACTION_TRIGGER"
            putExtra("ALARM_ID", alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, showPendingIntent)
        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d("AlarmScheduler", "Alarm scheduled for ID ${alarm.id} at $triggerTimeMs (${Date(triggerTimeMs)})")
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException scheduling exact alarm", e)
        }
    }

    fun cancel(alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.alarm.ACTION_TRIGGER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Alarm canceled for ID ${alarm.id}")
        }
    }

    private fun calculateNextTriggerTime(hour: Int, minute: Int, daysOfWeekStr: String, dateStr: String?): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()

        // 1. If explicit Calendar standard date is configured
        if (!dateStr.isNullOrEmpty()) {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                val month = (parts[1].toIntOrNull() ?: 1) - 1 // Calendar months is 0-indexed
                val day = parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                
                // If it is already in the past, let it trigger tomorrow or stay in place (standard system alert)
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }
        }

        // 2. Normal Days of the week repeat
        if (daysOfWeekStr.isEmpty()) {
            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        } else {
            val repeatingDays = daysOfWeekStr.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .sorted()
            
            if (repeatingDays.isEmpty()) {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }

            val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            var minDelayDays = 8

            for (day in repeatingDays) {
                var delay = day - currentDayOfWeek
                if (delay < 0) {
                    delay += 7
                } else if (delay == 0) {
                    if (calendar.timeInMillis <= now) {
                        delay = 7 // Scheduled for same day next week
                    }
                }
                if (delay < minDelayDays) {
                    minDelayDays = delay
                }
            }

            if (minDelayDays == 8) {
                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis
            }

            calendar.add(Calendar.DAY_OF_YEAR, minDelayDays)
            return calendar.timeInMillis
        }
    }
}
