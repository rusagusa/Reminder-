package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.Alarm
import com.example.data.AppDatabase
import com.example.data.AlarmRepository
import com.example.data.AthleticRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlarmRepository
    private val scheduler: AlarmScheduler

    val allAlarms: StateFlow<List<Alarm>>
    val allAthleticRecords: StateFlow<List<AthleticRecord>>

    init {
        val alarmDao = AppDatabase.getDatabase(application).alarmDao()
        repository = AlarmRepository(alarmDao)
        scheduler = AlarmScheduler(application)
        
        allAlarms = repository.allAlarms.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAthleticRecords = repository.allAthleticRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insert(alarm: Alarm) {
        viewModelScope.launch {
            val id = repository.insert(alarm)
            val updated = alarm.copy(id = id.toInt())
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            }
        }
    }

    fun update(alarm: Alarm) {
        viewModelScope.launch {
            repository.update(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }

    fun delete(alarm: Alarm) {
        viewModelScope.launch {
            scheduler.cancel(alarm)
            repository.delete(alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.update(updatedAlarm)
            if (updatedAlarm.isEnabled) {
                scheduler.schedule(updatedAlarm)
            } else {
                scheduler.cancel(updatedAlarm)
            }
        }
    }

    // Athletic/Workout Log Records
    fun insertAthleticRecord(record: AthleticRecord) {
        viewModelScope.launch {
            repository.insertRecord(record)
        }
    }

    fun deleteAthleticRecord(record: AthleticRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
        }
    }
}
