package com.example.ui.schedules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppDayOfWeek
import com.example.domain.model.Schedule
import com.example.ui.theme.StatusActive
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusScheduled
import com.example.ui.theme.ZuhraAccentRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    viewModel: SchedulesViewModel
) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val testRunMessage by viewModel.testRunMessage.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<Schedule?>(null) }
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }
    var scheduleToInspect by remember { mutableStateOf<Schedule?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(testRunMessage) {
        testRunMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearTestMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Class Schedules",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSchedule = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_schedule_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Schedule")
            }
        }
    ) { paddingValues ->
        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Class Schedules Configured",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add class schedules with days of week and start/end times to enable automated monitoring.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            editingSchedule = null
                            showAddEditDialog = true
                        }
                    ) {
                        Text("Add First Schedule")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .testTag("schedules_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(schedules, key = { it.id }) { schedule ->
                    val isActive = activeSession?.schedule?.id == schedule.id
                    ScheduleItemCard(
                        schedule = schedule,
                        isActive = isActive,
                        nextRun = viewModel.getNextRunFormatted(schedule),
                        onToggle = { isEnabled -> viewModel.toggleEnabled(schedule, isEnabled) },
                        onEdit = {
                            editingSchedule = schedule
                            showAddEditDialog = true
                        },
                        onDelete = { scheduleToDelete = schedule },
                        onInspect = { scheduleToInspect = schedule },
                        onStartNow = { viewModel.startScheduleNow(schedule) },
                        onTestRun = { viewModel.runTestAutomation(schedule, 30) }
                    )
                }
            }
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditScheduleDialog(
                initialSchedule = editingSchedule,
                onDismiss = {
                    showAddEditDialog = false
                    editingSchedule = null
                },
                onSave = { newOrUpdated ->
                    viewModel.saveSchedule(newOrUpdated)
                    showAddEditDialog = false
                    editingSchedule = null
                }
            )
        }

        // Delete Confirmation Dialog
        scheduleToDelete?.let { toDelete ->
            AlertDialog(
                onDismissRequest = { scheduleToDelete = null },
                title = { Text("Delete Schedule") },
                text = { Text("Are you sure you want to delete '${toDelete.name}'? Automated alarms for this class will be removed.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSchedule(toDelete)
                            scheduleToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { scheduleToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Schedule Detail Inspection Dialog (Section 14)
        scheduleToInspect?.let { inspected ->
            ScheduleDetailDialog(
                schedule = inspected,
                nextRun = viewModel.getNextRunFormatted(inspected),
                onDismiss = { scheduleToInspect = null },
                onStartNow = {
                    viewModel.startScheduleNow(inspected)
                    scheduleToInspect = null
                },
                onTestRun = {
                    viewModel.runTestAutomation(inspected, 30)
                    scheduleToInspect = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleItemCard(
    schedule: Schedule,
    isActive: Boolean,
    nextRun: String,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onInspect: () -> Unit,
    onStartNow: () -> Unit,
    onTestRun: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("schedule_card_${schedule.id}"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val durationMin = calculateDurationMinutes(schedule.startTime, schedule.endTime)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${formatDisplayTime(schedule.startTime)} – ${formatDisplayTime(schedule.endTime)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ZuhraAccentRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${durationMin}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = ZuhraAccentRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = schedule.enabled,
                        onCheckedChange = onToggle,
                        modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days of week badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppDayOfWeek.entries.forEach { day ->
                    val isSelected = schedule.daysOfWeek.contains(day)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = day.shortName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Meet link & next run
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Link,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = schedule.meetUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Event,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Next: $nextRun",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onTestRun,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("test_btn_${schedule.id}")
                ) {
                    Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test (30s)", style = MaterialTheme.typography.labelSmall)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onStartNow,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp).testTag("start_btn_${schedule.id}")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start Now", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddEditScheduleDialog(
    initialSchedule: Schedule?,
    onDismiss: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    var name by remember { mutableStateOf(initialSchedule?.name ?: "") }
    var meetUrl by remember { mutableStateOf(initialSchedule?.meetUrl ?: "https://meet.google.com/") }
    var startTime by remember { mutableStateOf(initialSchedule?.startTime ?: "19:00") }
    var endTime by remember { mutableStateOf(initialSchedule?.endTime ?: "20:00") }
    var selectedDays by remember { mutableStateOf(initialSchedule?.daysOfWeek ?: setOf(AppDayOfWeek.MONDAY, AppDayOfWeek.WEDNESDAY, AppDayOfWeek.FRIDAY)) }
    var enabled by remember { mutableStateOf(initialSchedule?.enabled ?: true) }
    var notes by remember { mutableStateOf(initialSchedule?.notes ?: "") }

    var showStartTimeClockPicker by remember { mutableStateOf(false) }
    var showEndTimeClockPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (showStartTimeClockPicker) {
        ClockTimePickerDialog(
            title = "Select Start Time (Clock)",
            initialTime = startTime,
            onDismiss = { showStartTimeClockPicker = false },
            onConfirm = { newTime ->
                val prevDuration = calculateDurationMinutes(startTime, endTime)
                startTime = newTime
                if (prevDuration > 0) {
                    endTime = addMinutesToTime(newTime, prevDuration)
                }
                showStartTimeClockPicker = false
            }
        )
    }

    if (showEndTimeClockPicker) {
        ClockTimePickerDialog(
            title = "Select End Time (Clock)",
            initialTime = endTime,
            onDismiss = { showEndTimeClockPicker = false },
            onConfirm = { newTime ->
                endTime = newTime
                showEndTimeClockPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialSchedule == null) "Add Class Schedule" else "Edit Class Schedule")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Class / Session Name") },
                    placeholder = { Text("e.g. Urdu Reading") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("schedule_name_input")
                )

                OutlinedTextField(
                    value = meetUrl,
                    onValueChange = { meetUrl = it },
                    label = { Text("Google Meet URL") },
                    placeholder = { Text("https://meet.google.com/xxx-xxxx-xxx") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("schedule_url_input")
                )

                Text(
                    text = "Class Time (Clock Dial Selection):",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeSelectorCard(
                        label = "START TIME",
                        time24 = startTime,
                        onClick = { showStartTimeClockPicker = true },
                        modifier = Modifier.weight(1f).testTag("start_time_input")
                    )
                    TimeSelectorCard(
                        label = "END TIME",
                        time24 = endTime,
                        onClick = { showEndTimeClockPicker = true },
                        modifier = Modifier.weight(1f).testTag("end_time_input")
                    )
                }

                // Quick duration presets
                val currentDuration = calculateDurationMinutes(startTime, endTime)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duration: ${currentDuration}m",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = ZuhraAccentRed
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(30 to "+30m", 45 to "+45m", 60 to "+1h", 90 to "+1.5h").forEach { (mins, chipLabel) ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    endTime = addMinutesToTime(startTime, mins)
                                }
                            ) {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Repeat On:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppDayOfWeek.entries.forEach { day ->
                        val isChecked = selectedDays.contains(day)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                selectedDays = if (isChecked) selectedDays - day else selectedDays + day
                            }
                        ) {
                            Text(
                                text = day.shortName,
                                color = if (isChecked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Schedule", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Class name cannot be empty"
                        return@Button
                    }
                    if (meetUrl.isBlank() || !meetUrl.contains("meet.google.com")) {
                        errorMessage = "Please enter a valid Google Meet link"
                        return@Button
                    }
                    if (selectedDays.isEmpty()) {
                        errorMessage = "Select at least one day of the week"
                        return@Button
                    }

                    val schedule = Schedule(
                        id = initialSchedule?.id ?: 0L,
                        name = name.trim(),
                        meetUrl = meetUrl.trim(),
                        startTime = startTime.trim(),
                        endTime = endTime.trim(),
                        daysOfWeek = selectedDays,
                        enabled = enabled,
                        notes = notes.trim()
                    )
                    onSave(schedule)
                },
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text("Save Schedule")
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
private fun ScheduleDetailDialog(
    schedule: Schedule,
    nextRun: String,
    onDismiss: () -> Unit,
    onStartNow: () -> Unit,
    onTestRun: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(schedule.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailField(label = "Google Meet URL", value = schedule.meetUrl)
                DetailField(
                    label = "Time",
                    value = "${formatDisplayTime(schedule.startTime)} – ${formatDisplayTime(schedule.endTime)} (${schedule.startTime} – ${schedule.endTime}, ${calculateDurationMinutes(schedule.startTime, schedule.endTime)}m)"
                )
                DetailField(label = "Days", value = schedule.daysOfWeek.joinToString(", ") { it.displayName })
                DetailField(label = "Status", value = if (schedule.enabled) "Enabled (Active Alarms)" else "Disabled")
                DetailField(label = "Next Scheduled Run", value = nextRun)
                DetailField(label = "Presence Mode", value = "Automated Join & Leave (Mic & Cam OFF)")
            }
        },
        confirmButton = {
            Button(onClick = onStartNow) {
                Text("Start Now")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onTestRun) {
                Text("Test (30s)")
            }
        }
    )
}

@Composable
private fun TimeSelectorCard(
    label: String,
    time24: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = ZuhraAccentRed
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = ZuhraAccentRed
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatDisplayTime(time24),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = time24,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Tap for clock",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockTimePickerDialog(
    title: String,
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 19
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = ZuhraAccentRed
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rotate or tap the clock dial to select time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = ZuhraAccentRed,
                        containerColor = MaterialTheme.colorScheme.surface,
                        periodSelectorBorderColor = ZuhraAccentRed,
                        periodSelectorSelectedContainerColor = ZuhraAccentRed.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = ZuhraAccentRed,
                        timeSelectorSelectedContainerColor = ZuhraAccentRed.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = ZuhraAccentRed
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onConfirm(formatted)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ZuhraAccentRed)
            ) {
                Text("Confirm Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun formatDisplayTime(time24: String): String {
    val parts = time24.split(":")
    if (parts.size != 2) return time24
    val hour = parts[0].toIntOrNull() ?: return time24
    val minute = parts[1].toIntOrNull() ?: return time24
    val amPm = if (hour >= 12) "PM" else "AM"
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format("%02d:%02d %s", hour12, minute, amPm)
}

fun calculateDurationMinutes(start: String, end: String): Int {
    val startParts = start.split(":")
    val endParts = end.split(":")
    if (startParts.size != 2 || endParts.size != 2) return 0
    val startMin = (startParts[0].toIntOrNull() ?: 0) * 60 + (startParts[1].toIntOrNull() ?: 0)
    val endMin = (endParts[0].toIntOrNull() ?: 0) * 60 + (endParts[1].toIntOrNull() ?: 0)
    val diff = endMin - startMin
    return if (diff < 0) diff + 24 * 60 else diff
}

fun addMinutesToTime(start: String, minutesToAdd: Int): String {
    val parts = start.split(":")
    if (parts.size != 2) return start
    val hour = parts[0].toIntOrNull() ?: 0
    val minute = parts[1].toIntOrNull() ?: 0
    val totalMin = (hour * 60 + minute + minutesToAdd) % (24 * 60)
    val newHour = totalMin / 60
    val newMinute = totalMin % 60
    return String.format("%02d:%02d", newHour, newMinute)
}

@Composable
private fun DetailField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
