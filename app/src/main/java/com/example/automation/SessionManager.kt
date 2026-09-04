package com.example.automation

import com.example.domain.model.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveSession(
    val executionId: Long,
    val schedule: Schedule,
    val startedAt: Long = System.currentTimeMillis(),
    val targetEndTime: Long,
    val state: AutomationState = AutomationState.IDLE,
    val isTestMode: Boolean = false
)

class SessionManager {

    private val _activeSessions = MutableStateFlow<Map<Long, ActiveSession>>(emptyMap())
    val activeSessions: StateFlow<Map<Long, ActiveSession>> = _activeSessions.asStateFlow()

    private val _currentSession = MutableStateFlow<ActiveSession?>(null)
    val currentSession: StateFlow<ActiveSession?> = _currentSession.asStateFlow()

    private val maxConcurrentSessions = 1 // Android device limitation for single browser/Meet instance

    sealed class SessionRegistrationResult {
        data class Allowed(val session: ActiveSession) : SessionRegistrationResult()
        data class ConcurrencyLimitReached(val reason: String) : SessionRegistrationResult()
    }

    /**
     * Checks if a new session can be registered according to Android device environment limits.
     */
    @Synchronized
    fun tryStartSession(
        executionId: Long,
        schedule: Schedule,
        targetEndTime: Long,
        isTestMode: Boolean = false
    ): SessionRegistrationResult {
        val currentActive = _activeSessions.value
        if (currentActive.size >= maxConcurrentSessions) {
            val conflicting = currentActive.values.firstOrNull()?.schedule?.name ?: "Active Class"
            return SessionRegistrationResult.ConcurrencyLimitReached(
                "Concurrent session limit reached ($maxConcurrentSessions session). " +
                        "This device cannot reliably maintain another Meet session while '$conflicting' is running."
            )
        }

        val session = ActiveSession(
            executionId = executionId,
            schedule = schedule,
            startedAt = System.currentTimeMillis(),
            targetEndTime = targetEndTime,
            state = AutomationState.WAITING_FOR_SCHEDULE,
            isTestMode = isTestMode
        )

        _activeSessions.value = currentActive + (executionId to session)
        _currentSession.value = session
        return SessionRegistrationResult.Allowed(session)
    }

    @Synchronized
    fun updateSessionState(executionId: Long, state: AutomationState) {
        val current = _activeSessions.value[executionId] ?: return
        val updated = current.copy(state = state)
        _activeSessions.value = _activeSessions.value + (executionId to updated)
        if (_currentSession.value?.executionId == executionId) {
            _currentSession.value = updated
        }
    }

    @Synchronized
    fun endSession(executionId: Long) {
        val currentActive = _activeSessions.value.toMutableMap()
        currentActive.remove(executionId)
        _activeSessions.value = currentActive
        if (_currentSession.value?.executionId == executionId) {
            _currentSession.value = currentActive.values.firstOrNull()
        }
    }

    fun isSessionActive(executionId: Long): Boolean {
        return _activeSessions.value.containsKey(executionId)
    }

    fun hasAnyActiveSession(): Boolean {
        return _activeSessions.value.isNotEmpty()
    }
}
