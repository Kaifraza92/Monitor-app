package com.example.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlzuhraApp
import com.example.data.secure.ConnectedAccount
import com.example.ui.util.SystemPermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.TimeZone

data class SetupChecklist(
    val isAccountConnected: Boolean = true,
    val areNotificationsEnabled: Boolean = true,
    val isAccessibilityEnabled: Boolean = false,
    val isBatteryExempted: Boolean = true,
    val canScheduleExactAlarms: Boolean = true
) {
    val completedCount: Int
        get() = listOf(
            isAccountConnected,
            areNotificationsEnabled,
            isAccessibilityEnabled,
            isBatteryExempted,
            canScheduleExactAlarms
        ).count { it }

    val totalCount: Int = 5

    val isAllComplete: Boolean
        get() = completedCount == totalCount
}

data class TimezoneInfo(
    val id: String,
    val displayName: String,
    val offsetString: String
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AlzuhraApp

    val account: StateFlow<ConnectedAccount> = app.secureAccountStorage.accountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), app.secureAccountStorage.getAccount())

    private val _checklist = MutableStateFlow(SetupChecklist())
    val checklist: StateFlow<SetupChecklist> = _checklist.asStateFlow()

    private val _timezoneInfo = MutableStateFlow(loadTimezoneInfo())
    val timezoneInfo: StateFlow<TimezoneInfo> = _timezoneInfo.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        val context = getApplication<Application>()
        val a11y = SystemPermissionHelper.isAccessibilityServiceEnabled(context)
        val notif = SystemPermissionHelper.areNotificationsEnabled(context)
        val battery = SystemPermissionHelper.isBatteryOptimizationExempted(context)
        val alarms = SystemPermissionHelper.canScheduleExactAlarms(context)
        val acc = app.secureAccountStorage.getAccount()

        _checklist.value = SetupChecklist(
            isAccountConnected = acc.isConnected,
            areNotificationsEnabled = notif,
            isAccessibilityEnabled = a11y,
            isBatteryExempted = battery,
            canScheduleExactAlarms = alarms
        )
        _timezoneInfo.value = loadTimezoneInfo()
    }

    fun disconnectAccount() {
        app.secureAccountStorage.disconnect()
        refreshStatus()
    }

    fun reconnectAccount() {
        app.secureAccountStorage.reconnect()
        refreshStatus()
    }

    fun updateAccount(email: String, displayName: String) {
        app.secureAccountStorage.setAccount(email, displayName)
        refreshStatus()
    }

    private fun loadTimezoneInfo(): TimezoneInfo {
        val tz = TimeZone.getDefault()
        val offsetMinutes = tz.rawOffset / (1000 * 60)
        val hours = offsetMinutes / 60
        val mins = Math.abs(offsetMinutes % 60)
        val sign = if (hours >= 0) "+" else "-"
        val offsetStr = String.format("UTC%s%02d:%02d", sign, Math.abs(hours), mins)

        return TimezoneInfo(
            id = tz.id,
            displayName = tz.getDisplayName(false, TimeZone.SHORT),
            offsetString = offsetStr
        )
    }
}
