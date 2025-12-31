package com.alvin.neuromind.ui.tasks

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Priority
import com.alvin.neuromind.data.Task
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TaskCard(
    task: Task,
    subtaskCount: Int = 0,
    isExpanded: Boolean = false,
    isSubtask: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    onCompletedChange: (Boolean) -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(start = if (isSubtask) 32.dp else 0.dp)
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSubtask) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = if (isSubtask) CardDefaults.cardElevation(defaultElevation = 0.dp) else CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onCompletedChange
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (task.dueDate != null) {
                            val date = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault())
                            val isOverdue = task.isOverdue
                            Text(
                                text = date.format(dateFormatter),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (task.priority == Priority.HIGH) {
                            Text(
                                text = "HIGH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (subtaskCount > 0 && onExpandToggle != null) {
                    IconButton(onClick = onExpandToggle) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                }
            }
            // Show description only if expanded (optional, but good for UI)
            if (isExpanded && !task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 52.dp, top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}