package com.alvin.neuromind.ui.settings

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
    var showDemoDataConfirmation by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Demo Data Confirmation Dialog
    if (showDemoDataConfirmation) {
        AlertDialog(
            onDismissRequest = { showDemoDataConfirmation = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
            title = { Text("Load Demo Data?") },
            text = { Text("This will add sample tasks and timetable entries to your app so you can test the features. It won't delete your existing data.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.generateDemoData()
                    showDemoDataConfirmation = false
                }) { Text("Load Data") }
            },
            dismissButton = { TextButton(onClick = { showDemoDataConfirmation = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            // Section: Data & Testing
            item { SettingsSectionHeader("Data & Testing") }
            item {
                SettingsItem(
                    icon = Icons.Default.Science,
                    title = "Load Demo Data",
                    subtitle = "Populate app with sample schedule",
                    onClick = { showDemoDataConfirmation = true }
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

            // Section: About
            item { SettingsSectionHeader("About") }
            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "Neuromind v3.2 (Beta)",
                    onClick = { }
                )
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