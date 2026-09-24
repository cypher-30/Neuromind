package com.alvin.neuromind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.alvin.neuromind.data.Priority

// Single source of truth for priority → color. HIGH always reads as urgent
// (the tuned danger red) regardless of due date; overdue-ness is signalled
// separately via icon/label, not by borrowing this same color for a different
// meaning. MEDIUM uses the terracotta brand accent, LOW the sage accent-2 —
// tertiary is an alias of secondary in the Organic scheme (see Theme.kt), so
// MEDIUM reads primary rather than doubling up on sage with LOW.
@Composable
fun priorityColor(priority: Priority): Color = when (priority) {
    Priority.HIGH -> MaterialTheme.colorScheme.error
    Priority.MEDIUM -> MaterialTheme.colorScheme.primary
    Priority.LOW -> MaterialTheme.colorScheme.secondary
}
