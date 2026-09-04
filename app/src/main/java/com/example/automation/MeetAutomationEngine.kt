package com.example.automation

import com.example.domain.model.Schedule

interface MeetingAutomationEngine {
    suspend fun join(schedule: Schedule, executionId: Long): AutomationResult
    suspend fun leave(executionId: Long): AutomationResult
    suspend fun testRun(schedule: Schedule, durationSeconds: Int = 30): AutomationResult
    fun cancelActiveSession()
}
