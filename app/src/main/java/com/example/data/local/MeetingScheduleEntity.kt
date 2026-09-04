package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meeting_schedules")
data class MeetingScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val meetUrl: String,
    val startTime: String, // HH:mm format (24hr)
    val endTime: String,   // HH:mm format (24hr)
    val daysOfWeek: String, // Comma separated enum names e.g. "MONDAY,WEDNESDAY,FRIDAY"
    val enabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
