package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.MeetingExecutionEntity
import com.example.data.local.ParticipantPresenceEntity
import com.example.domain.model.ExecutionRecord
import com.example.domain.model.ExecutionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExecutionRepository(private val appDao: AppDao) {

    val allExecutions: Flow<List<ExecutionRecord>> = appDao.getAllExecutions().map { list ->
        list.map { it.toDomain() }
    }

    val activeExecution: Flow<ExecutionRecord?> = appDao.getActiveExecution().map { it?.toDomain() }

    fun getExecutionsForSchedule(scheduleId: Long): Flow<List<ExecutionRecord>> =
        appDao.getExecutionsForSchedule(scheduleId).map { list -> list.map { it.toDomain() } }

    fun getExecutionsBetween(start: Long, end: Long): Flow<List<ExecutionRecord>> =
        appDao.getExecutionsBetween(start, end).map { list -> list.map { it.toDomain() } }

    suspend fun getActiveExecutionSync(): ExecutionRecord? =
        appDao.getActiveExecutionSync()?.toDomain()

    suspend fun getExecutionByIdSync(id: Long): ExecutionRecord? =
        appDao.getExecutionByIdSync(id)?.toDomain()

    suspend fun recordStart(
        scheduleId: Long,
        scheduleName: String,
        meetUrl: String,
        scheduledStart: Long,
        scheduledEnd: Long,
        actualJoinTime: Long? = System.currentTimeMillis()
    ): Long {
        val entity = MeetingExecutionEntity(
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            meetUrl = meetUrl,
            scheduledStart = scheduledStart,
            scheduledEnd = scheduledEnd,
            actualJoinTime = actualJoinTime,
            status = ExecutionStatus.IN_PROGRESS.name
        )
        return appDao.insertExecution(entity)
    }

    suspend fun recordCompletion(
        executionId: Long,
        actualLeaveTime: Long = System.currentTimeMillis(),
        status: ExecutionStatus = ExecutionStatus.COMPLETED,
        failureReason: String? = null,
        notes: String? = null
    ) {
        val existing = appDao.getExecutionByIdSync(executionId) ?: return
        val updated = existing.copy(
            actualLeaveTime = actualLeaveTime,
            status = status.name,
            failureReason = failureReason,
            notes = notes
        )
        appDao.updateExecution(updated)
    }

    suspend fun deleteExecution(id: Long) {
        appDao.deleteExecutionById(id)
    }

    suspend fun clearHistory() {
        appDao.clearAllExecutions()
    }

    val allParticipants: Flow<List<ParticipantPresenceEntity>> = appDao.getAllParticipantPresence()

    suspend fun addParticipantPresence(presence: ParticipantPresenceEntity): Long {
        return appDao.insertParticipantPresence(presence)
    }

    private fun MeetingExecutionEntity.toDomain(): ExecutionRecord {
        val parsedStatus = try {
            ExecutionStatus.valueOf(status)
        } catch (_: Exception) {
            ExecutionStatus.COMPLETED
        }
        return ExecutionRecord(
            id = id,
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            meetUrl = meetUrl,
            scheduledStart = scheduledStart,
            scheduledEnd = scheduledEnd,
            actualJoinTime = actualJoinTime,
            actualLeaveTime = actualLeaveTime,
            status = parsedStatus,
            failureReason = failureReason,
            notes = notes,
            createdAt = createdAt
        )
    }
}
