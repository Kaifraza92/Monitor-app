package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.AutomationLogEntity
import com.example.domain.model.AutomationLogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LogRepository(private val appDao: AppDao) {

    val allLogs: Flow<List<AutomationLogItem>> = appDao.getAllLogs().map { list ->
        list.map { it.toDomain() }
    }

    fun getLogsForExecution(executionId: Long): Flow<List<AutomationLogItem>> =
        appDao.getLogsForExecution(executionId).map { list -> list.map { it.toDomain() } }

    suspend fun log(
        message: String,
        level: String = "INFO",
        state: String = "IDLE",
        executionId: Long? = null,
        scheduleId: Long? = null,
        scheduleName: String? = null
    ): Long {
        val entity = AutomationLogEntity(
            executionId = executionId,
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            timestamp = System.currentTimeMillis(),
            level = level,
            state = state,
            message = message
        )
        return appDao.insertLog(entity)
    }

    suspend fun clearLogs() {
        appDao.clearLogs()
    }

    private fun AutomationLogEntity.toDomain(): AutomationLogItem {
        return AutomationLogItem(
            id = id,
            executionId = executionId,
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            timestamp = timestamp,
            level = level,
            state = state,
            message = message
        )
    }
}
