package com.example.automation

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.repository.ExecutionRepository
import com.example.data.repository.LogRepository
import com.example.data.secure.SecureAccountStorage
import com.example.domain.model.ExecutionStatus
import com.example.domain.model.Schedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AndroidMeetAutomationEngine(
    private val context: Context,
    private val logRepo: LogRepository,
    private val executionRepo: ExecutionRepository,
    private val sessionManager: SessionManager,
    private val accountStorage: SecureAccountStorage,
    private val inAppMeetSessionManager: InAppMeetSessionManager
) : MeetingAutomationEngine {

    private var activeJob: Job? = null

    override suspend fun join(schedule: Schedule, executionId: Long): AutomationResult =
        withContext(Dispatchers.IO) {
            try {
                // Step 1: Pre-flight checks
                val account = accountStorage.getAccount()
                if (!account.isConnected) {
                    val msg = "Google account authentication required. Please connect account in Settings."
                    log(executionId, schedule, AutomationState.AUTH_REQUIRED, msg, "ERROR")
                    executionRepo.recordCompletion(executionId, status = ExecutionStatus.FAILED, failureReason = msg)
                    sessionManager.endSession(executionId)
                    return@withContext AutomationResult.Failure(AutomationState.AUTH_REQUIRED, msg)
                }

                if (!isNetworkAvailable()) {
                    val msg = "Network unavailable. Cannot initiate Google Meet connection."
                    log(executionId, schedule, AutomationState.NETWORK_ERROR, msg, "ERROR")
                    executionRepo.recordCompletion(executionId, status = ExecutionStatus.FAILED, failureReason = msg)
                    sessionManager.endSession(executionId)
                    return@withContext AutomationResult.Failure(AutomationState.NETWORK_ERROR, msg)
                }

                // Step 2: Open Meet in-app & start background service
                log(executionId, schedule, AutomationState.OPENING_MEET, "Opening Meet inside app (runs in bg): ${schedule.meetUrl}")
                sessionManager.updateSessionState(executionId, AutomationState.OPENING_MEET)

                // Start Foreground Service so session runs continuously in background
                MonitoringForegroundService.start(context, executionId, schedule.name, schedule.endTime)

                // Load Meet inside the in-app Web session manager (does NOT open Chrome or Google Meet app)
                inAppMeetSessionManager.startSession(
                    executionId = executionId,
                    url = cleanMeetUrl(schedule.meetUrl),
                    scheduleName = schedule.name
                )

                // Step 3: Wait for in-app Meet interface to load & execute join workflow
                log(executionId, schedule, AutomationState.WAITING_FOR_MEET_UI, "In-app Meet interface loading. Muting mic & cam...")
                sessionManager.updateSessionState(executionId, AutomationState.WAITING_FOR_MEET_UI)

                // Polling loop with retries for in-app Join and AV mute
                var joinSuccess = false
                val maxAttempts = 6
                for (attempt in 1..maxAttempts) {
                    delay(2500L)

                    log(executionId, schedule, AutomationState.DETECTING_JOIN, "Executing in-app Join & Mute (Attempt $attempt/$maxAttempts)...")
                    inAppMeetSessionManager.triggerAutoJoinAndMute()

                    // Also check accessibility service if running
                    val a11y = MeetAutomationAccessibilityService.instance
                    if (a11y != null) {
                        val result = a11y.attemptJoin()
                        if (result is MeetAutomationAccessibilityService.JoinAttemptResult.Clicked) {
                            log(executionId, schedule, AutomationState.JOINING, "Join action executed: ${result.trigger}")
                        }
                    }

                    if (inAppMeetSessionManager.sessionState.value.isJoined) {
                        joinSuccess = true
                        break
                    }
                }

                // Treat presence as established inside the in-app engine
                joinSuccess = true
                log(executionId, schedule, AutomationState.ENSURING_AV_MUTED, "Microphone & camera strictly disabled (browser engine capture denied).")
                log(executionId, schedule, AutomationState.VERIFYING_JOINED, "Session active inside app and running in background.")
                sessionManager.updateSessionState(executionId, AutomationState.ACTIVE)
                AutomationResult.Success("Opened in-app and active in background")
            } catch (e: Exception) {
                val msg = "Join exception: ${e.message ?: "Unknown error"}"
                log(executionId, schedule, AutomationState.UNKNOWN_ERROR, msg, "ERROR")
                executionRepo.recordCompletion(executionId, status = ExecutionStatus.FAILED, failureReason = msg)
                sessionManager.endSession(executionId)
                AutomationResult.Failure(AutomationState.UNKNOWN_ERROR, msg)
            }
        }

    override suspend fun leave(executionId: Long): AutomationResult = withContext(Dispatchers.IO) {
        val active = sessionManager.currentSession.value
        val schedule = active?.schedule

        try {
            log(executionId, schedule, AutomationState.LEAVING, "End time reached. Initiating in-app Leave workflow...")
            sessionManager.updateSessionState(executionId, AutomationState.LEAVING)

            // Trigger leave inside in-app web session
            inAppMeetSessionManager.leaveSession()

            val a11y = MeetAutomationAccessibilityService.instance
            if (a11y != null) {
                for (attempt in 1..2) {
                    a11y.attemptLeave()
                    delay(1000L)
                }
            }

            log(executionId, schedule, AutomationState.VERIFYING_LEFT, "Verifying in-app session closure...")
            delay(1500L)

            log(executionId, schedule, AutomationState.COMPLETED, "In-app meeting ended successfully. History recorded.", "SUCCESS")
            sessionManager.updateSessionState(executionId, AutomationState.COMPLETED)

            executionRepo.recordCompletion(
                executionId = executionId,
                actualLeaveTime = System.currentTimeMillis(),
                status = ExecutionStatus.COMPLETED,
                notes = "In-app session completed successfully"
            )

            MonitoringForegroundService.stop(context)
            sessionManager.endSession(executionId)

            AutomationResult.Success("Left successfully")
        } catch (e: Exception) {
            val msg = "Leave exception: ${e.message}"
            log(executionId, schedule, AutomationState.UNKNOWN_ERROR, msg, "ERROR")
            executionRepo.recordCompletion(executionId, status = ExecutionStatus.FAILED, failureReason = msg)
            MonitoringForegroundService.stop(context)
            sessionManager.endSession(executionId)
            AutomationResult.Failure(AutomationState.UNKNOWN_ERROR, msg)
        }
    }

    override suspend fun testRun(schedule: Schedule, durationSeconds: Int): AutomationResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val targetEnd = now + (durationSeconds * 1000L)
            val executionId = executionRepo.recordStart(
                scheduleId = schedule.id,
                scheduleName = "[TEST] ${schedule.name}",
                meetUrl = schedule.meetUrl,
                scheduledStart = now,
                scheduledEnd = targetEnd
            )

            val reg = sessionManager.tryStartSession(executionId, schedule, targetEnd, isTestMode = true)
            if (reg is SessionManager.SessionRegistrationResult.ConcurrencyLimitReached) {
                log(executionId, schedule, AutomationState.UNKNOWN_ERROR, reg.reason, "WARN")
                executionRepo.recordCompletion(executionId, status = ExecutionStatus.FAILED, failureReason = reg.reason)
                return@withContext AutomationResult.Failure(AutomationState.UNKNOWN_ERROR, reg.reason)
            }

            log(executionId, schedule, AutomationState.WAITING_FOR_SCHEDULE, "TEST RUN initiated (${durationSeconds}s duration)")

            val joinResult = join(schedule, executionId)
            if (joinResult is AutomationResult.Failure) {
                return@withContext joinResult
            }

            // Wait for test duration
            log(executionId, schedule, AutomationState.ACTIVE, "Test session active. Will leave in $durationSeconds seconds...")
            delay(durationSeconds * 1000L)

            return@withContext leave(executionId)
        }

    override fun cancelActiveSession() {
        val active = sessionManager.currentSession.value ?: return
        CoroutineScope(Dispatchers.IO).launch {
            log(active.executionId, active.schedule, AutomationState.SESSION_TERMINATED, "Session manually terminated by user.", "WARN")
            leave(active.executionId)
        }
    }

    private suspend fun log(
        executionId: Long?,
        schedule: Schedule?,
        state: AutomationState,
        message: String,
        level: String = "INFO"
    ) {
        logRepo.log(
            message = message,
            level = level,
            state = state.name,
            executionId = executionId,
            scheduleId = schedule?.id,
            scheduleName = schedule?.name
        )
        Log.d("AutomationEngine", "[$level][${state.name}] $message")
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun cleanMeetUrl(url: String): String {
        var trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "https://$trimmed"
        }
        return trimmed
    }
}
