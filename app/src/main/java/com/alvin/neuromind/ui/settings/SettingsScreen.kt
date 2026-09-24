package com.alvin.neuromind.ui.settings

import com.alvin.neuromind.ui.components.OrganicConfirmDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.data.preferences.PeakWindow
import com.alvin.neuromind.data.preferences.TaskStyle
import com.alvin.neuromind.data.preferences.ThemeSetting
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.OrganicToggle
import com.alvin.neuromind.ui.components.PillChip
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.components.SectionKicker
import com.alvin.neuromind.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val SESSION_LENGTH_OPTIONS = listOf(15, 25, 45, 60)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToFeedback: () -> Unit,
    onNavigateToPreviews: () -> Unit,
    onReplaySplash: () -> Unit
) {
    val currentTheme by viewModel.themeSetting.collectAsStateWithLifecycle()
    val currentPeakWindow by viewModel.peakWindow.collectAsStateWithLifecycle()
    val currentSessionLength by viewModel.preferredSessionLength.collectAsStateWithLifecycle()
    val currentTaskStyle by viewModel.taskStyle.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val currentWidgetStackInterval by viewModel.widgetStackIntervalMinutes.collectAsStateWithLifecycle()

    val appInfo by viewModel.appInfo.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()

    var showWidgetStackDialog by remember { mutableStateOf(false) }
    var showMiniQuickLogHelpDialog by remember { mutableStateOf(false) }
    var showQuickTileHelpDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var showClearFeedbackConfirmation by remember { mutableStateOf(false) }
    var showClearFocusConfirmation by remember { mutableStateOf(false) }

    // Developer Mode State
    var isDevModeEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(backupMessage) {
        backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearBackupMessage()
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { viewModel.exportBackup(context, it) } }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.importBackup(context, it) } }

    if (showWidgetStackDialog) {
        AlertDialog(
            onDismissRequest = { showWidgetStackDialog = false },
            title = { Text("Widget Stack Interval") },
            text = {
                Column {
                    listOf(15, 30, 60).forEach { mins ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateWidgetStackInterval(mins)
                                    showWidgetStackDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentWidgetStackInterval == mins,
                                onClick = {
                                    viewModel.updateWidgetStackInterval(mins)
                                    showWidgetStackDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Every $mins minutes")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWidgetStackDialog = false }) { Text("Cancel") } }
        )
    }

    if (showMiniQuickLogHelpDialog) {
        AlertDialog(
            onDismissRequest = { showMiniQuickLogHelpDialog = false },
            title = { Text("Pin Mini Quick Log Widget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1) Long-press your home screen and tap Widgets.")
                    Text("2) Find Neuromind > Mini Quick Log (1x1).")
                    Text("3) Drag it to Home or Lock screen (if your launcher supports it).")
                    Text("4) Tap the widget anytime to open voice logging instantly.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showMiniQuickLogHelpDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showQuickTileHelpDialog) {
        AlertDialog(
            onDismissRequest = { showQuickTileHelpDialog = false },
            title = { Text("Add Quick Voice Log Tile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1) Swipe down twice to open Quick Settings.")
                    Text("2) Tap Edit (pencil icon).")
                    Text("3) Find 'Quick Voice Log' and drag it into active tiles.")
                    Text("4) Tap the tile to jump straight into voice capture.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickTileHelpDialog = false }) { Text("Got it") }
            }
        )
    }

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("App & Database Info") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppInfoRow("App version",    "v${appInfo.versionName}")
                    AppInfoRow("DB version",     "${appInfo.dbVersion}")
                    HorizontalDivider()
                    AppInfoRow("Tasks",           "${appInfo.taskCount} rows")
                    AppInfoRow("Timetable",       "${appInfo.timetableCount} rows")
                    AppInfoRow("Feedback logs",   "${appInfo.feedbackCount} rows")
                    AppInfoRow("Focus sessions",  "${appInfo.focusSessionCount} rows")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) { Text("Close") }
            }
        )
    }

    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Privacy Policy") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Last updated: August 30, 2026", style = MaterialTheme.typography.labelSmall)
                    Text("Neuromind stores your tasks, timetable, feedback logs, and focus history in local app storage on your device.")
                    Text("Voice notes use Android's system speech recognizer. Speech processing may be handled by your device provider (for many devices, Google). Neuromind receives the transcribed text result; Neuromind does not intentionally upload or keep raw audio recordings.")
                    Text("If cloud speech services are active on your device, voice data handling and retention are controlled by your device account settings and provider policies, not directly by Neuromind.")
                    Text("You can delete your local content from Neuromind by clearing feedback logs, deleting tasks, or resetting app data in Settings.")
                    Text("Neuromind does not sell your data. Any future analytics/third-party services will be disclosed here before release.")
                    Text("By using voice input, you acknowledge system-level speech processing may occur outside the app.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) { Text("Close") }
            }
        )
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            icon = { Icon(Icons.Default.Gavel, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Terms & Conditions") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Last updated: August 30, 2026", style = MaterialTheme.typography.labelSmall)
                    Text("Neuromind is provided as a personal productivity and wellbeing support tool.")
                    Text("It is not medical advice, diagnosis, treatment, or emergency support. In urgent situations, contact local emergency services.")
                    Text("You are responsible for how you use reminders, scheduling suggestions, and voice transcription outputs.")
                    Text("Speech transcription accuracy depends on your device recognizer and may contain errors. Review content before acting on it.")
                    Text("You may export, restore, and delete your app data from the Settings section.")
                    Text("Use of device-level services (such as system speech recognition and notifications) is subject to those platform terms.")
                    Text("Continued use of Neuromind means you accept these terms and the Privacy Policy.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) { Text("Close") }
            }
        )
    }

    if (showResetConfirmation) {
        DestructiveConfirmDialog(
            title = "Reset task & schedule data?",
            body = "This will delete all your tasks, timetable entries, events and unsaved drafts. Your settings (like Theme) will be saved. This can't be undone.",
            confirmLabel = "Reset",
            onConfirm = {
                viewModel.resetAppData()
                showResetConfirmation = false
                Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showResetConfirmation = false }
        )
    }

    if (showRestoreConfirmation) {
        DestructiveConfirmDialog(
            title = "Restore from backup?",
            body = "This replaces all current tasks, timetable and events, feedback, and focus history with the contents of the selected file, and clears unsaved drafts. This can't be undone.",
            confirmLabel = "Restore",
            onConfirm = {
                showRestoreConfirmation = false
                restoreBackupLauncher.launch(arrayOf("application/json"))
            },
            onDismiss = { showRestoreConfirmation = false }
        )
    }

    if (showClearFeedbackConfirmation) {
        DestructiveConfirmDialog(
            title = "Clear all feedback logs?",
            body = "Every mood/energy check-in will be deleted and Insights will reset. This can't be undone.",
            confirmLabel = "Clear",
            onConfirm = {
                showClearFeedbackConfirmation = false
                viewModel.clearFeedbackLogs()
                Toast.makeText(context, "Feedback logs cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearFeedbackConfirmation = false }
        )
    }

    if (showClearFocusConfirmation) {
        DestructiveConfirmDialog(
            title = "Clear all focus sessions?",
            body = "Your deep-work history will be deleted. This can't be undone.",
            confirmLabel = "Clear",
            onConfirm = {
                showClearFocusConfirmation = false
                viewModel.clearFocusSessions()
                Toast.makeText(context, "Focus sessions cleared", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showClearFocusConfirmation = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomWithNav)
    ) {
        item {
            Spacer(Modifier.height(Dimens.ScreenPaddingTop))
            ScreenHeader(title = "Settings")
            Spacer(Modifier.height(Dimens.SectionGap))
        }

        // Appearance + Notifications
        item {
            OrganicCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = when (currentTheme) {
                                ThemeSetting.LIGHT -> ThemeSetting.DARK
                                ThemeSetting.DARK -> ThemeSetting.SYSTEM
                                ThemeSetting.SYSTEM -> ThemeSetting.LIGHT
                            }
                            viewModel.updateTheme(next)
                        }
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Appearance", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            currentTheme.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    OrganicToggle(checked = notificationsEnabled, onCheckedChange = { viewModel.toggleNotifications() })
                }
            }
            Spacer(Modifier.height(Dimens.CardGap))
        }

        // Cognitive profile
        item {
            SectionKicker("Cognitive profile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OrganicCard {
                Text("Peak focus window", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PeakWindow.entries.forEach { window ->
                        PillChip(
                            label = window.label,
                            selected = currentPeakWindow == window,
                            onClick = { viewModel.selectPeakWindow(window) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text("Session length", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SESSION_LENGTH_OPTIONS.forEach { mins ->
                        EvenChip(
                            label = "$mins min",
                            selected = currentSessionLength == mins,
                            onClick = { viewModel.updateSessionLength(mins) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                Text("Planning style", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskStyle.entries.forEach { style ->
                        EvenChip(
                            label = when (style) {
                                TaskStyle.ANALYTICAL -> "Structured"
                                TaskStyle.CREATIVE -> "Flexible"
                                TaskStyle.BALANCED -> "Balanced"
                            },
                            selected = currentTaskStyle == style,
                            onClick = { viewModel.updateTaskStyle(style) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(Dimens.CardGap))
        }

        // Quick access rows
        item {
            SettingsRow(
                title = "End-of-day review",
                onClick = onNavigateToFeedback
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                title = "Widget & notifications",
                onClick = onNavigateToPreviews
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                title = "Pin Mini Quick Log Widget",
                subtitle = "One-tap voice logging from home or lock screen",
                onClick = { showMiniQuickLogHelpDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                title = "Add Quick Voice Log Tile",
                subtitle = "One-swipe access from Android Quick Settings",
                onClick = { showQuickTileHelpDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                title = "Widget Stack Interval",
                subtitle = "Auto Stack rotates every $currentWidgetStackInterval min",
                onClick = { showWidgetStackDialog = true },
                showArrow = false
            )
            Spacer(Modifier.height(Dimens.CardGap))
        }

        // Data (backup & restore)
        item {
            SectionKicker("Data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                title = "Export backup",
                subtitle = "Save all tasks, timetable, feedback, and focus history to a file",
                onClick = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    exportBackupLauncher.launch("neuromind_backup_$timestamp.json")
                },
                showArrow = false
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                title = "Restore backup",
                subtitle = "Replace current data with a previously exported file",
                onClick = { showRestoreConfirmation = true },
                showArrow = false
            )
            Spacer(Modifier.height(Dimens.CardGap))
        }

        // About
        item {
            SectionKicker("About", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                title = "Privacy Policy",
                subtitle = "How voice, text, and local data are handled",
                onClick = { showPrivacyPolicyDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            SettingsRow(
                title = "Terms & Conditions",
                subtitle = "Usage terms and important disclaimers",
                onClick = { showTermsDialog = true }
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (isDevModeEnabled) {
                    "Neuromind v${appInfo.versionName} (Dev Mode Active)"
                } else {
                    "Neuromind v${appInfo.versionName} — Preview launch animation"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    // Tap matches the design's replay affordance; long-press keeps
                    // the pre-existing hidden dev-mode unlock without the two
                    // gestures fighting over the same single-tap event.
                    .combinedClickable(
                        onClick = onReplaySplash,
                        onLongClick = {
                            isDevModeEnabled = !isDevModeEnabled
                            Toast.makeText(
                                context,
                                if (isDevModeEnabled) "Developer Mode Enabled" else "Developer Mode Disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    .padding(vertical = 8.dp)
            )
        }

        // Developer Options (hidden until unlocked)
        if (isDevModeEnabled) {
            item {
                Spacer(Modifier.height(Dimens.CardGap))
                SectionKicker("Developer options", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }
            item {
                SettingsRow(
                    title = "Load Sample Tasks",
                    subtitle = "Adds 5 realistic tasks at once",
                    onClick = {
                        viewModel.generateDemoData()
                        Toast.makeText(context, "Tasks Added", Toast.LENGTH_SHORT).show()
                    },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Load Full Timetable",
                    subtitle = "Adds a full Mon-Fri class schedule",
                    onClick = {
                        viewModel.generateBaseTimetable()
                        Toast.makeText(context, "Timetable Loaded", Toast.LENGTH_SHORT).show()
                    },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Test Notifications",
                    subtitle = "Trigger a notification immediately",
                    onClick = { viewModel.testNotification(context) },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Reset App Data",
                    subtitle = "Clear Tasks & Timetable DB",
                    onClick = { showResetConfirmation = true },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Seed 14-Day Feedback",
                    subtitle = "Populate Insights & retro cards with sample data",
                    onClick = {
                        viewModel.seedFeedbackLogs()
                        Toast.makeText(context, "14 days of feedback added", Toast.LENGTH_SHORT).show()
                    },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Clear Feedback Logs",
                    subtitle = "Wipe the FeedbackLog table only",
                    onClick = { showClearFeedbackConfirmation = true },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Seed Focus Sessions",
                    subtitle = "Populate the Deep Work card with sample sessions",
                    onClick = {
                        viewModel.seedFocusSessions()
                        Toast.makeText(context, "14 focus sessions added", Toast.LENGTH_SHORT).show()
                    },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "Clear Focus Sessions",
                    subtitle = "Wipe the focus_sessions table only",
                    onClick = { showClearFocusConfirmation = true },
                    showArrow = false
                )
                Spacer(Modifier.height(10.dp))
                SettingsRow(
                    title = "App & DB Info",
                    subtitle = "Version, DB version, and row counts",
                    onClick = { showAppInfoDialog = true },
                    showArrow = false
                )
            }
        }
    }
}

@Composable
private fun EvenChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showArrow: Boolean = true
) {
    OrganicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showArrow) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DestructiveConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    OrganicConfirmDialog(
        title = title,
        message = body,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.Default.Warning
    )
}
