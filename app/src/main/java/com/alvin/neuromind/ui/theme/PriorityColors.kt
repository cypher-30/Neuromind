package com.alvin.neuromind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alvin.neuromind.data.Priority

// Single source of truth for priority → color. HIGH always reads as urgent
// (error/ember) regardless of due date; overdue-ness is signalled separately
// via icon/label, not by borrowing this same color for a different meaning.
@Composable
fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> MaterialTheme.colorScheme.error
    Priority.MEDIUM -> MaterialTheme.colorScheme.tertiary
    Priority.LOW -> MaterialTheme.colorScheme.secondary
}
