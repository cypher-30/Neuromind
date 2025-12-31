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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                        Text(text = uiState.greeting)
                        Text(
                            text = uiState.currentDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardStatCard("Pending", uiState.pendingTaskCount.toString(), Icons.AutoMirrored.Filled.ListAlt, Modifier.weight(1f))
                    DashboardStatCard("Completed", uiState.completedTaskCount.toString(), Icons.Default.CheckCircleOutline, Modifier.weight(1f))
                }
            }
            item {
                TodaysPrioritiesCard(tasks = uiState.priorityTasks)
            }
            item {
                UpcomingTimetableCard(entries = uiState.upcomingEvents)
            }
            if (uiState.todaysPlan.isNotEmpty()) {
                item {
                    Text("AI-Generated Plan", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                }
                items(uiState.todaysPlan.entries.toList(), key = { it.value.id }) { (slot, task) ->
                    ScheduledTaskItem(slot = slot, task = task)
                }
            }
        }
    }
}

@Composable
private fun DashboardStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Icon(imageVector = icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            }
            Text(text = value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TodaysPrioritiesCard(tasks: List<Task>) {
    Column {
        Text("Today's Priorities", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            if (tasks.isEmpty()) {
                Text(
                    text = "No urgent tasks. A great day to get ahead!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    tasks.forEach { task ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            val priorityIcon = when {
                                task.isOverdue -> Icons.Default.Error
                                task.priority == Priority.HIGH -> Icons.Default.PriorityHigh
                                else -> Icons.AutoMirrored.Filled.LabelImportant
                            }
                            val iconColor = if (task.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                            Icon(imageVector = priorityIcon, contentDescription = "Priority", tint = iconColor)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingTimetableCard(entries: List<TimetableEntry>) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Column {
        Text("Today's Timetable", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            if (entries.isEmpty()) {
                Text(
                    text = "No more scheduled events for today.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    entries.forEach { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.startTime.format(timeFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(80.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = entry.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledTaskItem(slot: TimeSlot, task: Task) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = slot.start.format(timeFormatter),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(80.dp)
        )
        HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp).padding(horizontal = 8.dp))
        Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
    }
}