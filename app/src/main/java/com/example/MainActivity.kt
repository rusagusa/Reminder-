package com.example

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.Alarm
import com.example.data.AthleticRecord
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainDashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

enum class DashboardTab {
    REMINDERS,
    WORKOUT_TIMER,
    ATHLETIC_LOGS
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(DashboardTab.REMINDERS) }
    
    val alarms by viewModel.allAlarms.collectAsState()
    val records by viewModel.allAthleticRecords.collectAsState()

    // Permission check
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var hasExactAlarmPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
        )
    }

    val requestNotificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            hasExactAlarmPermission = alarmManager.canScheduleExactAlarms()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showAddEditAlarmDialog by remember { mutableStateOf(false) }
    var alarmToEdit by remember { mutableStateOf<Alarm?>(null) }
    var showAddRecordDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Zenith Fit",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "Reminders, Timer & Workout Tracker",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Header status badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsRun,
                            contentDescription = "Run Badge",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Athlete Mode",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Permissions warning Banner
            if (!hasNotificationPermission || !hasExactAlarmPermission) {
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    WarningCard(
                        missingNotification = !hasNotificationPermission,
                        missingExactAlarm = !hasExactAlarmPermission,
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onRequestExactAlarm = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Settings.ACTION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    )
                }
            }

            // Modern Elegant Tab Switcher
            TabBar(
                selectedTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )

            // Dynamic Tab Contents
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                when (currentTab) {
                    DashboardTab.REMINDERS -> {
                        RemindersTabContent(
                            alarms = alarms,
                            onToggleAlarm = { viewModel.toggleAlarm(it) },
                            onEditAlarm = {
                                alarmToEdit = it
                                showAddEditAlarmDialog = true
                            },
                            onAddNewAlarmClick = {
                                alarmToEdit = null
                                showAddEditAlarmDialog = true
                            }
                        )
                    }
                    DashboardTab.WORKOUT_TIMER -> {
                        TimerTabContent(modifier = Modifier.fillMaxSize())
                    }
                    DashboardTab.ATHLETIC_LOGS -> {
                        AthleticLogsTabContent(
                            records = records,
                            onDeleteRecord = { viewModel.deleteAthleticRecord(it) },
                            onAddNewRecordClick = {
                                showAddRecordDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Add/Edit Alarm dialog
    if (showAddEditAlarmDialog) {
        AddEditAlarmDialog(
            alarm = alarmToEdit,
            onDismiss = { showAddEditAlarmDialog = false },
            onSave = { hour, minute, label, isEnabled, isVibration, days, dateStr, sound ->
                val updated = Alarm(
                    id = alarmToEdit?.id ?: 0,
                    hour = hour,
                    minute = minute,
                    label = label,
                    isEnabled = isEnabled,
                    isVibrate = isVibration,
                    daysOfWeek = days,
                    date = dateStr,
                    ringtoneStyle = sound
                )
                if (alarmToEdit == null) {
                    viewModel.insert(updated)
                } else {
                    viewModel.update(updated)
                }
                showAddEditAlarmDialog = false
            },
            onDelete = {
                alarmToEdit?.let { viewModel.delete(it) }
                showAddEditAlarmDialog = false
            }
        )
    }

    // Modal Add Athletic record dialog
    if (showAddRecordDialog) {
        AddRecordDialog(
            onDismiss = { showAddRecordDialog = false },
            onSave = { activity, value, notes ->
                val dateStamp = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                val newRecord = AthleticRecord(
                    activityName = activity,
                    recordValue = value,
                    dateString = dateStamp,
                    notes = notes
                )
                viewModel.insertAthleticRecord(newRecord)
                showAddRecordDialog = false
            }
        )
    }
}

@Composable
fun WarningCard(
    missingNotification: Boolean,
    missingExactAlarm: Boolean,
    onRequestNotification: () -> Unit,
    onRequestExactAlarm: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Permission Issue",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Setup Incomplete",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Authorize permissions to ensure your workout reminders fire reliably and alerts ring loudly.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (missingNotification) {
                    TextButton(onClick = onRequestNotification) {
                        Text("Alert Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (missingExactAlarm) {
                    TextButton(onClick = onRequestExactAlarm) {
                        Text("Exact Alarm Clock", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun TabBar(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf(
                DashboardTab.REMINDERS to "Alarms",
                DashboardTab.WORKOUT_TIMER to "Timer",
                DashboardTab.ATHLETIC_LOGS to "Logs"
            )

            tabs.forEach { (tab, title) ->
                val isSelected = selectedTab == tab
                val bgTint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val contentTint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgTint)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = contentTint
                        )
                    )
                }
            }
        }
    }
}

// 1. REMINDERS TAB CONTENT
@Composable
fun RemindersTabContent(
    alarms: List<Alarm>,
    onToggleAlarm: (Alarm) -> Unit,
    onEditAlarm: (Alarm) -> Unit,
    onAddNewAlarmClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Reminders Scheduled",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            FilledTonalButton(
                onClick = onAddNewAlarmClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Schedule", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Schedule")
            }
        }

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.AlarmOff,
                        contentDescription = "Empty Alarms",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Reminders Found", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Add dynamic alarms for hydration, exercise, or training.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp)
            ) {
                items(alarms, key = { it.id }) { alarm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditAlarm(alarm) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (alarm.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = alarm.formattedTime,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ) {
                                        Text(
                                            text = alarm.ringtoneStyle,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alarm.getDaysDisplay(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (alarm.label.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = alarm.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Switch(
                                checked = alarm.isEnabled,
                                onCheckedChange = { onToggleAlarm(alarm) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 2. STOPWATCH/COUNTDOWN TIMER TAB CONTENT
@Composable
fun TimerTabContent(modifier: Modifier = Modifier) {
    var timerDurationMs by remember { mutableLongStateOf(60000L) } // 1 minute default
    var remainingTimeMs by remember { mutableLongStateOf(60000L) }
    var isTimerRunning by remember { mutableStateOf(false) }

    // Preset intervals for athletic recovery reps or rest times
    val presets = listOf(
        "30s" to 30000L,
        "1m" to 60000L,
        "2m" to 120000L,
        "5m" to 300000L
    )

    LaunchedEffect(isTimerRunning, remainingTimeMs) {
        if (isTimerRunning && remainingTimeMs > 0) {
            delay(100L)
            remainingTimeMs = (remainingTimeMs - 100L).coerceAtLeast(0)
            if (remainingTimeMs == 0L) {
                isTimerRunning = false
                // Play notification alert tone to signify timer done
                try {
                    val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000)
                } catch (e: Exception) {
                    // fallthrough
                }
            }
        }
    }

    val minutes = (remainingTimeMs / 60000).toInt()
    val seconds = ((remainingTimeMs % 60000) / 1000).toInt()
    val milliseconds = ((remainingTimeMs % 1000) / 100).toInt()
    val timeDisplay = String.format("%02d:%02d.%d", minutes, seconds, milliseconds)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Interval Recovery Timer",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
        )

        // Huge Circular Timer Face
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeDisplay,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 42.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Text(
                    text = if (isTimerRunning) "RECOVERING" else "IDLE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                )
            }
        }

        // Quick Presets Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { (label, duration) ->
                OutlinedButton(
                    onClick = {
                        isTimerRunning = false
                        timerDurationMs = duration
                        remainingTimeMs = duration
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = if (timerDurationMs == duration) ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(label, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Play/Pause controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { isTimerRunning = !isTimerRunning },
                modifier = Modifier
                    .weight(1.5f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = if (isTimerRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Trigger button"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isTimerRunning) "PAUSE TIMER" else "START TIMER", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    isTimerRunning = false
                    remainingTimeMs = timerDurationMs
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reset timer")
                Spacer(modifier = Modifier.width(4.dp))
                Text("RESET")
            }
        }
        
        Spacer(modifier = Modifier.height(1.dp))
    }
}

// 3. ATHLETIC RECORD LOGS TAB CONTENT
@Composable
fun AthleticLogsTabContent(
    records: List<AthleticRecord>,
    onDeleteRecord: (AthleticRecord) -> Unit,
    onAddNewRecordClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Personal Achievements & Records",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            FilledTonalButton(
                onClick = onAddNewRecordClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = "Log", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Log PR")
            }
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "No PR records",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Records Saved Yet", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text("Bench marks, run times, or repetitions go here.", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Stars,
                                        contentDescription = "Star Indicator",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = record.activityName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteRecord(record) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Delete record",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = record.recordValue,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = record.dateString,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            if (record.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = record.notes,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. ADD / EDIT ALARM DIALOG CONFIG
@Composable
fun AddEditAlarmDialog(
    alarm: Alarm?,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, label: String, isEnabled: Boolean, isVibrate: Boolean, days: String, date: String?, ringtone: String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var selectedHour by remember { mutableStateOf(alarm?.hour ?: 7) }
    var selectedMinute by remember { mutableStateOf(alarm?.minute ?: 0) }
    var labelInput by remember { mutableStateOf(alarm?.label ?: "") }
    var isVibrateChecked by remember { mutableStateOf(alarm?.isVibrate ?: true) }
    var ringtoneChosen by remember { mutableStateOf(alarm?.ringtoneStyle ?: "Melodic Rise") }
    
    // Calendar specific reminder date
    var selectedDateStr by remember { mutableStateOf<String?>(alarm?.date) }

    val daysList = remember {
        mutableStateListOf<Int>().apply {
            if (alarm != null && alarm.daysOfWeek.isNotEmpty()) {
                addAll(alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() })
            }
        }
    }

    val displayHour = when {
        selectedHour == 0 -> 12
        selectedHour > 12 -> selectedHour - 12
        else -> selectedHour
    }
    val amPm = if (selectedHour >= 12) "PM" else "AM"
    val timeLabelText = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("add_edit_alarm_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = if (alarm == null) "Schedule Reminder" else "Edit Reminder Plan",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Clock Picker triggers
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .clickable {
                                TimePickerDialog(context, { _, h, m ->
                                    selectedHour = h
                                    selectedMinute = m
                                }, selectedHour, selectedMinute, false).show()
                            }
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeLabelText,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Text("TAP TO CHANGE TIME", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Label Text input
                item {
                    OutlinedTextField(
                        value = labelInput,
                        onValueChange = { labelInput = it },
                        label = { Text("Label (e.g. Sprint laps, Hydrate)") },
                        modifier = Modifier.fillMaxWidth().testTag("alarm_label_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Calendar event date select button
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Calendar Day (Optional)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    val nowCal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            selectedDateStr = String.format("%04d-%02d-%02d", y, m + 1, d)
                                        },
                                        nowCal.get(Calendar.YEAR),
                                        nowCal.get(Calendar.MONTH),
                                        nowCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = "Cal icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedDateStr ?: "On-demand (No explicit calendar limit)",
                                    fontSize = 13.sp,
                                    color = if (selectedDateStr != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedDateStr != null) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear date",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { selectedDateStr = null }
                                )
                            }
                        }
                    }
                }

                // Repeat selector row (only enabled if specific catalog calendar date limit is empty)
                if (selectedDateStr == null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Repeat Days of Week", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val daysMap = listOf(
                                    Calendar.SUNDAY to "Sun",
                                    Calendar.MONDAY to "Mon",
                                    Calendar.TUESDAY to "Tue",
                                    Calendar.WEDNESDAY to "Wed",
                                    Calendar.THURSDAY to "Thu",
                                    Calendar.FRIDAY to "Fri",
                                    Calendar.SATURDAY to "Sat"
                                )

                                daysMap.forEach { (calVal, strName) ->
                                    val checked = daysList.contains(calVal)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .clickable {
                                                if (checked) daysList.remove(calVal) else daysList.add(calVal)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strName.first().toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Ringtone Style Sound picker
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Custom Ringtone Alarm Style", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val soundPacks = listOf("Melodic Rise", "Zen Bowl", "Classic Beeps", "Digital Siren")
                            soundPacks.forEach { item ->
                                val selected = ringtoneChosen == item
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(
                                            width = if (selected) 1.5.dp else 0.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { ringtoneChosen = item }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.split(" ").first(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // More preferences: Vibrate
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vibration Enabled", fontSize = 14.sp)
                        Switch(checked = isVibrateChecked, onCheckedChange = { isVibrateChecked = it })
                    }
                }

                // Final Controls
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (alarm != null) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }

                        Button(
                            onClick = {
                                val daysJoinedStr = if (selectedDateStr != null) "" else daysList.sorted().joinToString(",")
                                onSave(selectedHour, selectedMinute, labelInput, true, isVibrateChecked, daysJoinedStr, selectedDateStr, ringtoneChosen)
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SAVE TARGET REMINDER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 5. ADD ATHLETIC ACHIEVEMENT DIALOG
@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (activityName: String, recordValue: String, notes: String) -> Unit
) {
    var activityInput by remember { mutableStateOf("") }
    var scoreInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val exercises = listOf("5K Sprint Run", "100m Sprint", "Plank Duration", "Bench Press Max", "Swimming Laps", "Heart Rate Sleep")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        "Track Personal Best (PR)",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Presets Activity", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            exercises.take(3).forEach { prep ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                        .clickable { activityInput = prep }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(prep.split(" ").first(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = activityInput,
                        onValueChange = { activityInput = it },
                        label = { Text("Activity/Exercise Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = scoreInput,
                        onValueChange = { scoreInput = it },
                        label = { Text("Record (e.g., 18:32 mins, 43 reps)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Workout Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("CANCEL")
                        }

                        Button(
                            onClick = {
                                if (activityInput.isNotBlank() && scoreInput.isNotBlank()) {
                                    onSave(activityInput, scoreInput, notesInput)
                                }
                            },
                            modifier = Modifier.weight(1.5f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = activityInput.isNotBlank() && scoreInput.isNotBlank()
                        ) {
                            Text("LOG ACHIEVEMENT", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}
