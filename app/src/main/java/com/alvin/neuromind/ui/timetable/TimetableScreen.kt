package com.alvin.neuromind.ui.timetable

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*

private const val DAY_START_HOUR = 8
private const val DAY_END_HOUR = 22
private val HOUR_HEIGHT = 80.dp
private val DAY_WIDTH = 250.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(viewModel: TimetableViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, day, start, end, venue, details ->
                viewModel.addEntry(title, day, start, end, venue, details)
                showAddDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Timetable") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Timetable Entry")
            }
        }
    ) { innerPadding ->
        WeeklyScheduleGrid(
            modifier = Modifier.padding(innerPadding),
            entriesByDay = uiState.entriesByDay
        )
    }
}

@Composable
private fun WeeklyScheduleGrid(
    modifier: Modifier = Modifier,
    entriesByDay: Map<DayOfWeek, List<TimetableEntry>>
) {
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(60_000L) // Update every minute
        }
    }
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val daysOfWeek = DayOfWeek.entries.toTypedArray()

    Box(modifier = modifier.verticalScroll(verticalScrollState).horizontalScroll(horizontalScrollState)) {
        Row {
            TimeAxis(hourHeight = HOUR_HEIGHT)
            daysOfWeek.forEach { day ->
                DayColumn(
                    day = day,
                    entriesForDay = entriesByDay[day] ?: emptyList(),
                    hourHeight = HOUR_HEIGHT,
                    currentTime = currentTime
                )
            }
        }
    }
}

@Composable
private fun TimeAxis(modifier: Modifier = Modifier, hourHeight: Dp) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.height(48.dp))
        (DAY_START_HOUR..DAY_END_HOUR).forEach { hour ->
            Box(
                modifier = Modifier.height(hourHeight).padding(horizontal = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "${if (hour == 0 || hour == 12) 12 else hour % 12} ${if (hour < 12) "AM" else "PM"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun DayColumn(day: DayOfWeek, entriesForDay: List<TimetableEntry>, hourHeight: Dp, currentTime: LocalTime) {
    val dayToday = LocalDate.now().dayOfWeek
    val dpPerMinute = hourHeight.value / 60
    Column(modifier = Modifier.width(DAY_WIDTH).fillMaxHeight()) {
        // --- Day Header Box ---
        Box(
            modifier = Modifier.height(48.dp).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        // --- Canvas Box for events and lines ---
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().drawBehind {
                (DAY_START_HOUR..DAY_END_HOUR).forEach { hour ->
                    val y = ((hour - DAY_START_HOUR) * hourHeight.toPx())
                    drawLine(color = Color.LightGray.copy(alpha = 0.5f), start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
        ) {
            entriesForDay.forEach { entry ->
                val duration = Duration.between(entry.startTime, entry.endTime).toMinutes()
                val offset = ChronoUnit.MINUTES.between(LocalTime.of(DAY_START_HOUR, 0), entry.startTime)
                if (entry.endTime.isAfter(LocalTime.of(DAY_START_HOUR, 0))) {
                    EventBlock(
                        entry = entry,
                        modifier = Modifier
                            .height((duration * dpPerMinute).dp)
                            .offset(y = (offset * dpPerMinute).dp)
                    )
                }
            }
            if (day == dayToday && currentTime.hour in DAY_START_HOUR..DAY_END_HOUR) {
                val offset = ChronoUnit.MINUTES.between(LocalTime.of(DAY_START_HOUR, 0), currentTime)
                HorizontalDivider(color = MaterialTheme.colorScheme.error, thickness = 2.dp, modifier = Modifier.offset(y = (offset * dpPerMinute).dp))
            }
        }
    }
}

@Composable
private fun EventBlock(entry: TimetableEntry, modifier: Modifier = Modifier) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(all = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            entry.venue?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Venue", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(text = it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(text = "${entry.startTime.format(timeFormatter)} - ${entry.endTime.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(onDismiss: () -> Unit, onSave: (title: String, day: DayOfWeek, startTime: LocalTime, endTime: LocalTime, venue: String?, details: String?) -> Unit) {
    var title by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf(LocalDate.now().dayOfWeek) }
    var startTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var endTime by remember { mutableStateOf(LocalTime.of(10, 0)) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    if (showStartTimePicker) {
        TimePickerDialog(onDismiss = { showStartTimePicker = false }, onConfirm = { newTime -> startTime = newTime }, initialTime = startTime)
    }
    if (showEndTimePicker) {
        TimePickerDialog(onDismiss = { showEndTimePicker = false }, onConfirm = { newTime -> endTime = newTime }, initialTime = endTime)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Schedule Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, singleLine = true)
                OutlinedTextField(value = venue, onValueChange = { venue = it }, label = { Text("Venue (Optional)") }, singleLine = true)
                OutlinedTextField(value = details, onValueChange = { details = it }, label = { Text("Details (Optional)") })
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedDay.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(text = { Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault())) }, onClick = { selectedDay = day; expanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("Start: ${startTime.format(timeFormatter)}") }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("End: ${endTime.format(timeFormatter)}") }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(title, selectedDay, startTime, endTime, venue, details) }) { Text("Save") } },
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