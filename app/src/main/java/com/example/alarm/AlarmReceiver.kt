package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        Log.d("AlarmReceiver", "onReceive triggered with action: $action, alarmId: $alarmId")

        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == "android.intent.action.QUICKBOOT_POWERON" || 
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {
            
            val database = AppDatabase.getDatabase(context)
            val repository = AlarmRepository(database.alarmDao())
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = repository.getAlarmsOnce()
                    alarms.forEach { alarm ->
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                        }
                    }
                    Log.d("AlarmReceiver", "Rescheduled active alarms post boot successfully.")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error rescheduling alarms on boot", e)
                }
            }
        } else {
            if (alarmId != -1) {
                // Launch the persistent alarm foreground service
                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("ALARM_ID", alarmId)
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Log.d("AlarmReceiver", "Successfully launched AlarmService for Alarm ID: $alarmId")
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Failed to start AlarmService", e)
                }
            } else {
                Log.w("AlarmReceiver", "Received trigger broadcast but Alarm ID was invalid (-1)")
            }
        }
    }
}
