package com.alvin.neuromind.ui.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    isRescheduleMode: Boolean,
    onAddTaskClicked: () -> Unit,
    onEditTaskClicked: (Task) -> Unit // New parameter for navigation
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            if (!isRescheduleMode) {
                FloatingActionButton(onClick = onAddTaskClicked) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.overdueTasks.isEmpty() && uiState.todayTasks.isEmpty() && uiState.upcomingTasks.isEmpty() && uiState.completedTasks.isEmpty()) {
            EmptyState(innerPadding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Overdue Section
                if (uiState.overdueTasks.isNotEmpty()) {
                    item { TaskSectionHeader("Overdue", MaterialTheme.colorScheme.error) }
                    items(items = uiState.overdueTasks, key = { it.id }) { task ->
                        TaskCard(task, viewModel, onClick = { onEditTaskClicked(task) })
                    }
                }

                // 2. Today Section
                if (uiState.todayTasks.isNotEmpty()) {
                    item { TaskSectionHeader("Today", MaterialTheme.colorScheme.primary) }
                    items(items = uiState.todayTasks, key = { it.id }) { task ->
                        TaskCard(task, viewModel, onClick = { onEditTaskClicked(task) })
                    }
                }

                // 3. Upcoming Section
                if (uiState.upcomingTasks.isNotEmpty()) {
                    item { TaskSectionHeader("Upcoming", MaterialTheme.colorScheme.secondary) }
                    items(items = uiState.upcomingTasks, key = { it.id }) { task ->
                        TaskCard(task, viewModel, onClick = { onEditTaskClicked(task) })
                    }
                }

                // 4. Completed Section
                if (uiState.completedTasks.isNotEmpty()) {
                    item { TaskSectionHeader("Completed", MaterialTheme.colorScheme.outline) }
                    items(items = uiState.completedTasks, key = { it.id }) { task ->
                        TaskCard(task, viewModel, onClick = { onEditTaskClicked(task) })
                    }
                }
            }
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    viewModel: TaskViewModel,
    onClick: () -> Unit
) {
    val priorityColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        Priority.LOW -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Makes the whole card tap-to-edit
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { viewModel.onTaskCheckedChange(task, it) },
                modifier = Modifier.size(24.dp).padding(end = 8.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )

                if (!task.description.isNullOrBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meta Row (Priority Badge & Date)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = {},
                        label = { Text(task.priority.name, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Flag, null, tint = priorityColor, modifier = Modifier.size(12.dp)) },
                        modifier = Modifier.height(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (task.dueDate != null) {
                        val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        Text(
                            text = format.format(Date(task.dueDate)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!task.isCompleted && task.dueDate < System.currentTimeMillis()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Delete Button
            IconButton(onClick = { viewModel.deleteTask(task) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun TaskSectionHeader(title: String, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No tasks found", style = MaterialTheme.typography.headlineSmall)
            Text("Tap + to add one", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}