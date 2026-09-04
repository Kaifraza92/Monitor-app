package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.AlzuhraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val app = context.applicationContext as? AlzuhraApp ?: return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val enabledSchedules = app.scheduleRepository.getEnabledSchedulesSync()
                    Log.i(TAG, "Restoring ${enabledSchedules.size} enabled schedules after $action")

                    for (schedule in enabledSchedules) {
                        app.scheduleManager.scheduleNextRun(schedule)
                    }

                    app.logRepository.log(
                        message = "Device state change ($action). Restored ${enabledSchedules.size} active schedule alarms.",
                        level = "INFO",
                        state = "IDLE"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore schedules: ${e.message}", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
