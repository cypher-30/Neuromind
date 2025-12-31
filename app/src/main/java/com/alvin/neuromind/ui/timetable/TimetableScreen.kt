package com.alvin.neuromind.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.alvin.neuromind.data.TimetableEntry
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

// --- CONFIGURATION ---
private const val DAY_START_HOUR = 6  // 6 AM
private const val DAY_END_HOUR = 23   // 11 PM
private val HOUR_HEIGHT = 90.dp       // Height of one hour block
private val DAY_WIDTH = 200.dp        // Width of one day column

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onNavigateBack: () -> Unit
) {
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
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { innerPadding ->
        if (uiState.entriesByDay.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Your schedule is empty!", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap + to add a class, or load Demo Data in Settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            WeeklyScheduleGrid(
                modifier = Modifier.padding(innerPadding),
                entriesByDay = uiState.entriesByDay
            )
        }
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

    // Calculate exact height of the grid content
    val totalHours = DAY_END_HOUR - DAY_START_HOUR + 1
    val gridContentHeight = HOUR_HEIGHT * totalHours

    Box(
        modifier = modifier
            .fillMaxSize() // Fill screen
            .verticalScroll(verticalScrollState) // Allow scrolling vertical
            .horizontalScroll(horizontalScrollState) // Allow scrolling horizontal
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Time Axis (Left side)
            TimeAxis(hourHeight = HOUR_HEIGHT, totalHours = totalHours)

            // Day Columns
            daysOfWeek.forEach { day ->
                DayColumn(
                    day = day,
                    entriesForDay = entriesByDay[day] ?: emptyList(),
                    hourHeight = HOUR_HEIGHT,
                    gridHeight = gridContentHeight,
                    currentTime = currentTime
                )
            }
        }
    }
}

@Composable
private fun TimeAxis(modifier: Modifier = Modifier, hourHeight: Dp, totalHours: Int) {
    Column(modifier = modifier) {
        Box(modifier = Modifier.height(48.dp)) // Spacer for Day Header
        (0 until totalHours).forEach { i ->
            val hour = DAY_START_HOUR + i
            Box(
                modifier = Modifier.height(hourHeight).padding(horizontal = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h a")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    day: DayOfWeek,
    entriesForDay: List<TimetableEntry>,
    hourHeight: Dp,
    gridHeight: Dp,
    currentTime: LocalTime
) {
    val dayToday = LocalDate.now().dayOfWeek
    val isToday = day == dayToday
    val dpPerMinute = hourHeight.value / 60

    Column(modifier = Modifier.width(DAY_WIDTH)) {
        // 1. Header (Fixed Height)
        Box(
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth()
                .background(if(isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if(isToday) FontWeight.Bold else FontWeight.Normal,
                color = if(isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }

        // 2. The Grid Area (With Overlap Logic)
        Box(
            modifier = Modifier
                .height(gridHeight)
                .fillMaxWidth()
                .drawBehind {
                    val totalHours = DAY_END_HOUR - DAY_START_HOUR + 1
                    (0 until totalHours).forEach { i ->
                        val y = i * hourHeight.toPx()
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
        ) {
            val sortedEntries = entriesForDay.sortedBy { it.startTime }
            val processedEntries = mutableListOf<TimetableEntry>()

            sortedEntries.forEach { entry ->
                if (entry.endTime.hour >= DAY_START_HOUR && entry.startTime.hour <= DAY_END_HOUR) {
                    val duration = Duration.between(entry.startTime, entry.endTime).toMinutes()
                    val startMinutes = (entry.startTime.hour * 60) + entry.startTime.minute
                    val gridStartMinutes = DAY_START_HOUR * 60
                    val offsetMinutes = startMinutes - gridStartMinutes

                    if (offsetMinutes >= 0) {
                        // Check for overlaps with already processed entries
                        val overlapping = processedEntries.filter {
                            it.startTime < entry.endTime && it.endTime > entry.startTime
                        }

                        val overlapCount = overlapping.size
                        // Simple logic: if overlapping, shrink width and shift right
                        val widthFraction = 1f / (overlapCount + 1)
                        val xOffsetFraction = widthFraction * overlapCount

                        EventBlock(
                            entry = entry,
                            modifier = Modifier
                                .fillMaxWidth(widthFraction)
                                .align(Alignment.TopStart)
                                .offset(
                                    x = (xOffsetFraction * DAY_WIDTH.value).dp,
                                    y = offsetMinutes.toFloat() * dpPerMinute.dp
                                )
                                .height(duration.toFloat() * dpPerMinute.dp)
                        )
                    }
                    processedEntries.add(entry)
                }
            }

            // Current Time Indicator (Red Line)
            if (isToday && currentTime.hour in DAY_START_HOUR..DAY_END_HOUR) {
                val currentMinutes = (currentTime.hour * 60) + currentTime.minute
                val gridStartMinutes = DAY_START_HOUR * 60
                val offsetMinutes = currentMinutes - gridStartMinutes

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.error,
                    thickness = 2.dp,
                    modifier = Modifier.offset(y = offsetMinutes.toFloat() * dpPerMinute.dp)
                )
            }
        }
    }
}

@Composable
private fun EventBlock(entry: TimetableEntry, modifier: Modifier = Modifier) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    Card(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 1.dp), // Reduced padding slightly for side-by-side
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (!entry.venue.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = entry.venue, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Text(
                text = "${entry.startTime.format(timeFormatter)} - ${entry.endTime.format(timeFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                maxLines = 1
            )
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

                // Day Selector
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedDay.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(text = { Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault())) }, onClick = { selectedDay = day; expanded = false })
                        }
                    }
                }

                // Time Selectors
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f)) { Text("Start: ${startTime.format(timeFormatter)}") }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f)) { Text("End: ${endTime.format(timeFormatter)}") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) onSave(title, selectedDay, startTime, endTime, venue, details)
            }) { Text("Save") }
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