package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.domain.model.AppDayOfWeek
import com.example.domain.model.Schedule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ScheduleManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules the next start alarm for the given schedule.
     */
    fun scheduleNextRun(schedule: Schedule) {
        if (!schedule.enabled || schedule.daysOfWeek.isEmpty()) {
            cancelSchedule(schedule.id)
            return
        }

        val nextStartMillis = calculateNextOccurrence(schedule.daysOfWeek, schedule.startTime)
        if (nextStartMillis == null) {
            Log.w(TAG, "Could not determine next occurrence for schedule ${schedule.id} (${schedule.name})")
            return
        }

        val startIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_START
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_SCHEDULE_NAME, schedule.name)
            putExtra(EXTRA_SCHEDULE_URL, schedule.meetUrl)
            putExtra(EXTRA_START_TIME, schedule.startTime)
            putExtra(EXTRA_END_TIME, schedule.endTime)
            putExtra(EXTRA_SCHEDULED_START, nextStartMillis)
        }

        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            getStartRequestCode(schedule.id),
            startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(nextStartMillis, startPendingIntent)
        Log.i(TAG, "Scheduled next run for '${schedule.name}' at ${formatTimestamp(nextStartMillis)}")
    }

    /**
     * Schedules the end alarm when a session actually begins.
     */
    fun scheduleEndAlarm(scheduleId: Long, executionId: Long, scheduleName: String, endTime: String) {
        val endMillis = calculateEndTimeForToday(endTime)

        val endIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_END
            putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(EXTRA_EXECUTION_ID, executionId)
            putExtra(EXTRA_SCHEDULE_NAME, scheduleName)
        }

        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            getEndRequestCode(scheduleId),
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(endMillis, endPendingIntent)
        Log.i(TAG, "Scheduled leave alarm for execution $executionId ($scheduleName) at ${formatTimestamp(endMillis)}")
    }

    fun cancelSchedule(scheduleId: Long) {
        val startIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_START
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            getStartRequestCode(scheduleId),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
            startPendingIntent.cancel()
        }

        val endIntent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_END
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            getEndRequestCode(scheduleId),
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPendingIntent != null) {
            alarmManager.cancel(endPendingIntent)
            endPendingIntent.cancel()
        }
    }

    private fun scheduleExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    // Fallback to window/inexact alarm when exact alarm permission is not granted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm permission restriction: ${e.message}")
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * Computes the next scheduled timestamp strictly in the future.
     */
    fun calculateNextOccurrence(days: Set<AppDayOfWeek>, timeString: String): Long? {
        if (days.isEmpty()) return null
        val parts = timeString.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null

        val now = Calendar.getInstance()
        var earliestFuture: Long? = null

        // Check for the next 8 days
        for (dayOffset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = now.timeInMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = AppDayOfWeek.fromCalendar(candidate.get(Calendar.DAY_OF_WEEK))
            if (days.contains(dayOfWeek) && candidate.timeInMillis > now.timeInMillis + 5000L) {
                val candidateMillis = candidate.timeInMillis
                if (earliestFuture == null || candidateMillis < earliestFuture) {
                    earliestFuture = candidateMillis
                }
            }
        }

        return earliestFuture
    }

    fun calculateEndTimeForToday(timeString: String): Long {
        val parts = timeString.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If end time is before current time (e.g. crossing midnight or scheduled near start), add 1 day
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun getNextRunFormatted(schedule: Schedule): String {
        val nextMillis = calculateNextOccurrence(schedule.daysOfWeek, schedule.startTime)
            ?: return "No upcoming runs scheduled"
        val sdf = SimpleDateFormat("EEEE, hh:mm a", Locale.getDefault())
        return sdf.format(nextMillis)
    }

    fun formatTime24to12(time24: String): String {
        return try {
            val inSdf = SimpleDateFormat("HH:mm", Locale.US)
            val outSdf = SimpleDateFormat("hh:mm a", Locale.US)
            val date = inSdf.parse(time24)
            if (date != null) outSdf.format(date) else time24
        } catch (_: Exception) {
            time24
        }
    }

    private fun formatTimestamp(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(millis)
    }

    private fun getStartRequestCode(scheduleId: Long): Int = (scheduleId * 10).toInt()
    private fun getEndRequestCode(scheduleId: Long): Int = (scheduleId * 10 + 1).toInt()

    companion object {
        private const val TAG = "ScheduleManager"

        const val ACTION_SCHEDULE_START = "com.alzuhra.ACTION_SCHEDULE_START"
        const val ACTION_SCHEDULE_END = "com.alzuhra.ACTION_SCHEDULE_END"

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_EXECUTION_ID = "extra_execution_id"
        const val EXTRA_SCHEDULE_NAME = "extra_schedule_name"
        const val EXTRA_SCHEDULE_URL = "extra_schedule_url"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_SCHEDULED_START = "extra_scheduled_start"
    }
}
