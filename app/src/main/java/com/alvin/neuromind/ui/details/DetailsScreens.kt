package com.alvin.neuromind.ui.details

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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.data.EventReminder
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.EventReminderPolicy
import com.alvin.neuromind.domain.TeamsMeetingHelper
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.OrganicConfirmDialog
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.components.SectionKicker
import com.alvin.neuromind.ui.theme.Dimens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// Read-only "details" screens: tapping a task, class or event (in the app, a
// widget or a reminder) lands here first; editing is one button away.

private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val shortDateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
private val dateTimeFmt = DateTimeFormatter.ofPattern("EEE, MMM d 'at' h:mm a")

// ---------------------------------------------------------------------------
// Class / event
// ---------------------------------------------------------------------------

@Composable
fun EntryDetailsScreen(
    viewModel: EntryDetailsViewModel,
    onNavigateUp: () -> Unit,
    onEditEvent: (TimetableEntry) -> Unit,
    onEditClass: (TimetableEntry) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // Deleted here or in the editor: nothing left to show.
    LaunchedEffect(state) { if (state is Details.Missing) onNavigateUp() }
    val entry = (state as? Details.Loaded)?.value
    if (entry == null) {
        LoadingBox()
        return
    }
    val kind = if (entry.isRecurring) "class" else "event"

    if (confirmDelete) {
        OrganicConfirmDialog(
            title = "Delete this $kind?",
            message = if (entry.isRecurring) "\"${entry.title}\" will be removed from every week. This can't be undone."
            else "\"${entry.title}\" and its reminder will be permanently removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false }
        )
    }

    val teams = remember(entry.details, entry.venue) { TeamsMeetingHelper.parseMeeting(entry.details, entry.venue) }
    val timeText = if (entry.isAllDay) "All day" else "${entry.startTime.format(timeFmt)} – ${entry.endTime.format(timeFmt)}"

    DetailsColumn(
        header = {
            ScreenHeader(
                title = if (entry.isRecurring) "Class" else "Event",
                onBack = onNavigateUp,
                actions = { DeleteAction("Delete $kind") { confirmDelete = true } }
            )
        }
    ) {
        item {
            HeroCard(
                kicker = when {
                    entry.isRecurring -> "Weekly class"
                    entry.title.contains("exam", ignoreCase = true) -> "Exam"
                    else -> "Event"
                },
                title = entry.title
            ) {
                if (entry.isRecurring) {
                    InfoRow(Icons.Default.Repeat, "When", "Every ${entry.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} · $timeText")
                    entry.lastDate?.let { InfoRow(Icons.Default.Event, "Runs until", it.format(shortDateFmt)) }
                } else {
                    entry.date?.let { date ->
                        InfoRow(Icons.Default.Event, "Date", "${date.format(dateFmt)}${relativeDay(date)}")
                    }
                    InfoRow(Icons.Default.Schedule, "Time", timeText)
                }
                entry.venue?.takeIf { it.isNotBlank() }?.let { InfoRow(Icons.Default.LocationOn, "Where", it) }
                if (!entry.isRecurring) InfoRow(Icons.Default.Notifications, "Reminder", reminderText(entry))
            }
        }

        entry.details?.takeIf { it.isNotBlank() }?.let { notes -> item { NotesCard(notes) } }

        if (teams.hasLaunchAction) {
            item {
                PillButton(text = "Join Teams meeting", ghost = true, onClick = { TeamsMeetingHelper.launchMeeting(context, teams) })
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            PillButton(
                text = if (entry.isRecurring) "Edit class" else "Edit event",
                onClick = { if (entry.isRecurring) onEditClass(entry) else onEditEvent(entry) }
            )
        }
    }
}

private fun relativeDay(date: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days == 0L -> " · today"
        days == 1L -> " · tomorrow"
        days > 1 -> " · in $days days"
        days == -1L -> " · yesterday"
        else -> " · ${-days} days ago"
    }
}

private fun reminderText(entry: TimetableEntry): String {
    val mode = entry.reminderMode ?: return "Off"
    if (mode == EventReminder.OFF) return "Off"
    val at = EventReminderPolicy.triggerAt(entry) ?: return EventReminderPolicy.label(mode)
    val label = if (mode == EventReminder.AUTO) "" else "${EventReminderPolicy.label(mode)} · "
    val sent = if (at.isBefore(LocalDateTime.now())) " (already sent)" else ""
    return "$label${at.format(dateTimeFmt)}$sent"
}

// ---------------------------------------------------------------------------
// Task
// ---------------------------------------------------------------------------

@Composable
fun TaskDetailsScreen(
    viewModel: TaskDetailsViewModel,
    onNavigateUp: () -> Unit,
    onEdit: (Task) -> Unit,
    onFocus: (Task) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state) { if (state is Details.Missing) onNavigateUp() }
    val details = (state as? Details.Loaded)?.value
    if (details == null) {
        LoadingBox()
        return
    }
    val task = details.task

    if (confirmDelete) {
        OrganicConfirmDialog(
            title = "Delete this task?",
            message = "\"${task.title}\" will be permanently removed. This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false }
        )
    }

    val status = when {
        task.isCompleted -> "Done"
        details.isBlocked -> "Waiting"
        task.isOverdue -> "Overdue"
        else -> "To do"
    }
    val statusColor = when (status) {
        "Overdue" -> MaterialTheme.colorScheme.error
        "Done" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    DetailsColumn(
        header = {
            ScreenHeader(
                title = "Task",
                onBack = onNavigateUp,
                actions = { DeleteAction("Delete task") { confirmDelete = true } }
            )
        }
    ) {
        item {
            HeroCard(kicker = status, kickerColor = statusColor, title = task.title) {
                task.dueDate?.let { InfoRow(Icons.Default.Event, if (task.onDueDay) "On" else "Due", formatMillis(it)) }
                    ?: InfoRow(Icons.Default.Event, "Due", "No due date")
                if (task.onDueDay) {
                    InfoRow(Icons.Default.Repeat, "Kind", "Do it on the day: kept out of your plan and suggestions until then")
                }
                InfoRow(Icons.Default.Flag, "Priority", task.priority.name.titleCase())
                InfoRow(Icons.Default.Speed, "Difficulty", task.difficulty.name.titleCase())
                InfoRow(Icons.Default.Timer, "Takes about", durationText(task.durationMinutes))
                val subject = task.subject?.takeIf { it.isNotBlank() }
                InfoRow(Icons.Default.Label, "Subject", listOfNotNull(subject, task.category.name.titleCase()).joinToString(" · "))
                details.prerequisite?.let { pre ->
                    InfoRow(
                        Icons.Default.Lock,
                        "Waits on",
                        if (pre.isCompleted) "${pre.title} (done)" else "${pre.title}. Finish it first"
                    )
                }
                task.completedAt?.let { InfoRow(Icons.Default.CheckCircle, "Finished", formatMillis(it)) }
            }
        }

        task.description?.takeIf { it.isNotBlank() }?.let { notes -> item { NotesCard(notes) } }

        item {
            Spacer(Modifier.height(8.dp))
            PillButton(text = "Edit task", onClick = { onEdit(task) })
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PillButton(
                    text = if (task.isCompleted) "Mark not done" else "Mark done",
                    ghost = true,
                    enabled = task.isCompleted || !details.isBlocked,
                    onClick = { viewModel.setCompleted(!task.isCompleted) },
                    modifier = Modifier.weight(1f)
                )
                // Errands aren't Focus work.
                if (!task.isCompleted && !task.onDueDay) {
                    PillButton(text = "Focus on it", ghost = true, onClick = { onFocus(task) }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val at = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "${at.format(dateTimeFmt)}${relativeDay(at.toLocalDate())}"
}

private fun durationText(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

private fun String.titleCase() = lowercase().replaceFirstChar { it.uppercase() }

// ---------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun DeleteAction(description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.Delete, contentDescription = description, tint = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun DetailsColumn(
    header: @Composable () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomNoNav),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        item {
            Spacer(Modifier.height(Dimens.ScreenPaddingTop - Dimens.CardGap))
            header()
        }
        content()
    }
}

@Composable
private fun HeroCard(
    kicker: String,
    title: String,
    kickerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    rows: @Composable () -> Unit
) {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionKicker(kicker, color = kickerColor)
            Spacer(Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(4.dp))
            rows()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun NotesCard(notes: String) {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            SectionKicker("Notes")
            Spacer(Modifier.height(6.dp))
            // Selectable so links, meeting IDs and passcodes can be copied.
            SelectionContainer { Text(notes, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
