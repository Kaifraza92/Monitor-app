package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusFailed
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.ZuhraAccentRed
import com.example.ui.theme.ZuhraCrimson
import com.example.ui.util.SystemPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val account by viewModel.account.collectAsStateWithLifecycle()
    val checklist by viewModel.checklist.collectAsStateWithLifecycle()
    val timezone by viewModel.timezoneInfo.collectAsStateWithLifecycle()

    var showEditAccountDialog by remember { mutableStateOf(false) }
    var showA11yGuideDialog by remember { mutableStateOf(false) }
    var showLimitationsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Permissions",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("settings_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Google Account Card (Section 4)
            item {
                GoogleAccountCard(
                    email = account.email,
                    displayName = account.displayName,
                    isConnected = account.isConnected,
                    onEdit = { showEditAccountDialog = true },
                    onToggleConnection = {
                        if (account.isConnected) {
                            viewModel.disconnectAccount()
                        } else {
                            viewModel.reconnectAccount()
                        }
                    }
                )
            }

            // Setup Checklist (Section 17)
            item {
                SetupChecklistCard(
                    checklist = checklist,
                    onOpenA11y = { showA11yGuideDialog = true },
                    onOpenBattery = { SystemPermissionHelper.openBatteryOptimizationSettings(context) },
                    onOpenExactAlarm = { SystemPermissionHelper.openExactAlarmSettings(context) },
                    onOpenNotifications = { SystemPermissionHelper.openAppSettings(context) }
                )
            }

            // Timezone Display (Section 28)
            item {
                TimezoneCard(timezone = timezone)
            }

            // Android Architecture Limitations & Integrity (Section 2, 11, 34)
            item {
                KnownLimitationsCard(onLearnMore = { showLimitationsDialog = true })
            }
        }

        if (showEditAccountDialog) {
            EditAccountDialog(
                initialEmail = account.email,
                initialName = account.displayName,
                onDismiss = { showEditAccountDialog = false },
                onSave = { email, name ->
                    viewModel.updateAccount(email, name)
                    showEditAccountDialog = false
                }
            )
        }

        if (showA11yGuideDialog) {
            AccessibilityGuideDialog(
                onDismiss = { showA11yGuideDialog = false },
                onOpenSettings = {
                    showA11yGuideDialog = false
                    SystemPermissionHelper.openAccessibilitySettings(context)
                }
            )
        }

        if (showLimitationsDialog) {
            AndroidLimitationsDialog(onDismiss = { showLimitationsDialog = false })
        }
    }
}

@Composable
private fun GoogleAccountCard(
    email: String,
    displayName: String,
    isConnected: Boolean,
    onEdit: () -> Unit,
    onToggleConnection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_account_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connected Google Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isConnected) StatusActive.copy(alpha = 0.15f) else StatusFailed.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isConnected) "● Connected" else "● Disconnected",
                        color = if (isConnected) StatusActive else StatusFailed,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zero-credential storage: Passwords are never requested or stored. Meet sessions use native Android accounts.",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(34.dp).testTag("switch_account_btn")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Switch", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onToggleConnection,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) ZuhraCrimson else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.height(34.dp).testTag("toggle_account_btn")
                ) {
                    Text(if (isConnected) "Disconnect" else "Reconnect", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun SetupChecklistCard(
    checklist: SetupChecklist,
    onOpenA11y: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenExactAlarm: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("setup_checklist_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Initial Setup & Permissions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${checklist.completedCount}/${checklist.totalCount} Ready",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (checklist.isAllComplete) StatusActive else StatusWarning
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { checklist.completedCount.toFloat() / checklist.totalCount.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (checklist.isAllComplete) StatusActive else ZuhraAccentRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ChecklistRow(
                    title = "Google Account",
                    subtitle = "Configured for class presence",
                    isComplete = checklist.isAccountConnected,
                    onClick = null
                )
                ChecklistRow(
                    title = "Notifications Permission",
                    subtitle = "Required for foreground monitoring notification",
                    isComplete = checklist.areNotificationsEnabled,
                    onClick = onOpenNotifications
                )
                ChecklistRow(
                    title = "Accessibility Service",
                    subtitle = "Required to trigger Join & Leave buttons automatically",
                    isComplete = checklist.isAccessibilityEnabled,
                    onClick = onOpenA11y
                )
                ChecklistRow(
                    title = "Battery Optimization",
                    subtitle = "Exemption prevents Android from killing background alarms",
                    isComplete = checklist.isBatteryExempted,
                    onClick = onOpenBattery
                )
                ChecklistRow(
                    title = "Exact Alarm Permission",
                    subtitle = "Required for exact scheduled start and end triggers",
                    isComplete = checklist.canScheduleExactAlarms,
                    onClick = onOpenExactAlarm
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    subtitle: String,
    isComplete: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isComplete) StatusActive.copy(alpha = 0.15f) else StatusWarning.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isComplete) Icons.Default.Check else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isComplete) StatusActive else StatusWarning,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Configure",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun TimezoneCard(timezone: TimezoneInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Device Timezone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = timezone.id,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${timezone.displayName} (${timezone.offsetString})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Class schedules are automatically computed against the local device timezone and survive timezone changes.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KnownLimitationsCard(onLearnMore: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("known_limitations_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Known Android Limitations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(onClick = onLearnMore) {
                    Text("Read Details", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Android OS enforces strict background execution limits, Doze mode battery idling, and accessibility boundaries. Learn how al ZUHRA handles these transparently.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccessibilityGuideDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Automation Permission") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Android requires Accessibility Service access to perform Join and Leave actions automatically on Google Meet.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "The service is used strictly for:\n• Detecting Meet controls\n• Pressing Join\n• Pressing Leave\n• Verifying automation state",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Security guarantee: No keystrokes, passwords, messages, or unrelated screen content is ever read, recorded, or stored.",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onOpenSettings, modifier = Modifier.testTag("open_a11y_settings_btn")) {
                Text("Open Accessibility Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AndroidLimitationsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Known Android Limitations") },
        text = {
            LazyColumn(modifier = Modifier.height(300.dp)) {
                item {
                    Text(
                        text = "1. Background Activity Restrictions:\n" +
                                "Starting with Android 10, background activities cannot be launched silently without user interaction or full-screen notification intent.\n\n" +
                                "2. Doze Mode & Battery Optimization:\n" +
                                "When the device is unplugged and stationary, Doze mode suspends alarms and network. Battery optimization exemption is required.\n\n" +
                                "3. Multiple Concurrent Meet Sessions:\n" +
                                "One Android device and Chrome instance cannot simultaneously maintain multiple independent Meet sessions. The SessionManager enforces a 1-session concurrent limit.\n\n" +
                                "4. Google Meet UI Changes:\n" +
                                "Meet periodically updates button labels and layouts. The accessibility engine uses multi-attribute fuzzy matching to minimize breakage.\n\n" +
                                "5. Participant Rosters:\n" +
                                "Individual student attendance requires Google Workspace Admin audit APIs. Client-side presence is strictly limited to academy session tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}

@Composable
private fun EditAccountDialog(
    initialEmail: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Connected Google Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Account Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name / Organization") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(email.trim(), name.trim()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
