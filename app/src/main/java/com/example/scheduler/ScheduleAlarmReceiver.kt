package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.AlzuhraApp
import com.example.automation.AutomationState
import com.example.automation.SessionManager
import com.example.domain.model.ExecutionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "alZUHRA:ScheduleAlarmWakeLock"
        ).apply {
            acquire(30000L) // 30s timeout max
        }

        val app = context.applicationContext as? AlzuhraApp
        if (app == null) {
            Log.e(TAG, "Application context is not AlzuhraApp")
            wakeLock.release()
            return
        }

        when (action) {
            ScheduleManager.ACTION_SCHEDULE_START -> {
                val scheduleId = intent.getLongExtra(ScheduleManager.EXTRA_SCHEDULE_ID, -1L)
                val scheduleName = intent.getStringExtra(ScheduleManager.EXTRA_SCHEDULE_NAME) ?: "Class"
                val meetUrl = intent.getStringExtra(ScheduleManager.EXTRA_SCHEDULE_URL) ?: ""
                val startTime = intent.getStringExtra(ScheduleManager.EXTRA_START_TIME) ?: "00:00"
                val endTime = intent.getStringExtra(ScheduleManager.EXTRA_END_TIME) ?: "00:00"
                val scheduledStart = intent.getLongExtra(ScheduleManager.EXTRA_SCHEDULED_START, System.currentTimeMillis())

                Log.i(TAG, "Alarm triggered: START schedule $scheduleId ($scheduleName)")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val schedule = app.scheduleRepository.getScheduleSync(scheduleId)
                        if (schedule == null || !schedule.enabled) {
                            Log.w(TAG, "Schedule $scheduleId not found or disabled. Skipping.")
                            return@launch
                        }

                        val targetEnd = app.scheduleManager.calculateEndTimeForToday(schedule.endTime)

                        // Record start in DB
                        val executionId = app.executionRepository.recordStart(
                            scheduleId = schedule.id,
                            scheduleName = schedule.name,
                            meetUrl = schedule.meetUrl,
                            scheduledStart = scheduledStart,
                            scheduledEnd = targetEnd
                        )

                        // Register with SessionManager
                        val reg = app.sessionManager.tryStartSession(executionId, schedule, targetEnd)
                        if (reg is SessionManager.SessionRegistrationResult.ConcurrencyLimitReached) {
                            app.logRepository.log(
                                message = reg.reason,
                                level = "WARN",
                                state = AutomationState.UNKNOWN_ERROR.name,
                                executionId = executionId,
                                scheduleId = schedule.id,
                                scheduleName = schedule.name
                            )
                            app.executionRepository.recordCompletion(
                                executionId = executionId,
                                status = ExecutionStatus.FAILED,
                                failureReason = reg.reason
                            )
                            return@launch
                        }

                        // Schedule the leave alarm for end time
                        app.scheduleManager.scheduleEndAlarm(schedule.id, executionId, schedule.name, schedule.endTime)

                        // Re-schedule the NEXT occurrence for next week/day
                        app.scheduleManager.scheduleNextRun(schedule)

                        // Trigger Join workflow
                        app.automationEngine.join(schedule, executionId)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing start alarm: ${e.message}", e)
                    } finally {
                        if (wakeLock.isHeld) {
                            wakeLock.release()
                        }
                    }
                }
            }

            ScheduleManager.ACTION_SCHEDULE_END -> {
                val executionId = intent.getLongExtra(ScheduleManager.EXTRA_EXECUTION_ID, -1L)
                val scheduleName = intent.getStringExtra(ScheduleManager.EXTRA_SCHEDULE_NAME) ?: "Class"
                Log.i(TAG, "Alarm triggered: END execution $executionId ($scheduleName)")

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (executionId != -1L) {
                            app.automationEngine.leave(executionId)
                        } else {
                            app.sessionManager.currentSession.value?.let { active ->
                                app.automationEngine.leave(active.executionId)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error executing end alarm: ${e.message}", e)
                    } finally {
                        if (wakeLock.isHeld) {
                            wakeLock.release()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScheduleAlarmReceiver"
    }
}
