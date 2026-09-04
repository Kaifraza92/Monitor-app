package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlzuhraApp
import com.example.automation.ActiveSession
import com.example.automation.SessionManager
import com.example.data.secure.ConnectedAccount
import com.example.domain.model.AppDayOfWeek
import com.example.domain.model.ExecutionRecord
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.Schedule
import com.example.ui.util.SystemPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class SystemStatus(
    val isAutomationActive: Boolean = false,
    val isAccessibilityEnabled: Boolean = false,
    val isAccountConnected: Boolean = true,
    val accountEmail: String = "alzuhraacademy@gmail.com",
    val areNotificationsEnabled: Boolean = true,
    val isBatteryExempted: Boolean = true
)

data class DashboardKpis(
    val todayTotal: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val activeCount: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AlzuhraApp

    val allSchedules: StateFlow<List<Schedule>> = app.scheduleRepository.allSchedules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSession: StateFlow<ActiveSession?> = app.sessionManager.currentSession

    val connectedAccount: StateFlow<ConnectedAccount> = app.secureAccountStorage.accountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), app.secureAccountStorage.getAccount())

    private val _systemStatus = MutableStateFlow(SystemStatus())
    val systemStatus: StateFlow<SystemStatus> = _systemStatus.asStateFlow()

    val recentExecutions: StateFlow<List<ExecutionRecord>> = app.executionRepository.allExecutions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val kpis: StateFlow<DashboardKpis> = combine(
        allSchedules,
        recentExecutions,
        activeSession
    ) { schedules, executions, active ->
        val todayCalendar = Calendar.getInstance()
        val currentDay = AppDayOfWeek.fromCalendar(todayCalendar.get(Calendar.DAY_OF_WEEK))
        val todaySchedulesCount = schedules.count { it.daysOfWeek.contains(currentDay) }

        // Start of today millis
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayExecutions = executions.filter { it.scheduledStart >= startOfToday }
        val completed = todayExecutions.count { it.status == ExecutionStatus.COMPLETED }
        val failed = todayExecutions.count { it.status == ExecutionStatus.FAILED }
        val activeCount = if (active != null) 1 else 0

        DashboardKpis(
            todayTotal = maxOf(todaySchedulesCount, todayExecutions.size),
            completedCount = completed,
            failedCount = failed,
            activeCount = activeCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardKpis())

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val a11y = SystemPermissionHelper.isAccessibilityServiceEnabled(context)
        val notif = SystemPermissionHelper.areNotificationsEnabled(context)
        val battery = SystemPermissionHelper.isBatteryOptimizationExempted(context)
        val account = app.secureAccountStorage.getAccount()

        _systemStatus.value = SystemStatus(
            isAutomationActive = app.sessionManager.hasAnyActiveSession(),
            isAccessibilityEnabled = a11y,
            isAccountConnected = account.isConnected,
            accountEmail = account.email,
            areNotificationsEnabled = notif,
            isBatteryExempted = battery
        )
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
                refreshPermissions()
            }
        }
    }

    fun stopActiveSession() {
        app.automationEngine.cancelActiveSession()
        refreshPermissions()
    }
}
