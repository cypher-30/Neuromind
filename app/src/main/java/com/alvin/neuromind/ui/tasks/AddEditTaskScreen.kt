package com.alvin.neuromind.ui.tasks

import com.alvin.neuromind.ui.components.OrganicConfirmDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TaskCategory
import com.alvin.neuromind.domain.AvailableSlot
import com.alvin.neuromind.ui.components.LabeledField
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.components.PillChip
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: AddEditTaskViewModel,
    onNavigateUp: () -> Unit,
    topContent: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isTaskSaved) {
        if (uiState.isTaskSaved) onNavigateUp()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    if (confirmDiscard) {
        ConfirmDiscardDraftDialog(
            onConfirm = { viewModel.discardDraft(); confirmDiscard = false },
            onDismiss = { confirmDiscard = false }
        )
    }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // DATE PICKER — DatePicker works in UTC-midnight millis; convert to/from local dates explicitly.
    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val initialLocalDate = uiState.dueDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialLocalDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val pickedDate = datePickerState.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        ?: LocalDate.now()
                    val keepTime = uiState.dueDate?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() }
                        ?: LocalTime.of(9, 0)
                    viewModel.onDueDateChange(pickedDate.atTime(keepTime).atZone(zone).toInstant().toEpochMilli())
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next: Set Time") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    // TIME PICKER
    if (showTimePicker) {
        val initialTime = uiState.dueDate?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        } ?: LocalTime.of(9, 0)
        val timeState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Due Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val currentDue = uiState.dueDate ?: System.currentTimeMillis()
                    val newTime = Instant.ofEpochMilli(currentDue).atZone(ZoneId.systemDefault())
                        .withHour(timeState.hour).withMinute(timeState.minute)
                        .toInstant().toEpochMilli()
                    viewModel.onDueDateChange(newTime)
                    showTimePicker = false
                }) { Text("Set Time") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    // FIND A TIME DIALOG
    if (uiState.availableTimeSlots.isNotEmpty()) {
        FindTimeDialog(
            slots = uiState.availableTimeSlots,
            onSlotSelected = { viewModel.applyTimeSlot(it) },
            onDismiss = { viewModel.dismissTimeSlots() }
        )
    }

    // BREAK THIS DOWN DIALOG
    if (uiState.subTaskProposal.isNotEmpty()) {
        SubTaskProposalDialog(
            proposals = uiState.subTaskProposal,
            onConfirm = { viewModel.confirmSubTaskBreakdown() },
            onDismiss = { viewModel.dismissSubTaskProposal() }
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
                title = if (uiState.isEditing) "Edit task" else "New task",
                onBack = onNavigateUp,
                actions = {
                    if (uiState.isTaskSaved) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            )
        }

        item { topContent() }

        if (uiState.restoredDraft) {
            item {
                DraftRestoredBanner(
                    onKeep = viewModel::dismissRestoredBanner,
                    onDiscard = { confirmDiscard = true }
                )
            }
        }

        item { LabeledField(label = "Title", value = uiState.title, onValueChange = viewModel::onTitleChange, placeholder = "What needs doing?") }
        item {
            LabeledField(
                label = "Notes",
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                placeholder = "Optional details",
                singleLine = false,
                minLines = 3
            )
        }
        item { LabeledField(label = "Subject", value = uiState.subject, onValueChange = viewModel::onSubjectChange, placeholder = "Optional — e.g. Chemistry") }

        // Due Date & Time + Find a Time
        item {
            Text("Due Date & Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.DateRange, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.dueDate?.let { dateFormat.format(Date(it)) } ?: "Set Date")
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.Schedule, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(uiState.dueDate?.let { timeFormat.format(Date(it)) } ?: "Set Time")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (uiState.dueDate != null) {
                    PillButton(text = "Clear date", onClick = { viewModel.onDueDateChange(null) }, ghost = true)
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                PillButton(text = "Find a Time", onClick = { viewModel.findAvailableTime() }, ghost = true)
            }
        }

        // Duration stepper
        item {
            Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                DurationStepButton(icon = Icons.Default.Remove, onClick = { viewModel.onDurationChange((uiState.durationMinutes - 5).coerceAtLeast(5)) })
                Text(
                    "${uiState.durationMinutes} min",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                DurationStepButton(icon = Icons.Default.Add, onClick = { viewModel.onDurationChange((uiState.durationMinutes + 5).coerceAtMost(240)) })
            }
        }

        item {
            PillOptionGroup(
                label = "Priority",
                options = Priority.entries,
                selectedOption = uiState.priority,
                onOptionSelected = viewModel::onPriorityChange
            )
        }
        item {
            PillOptionGroup(
                label = "Difficulty",
                options = Difficulty.entries,
                selectedOption = uiState.difficulty,
                onOptionSelected = viewModel::onDifficultyChange
            )
        }
        item {
            PillOptionGroup(
                label = "Category",
                options = TaskCategory.entries,
                selectedOption = uiState.category,
                onOptionSelected = viewModel::onCategoryChange
            )
        }

        item {
            DependencySelector(
                availableTasks = uiState.availableTasks,
                selectedId = uiState.prerequisiteTaskId,
                onIdSelected = { viewModel.onPrerequisiteChange(it) }
            )
        }

        // Break this down button
        if (uiState.title.isNotBlank()) {
            item {
                OutlinedButton(
                    onClick = { viewModel.generateSubTaskProposal() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Break this down into sub-tasks")
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            PillButton(
                text = "Save task",
                onClick = { viewModel.saveTask() },
                enabled = uiState.isReady && uiState.title.isNotBlank() && !uiState.isTaskSaved
            )
            if (uiState.hasUnsavedChanges && !uiState.isTaskSaved) {
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

@Composable
internal fun DraftRestoredBanner(onKeep: () -> Unit, onDiscard: () -> Unit) {
    OrganicCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text("Restored your unsaved draft", style = MaterialTheme.typography.titleSmall)
        Text(
            "You left this form before saving, so we kept what you typed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton(text = "Keep editing", onClick = onKeep, modifier = Modifier.weight(1f))
            PillButton(text = "Discard draft", onClick = onDiscard, ghost = true)
        }
    }
}

@Composable
internal fun ConfirmDiscardDraftDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    OrganicConfirmDialog(
        title = "Discard draft?",
        message = "Your unsaved changes will be lost. This can't be undone.",
        confirmLabel = "Discard",
        dismissLabel = "Keep",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DurationStepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FindTimeDialog(
    slots: List<AvailableSlot>,
    onSlotSelected: (AvailableSlot) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Available Time Slots") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Pick a slot to set your due date:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (slots.isEmpty()) {
                    Text("No free slots found in the next 7 days.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    slots.forEach { slot ->
                        val isToday = slot.date == LocalDate.now()
                        val dayLabel = if (isToday) "Today" else slot.date.format(dateFormatter)
                        ListItem(
                            headlineContent = { Text("$dayLabel · ${slot.startTime.format(timeFormatter)} – ${slot.endTime.format(timeFormatter)}") },
                            modifier = Modifier.clickable { onSlotSelected(slot) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SubTaskProposalDialog(
    proposals: List<Task>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Break into Sub-Tasks") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The following sub-tasks will be created:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                proposals.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "${task.durationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Create Sub-Tasks") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DependencySelector(
    availableTasks: List<Task>,
    selectedId: Int?,
    onIdSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTask = availableTasks.find { it.id == selectedId }

    Column {
        Text("Depends on (Optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small
            ) {
                Text(selectedTask?.title ?: "Select a prerequisite task")
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.Schedule, null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = { onIdSelected(null); expanded = false }
                )
                availableTasks.forEach { task ->
                    DropdownMenuItem(
                        text = { Text(task.title) },
                        onClick = { onIdSelected(task.id); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun <T : Any> PillOptionGroup(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            options.forEach { option ->
                PillChip(
                    label = option.toDisplayLabel(),
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    modifier = Modifier
                )
            }
        }
    }
}

private fun Any.toDisplayLabel(): String =
    toString()
        .lowercase(Locale.getDefault())
        .replace('_', ' ')
        .split(' ')
        .joinToString(" ") { token -> token.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } }

