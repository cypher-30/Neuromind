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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.preferences.ThemeSetting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToTimetable: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val currentTheme by viewModel.themeSetting.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
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

            // Section: Features
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