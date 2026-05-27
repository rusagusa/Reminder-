package com.example.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {
    // Alarms
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()

    suspend fun getAlarmById(id: Int): Alarm? = alarmDao.getAlarmById(id)

    suspend fun getAlarmsOnce(): List<Alarm> = alarmDao.getAlarmsOnce()

    suspend fun insert(alarm: Alarm): Long = alarmDao.insertAlarm(alarm)

    suspend fun update(alarm: Alarm) = alarmDao.updateAlarm(alarm)

    suspend fun delete(alarm: Alarm) = alarmDao.deleteAlarm(alarm)

    // Athletic Records
    val allAthleticRecords: Flow<List<AthleticRecord>> = alarmDao.getAllAthleticRecords()

    suspend fun insertRecord(record: AthleticRecord): Long = alarmDao.insertAthleticRecord(record)

    suspend fun deleteRecord(record: AthleticRecord) = alarmDao.deleteAthleticRecord(record)
}
