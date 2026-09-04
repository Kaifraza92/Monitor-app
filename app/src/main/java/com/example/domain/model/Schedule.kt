package com.example.domain.model

data class Schedule(
    val id: Long = 0,
    val name: String,
    val meetUrl: String,
    val startTime: String, // HH:mm format (e.g. "19:00")
    val endTime: String,   // HH:mm format (e.g. "20:00")
    val daysOfWeek: Set<AppDayOfWeek>,
    val enabled: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
