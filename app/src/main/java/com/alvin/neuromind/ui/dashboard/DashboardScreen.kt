package com.alvin.neuromind.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LabelImportant
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.TimeSlot
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToTimetable: () -> Unit,
    onNavigateToFeedback: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = uiState.greeting, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = uiState.currentDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToFeedback) {
                Icon(Icons.Default.Reviews, contentDescription = "Daily Review")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Stats Row
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardStatCard("Pending", uiState.pendingTaskCount.toString(), Icons.AutoMirrored.Filled.ListAlt, Modifier.weight(1f))
                    DashboardStatCard("Done", uiState.completedTaskCount.toString(), Icons.Default.CheckCircle, Modifier.weight(1f))
                }
            }

            // 2. Today's Priorities
            item {
                SectionHeader("Today's Priorities")
                if (uiState.priorityTasks.isEmpty()) {
                    EmptyStateCard("No urgent tasks. You're catching up!")
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            uiState.priorityTasks.forEach { task ->
                                PriorityTaskRow(task)
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // 3. Upcoming Timetable
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader("Upcoming Events")
                    TextButton(onClick = onNavigateToTimetable) { Text("See All") }
                }
                if (uiState.upcomingEvents.isEmpty()) {
                    EmptyStateCard("No classes or events left today.")
                } else {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            uiState.upcomingEvents.forEach { entry ->
                                TimetableEventRow(entry)
                                HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // 4. AI Plan
            if (uiState.todaysPlan.isNotEmpty()) {
                item {
                    SectionHeader("AI Suggested Plan")
                    Text(
                        "Based on your free time slots today:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                items(uiState.todaysPlan.entries.toList().sortedBy { it.key.start }) { (slot, task) ->
                    AiPlanItem(slot, task)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun DashboardStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(message, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PriorityTaskRow(task: Task) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = if (task.isOverdue) Icons.Default.Warning else Icons.AutoMirrored.Filled.LabelImportant
        val color = if (task.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        Icon(icon, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (task.isOverdue) {
                Text("Overdue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TimetableEventRow(entry: TimetableEntry) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            entry.startTime.format(formatter),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(60.dp)
        )
        Column {
            Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            entry.venue?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun AiPlanItem(slot: TimeSlot, task: Task) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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