package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meeting_executions")
data class MeetingExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val scheduleName: String,
    val meetUrl: String,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val actualJoinTime: Long? = null,
    val actualLeaveTime: Long? = null,
    val status: String, // "SCHEDULED", "IN_PROGRESS", "COMPLETED", "FAILED", "MISSED", "CANCELLED"
    val failureReason: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
