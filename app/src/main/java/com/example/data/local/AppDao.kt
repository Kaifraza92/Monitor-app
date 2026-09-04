package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Meeting Schedules ---
    @Query("SELECT * FROM meeting_schedules ORDER BY createdAt DESC")
    fun getAllSchedules(): Flow<List<MeetingScheduleEntity>>

    @Query("SELECT * FROM meeting_schedules WHERE enabled = 1")
    fun getEnabledSchedules(): Flow<List<MeetingScheduleEntity>>

    @Query("SELECT * FROM meeting_schedules WHERE enabled = 1")
    suspend fun getEnabledSchedulesSync(): List<MeetingScheduleEntity>

    @Query("SELECT * FROM meeting_schedules WHERE id = :id")
    fun getScheduleById(id: Long): Flow<MeetingScheduleEntity?>

    @Query("SELECT * FROM meeting_schedules WHERE id = :id")
    suspend fun getScheduleByIdSync(id: Long): MeetingScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: MeetingScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: MeetingScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: MeetingScheduleEntity)

    @Query("UPDATE meeting_schedules SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateScheduleEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    // --- Meeting Executions ---
    @Query("SELECT * FROM meeting_executions ORDER BY scheduledStart DESC")
    fun getAllExecutions(): Flow<List<MeetingExecutionEntity>>

    @Query("SELECT * FROM meeting_executions WHERE scheduleId = :scheduleId ORDER BY scheduledStart DESC")
    fun getExecutionsForSchedule(scheduleId: Long): Flow<List<MeetingExecutionEntity>>

    @Query("SELECT * FROM meeting_executions WHERE scheduledStart >= :startTime AND scheduledStart <= :endTime ORDER BY scheduledStart ASC")
    fun getExecutionsBetween(startTime: Long, endTime: Long): Flow<List<MeetingExecutionEntity>>

    @Query("SELECT * FROM meeting_executions WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun getActiveExecution(): Flow<MeetingExecutionEntity?>

    @Query("SELECT * FROM meeting_executions WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveExecutionSync(): MeetingExecutionEntity?

    @Query("SELECT * FROM meeting_executions WHERE id = :id")
    suspend fun getExecutionByIdSync(id: Long): MeetingExecutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecution(execution: MeetingExecutionEntity): Long

    @Update
    suspend fun updateExecution(execution: MeetingExecutionEntity)

    @Query("DELETE FROM meeting_executions WHERE id = :id")
    suspend fun deleteExecutionById(id: Long)

    @Query("DELETE FROM meeting_executions")
    suspend fun clearAllExecutions()

    // --- Participant Presence ---
    @Query("SELECT * FROM participant_presence WHERE executionId = :executionId")
    fun getParticipantsForExecution(executionId: Long): Flow<List<ParticipantPresenceEntity>>

    @Query("SELECT * FROM participant_presence ORDER BY id DESC")
    fun getAllParticipantPresence(): Flow<List<ParticipantPresenceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipantPresence(presence: ParticipantPresenceEntity): Long

    // --- Automation Logs ---
    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 500")
    fun getAllLogs(): Flow<List<AutomationLogEntity>>

    @Query("SELECT * FROM automation_logs WHERE executionId = :executionId ORDER BY timestamp ASC")
    fun getLogsForExecution(executionId: Long): Flow<List<AutomationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AutomationLogEntity): Long

    @Query("DELETE FROM automation_logs")
    suspend fun clearLogs()
}
