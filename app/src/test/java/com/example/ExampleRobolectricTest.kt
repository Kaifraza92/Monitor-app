package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AppDayOfWeek
import com.example.domain.model.Schedule
import com.example.scheduler.ScheduleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Al Zuhra Class Monitoring", appName)
    }

    @Test
    fun `schedule manager calculates future occurrence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduleManager = ScheduleManager(context)

        val schedule = Schedule(
            id = 1L,
            name = "Urdu Reading",
            meetUrl = "https://meet.google.com/abc-defg-hij",
            startTime = "19:00",
            endTime = "20:00",
            daysOfWeek = setOf(AppDayOfWeek.MONDAY, AppDayOfWeek.WEDNESDAY, AppDayOfWeek.FRIDAY),
            enabled = true
        )

        val nextOccurrence = scheduleManager.calculateNextOccurrence(schedule.daysOfWeek, schedule.startTime)
        assertNotNull(nextOccurrence)
        assertTrue(nextOccurrence!! > System.currentTimeMillis())
    }

    @Test
    fun `time utilities convert and calculate duration properly`() {
        val displayTime1 = com.example.ui.schedules.formatDisplayTime("19:00")
        assertEquals("07:00 PM", displayTime1)

        val displayTime2 = com.example.ui.schedules.formatDisplayTime("08:30")
        assertEquals("08:30 AM", displayTime2)

        val duration = com.example.ui.schedules.calculateDurationMinutes("19:00", "20:30")
        assertEquals(90, duration)

        val addedTime = com.example.ui.schedules.addMinutesToTime("19:00", 45)
        assertEquals("19:45", addedTime)
    }
}
