package com.example.ui.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlzuhraApp
import com.example.domain.model.AutomationLogItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LogFilter(val label: String) {
    ALL("All"),
    INFO("Info"),
    WARN("Warnings"),
    ERROR("Errors"),
    SUCCESS("Success")
}

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AlzuhraApp

    private val _filter = MutableStateFlow(LogFilter.ALL)
    val filter: StateFlow<LogFilter> = _filter.asStateFlow()

    val logs: StateFlow<List<AutomationLogItem>> = combine(
        app.logRepository.allLogs,
        _filter
    ) { allLogs, filter ->
        when (filter) {
            LogFilter.ALL -> allLogs
            LogFilter.INFO -> allLogs.filter { it.level.equals("INFO", ignoreCase = true) }
            LogFilter.WARN -> allLogs.filter { it.level.equals("WARN", ignoreCase = true) }
            LogFilter.ERROR -> allLogs.filter { it.level.equals("ERROR", ignoreCase = true) }
            LogFilter.SUCCESS -> allLogs.filter { it.level.equals("SUCCESS", ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: LogFilter) {
        _filter.value = filter
    }

    fun clearLogs() {
        viewModelScope.launch {
            app.logRepository.clearLogs()
        }
    }
}
