package com.alvin.neuromind.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task: Task,
    subtaskCount: Int = 0,
    isExpanded: Boolean = false,
    isSubtask: Boolean = false,
    onExpandToggle: () -> Unit = {},
    onCompletedChange: (Boolean) -> Unit // <-- NEW PARAMETER
) {
    val titleStyle = if (task.isCompleted) {
        MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough)
    } else {
        MaterialTheme.typography.titleMedium
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isSubtask) 24.dp else 0.dp,
                top = 4.dp,
                bottom = 4.dp
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // FIX: The checkbox now correctly calls the onCompletedChange lambda
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCompletedChange(it) }
            )

            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(text = task.title, style = titleStyle, fontWeight = FontWeight.Bold)

                task.dueDate?.let {
                    Text(
                        text = "Due: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            if (subtaskCount > 0) {
                IconButton(onClick = onExpandToggle) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand Subtasks"
                    )
                }
            }
        }
    }
}