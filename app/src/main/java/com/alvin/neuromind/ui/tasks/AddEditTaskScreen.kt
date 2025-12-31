package com.alvin.neuromind.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: AddEditTaskViewModel,
    onNavigateUp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Dialog States
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Helper to format date/time for display
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Logic to handle saving Date + Time
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    // Update ONLY the date part, keep the time part if it existed, else default to 9 AM
                    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val currentDue = uiState.dueDate

                    val newDateTime = if (currentDue == null) {
                        selectedDate // Just date (midnight UTC usually)
                    } else {
                        // Combine new date with old time
                        val oldZoned = Instant.ofEpochMilli(currentDue).atZone(ZoneId.systemDefault())
                        val newZoned = Instant.ofEpochMilli(selectedDate).atZone(ZoneId.systemDefault())

                        newZoned.withHour(oldZoned.hour).withMinute(oldZoned.minute)
                            .toInstant().toEpochMilli()
                    }

                    viewModel.onDueDateChange(newDateTime)
                    showDatePicker = false
                    // Optionally open time picker right after
                    showTimePicker = true
                }) { Text("Next: Set Time") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

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
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                Button(onClick = {
                    // Merge selected time into the existing due date
                    val currentDue = uiState.dueDate ?: System.currentTimeMillis()
                    val zoned = Instant.ofEpochMilli(currentDue).atZone(ZoneId.systemDefault())

                    val newTime = zoned.withHour(timeState.hour).withMinute(timeState.minute)
                        .toInstant().toEpochMilli()

                    viewModel.onDueDateChange(newTime)
                    showTimePicker = false
                }) { Text("Set Time") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveTask()
                            onNavigateUp()
                        },
                        enabled = uiState.title.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save Task")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
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

            // DATE & TIME BUTTONS
            item {
                Text("Due Date & Time", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Date Button
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DateRange, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.dueDate?.let { dateFormat.format(Date(it)) } ?: "Set Date"
                        )
                    }

                    // Time Button
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.dueDate?.let { timeFormat.format(Date(it)) } ?: "Set Time"
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