package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.automation.MonitoringForegroundService
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.attendance.AttendanceViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.logs.LogsScreen
import com.example.ui.logs.LogsViewModel
import com.example.ui.meeting.InAppMeetingScreen
import com.example.ui.navigation.Screen
import com.example.ui.schedules.SchedulesScreen
import com.example.ui.schedules.SchedulesViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.AlzuhraTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val shouldNavigateToMeeting = intent?.getBooleanExtra(
            MonitoringForegroundService.EXTRA_NAVIGATE_TO_MEETING,
            false
        ) ?: false

        setContent {
            AlzuhraTheme {
                val postNotificationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { _ -> }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasPermission) {
                            postNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                AlzuhraMainApp(openMeetingOnStart = shouldNavigateToMeeting)
            }
        }
    }
}

@Composable
fun AlzuhraMainApp(openMeetingOnStart: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(openMeetingOnStart) {
        if (openMeetingOnStart) {
            navController.navigate(Screen.LiveMeeting.route)
        }
    }

    val homeViewModel: HomeViewModel = viewModel()
    val schedulesViewModel: SchedulesViewModel = viewModel()
    val attendanceViewModel: AttendanceViewModel = viewModel()
    val logsViewModel: LogsViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val navItems = listOf(
        NavigationItemData(Screen.Home.route, "Home", Icons.Default.Home, "nav_home"),
        NavigationItemData(Screen.Schedules.route, "Schedules", Icons.Default.Schedule, "nav_schedules"),
        NavigationItemData(Screen.Attendance.route, "Attendance", Icons.Default.AssignmentTurnedIn, "nav_attendance"),
        NavigationItemData(Screen.Logs.route, "Logs", Icons.Default.Terminal, "nav_logs"),
        NavigationItemData(Screen.Settings.route, "Settings", Icons.Default.Settings, "nav_settings")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentRoute != Screen.LiveMeeting.route) {
                NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(imageVector = item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToSchedules = { navController.navigate(Screen.Schedules.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                    onNavigateToLiveMeeting = { navController.navigate(Screen.LiveMeeting.route) }
                )
            }
            composable(Screen.Schedules.route) {
                SchedulesScreen(viewModel = schedulesViewModel)
            }
            composable(Screen.Attendance.route) {
                AttendanceScreen(viewModel = attendanceViewModel)
            }
            composable(Screen.Logs.route) {
                LogsScreen(viewModel = logsViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable(Screen.LiveMeeting.route) {
                InAppMeetingScreen(
                    sessionManager = AlzuhraApp.instance.inAppMeetSessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onStopSession = {
                        AlzuhraApp.instance.automationEngine.cancelActiveSession()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private data class NavigationItemData(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
)
