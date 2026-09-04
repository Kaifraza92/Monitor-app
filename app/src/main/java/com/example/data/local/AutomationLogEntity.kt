package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_logs")
data class AutomationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val executionId: Long? = null,
    val scheduleId: Long? = null,
    val scheduleName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO", // "INFO", "WARN", "ERROR", "SUCCESS"
    val state: String = "IDLE",
    val message: String
)
