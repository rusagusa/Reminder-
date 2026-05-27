package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "athletic_records")
data class AthleticRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val activityName: String,
    val recordValue: String,
    val dateString: String,
    val notes: String = ""
)
