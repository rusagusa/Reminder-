package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.Alarm
import com.example.data.AppDatabase
import com.example.data.AlarmRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class AlarmService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    
    private val _serviceState = MutableStateFlow(AlarmServiceState())
    val serviceState: StateFlow<AlarmServiceState> = _serviceState.asStateFlow()

    private val binder = LocalBinder()
    private var repository: AlarmRepository? = null

    companion object {
        const val CHANNEL_ID = "alarm_service_notifications"
        const val NOTIFICATION_ID = 8271
        var isRunning = false
            private set
    }

    inner class LocalBinder : Binder() {
        fun getService(): AlarmService = this@AlarmService
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()

        val db = AppDatabase.getDatabase(this)
        repository = AlarmRepository(db.alarmDao())

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FitnessAlarm::WakeLock").apply {
            acquire(15 * 60 * 1000L) // 15 mins timeout max
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        Log.d("AlarmService", "AlarmService starting for alarmId: $alarmId")

        if (alarmId != -1) {
            loadAlarmAndStart(alarmId)
        } else {
            if (!_serviceState.value.isActive) {
                initializeStateAndStart(null)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Plays alarm ringtones and shows reminder alerts"
                enableVibration(true)
                setSound(null, null)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun loadAlarmAndStart(alarmId: Int) {
        serviceScope.launch {
            try {
                val alarm = repository?.getAlarmById(alarmId)
                initializeStateAndStart(alarm)
            } catch (e: Exception) {
                Log.e("AlarmService", "Failed to load alarm", e)
                initializeStateAndStart(null)
            }
        }
    }

    private fun initializeStateAndStart(alarm: Alarm?) {
        val label = alarm?.label?.ifBlank { "Reminder Alert" } ?: "Reminder Alert"
        val style = alarm?.ringtoneStyle ?: "Melodic Rise"
        val timeStr = alarm?.formattedTime ?: "Now"

        _serviceState.value = AlarmServiceState(
            isActive = true,
            alarmId = alarm?.id ?: -1,
            alarmLabel = label,
            ringtoneStyle = style,
            formattedTime = timeStr,
            isVibrate = alarm?.isVibrate ?: true
        )

        playAlarmAudio()

        val notificationText = if (alarm != null) {
            "Time for: $label ($timeStr)"
        } else {
            "Active Reminder Triggered"
        }

        val notification = buildForegroundNotification(label, notificationText)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildForegroundNotification(title: String, text: String): Notification {
        // Broad Activity launcher
        val alarmIntent = Intent().setClassName(this, "com.example.ui.AlarmActivity").apply {
            putExtra("ALARM_ID", _serviceState.value.alarmId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    private fun playAlarmAudio() {
        stopAlarmAudio()
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                setVolume(1.0f, 1.0f)
                start()
            }
            Log.d("AlarmService", "Alarm audio play started successfully.")
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to play default alarm sound", e)
        }
    }

    private fun stopAlarmAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    fun snoozeAlarm() {
        Log.d("AlarmService", "Snoozing alarm for 10 minutes")
        val alarmId = _serviceState.value.alarmId
        if (alarmId != -1) {
            serviceScope.launch {
                try {
                    val alarm = repository?.getAlarmById(alarmId)
                    if (alarm != null) {
                        // Reschedule alarm 10 minutes from now
                        val scheduler = AlarmScheduler(this@AlarmService)
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.MINUTE, 10)
                        }
                        val snoozedAlarm = alarm.copy(
                            hour = cal.get(Calendar.HOUR_OF_DAY),
                            minute = cal.get(Calendar.MINUTE),
                            isEnabled = true
                        )
                        // Temporarily save/update in DB or schedule directly
                        scheduler.schedule(snoozedAlarm)
                        Log.d("AlarmService", "Snoozed alarm scheduled for ${snoozedAlarm.formattedTime}")
                    }
                } catch (e: Exception) {
                    Log.e("AlarmService", "Failed to snooze alarm", e)
                } finally {
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }
    }

    fun dismissAlarm() {
        Log.d("AlarmService", "Alarm dismissed.")
        stopSelf()
    }

    override fun onDestroy() {
        isRunning = false
        stopAlarmAudio()
        turnOffCompletedAlarm()

        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        super.onDestroy()
        Log.d("AlarmService", "AlarmService destroyed completely.")
    }

    private fun turnOffCompletedAlarm() {
        val alarmId = _serviceState.value.alarmId
        if (alarmId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarm = repository?.getAlarmById(alarmId)
                    if (alarm != null && alarm.daysOfWeek.isEmpty() && alarm.date.isNullOrEmpty()) {
                        // Disable one-off alarms
                        repository?.update(alarm.copy(isEnabled = false))
                    }
                } catch (e: Exception) {
                    Log.e("AlarmService", "Failed to update alarm status on exit", e)
                }
            }
        }
    }
}

data class AlarmServiceState(
    val isActive: Boolean = false,
    val alarmId: Int = -1,
    val alarmLabel: String = "",
    val ringtoneStyle: String = "Melodic Rise",
    val formattedTime: String = "",
    val isVibrate: Boolean = true
)
