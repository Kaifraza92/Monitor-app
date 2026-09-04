package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.MeetingScheduleEntity
import com.example.domain.model.AppDayOfWeek
import com.example.domain.model.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScheduleRepository(private val appDao: AppDao) {

    val allSchedules: Flow<List<Schedule>> = appDao.getAllSchedules().map { list ->
        list.map { it.toDomain() }
    }

    val enabledSchedules: Flow<List<Schedule>> = appDao.getEnabledSchedules().map { list ->
        list.map { it.toDomain() }
    }

    fun getSchedule(id: Long): Flow<Schedule?> = appDao.getScheduleById(id).map { it?.toDomain() }

    suspend fun getScheduleSync(id: Long): Schedule? = appDao.getScheduleByIdSync(id)?.toDomain()

    suspend fun getEnabledSchedulesSync(): List<Schedule> = appDao.getEnabledSchedulesSync().map { it.toDomain() }

    suspend fun saveSchedule(schedule: Schedule): Long {
        val entity = schedule.toEntity()
        return if (schedule.id == 0L) {
            appDao.insertSchedule(entity)
        } else {
            appDao.updateSchedule(entity.copy(updatedAt = System.currentTimeMillis()))
            schedule.id
        }
    }

    suspend fun setScheduleEnabled(id: Long, enabled: Boolean) {
        appDao.updateScheduleEnabled(id, enabled)
    }

    suspend fun deleteSchedule(schedule: Schedule) {
        appDao.deleteSchedule(schedule.toEntity())
    }

    private fun MeetingScheduleEntity.toDomain(): Schedule {
        return Schedule(
            id = id,
            name = name,
            meetUrl = meetUrl,
            startTime = startTime,
            endTime = endTime,
            daysOfWeek = AppDayOfWeek.parseDays(daysOfWeek),
            enabled = enabled,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun Schedule.toEntity(): MeetingScheduleEntity {
        return MeetingScheduleEntity(
            id = id,
            name = name,
            meetUrl = meetUrl,
            startTime = startTime,
            endTime = endTime,
            daysOfWeek = AppDayOfWeek.formatDays(daysOfWeek),
            enabled = enabled,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
