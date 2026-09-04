package com.example.ui.attendance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlzuhraApp
import com.example.domain.model.ExecutionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

enum class AttendanceFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    ALL_TIME("All Time")
}

data class SessionPresenceItem(
    val execution: ExecutionRecord,
    val scheduledDurationMinutes: Long,
    val actualDurationMinutes: Long,
    val presencePercentage: Double,
    val formattedDate: String,
    val formattedJoinTime: String,
    val formattedLeaveTime: String
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AlzuhraApp

    private val _currentFilter = MutableStateFlow(AttendanceFilter.ALL_TIME)
    val currentFilter: StateFlow<AttendanceFilter> = _currentFilter.asStateFlow()

    private val rawExecutions = app.executionRepository.allExecutions

    val sessionPresenceList: StateFlow<List<SessionPresenceItem>> = combine(
        rawExecutions,
        _currentFilter
    ) { executions, filter ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val filtered = executions.filter { item ->
            when (filter) {
                AttendanceFilter.TODAY -> {
                    val calStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    item.scheduledStart >= calStart
                }
                AttendanceFilter.THIS_WEEK -> {
                    val calStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    item.scheduledStart >= calStart
                }
                AttendanceFilter.THIS_MONTH -> {
                    val calStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    item.scheduledStart >= calStart
                }
                AttendanceFilter.ALL_TIME -> true
            }
        }

        filtered.map { exec ->
            val schedDuration = maxOf(1L, (exec.scheduledEnd - exec.scheduledStart) / 60000L)
            val actualDuration = if (exec.actualJoinTime != null && exec.actualLeaveTime != null) {
                maxOf(0L, (exec.actualLeaveTime - exec.actualJoinTime) / 60000L)
            } else if (exec.actualJoinTime != null) {
                maxOf(0L, (now - exec.actualJoinTime) / 60000L)
            } else {
                0L
            }

            val pct = (actualDuration.toDouble() / schedDuration.toDouble() * 100.0).coerceIn(0.0, 100.0)

            val dateSdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
            val timeSdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

            SessionPresenceItem(
                execution = exec,
                scheduledDurationMinutes = schedDuration,
                actualDurationMinutes = actualDuration,
                presencePercentage = pct,
                formattedDate = dateSdf.format(exec.scheduledStart),
                formattedJoinTime = exec.actualJoinTime?.let { timeSdf.format(it) } ?: "Not joined",
                formattedLeaveTime = exec.actualLeaveTime?.let { timeSdf.format(it) } ?: if (exec.status.name == "IN_PROGRESS") "Active" else "--"
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: AttendanceFilter) {
        _currentFilter.value = filter
    }
}
