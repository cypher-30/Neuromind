package com.alvin.neuromind.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Difficulty
import com.alvin.neuromind.data.Priority
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskScreen(
    viewModel: AddEditTaskViewModel,
    onNavigateUp: () -> Unit
) {
    // Collect the StateFlow from the ViewModel
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.dueDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    viewModel.onDueDateChange(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Task") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveTask()
                            onNavigateUp()
                        },
                        // The button is enabled only when the title is not empty
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
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth().height(120.dp)
                )
            }
            item {
                val formattedDate = uiState.dueDate?.let {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Not Set"
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Due Date: $formattedDate")
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