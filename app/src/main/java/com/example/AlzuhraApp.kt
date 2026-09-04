package com.example

import android.app.Application
import com.example.automation.AndroidMeetAutomationEngine
import com.example.automation.MeetingAutomationEngine
import com.example.automation.SessionManager
import com.example.data.local.AppDatabase
import com.example.data.repository.ExecutionRepository
import com.example.data.repository.LogRepository
import com.example.data.repository.ScheduleRepository
import com.example.data.secure.SecureAccountStorage
import com.example.scheduler.ScheduleManager

class AlzuhraApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var scheduleRepository: ScheduleRepository
        private set

    lateinit var executionRepository: ExecutionRepository
        private set

    lateinit var logRepository: LogRepository
        private set

    lateinit var secureAccountStorage: SecureAccountStorage
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var scheduleManager: ScheduleManager
        private set

    lateinit var inAppMeetSessionManager: com.example.automation.InAppMeetSessionManager
        private set

    lateinit var automationEngine: MeetingAutomationEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        val appDao = database.appDao()

        scheduleRepository = ScheduleRepository(appDao)
        executionRepository = ExecutionRepository(appDao)
        logRepository = LogRepository(appDao)
        secureAccountStorage = SecureAccountStorage(this)
        sessionManager = SessionManager()
        scheduleManager = ScheduleManager(this)
        inAppMeetSessionManager = com.example.automation.InAppMeetSessionManager(this)

        automationEngine = AndroidMeetAutomationEngine(
            context = this,
            logRepo = logRepository,
            executionRepo = executionRepository,
            sessionManager = sessionManager,
            accountStorage = secureAccountStorage,
            inAppMeetSessionManager = inAppMeetSessionManager
        )
    }

    companion object {
        lateinit var instance: AlzuhraApp
            private set
    }
}
