package com.alvin.neuromind.ui.dashboard

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.ui.components.CircleIconButton
import com.alvin.neuromind.ui.components.EmptyState
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.SectionKicker
import com.alvin.neuromind.ui.components.StatTile
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Pill
import com.alvin.neuromind.ui.theme.priorityColor
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.BurnoutState
import com.alvin.neuromind.domain.RebalanceProposal
import com.alvin.neuromind.domain.Suggestion
import com.alvin.neuromind.domain.TimeSlot
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToAssistant: () -> Unit,
    onNavigateToTask: (Int) -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showRebalanceDialog by remember { mutableStateOf(false) }

    if (selectedTask != null) {
        TaskDetailsDialog(task = selectedTask!!, onDismiss = { selectedTask = null })
    }

    if (showRebalanceDialog) {
        RebalanceDialog(
            proposals = uiState.rebalanceProposals,
            onConfirm = {
                viewModel.confirmRebalance()
                showRebalanceDialog = false
            },
            onDismiss = {
                viewModel.dismissRebalance()
                showRebalanceDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPaddingHorizontal,
            end = Dimens.ScreenPaddingHorizontal,
            top = Dimens.ScreenPaddingTop,
            bottom = Dimens.ScreenPaddingBottomWithNav
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        // Greeting header + theme toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = uiState.greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = uiState.currentDate, style = MaterialTheme.typography.headlineLarge)
                }
                CircleIconButton(
                    icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme",
                    onClick = onToggleTheme
                )
            }
        }

        // Streak pill
        if (uiState.currentStreak > 0) {
            item { StreakPill(uiState.currentStreak) }
        }

        // Pending / Done — today's due tasks only
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(label = "Pending today", value = uiState.todayProgress.pending.toString(), modifier = Modifier.weight(1f))
                StatTile(label = "Done today", value = uiState.todayProgress.completed.toString(), modifier = Modifier.weight(1f))
            }
        }

        // Today's progress
        item {
            ProgressCard(
                completed = uiState.todayProgress.completed,
                total = uiState.todayProgress.total
            )
        }

        // Happening now, then Up next — both above coaching and priorities
        uiState.happeningNow.forEach { entry ->
            item(key = "now_${entry.id}") {
                HappeningNowCard(entry = entry, onClick = onNavigateToTimetable)
            }
        }
        uiState.upcomingEvents.firstOrNull()?.let { entry ->
            item(key = "next_${entry.id}") {
                UpNextCard(entry = entry, onClick = onNavigateToTimetable)
            }
        }

        // Coaching cards — strictly mutually exclusive, in priority order
        uiState.burnoutState?.let { state ->
            item { BurnoutWarningCard(state = state) }
        }
        if (uiState.burnoutState == null && uiState.rebalanceProposals.isNotEmpty()) {
            item {
                RebalanceCard(
                    overdueCount = uiState.rebalanceProposals.size,
                    onRebalanceClick = { showRebalanceDialog = true }
                )
            }
        }
        if (uiState.burnoutState == null && uiState.rebalanceProposals.isEmpty()) {
            uiState.suggestion?.let { suggestion ->
                item {
                    SuggestionCard(
                        suggestion = suggestion,
                        onActionClick = { taskId -> if (taskId != null) onNavigateToTask(taskId) },
                        onAskClick = onNavigateToAssistant
                    )
                }
            }
        }

        // Today's Priorities
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today's priorities", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onNavigateToTasks) { Text("See all", color = MaterialTheme.colorScheme.primary) }
            }
        }
        if (uiState.priorityTasks.isEmpty()) {
            item { EmptyState("Nothing urgent — good place to be.") }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)) {
                    uiState.priorityTasks.forEach { task ->
                        key(task.id) {
                            PriorityTaskRow(task = task, onClick = { selectedTask = task })
                        }
                    }
                }
            }
        }

        // AI Suggested Plan
        if (uiState.todaysPlan.isNotEmpty()) {
            item { Text("AI suggested plan", style = MaterialTheme.typography.titleLarge) }
            items(uiState.todaysPlan.entries.toList().sortedBy { it.key.start }) { (slot, task) ->
                AiPlanItem(slot, task)
            }
        }
    }
}

@Composable
private fun StreakPill(days: Int) {
    Row(
        modifier = Modifier
            .clip(Pill)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Default.Bolt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(13.dp)
        )
        Text(
            "$days-day streak",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun ProgressCard(completed: Int, total: Int) {
    val progress = if (total > 0) completed.toFloat() / total else 0f
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's progress", style = MaterialTheme.typography.titleLarge)
            Text(
                "$completed of $total done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (total == 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "No tasks due today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(Pill),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun HappeningNowCard(entry: TimetableEntry, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    OrganicCard(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SectionKicker("Happening now", color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!entry.venue.isNullOrBlank()) {
                    Text(
                        entry.venue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Text(
                if (entry.isAllDay) "All day" else "until ${entry.endTime.format(formatter)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!entry.isAllDay) {
            val now = java.time.LocalTime.now()
            val total = java.time.Duration.between(entry.startTime, entry.endTime).toMinutes().coerceAtLeast(1)
            val elapsed = java.time.Duration.between(entry.startTime, now).toMinutes().coerceIn(0, total)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { elapsed.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(Pill),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun UpNextCard(entry: TimetableEntry, onClick: () -> Unit) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    OrganicCard(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SectionKicker("Up next", color = MaterialTheme.colorScheme.primary)
                Text(entry.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                entry.startTime.format(formatter),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BurnoutWarningCard(state: BurnoutState) {
    val message = when (state) {
        is BurnoutState.LowEnergy ->
            "You've had low energy for ${state.consecutiveDays} days in a row. Consider scheduling a rest day."
        is BurnoutState.WeekdayStress ->
            "You often feel stressed on ${state.weekday}s. Consider a lighter schedule on that day."
    }
    OrganicCard(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Wellbeing alert",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                SectionKicker("Wellbeing check", color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun RebalanceCard(overdueCount: Int, onRebalanceClick: () -> Unit) {
    OrganicCard(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionKicker("Behind schedule?", color = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "You have $overdueCount overdue tasks. Neuromind can spread them across the week.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(Pill)
                .background(MaterialTheme.colorScheme.secondary)
                .clickable(onClick = onRebalanceClick)
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text("Rebalance", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondary)
        }
    }
}

@Composable
fun RebalanceDialog(
    proposals: List<RebalanceProposal>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
        title = { Text("Rebalance Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Move these overdue tasks to free slots this week:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                proposals.forEach { proposal ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                proposal.task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${proposal.suggestedDate.format(dateFormatter)} at ${proposal.suggestedTime.format(timeFormatter)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

@Composable
fun SuggestionCard(suggestion: Suggestion, onActionClick: (Int?) -> Unit, onAskClick: () -> Unit = {}) {
    OrganicCard(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        SectionKicker("Neuromind noticed", color = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            suggestion.message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .clip(Pill)
                .background(MaterialTheme.colorScheme.secondary)
                .clickable {
                    if (suggestion.actionLabel != null && suggestion.taskId != null) onActionClick(suggestion.taskId)
                    else onAskClick()
                }
                .padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Text(
                suggestion.actionLabel ?: "Ask Neuromind",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}

@Composable
fun TaskDetailsDialog(task: Task, onDismiss: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!task.description.isNullOrBlank()) {
                    Text(task.description, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("No details provided.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(task.priority.name) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.LabelImportant, null, modifier = Modifier.size(16.dp)) })
                    AssistChip(onClick = {}, label = { Text(task.difficulty.name) })
                }
                if (task.dueDate != null) {
                    Text("Due: ${formatter.format(Date(task.dueDate))}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun PriorityTaskRow(task: Task, onClick: () -> Unit) {
    OrganicCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(priorityColor(task.priority))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(task.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (task.dueDate != null) {
                val format = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                Text(format.format(Date(task.dueDate)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AiPlanItem(slot: TimeSlot, task: Task) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    OrganicCard(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
                Text(slot.start.format(formatter), style = MaterialTheme.typography.labelMedium)
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(slot.end.format(formatter), style = MaterialTheme.typography.labelMedium)
            }
            VerticalDivider(modifier = Modifier.height(30.dp).padding(horizontal = 8.dp))
            Column {
                Text(task.title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Estimated: ${task.durationMinutes} mins", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
