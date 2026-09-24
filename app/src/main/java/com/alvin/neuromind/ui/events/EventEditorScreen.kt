package com.alvin.neuromind.ui.events

import com.alvin.neuromind.ui.components.OrganicConfirmDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.data.EventReminder
import com.alvin.neuromind.domain.EventReminderPolicy
import com.alvin.neuromind.domain.EventReminderScheduler
import com.alvin.neuromind.ui.components.LabeledField
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.OrganicToggle
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.PillChip
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.tasks.ConfirmDiscardDraftDialog
import com.alvin.neuromind.ui.tasks.DraftRestoredBanner
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.timetable.TimePickerDialog
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEditorScreen(
    viewModel: EventEditorViewModel,
    reminderScheduler: EventReminderScheduler,
    appNotificationsEnabled: Boolean,
    onNavigateUp: () -> Unit,
    topContent: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.resultMessage) {
        uiState.resultMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            onNavigateUp()
        }
    }

    // Re-check system permissions whenever we come back from the Settings app.
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumeTick++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val canExact = remember(resumeTick) { reminderScheduler.canScheduleExact() }
    val systemNotificationsOn = remember(resumeTick) { NotificationManagerCompat.from(context).areNotificationsEnabled() }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showStartPicker by rememberSaveable { mutableStateOf(false) }
    var showEndPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val timeFormat = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val reminderFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a") }

    if (showDatePicker) {
        // DatePicker speaks UTC-midnight millis; convert explicitly so the day never shifts.
        val initial = (uiState.date ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = pickerState) }
    }
    if (showStartPicker) TimePickerDialog({ showStartPicker = false }, viewModel::onStartTimeChange, uiState.startTime)
    if (showEndPicker) TimePickerDialog({ showEndPicker = false }, viewModel::onEndTimeChange, uiState.endTime)
    if (confirmDiscard) {
        ConfirmDiscardDraftDialog(
            onConfirm = { viewModel.discardDraft(); confirmDiscard = false },
            onDismiss = { confirmDiscard = false }
        )
    }
    if (confirmDelete) {
        OrganicConfirmDialog(
            title = "Delete this event?",
            message = "\"${uiState.title.ifBlank { "Untitled event" }}\" and its reminder will be permanently removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomNoNav),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        item {
            Spacer(Modifier.height(Dimens.ScreenPaddingTop - Dimens.CardGap))
            ScreenHeader(
                title = if (uiState.isEditing) "Edit event" else "New event",
                onBack = onNavigateUp,
                actions = {
                    if (uiState.isEditing && !uiState.isFinished) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete event", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }

        item { topContent() }

        if (uiState.restoredDraft) {
            item { DraftRestoredBanner(onKeep = viewModel::dismissRestoredBanner, onDiscard = { confirmDiscard = true }) }
        }

        item {
            LabeledField(
                label = "Event",
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                placeholder = "e.g. Research conference"
            )
        }

        item {
            Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Icon(Icons.Default.DateRange, null)
                Spacer(Modifier.width(8.dp))
                Text(uiState.date?.format(dateFormat) ?: "Pick a date")
            }
            if (uiState.date?.isBefore(LocalDate.now()) == true) {
                Text(
                    "This date is in the past.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("All day", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                OrganicToggle(checked = uiState.isAllDay, onCheckedChange = viewModel::onAllDayChange)
            }
            if (!uiState.isAllDay) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Starts ${uiState.startTime.format(timeFormat)}", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Ends ${uiState.endTime.format(timeFormat)}", maxLines = 1)
                    }
                }
            }
        }

        item { LabeledField(label = "Venue", value = uiState.venue, onValueChange = viewModel::onVenueChange, placeholder = "Optional — where is it?") }
        item {
            LabeledField(
                label = "Notes",
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                placeholder = "Optional — agenda, what to bring, links",
                singleLine = false,
                minLines = 3
            )
        }

        item {
            Text("Reminder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EventReminder.entries.forEach { option ->
                    PillChip(
                        label = EventReminderPolicy.label(option),
                        selected = uiState.reminder == option,
                        onClick = { viewModel.onReminderChange(option) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                reminderSummary(uiState.reminder, uiState.reminderTime, uiState.date != null, reminderFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (uiState.reminder != EventReminder.OFF) {
            if (!systemNotificationsOn || !appNotificationsEnabled) {
                item {
                    PermissionWarning(
                        title = "Notifications are off",
                        body = if (!appNotificationsEnabled) "Turn on notifications in Neuromind's Settings screen, or this reminder won't show."
                        else "Android is blocking Neuromind's notifications, so this reminder won't show.",
                        actionLabel = if (!systemNotificationsOn) "Allow notifications" else null,
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }
            }
            if (!canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item {
                    PermissionWarning(
                        title = "Reminders may be late",
                        body = "Allow \"Alarms & reminders\" so event reminders arrive exactly on time. Without it Android may delay them by several minutes.",
                        actionLabel = "Allow exact reminders",
                        onAction = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                    )
                }
            }
        }

        uiState.error?.let { message ->
            item { Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        }

        item {
            Spacer(Modifier.height(4.dp))
            PillButton(
                text = "Save event",
                onClick = viewModel::save,
                enabled = uiState.isReady && !uiState.isFinished
            )
            if (uiState.hasUnsavedChanges && !uiState.isFinished) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Draft saved automatically — you can leave and come back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PillButton(text = "Discard draft", onClick = { confirmDiscard = true }, ghost = true)
            }
        }
    }
}

private fun reminderSummary(
    mode: EventReminder,
    time: LocalDateTime?,
    hasDate: Boolean,
    format: DateTimeFormatter
): String {
    if (mode == EventReminder.OFF) return "No reminder for this event."
    if (!hasDate || time == null) return "Pick a date to see when you'll be reminded."
    val base = "You'll be reminded ${time.format(format)}."
    val rule = if (mode == EventReminder.AUTO) {
        " Default: 10 min before; the evening before for events starting before 9 AM; 9 AM for all-day events."
    } else ""
    val past = if (!time.isAfter(LocalDateTime.now())) " That time has already passed." else ""
    return base + past + rule
}

@Composable
private fun PermissionWarning(title: String, body: String, actionLabel: String?, onAction: () -> Unit) {
    OrganicCard(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.errorContainer) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
        if (actionLabel != null) {
            Spacer(Modifier.height(8.dp))
            PillButton(text = actionLabel, onClick = onAction)
        }
    }
}
