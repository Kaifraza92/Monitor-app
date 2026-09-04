package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "participant_presence")
data class ParticipantPresenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val executionId: Long,
    val participantIdentifier: String,
    val joinTime: Long? = null,
    val leaveTime: Long? = null,
    val durationSeconds: Long? = null,
    val attendancePercentage: Double? = null
)
