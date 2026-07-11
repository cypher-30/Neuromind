package com.alvin.neuromind.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.domain.AvailableSlot
import com.alvin.neuromind.ui.components.NeuromindTopBar
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: AddEditTaskViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isTaskSaved) {
        if (uiState.isTaskSaved) onNavigateUp()
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // DATE PICKER
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val newDateTime = uiState.dueDate?.let { existing ->
                        val oldZoned = Instant.ofEpochMilli(existing).atZone(ZoneId.systemDefault())
                        Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault())
                            .withHour(oldZoned.hour).withMinute(oldZoned.minute)
                            .toInstant().toEpochMilli()
                    } ?: selectedDate
                    viewModel.onDueDateChange(newDateTime)
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

    Scaffold(
        topBar = {
            NeuromindTopBar(
                title = "New Task",
                onNavigateBack = onNavigateUp,
                actions = {
                    IconButton(
                        onClick = { viewModel.saveTask() },
                        enabled = uiState.title.isNotBlank() && !uiState.isTaskSaved
                    ) {
                        if (uiState.isTaskSaved) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save Task")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = { viewModel.onTitleChange(it) },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = { viewModel.onDescriptionChange(it) },
                    label = { Text("Description & Details") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("Enter the details here...") }
                )
            }

            // Due Date & Time + Find a Time
            item {
                Text("Due Date & Time", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uiState.dueDate?.let { dateFormat.format(Date(it)) } ?: "Set Date")
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
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
                        TextButton(
                            onClick = { viewModel.onDueDateChange(null) },
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear date", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = { viewModel.findAvailableTime() },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Find a Time", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Duration
            item {
                Text("Estimated Duration", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 45, 60, 90, 120).forEach { mins ->
                        FilterChip(
                            selected = uiState.durationMinutes == mins,
                            onClick = { viewModel.onDurationChange(mins) },
                            label = { Text(if (mins < 60) "${mins}m" else "${mins / 60}h${if (mins % 60 != 0) "${mins % 60}m" else ""}") }
                        )
                    }
                }
            }

            item {
                SimpleToggleGroup(
                    label = "Priority",
                    options = Priority.entries,
                    selectedOption = uiState.priority,
                    onOptionSelected = { viewModel.onPriorityChange(it) }
                )
            }
            item {
                SimpleToggleGroup(
                    label = "Difficulty",
                    options = Difficulty.entries,
                    selectedOption = uiState.difficulty,
                    onOptionSelected = { viewModel.onDifficultyChange(it) }
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.CallSplit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Break this down into sub-tasks")
                    }
                }
            }
        }
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
        Text("Depends on (Optional)", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SimpleToggleGroup(
    label: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selectedOption,
                    onClick = { onOptionSelected(option) },
                    label = { Text(option.toString().lowercase().replaceFirstChar { it.titlecase() }) }
                )
            }
        }
    }
}
