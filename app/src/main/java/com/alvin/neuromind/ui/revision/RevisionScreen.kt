package com.alvin.neuromind.ui.revision

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.alvin.neuromind.data.TaskRepository
import com.alvin.neuromind.domain.RevisionKind
import com.alvin.neuromind.domain.RevisionPlan
import com.alvin.neuromind.domain.RevisionPlanner
import com.alvin.neuromind.domain.RevisionSession
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.ScreenHeader
import com.alvin.neuromind.ui.components.SectionKicker
import com.alvin.neuromind.ui.theme.Dimens
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RevisionViewModel(repository: TaskRepository) : ViewModel() {
    // Planned from the start of today so sessions don't reshuffle while you look.
    val plan: StateFlow<RevisionPlan?> = repository.allTimetableEntries
        .map { RevisionPlanner.plan(it, LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class RevisionViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RevisionViewModel(repository) as T
    }
}

private val dayFmt = DateTimeFormatter.ofPattern("EEE d MMM")

fun minutesText(m: Int): String = when {
    m < 60 -> "$m min"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60} h ${m % 60} min"
}

@Composable
fun revisionKindColor(kind: RevisionKind): Color = when (kind) {
    RevisionKind.FIRST_PASS -> MaterialTheme.colorScheme.secondary
    RevisionKind.PRACTICE -> MaterialTheme.colorScheme.primary
    RevisionKind.FINAL_PASS -> MaterialTheme.colorScheme.tertiary
    RevisionKind.LIGHT_REVIEW -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Day-by-day revision plan for every upcoming exam. */
@Composable
fun RevisionScreen(
    viewModel: RevisionViewModel,
    onNavigateUp: () -> Unit,
    onOpenExam: (Int) -> Unit
) {
    val plan by viewModel.plan.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.ScreenPaddingHorizontal),
        contentPadding = PaddingValues(bottom = Dimens.ScreenPaddingBottomNoNav),
        verticalArrangement = Arrangement.spacedBy(Dimens.ListGap)
    ) {
        item {
            Spacer(Modifier.height(Dimens.ScreenPaddingTop - Dimens.CardGap))
            ScreenHeader(title = "Revision plan", onBack = onNavigateUp)
            Text(
                "Built from your exam events (any event with \"exam\" in its title) and your free time. " +
                    "Each unit gets a first pass early, practice close to the exam, a final pass and a light " +
                    "review on the day. It updates when your timetable changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val p = plan ?: return@LazyColumn
        if (p.sessions.isEmpty()) {
            item { Text("No exams coming up. Add an event with \"exam\" in its title.", style = MaterialTheme.typography.bodyMedium) }
            return@LazyColumn
        }
        if (p.shortfall.isNotEmpty()) {
            item {
                OrganicCard(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SectionKicker("Not enough time", color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            p.shortfall.entries.joinToString("\n") { "${it.key}: ${minutesText(it.value)} short" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        p.days.filter { !it.isBefore(today) }.forEach { day ->
            val sessions = p.on(day)
            item(key = "h$day") {
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (day == today) "Today · ${day.format(dayFmt)}" else day.format(dayFmt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(minutesText(sessions.sumOf { it.minutes }), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(sessions, key = { "$day-${it.examName}-${it.kind}-${it.minutes}-${sessions.indexOf(it)}" }) { s ->
                RevisionSessionRow(s, onClick = { onOpenExam(s.examEntryId) })
            }
        }
    }
}

@Composable
fun RevisionSessionRow(session: RevisionSession, onClick: () -> Unit, showHint: Boolean = false) {
    val color = revisionKindColor(session.kind)
    OrganicCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.examName, style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.kind.label, style = MaterialTheme.typography.labelMedium, color = color)
                    if (session.date == session.examDate && session.kind == RevisionKind.LIGHT_REVIEW) {
                        Text(" · exam today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (showHint) {
                    Text(session.kind.hint, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(minutesText(session.minutes), style = MaterialTheme.typography.labelLarge)
        }
    }
}
