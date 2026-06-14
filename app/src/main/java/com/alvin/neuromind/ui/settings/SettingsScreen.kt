package com.alvin.neuromind.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.preferences.TaskStyle
import com.alvin.neuromind.data.preferences.ThemeSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val currentTheme by viewModel.themeSetting.collectAsStateWithLifecycle()
    val currentPeakStart by viewModel.peakStartHour.collectAsStateWithLifecycle()
    val currentPeakEnd by viewModel.peakEndHour.collectAsStateWithLifecycle()
    val currentSessionLength by viewModel.preferredSessionLength.collectAsStateWithLifecycle()
    val currentTaskStyle by viewModel.taskStyle.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPeakHoursDialog by remember { mutableStateOf(false) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var showTaskStyleDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    // Developer Mode State
    var devModeClicks by remember { mutableIntStateOf(0) }
    var isDevModeEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    ThemeSetting.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTheme(theme)
                                    showThemeDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTheme == theme,
                                onClick = {
                                    viewModel.updateTheme(theme)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = theme.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") } }
        )
    }

    if (showPeakHoursDialog) {
        AlertDialog(
            onDismissRequest = { showPeakHoursDialog = false },
            title = { Text("Peak Focus Window") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "When is your mind at its sharpest?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HourPickerRow(
                        label = "Start",
                        hour = currentPeakStart,
                        onDecrement = { if (currentPeakStart > 0) viewModel.updatePeakStartHour(currentPeakStart - 1) },
                        onIncrement = { if (currentPeakStart < currentPeakEnd - 1) viewModel.updatePeakStartHour(currentPeakStart + 1) }
                    )
                    HourPickerRow(
                        label = "End",
                        hour = currentPeakEnd,
                        onDecrement = { if (currentPeakEnd > currentPeakStart + 1) viewModel.updatePeakEndHour(currentPeakEnd - 1) },
                        onIncrement = { if (currentPeakEnd < 23) viewModel.updatePeakEndHour(currentPeakEnd + 1) }
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showPeakHoursDialog = false }) { Text("Done") } }
        )
    }

    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = { Text("Session Length") },
            text = {
                Column {
                    listOf(25, 45, 60, 90).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSessionLength(mins)
                                    showSessionDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSessionLength == mins,
                                onClick = {
                                    viewModel.updateSessionLength(mins)
                                    showSessionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$mins minutes")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSessionDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTaskStyleDialog) {
        AlertDialog(
            onDismissRequest = { showTaskStyleDialog = false },
            title = { Text("Task Style") },
            text = {
                Column {
                    TaskStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTaskStyle(style)
                                    showTaskStyleDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentTaskStyle == style,
                                onClick = {
                                    viewModel.updateTaskStyle(style)
                                    showTaskStyleDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(style.name.lowercase().replaceFirstChar { it.uppercase() })
                                val description = when (style) {
                                    TaskStyle.ANALYTICAL -> "Structured, logical tasks first"
                                    TaskStyle.CREATIVE -> "Open-ended, ideation-heavy tasks first"
                                    TaskStyle.BALANCED -> "Mix of both types"
                                }
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTaskStyleDialog = false }) { Text("Cancel") } }
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Reset Task & Schedule Data?") },
            text = { Text("This will delete all your tasks and timetable entries. Your settings (like Theme) will be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAppData()
                        showResetConfirmation = false
                        Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            // Section: Appearance
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "App Theme",
                    subtitle = currentTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showThemeDialog = true }
                )
            }

            // Section: Cognitive Profile
            item { SettingsSectionHeader("Cognitive Profile") }
            item {
                SettingsItem(
                    icon = Icons.Default.Schedule,
                    title = "Peak Focus Hours",
                    subtitle = "${formatHour(currentPeakStart)} – ${formatHour(currentPeakEnd)}",
                    onClick = { showPeakHoursDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Timer,
                    title = "Session Length",
                    subtitle = "$currentSessionLength minutes",
                    onClick = { showSessionDialog = true }
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Lightbulb,
                    title = "Task Style",
                    subtitle = currentTaskStyle.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showTaskStyleDialog = true }
                )
            }

            // Section: Quick Access
            item { SettingsSectionHeader("Quick Access") }
            item {
                SettingsItem(
                    icon = Icons.Default.CalendarViewWeek,
                    title = "Weekly Timetable",
                    onClick = onNavigateToTimetable,
                    showArrow = true
                )
            }
            item {
                SettingsItem(
                    icon = Icons.Default.Reviews,
                    title = "End-of-Day Review",
                    onClick = onNavigateToFeedback,
                    showArrow = true
                )
            }

            // Section: About (With Dev Mode Trigger)
            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = if (isDevModeEnabled) "Neuromind v3.7 (Dev Mode Active)" else "Neuromind v3.7",
                    onClick = {
                        if (!isDevModeEnabled) {
                            devModeClicks++
                            if (devModeClicks >= 4) {
                                isDevModeEnabled = true
                                Toast.makeText(context, "Developer Mode Enabled", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            // Section: Developer Options (Hidden until unlocked)
            if (isDevModeEnabled) {
                item { SettingsSectionHeader("Developer Options") }
                item {
                    SettingsItem(
                        icon = Icons.Default.AddCircleOutline,
                        title = "Load Sample Tasks",
                        subtitle = "Adds 5 realistic tasks at once",
                        onClick = {
                            viewModel.generateDemoData()
                            Toast.makeText(context, "Tasks Added", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.CalendarViewWeek,
                        title = "Load Full Timetable",
                        subtitle = "Adds a full Mon-Fri class schedule",
                        onClick = {
                            viewModel.generateBaseTimetable()
                            Toast.makeText(context, "Timetable Loaded", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Test Notifications",
                        subtitle = "Trigger a notification immediately",
                        onClick = { viewModel.testNotification(context) }
                    )
                }
                item {
                    SettingsItem(
                        icon = Icons.Default.DeleteForever,
                        title = "Reset App Data",
                        subtitle = "Clear Tasks & Timetable DB",
                        onClick = { showResetConfirmation = true }
                    )
                }
            }
        }
    }
}

@Composable
private fun HourPickerRow(
    label: String,
    hour: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease hour")
            }
            Text(
                text = formatHour(hour),
                modifier = Modifier.width(64.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Default.Add, contentDescription = "Increase hour")
            }
        }
    }
}

private fun formatHour(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = if (showArrow) { { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) } } else null,
        modifier = Modifier.clickable { onClick() }
    )
}
