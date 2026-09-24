package com.alvin.neuromind.ui.timetable

import com.alvin.neuromind.ui.components.OrganicConfirmDialog
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.data.TimetableEntry
import com.alvin.neuromind.domain.ParsedTeamsMeeting
import com.alvin.neuromind.domain.TeamsMeetingHelper
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.PillButton
import com.alvin.neuromind.ui.tasks.ConfirmDiscardDraftDialog
import com.alvin.neuromind.ui.theme.Dimens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

// --- Category detection ---
// Organic's runtime derives category color from just two roles (categoryColorFor:
// Academic/Social -> accent, Fitness/Personal -> accent-2) rather than four
// distinct hues, so the dedicated Social plum accent from the old palette is
// retired along with it.

enum class EntryCategory { ACADEMIC, FITNESS, SOCIAL, PERSONAL }

fun TimetableEntry.deriveCategory(): EntryCategory {
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
private fun entryAccentColor(category: EntryCategory): androidx.compose.ui.graphics.Color = when (category) {
    EntryCategory.ACADEMIC, EntryCategory.SOCIAL -> MaterialTheme.colorScheme.primary
    EntryCategory.FITNESS, EntryCategory.PERSONAL -> MaterialTheme.colorScheme.secondary
}

// --- View ---
// Folds into Tasks as the "Timetable" side of that screen's List/Timetable
// toggle, so it takes no Scaffold of its own. Sections are date-aware: today
// and the next six days (weekly classes + dated events), then later events,
// then past events so old ones can still be reviewed or deleted.
@Composable
fun TimetableAgendaView(
    viewModel: TimetableViewModel,
    searchQuery: String,
    onEditEvent: (TimetableEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val today = LocalDate.now()

    pendingDeleteId?.let { id ->
        val entry = uiState.entries.find { it.id == id }
        if (entry == null) {
            LaunchedEffect(id) { pendingDeleteId = null }
        } else {
            ConfirmDeleteEntryDialog(
                entry = entry,
                onConfirm = { viewModel.deleteEntry(entry); pendingDeleteId = null },
                onDismiss = { pendingDeleteId = null }
            )
        }
    }

    val sections = remember(uiState.entries, searchQuery, today) {
        val filtered = if (searchQuery.isBlank()) uiState.entries
        else uiState.entries.filter { it.title.contains(searchQuery, ignoreCase = true) }
        TimetableViewModel.buildSections(filtered, today)
    }

    if (sections.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isBlank()) "No classes or events yet" else "No matching entries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        AgendaList(
            modifier = modifier,
            sections = sections,
            onEdit = { entry -> if (entry.isRecurring) viewModel.openEditEntry(entry) else onEditEvent(entry) },
            onDelete = { pendingDeleteId = it.id }
        )
    }
}

@Composable
fun ConfirmDeleteEntryDialog(entry: TimetableEntry, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val kind = if (entry.isRecurring) "class" else "event"
    OrganicConfirmDialog(
        title = "Delete this $kind?",
        message = if (entry.isRecurring) "\"${entry.title}\" will be removed from every week. This can't be undone."
        else "\"${entry.title}\" and its reminder will be permanently removed. This can't be undone.",
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/** Shows the weekly-class dialog whenever [TimetableViewModel.editor] is open. */
@Composable
fun TimetableEntryEditorDialog(viewModel: TimetableViewModel) {
    val state by viewModel.editor.collectAsStateWithLifecycle()
    state?.let { editor ->
        AddEditEntryDialog(
            state = editor,
            onChange = viewModel::editEntry,
            onSave = viewModel::saveEditor,
            onClose = viewModel::closeEditor,
            onDiscard = viewModel::discardEditorDraft,
            onDismissBanner = viewModel::dismissRestoredBanner
        )
    }
}

@Composable
fun AgendaList(
    modifier: Modifier,
    sections: List<AgendaSection>,
    onEdit: (TimetableEntry) -> Unit,
    onDelete: (TimetableEntry) -> Unit
) {
    val listState = rememberLazyListState()
    val nowTime = remember { LocalTime.now() }
    val todaySection = sections.firstOrNull { it.isToday }

    val scrollTargetIndex = remember(todaySection) {
        val pastCount = todaySection?.entries?.count { !it.isAllDay && it.endTime.isBefore(nowTime) } ?: 0
        if (pastCount > 0) 1 + pastCount else 0
    }

    LaunchedEffect(scrollTargetIndex) {
        if (scrollTargetIndex > 0) listState.animateScrollToItem(scrollTargetIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomWithNav)
    ) {
        sections.forEach { section ->
            item(key = "header_${section.title}") { DayHeader(section.title, section.date, section.isToday) }
            items(section.entries, key = { "${section.title}_${it.id}" }) { entry ->
                AgendaEventCard(entry, section.showDates, onEdit, onDelete)
            }
            item(key = "gap_${section.title}") { Spacer(modifier = Modifier.height(Dimens.SectionGap)) }
        }
    }
}

@Composable
fun AgendaEventCard(
    entry: TimetableEntry,
    showDate: Boolean,
    onEdit: (TimetableEntry) -> Unit,
    onDelete: (TimetableEntry) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val category = remember(entry.title) { entry.deriveCategory() }
    val accent = entryAccentColor(category)
    val teamsMeeting = remember(entry.details, entry.venue) {
        TeamsMeetingHelper.parseMeeting(entry.details, entry.venue)
    }
    val timeText = if (entry.isAllDay) "All day" else "${entry.startTime.format(timeFormat)} – ${entry.endTime.format(timeFormat)}"
    val whenText = if (showDate && entry.date != null) "${entry.date.format(dateFormat)} · $timeText" else timeText

    OrganicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit(entry) }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(accent, MaterialTheme.shapes.extraSmall)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!entry.isRecurring) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Event,
                            contentDescription = "Event",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!entry.venue.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = accent
                        )
                        Text(
                            text = entry.venue,
                            style = MaterialTheme.typography.bodySmall,
                            color = accent
                        )
                    }
                }
                if (!entry.details.isNullOrBlank()) {
                    Text(
                        text = entry.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (teamsMeeting.hasLaunchAction) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AssistChip(
                        onClick = { TeamsMeetingHelper.launchMeeting(context, teamsMeeting) },
                        label = { Text("Join Teams") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.VideoCall,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
fun DayHeader(title: String, date: LocalDate?, isToday: Boolean) {
    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM d") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (date != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = date.format(dateFormat),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isToday) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(color = MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape) {
                Text(
                    text = "TODAY",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditEntryDialog(
    state: TimetableEditorState,
    onChange: ((TimetableEditorState) -> TimetableEditorState) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onDiscard: () -> Unit,
    onDismissBanner: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var showStartTimePicker by rememberSaveable { mutableStateOf(false) }
    var showEndTimePicker by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var pastePreview by remember { mutableStateOf<String?>(null) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }

    if (showStartTimePicker) TimePickerDialog({ showStartTimePicker = false }, { t -> onChange { it.copy(startTime = t) } }, state.startTime)
    if (showEndTimePicker) TimePickerDialog({ showEndTimePicker = false }, { t -> onChange { it.copy(endTime = t) } }, state.endTime)
    if (confirmDiscard) {
        ConfirmDiscardDraftDialog(
            onConfirm = { onDiscard(); confirmDiscard = false },
            onDismiss = { confirmDiscard = false }
        )
    }

    AlertDialog(
        // Tapping outside or Back just closes; the draft is kept.
        onDismissRequest = onClose,
        title = { Text(if (state.entryId == 0) "Add weekly class" else "Edit weekly class") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (state.restoredDraft) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Restored your unsaved draft",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            PillButton(text = "OK", onClick = onDismissBanner, ghost = true)
                        }
                    }
                }
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { v -> onChange { it.copy(title = v) } },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                AssistChip(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val invite = clipboard.primaryClip
                            ?.getItemAt(0)
                            ?.coerceToText(context)
                            ?.toString()
                            ?.trim()
                            .orEmpty()
                        if (invite.isBlank()) {
                            Toast.makeText(context, "Clipboard is empty.", Toast.LENGTH_SHORT).show()
                        } else {
                            var details = state.details
                            var venue = state.venue
                            val parsed = TeamsMeetingHelper.parseMeeting(invite)
                            val detailsHasCreds = hasIdOrPass(details)
                            val venueHasCreds = hasIdOrPass(venue)
                            val target = when {
                                detailsHasCreds -> "Details"
                                venueHasCreds -> "Venue"
                                else -> "Details"
                            }
                            pastePreview = buildPastePreview(parsed, target)
                            if (parsed.hasLaunchAction) {
                                when {
                                    detailsHasCreds -> {
                                        details = appendTeamsLink(details, parsed.joinUrl)
                                        Toast.makeText(context, "Added Teams link under existing details.", Toast.LENGTH_SHORT).show()
                                    }
                                    venueHasCreds -> {
                                        venue = appendTeamsLink(venue, parsed.joinUrl)
                                        Toast.makeText(context, "Added Teams link under existing venue info.", Toast.LENGTH_SHORT).show()
                                    }
                                    else -> {
                                        val block = buildTeamsBlock(parsed, invite)
                                        details = upsertTeamsBlock(details, block)
                                        Toast.makeText(context, "Added Teams info to details.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                if (venue.isBlank()) venue = "Online (Teams)"
                            } else {
                                details = compactMultiline(invite)
                                Toast.makeText(context, "Invite pasted. Add Teams ID/link format for quick join.", Toast.LENGTH_LONG).show()
                            }
                            onChange { it.copy(details = details, venue = venue) }
                        }
                    },
                    label = { Text("Paste Teams invite") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                pastePreview?.let { preview ->
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = state.venue,
                    onValueChange = { v -> onChange { it.copy(venue = v) } },
                    label = { Text("Venue") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.details,
                    onValueChange = { v -> onChange { it.copy(details = v) } },
                    label = { Text("Details") },
                    minLines = 2,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "Every " + state.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                        onValueChange = {}, readOnly = true, label = { Text("Repeats") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DayOfWeek.entries.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day.getDisplayName(TextStyle.FULL, Locale.getDefault())) },
                                onClick = { onChange { it.copy(dayOfWeek = day) }; expanded = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showStartTimePicker = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) { Text("Start ${state.startTime.format(timeFormatter)}", maxLines = 1) }
                    OutlinedButton(onClick = { showEndTimePicker = true }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)) { Text("End ${state.endTime.format(timeFormatter)}", maxLines = 1) }
                }
                Text(
                    "Something on one specific date (a conference, exam, meeting)? Add it as an Event from the + button instead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                if (state.hasUnsavedChanges) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Draft saved automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        PillButton(text = "Discard draft", onClick = { confirmDiscard = true }, ghost = true)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave, enabled = state.isReady) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}

private fun hasIdOrPass(text: String): Boolean {
    val parsed = TeamsMeetingHelper.parseMeeting(text)
    return !parsed.meetingId.isNullOrBlank() || !parsed.passcode.isNullOrBlank()
}

private fun buildTeamsBlock(parsed: ParsedTeamsMeeting, fallbackRaw: String): String {
    val lines = mutableListOf<String>()
    parsed.meetingId?.let { lines += "Meeting ID: $it" }
    parsed.passcode?.let { lines += "Passcode: $it" }
    parsed.joinUrl?.let { lines += it }
    return if (lines.isNotEmpty()) lines.joinToString("\n") else compactMultiline(fallbackRaw)
}

private fun upsertTeamsBlock(existing: String, teamsBlock: String): String {
    val cleaned = existing
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .filterNot { line ->
            line.matches(Regex("(?i)^meeting\\s*(id|number)\\b.*")) ||
                line.matches(Regex("(?i)^id\\b.*")) ||
                line.matches(Regex("(?i)^(pass\\s*code|passcode|password|pwd|pass)\\b.*")) ||
                line.contains(Regex("https?://teams\\.microsoft\\.com/", RegexOption.IGNORE_CASE))
        }
        .toMutableList()

    val teamsLines = compactMultiline(teamsBlock)
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toList()

    cleaned.addAll(teamsLines)
    return cleaned.distinct().joinToString("\n")
}

private fun appendTeamsLink(existing: String, link: String?): String {
    if (link.isNullOrBlank()) return compactMultiline(existing)
    val lines = existing
        .replace("\r\n", "\n")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toMutableList()
    if (lines.none { it.equals(link, ignoreCase = true) }) {
        lines += link
    }
    return lines.joinToString("\n")
}

private fun buildPastePreview(parsed: ParsedTeamsMeeting, target: String): String {
    val pieces = mutableListOf<String>()
    if (!parsed.meetingId.isNullOrBlank()) pieces += "ID"
    if (!parsed.passcode.isNullOrBlank()) pieces += "Pass"
    if (!parsed.joinUrl.isNullOrBlank()) pieces += "Link"
    val detected = if (pieces.isEmpty()) "none" else pieces.joinToString(" + ")
    return "Detected: $detected. Target: $target"
}

private fun compactMultiline(text: String): String {
    return text
        .replace("\r\n", "\n")
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimePickerDialog(onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit, initialTime: LocalTime) {
    val timeState = rememberTimePickerState(initialHour = initialTime.hour, initialMinute = initialTime.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timeState) } },
        confirmButton = { Button(onClick = { onConfirm(LocalTime.of(timeState.hour, timeState.minute)); onDismiss() }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
