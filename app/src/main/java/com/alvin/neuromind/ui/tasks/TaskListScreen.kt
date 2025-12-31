package com.alvin.neuromind.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.domain.ProposedSlot
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    isRescheduleMode: Boolean,
    onAddTaskClicked: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFindingProposals by viewModel.isFindingProposals.collectAsState()
    val proposals by viewModel.proposals.collectAsState()

    LaunchedEffect(key1 = isRescheduleMode) {
        viewModel.setMode(isRescheduleMode)
    }

    if (proposals.isNotEmpty()) {
        RescheduleProposalDialog(
            proposals = proposals,
            tasks = uiState.hierarchicalTasks.map { it.parent },
            onAccept = { viewModel.acceptProposals() },
            onDismiss = { viewModel.clearProposals() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isRescheduleMode) "Reschedule Overdue" else "My Tasks") },
                actions = { /* ... */ },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTaskClicked,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }

    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            FilterChipGroup(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                enabled = !uiState.isRescheduleMode
            )
            HorizontalDivider()

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.hierarchicalTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(if (uiState.isRescheduleMode) "No overdue tasks. Great job!" else "No tasks here!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    uiState.hierarchicalTasks.forEach { hierarchicalTask ->
                        val parentTask = hierarchicalTask.parent
                        val isExpanded = uiState.expandedTaskIds.contains(parentTask.id)
                        item(key = parentTask.id) {
                            TaskCard(
                                task = parentTask,
                                subtaskCount = hierarchicalTask.subTasks.size,
                                isExpanded = isExpanded,
                                onExpandToggle = { viewModel.toggleTaskExpansion(parentTask.id) },
                                onCompletedChange = { isChecked ->
                                    viewModel.onTaskCompleted(parentTask, isChecked)
                                }
                            )
                        }
                        if (isExpanded) {
                            items(hierarchicalTask.subTasks, key = { "sub_${it.id}" }) { subtask ->
                                TaskCard(
                                    task = subtask,
                                    isSubtask = true,
                                    onCompletedChange = { isChecked ->
                                        viewModel.onTaskCompleted(subtask, isChecked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleProposalDialog(
    proposals: Map<UUID, ProposedSlot>,
    tasks: List<Task>,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Times Suggested") },
        text = {
            LazyColumn {
                items(proposals.entries.toList()) { (taskId, proposal) ->
                    val task = tasks.find { it.id == taskId }
                    if (task != null) {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(text = task.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                text = "→ New time: ${proposal.date.format(dateFormatter)} at ${proposal.timeSlot.start.format(timeFormatter)}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text("Accept All") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipGroup(selectedFilter: TaskFilter, onFilterSelected: (TaskFilter) -> Unit, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskFilter.entries.forEach { filter ->
            FilterChip(
                enabled = enabled,
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = filter.name.lowercase().replaceFirstChar { it.titlecase() }) },
                leadingIcon = if (selectedFilter == filter) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}