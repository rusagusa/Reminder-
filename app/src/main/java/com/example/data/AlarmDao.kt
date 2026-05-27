package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    // Alarms
    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): Alarm?

    @Query("SELECT * FROM alarms")
    suspend fun getAlarmsOnce(): List<Alarm>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    // Athletic Records
    @Query("SELECT * FROM athletic_records ORDER BY id DESC")
    fun getAllAthleticRecords(): Flow<List<AthleticRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAthleticRecord(record: AthleticRecord): Long

    @Delete
    suspend fun deleteAthleticRecord(record: AthleticRecord)
}
