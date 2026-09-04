package com.example.automation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlzuhraApp
import com.example.MainActivity
import com.example.R

class MonitoringForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            try {
                AlzuhraApp.instance.automationEngine.cancelActiveSession()
            } catch (e: Exception) {
                Log.e("ForegroundService", "Error stopping session: ${e.message}")
            }
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Acquire WakeLock to ensure background network and Web session stay alive
        acquireWakeLock()

        val className = intent?.getStringExtra(EXTRA_CLASS_NAME) ?: "Google Meet Class"
        val endTime = intent?.getStringExtra(EXTRA_END_TIME) ?: ""
        val executionId = intent?.getLongExtra(EXTRA_EXECUTION_ID, -1L) ?: -1L

        val notification = buildForegroundNotification(className, endTime, executionId)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "alZUHRA:ForegroundServiceWakeLock"
            ).apply {
                acquire(4 * 60 * 60 * 1000L) // 4 hours safety max
            }
            Log.d("ForegroundService", "Acquired partial WakeLock for background monitoring")
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d("ForegroundService", "Released partial WakeLock")
            }
        }
        wakeLock = null
    }

    private fun buildForegroundNotification(
        className: String,
        endTime: String,
        executionId: Long
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO_MEETING, true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MonitoringForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_MONITORING)
            .setSmallIcon(R.drawable.ic_stat_monitoring)
            .setContentTitle("Al Zuhra Class Monitoring")
            .setContentText("$className • Running in Background")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$className\nRunning In-App & in Background • Mic & Camera OFF\nScheduled end: $endTime"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Session", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_MONITORING,
                "Active Meet Monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing notification when an automated Meet class session is active"
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_MONITORING = "channel_monitoring_active"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.alzuhra.ACTION_START_FOREGROUND"
        const val ACTION_STOP = "com.alzuhra.ACTION_STOP_FOREGROUND"

        const val EXTRA_CLASS_NAME = "extra_class_name"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_EXECUTION_ID = "extra_execution_id"
        const val EXTRA_NAVIGATE_TO_MEETING = "extra_navigate_to_meeting"

        fun start(context: Context, executionId: Long, className: String, endTime: String) {
            val intent = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_EXECUTION_ID, executionId)
                putExtra(EXTRA_CLASS_NAME, className)
                putExtra(EXTRA_END_TIME, endTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MonitoringForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
