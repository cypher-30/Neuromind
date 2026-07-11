package com.alvin.neuromind.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.ui.components.NeuromindTopBar
import com.alvin.neuromind.ui.theme.CategorySocialContainerDark
import com.alvin.neuromind.ui.theme.CategorySocialContainerLight
import com.alvin.neuromind.ui.theme.CategorySocialDark
import com.alvin.neuromind.ui.theme.CategorySocialLight
import com.alvin.neuromind.ui.theme.OnCategorySocialContainerDark
import com.alvin.neuromind.ui.theme.OnCategorySocialContainerLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

// --- Category detection ---

private enum class EntryCategory { ACADEMIC, FITNESS, SOCIAL, PERSONAL }

private fun TimetableEntry.deriveCategory(): EntryCategory {
    val lower = title.lowercase()
    return when {
        lower.contains(Regex("lecture|class|lab|tutorial|seminar|exam|study|course|assignment|revision|revise")) ->
            EntryCategory.ACADEMIC
        lower.contains(Regex("gym|workout|yoga|fitness|run|swim|sport|exercise|training|jog")) ->
            EntryCategory.FITNESS
        lower.contains(Regex("group|meeting|club|social|party|dinner|lunch|coffee|chat|session")) ->
            EntryCategory.SOCIAL
        else -> EntryCategory.PERSONAL
    }
}

@Composable
private fun entryContainerColor(category: EntryCategory): Color = when (category) {
    EntryCategory.ACADEMIC -> MaterialTheme.colorScheme.primaryContainer
    EntryCategory.FITNESS  -> MaterialTheme.colorScheme.tertiaryContainer
    EntryCategory.SOCIAL   -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) CategorySocialContainerDark else CategorySocialContainerLight
    EntryCategory.PERSONAL -> MaterialTheme.colorScheme.surfaceContainer
}

@Composable
private fun entryAccentColor(category: EntryCategory): Color = when (category) {
    EntryCategory.ACADEMIC -> MaterialTheme.colorScheme.primary
    EntryCategory.FITNESS  -> MaterialTheme.colorScheme.tertiary
    EntryCategory.SOCIAL   -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) CategorySocialDark else CategorySocialLight
    EntryCategory.PERSONAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

// contentColorFor() only resolves known M3 roles; SOCIAL's container is a
// custom categorical color, so its "on" color needs to be supplied explicitly
// or Card falls back to Color.Unspecified for entry title/body text.
@Composable
private fun entryContentColor(category: EntryCategory): Color = when (category) {
    EntryCategory.SOCIAL -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) OnCategorySocialContainerDark else OnCategorySocialContainerLight
    else -> MaterialTheme.colorScheme.contentColorFor(entryContainerColor(category))
}

// --- Screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: TimetableViewModel, onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog || editingEntry != null) {
        AddEditEntryDialog(
            existingEntry = editingEntry,
            onDismiss = { showAddDialog = false; editingEntry = null },
            onSave = { id, title, day, start, end, venue, details, isRec ->
                viewModel.saveEntry(id, title, day, start, end, venue, details, isRec)
                showAddDialog = false; editingEntry = null
            }
        )
    }

    Scaffold(
        topBar = {
            NeuromindTopBar(title = "Schedule", onNavigateBack = onNavigateBack)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (uiState.entriesByDay.isEmpty()) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No upcoming events", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            AgendaList(
                modifier = Modifier.padding(innerPadding),
                entriesByDay = uiState.entriesByDay,
                onEdit = { editingEntry = it },
                onDelete = { viewModel.deleteEntry(it) }
            )
        }
    }
}

@Composable
fun AgendaList(
    modifier: Modifier,
    entriesByDay: Map<DayOfWeek, List<TimetableEntry>>,
    onEdit: (TimetableEntry) -> Unit,
    onDelete: (TimetableEntry) -> Unit
) {
    val today = LocalDate.now().dayOfWeek
    val orderedDays = DayOfWeek.entries.sortedBy { (it.value - today.value + 7) % 7 }
    val listState = rememberLazyListState()
    val nowTime = remember { LocalTime.now() }

    // Compute index of the first upcoming event today for smooth scroll on open
    val todayEntries = remember(entriesByDay) {
        entriesByDay[today]?.sortedBy { it.startTime } ?: emptyList()
    }
    val scrollTargetIndex = remember(todayEntries) {
        val pastCount = todayEntries.count { it.endTime.isBefore(nowTime) }
        if (todayEntries.isNotEmpty() && pastCount > 0) 1 + pastCount else 0
    }

    LaunchedEffect(scrollTargetIndex) {
        if (scrollTargetIndex > 0) listState.animateScrollToItem(scrollTargetIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        orderedDays.forEach { day ->
            val entries = entriesByDay[day]?.sortedBy { it.startTime } ?: emptyList()
            if (entries.isNotEmpty()) {
                item { DayHeader(day = day, isToday = day == today) }
                items(entries, key = { it.id }) { entry ->
                    AgendaEventCard(entry, onEdit, onDelete)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun AgendaEventCard(
    entry: TimetableEntry,
    onEdit: (TimetableEntry) -> Unit,
    onDelete: (TimetableEntry) -> Unit
) {
    val timeFormat = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val category = remember(entry.title) { entry.deriveCategory() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(entry) },
        colors = CardDefaults.cardColors(
            containerColor = entryContainerColor(category),
            contentColor   = entryContentColor(category)
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(75.dp)) {
                Text(
                    text = entry.startTime.format(timeFormat),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.endTime.format(timeFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            VerticalDivider(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .height(40.dp),
                color = entryAccentColor(category)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!entry.venue.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = entryAccentColor(category)
                        )
                        Text(
                            text = entry.venue!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = entryAccentColor(category)
                        )
                    }
                }
                if (!entry.details.isNullOrBlank()) {
                    Text(
                        text = entry.details!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = { onDelete(entry) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun DayHeader(day: DayOfWeek, isToday: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = day.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isToday) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small) {
                Text(
                    text = "TODAY",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditEntryDialog(
    existingEntry: TimetableEntry?,
    onDismiss: () -> Unit,
    onSave: (Int, String, DayOfWeek, LocalTime, LocalTime, String?, String?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existingEntry?.title ?: "") }
    var venue by remember { mutableStateOf(existingEntry?.venue ?: "") }
    var details by remember { mutableStateOf(existingEntry?.details ?: "") }
    var isRecurring by remember { mutableStateOf(existingEntry?.isRecurring ?: true) }
    var selectedDay by remember { mutableStateOf(existingEntry?.dayOfWeek ?: LocalDate.now().dayOfWeek) }
    var startTime by remember { mutableStateOf(existingEntry?.startTime ?: LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(existingEntry?.endTime ?: LocalTime.of(10, 0)) }
    var expanded by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    if (showStartTimePicker) TimePickerDialog({ showStartTimePicker = false }, { startTime = it }, startTime)
    if (showEndTimePicker) TimePickerDialog({ showEndTimePicker = false }, { endTime = it }, endTime)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingEntry == null) "Add Entry" else "Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Weekly Recurring?", modifier = Modifier.weight(1f))
                    Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedDay.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        onValueChange = {}, readOnly = true, label = { Text("Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(text = { Text(day.name) }, onClick = { selectedDay = day; expanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("Start: ${startTime.format(timeFormatter)}") }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("End: ${endTime.format(timeFormatter)}") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(existingEntry?.id ?: 0, title, selectedDay, startTime, endTime, venue, details, isRecurring) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit, initialTime: LocalTime) {
    val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timeState) } },
        confirmButton = { Button(onClick = { onConfirm(LocalTime.of(timeState.hour, timeState.minute)); onDismiss() }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
