package com.example.domain.model

enum class ExecutionStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    MISSED("Missed"),
    CANCELLED("Cancelled")
}

data class ExecutionRecord(
    val id: Long = 0,
    val scheduleId: Long,
    val scheduleName: String,
    val meetUrl: String,
    val scheduledStart: Long,
    val scheduledEnd: Long,
    val actualJoinTime: Long? = null,
    val actualLeaveTime: Long? = null,
    val status: ExecutionStatus,
    val failureReason: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AutomationLogItem(
    val id: Long = 0,
    val executionId: Long? = null,
    val scheduleId: Long? = null,
    val scheduleName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val level: String = "INFO",
    val state: String = "IDLE",
    val message: String
)
