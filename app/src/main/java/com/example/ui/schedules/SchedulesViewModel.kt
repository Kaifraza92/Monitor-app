package com.example.ui.schedules

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlzuhraApp
import com.example.automation.ActiveSession
import com.example.automation.AutomationResult
import com.example.automation.SessionManager
import com.example.domain.model.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SchedulesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AlzuhraApp

    val schedules: StateFlow<List<Schedule>> = app.scheduleRepository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<ActiveSession?> = app.sessionManager.currentSession

    private val _testRunMessage = MutableStateFlow<String?>(null)
    val testRunMessage: StateFlow<String?> = _testRunMessage.asStateFlow()

    fun toggleEnabled(schedule: Schedule, enabled: Boolean) {
        viewModelScope.launch {
            app.scheduleRepository.setScheduleEnabled(schedule.id, enabled)
            if (enabled) {
                app.scheduleManager.scheduleNextRun(schedule.copy(enabled = true))
            } else {
                app.scheduleManager.cancelSchedule(schedule.id)
            }
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch {
            val id = app.scheduleRepository.saveSchedule(schedule)
            val updated = schedule.copy(id = id)
            if (updated.enabled) {
                app.scheduleManager.scheduleNextRun(updated)
            } else {
                app.scheduleManager.cancelSchedule(updated.id)
            }
        }
    }

    fun deleteSchedule(schedule: Schedule) {
        viewModelScope.launch {
            app.scheduleManager.cancelSchedule(schedule.id)
            app.scheduleRepository.deleteSchedule(schedule)
        }
    }

    fun startScheduleNow(schedule: Schedule) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val targetEnd = app.scheduleManager.calculateEndTimeForToday(schedule.endTime)
            val executionId = app.executionRepository.recordStart(
                scheduleId = schedule.id,
                scheduleName = schedule.name,
                meetUrl = schedule.meetUrl,
                scheduledStart = now,
                scheduledEnd = targetEnd
            )

            val reg = app.sessionManager.tryStartSession(executionId, schedule, targetEnd)
            if (reg is SessionManager.SessionRegistrationResult.Allowed) {
                app.scheduleManager.scheduleEndAlarm(schedule.id, executionId, schedule.name, schedule.endTime)
                app.automationEngine.join(schedule, executionId)
            } else if (reg is SessionManager.SessionRegistrationResult.ConcurrencyLimitReached) {
                _testRunMessage.value = reg.reason
            }
        }
    }

    fun runTestAutomation(schedule: Schedule, durationSeconds: Int = 30) {
        viewModelScope.launch {
            _testRunMessage.value = "Test initiated: Will open Meet, mute AV, stay ${durationSeconds}s, and leave..."
            val result = app.automationEngine.testRun(schedule, durationSeconds)
            when (result) {
                is AutomationResult.Success -> {
                    _testRunMessage.value = "Test completed successfully: ${result.message}"
                }
                is AutomationResult.Failure -> {
                    _testRunMessage.value = "Test failed: ${result.reason}"
                }
            }
        }
    }

    fun clearTestMessage() {
        _testRunMessage.value = null
    }

    fun getNextRunFormatted(schedule: Schedule): String {
        return app.scheduleManager.getNextRunFormatted(schedule)
    }

    fun formatTime24to12(time: String): String {
        return app.scheduleManager.formatTime24to12(time)
    }
}
