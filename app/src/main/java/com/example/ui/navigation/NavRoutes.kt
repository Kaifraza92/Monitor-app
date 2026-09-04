package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Home : Screen("home", "Home")
    data object Schedules : Screen("schedules", "Schedules")
    data object Attendance : Screen("attendance", "Attendance")
    data object Logs : Screen("logs", "Logs")
    data object Settings : Screen("settings", "Settings")
    data object LiveMeeting : Screen("live_meeting", "In-App Meeting")
}
