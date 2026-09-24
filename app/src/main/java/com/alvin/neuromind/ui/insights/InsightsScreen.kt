package com.alvin.neuromind.ui.insights

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.domain.FocusSummary
import com.alvin.neuromind.domain.RetroInsights
import com.alvin.neuromind.ui.components.AnimatedBarChart
import com.alvin.neuromind.ui.components.OrganicCard
import com.alvin.neuromind.ui.components.SectionKicker
import com.alvin.neuromind.ui.components.StatTile
import com.alvin.neuromind.ui.theme.Dimens
import com.alvin.neuromind.ui.theme.Pill
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Dimens.ScreenPaddingHorizontal,
            end = Dimens.ScreenPaddingHorizontal,
            top = Dimens.ScreenPaddingTop,
            bottom = Dimens.ScreenPaddingBottomWithNav
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.CardGap)
    ) {
        item { Text("Insights", style = MaterialTheme.typography.headlineMedium) }

        item { WellnessCard(score = (uiState.wellnessScore * 100).toInt()) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                StatTile(label = "Avg Mood", value = uiState.averageMood, modifier = Modifier.weight(1f))
                StatTile(label = "Avg Energy", value = "${uiState.averageEnergy}/5", modifier = Modifier.weight(1f))
            }
        }

        item {
            StatTile(
                label = "Dominant Tone",
                value = uiState.dominantTone,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OrganicCard(modifier = Modifier.fillMaxWidth()) {
                Text("Tasks completed this week", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedBarChart(
                    entries = uiState.completionData.map { (day, count) -> day to count.toFloat() },
                    valueLabel = { it.toInt().toString() },
                    emptyMessage = "No activity recorded yet."
                )
            }
        }

        uiState.retroInsights?.let { retro ->
            item { WeeklyTrendCard(weeklyTrend = retro.weeklyTrend) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                    BestDayCard(retro = retro, modifier = Modifier.weight(1f))
                    OnTrackCard(rate = retro.onTrackRate, modifier = Modifier.weight(1f))
                }
            }
            item { MoodProductivityCard(retro = retro) }
        }

        if (uiState.subjectBreakdown.isNotEmpty()) {
            item { SubjectBreakdownCard(uiState.subjectBreakdown) }
        }

        uiState.focusSummary?.let { summary ->
            item { DeepWorkCard(summary = summary) }
        }

        item {
            Text(
                "Recent Journal Entries",
                style = MaterialTheme.typography.titleLarge
            )
        }
        if (uiState.recentNotes.isEmpty()) {
            item {
                OrganicCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "No journal entries yet. Add thoughts in your End-of-Day Review.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            item {
                OrganicCard(modifier = Modifier.fillMaxWidth()) {
                    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                    uiState.recentNotes.forEachIndexed { index, note ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                Icons.Default.Notes,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Column {
                                Text(note.comment ?: "", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                val toneLabel = note.toneLabel
                                    ?.name
                                    ?.lowercase()
                                    ?.replaceFirstChar { it.uppercase() }
                                if (toneLabel != null) {
                                    val score = note.sentimentScore?.let { " (${"%.2f".format(it)})" } ?: ""
                                    Text(
                                        "Tone: $toneLabel$score",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Text(
                                    dateFormat.format(Date(note.date)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (index < uiState.recentNotes.lastIndex) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTrendCard(weeklyTrend: List<com.alvin.neuromind.domain.WeekStat>) {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Your Streak", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "4-week completion %",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedBarChart(
            entries = weeklyTrend.map { it.weekLabel to (it.completionPct * 100f) },
            valueLabel = { "${it.toInt()}%" },
            emptyMessage = "No trend data yet."
        )
    }
}

@Composable
private fun BestDayCard(retro: RetroInsights, modifier: Modifier = Modifier) {
    val bestDay = retro.bestDay
    OrganicCard(color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Best Day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(8.dp))
        if (bestDay != null) {
            Text(dayName(bestDay.day), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "avg ${bestDay.completedCount} tasks",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        } else {
            Text(
                "Keep logging to see your best day",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun OnTrackCard(rate: Float, modifier: Modifier = Modifier) {
    val animatedRate by animateFloatAsState(
        targetValue = rate,
        animationSpec = tween(durationMillis = 1000, easing = EaseOut),
        label = "on_track_rate"
    )
    val pct = (rate * 100).toInt()
    val tip = when {
        rate < 0.5f -> "Consider smaller tasks or longer deadlines"
        rate > 0.8f -> "Excellent — keep your pace"
        else -> "Good progress — stay consistent"
    }

    OrganicCard(color = MaterialTheme.colorScheme.secondaryContainer, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("On-Track", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
                CircularProgressIndicator(
                    progress = { animatedRate },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                Text("$pct%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Text(
                tip,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MoodProductivityCard(retro: RetroInsights) {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SentimentSatisfiedAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Mood & Productivity", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "Avg tasks completed per mood",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        retro.moodProductivity.forEach { moodStat ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(moodEmoji(moodStat.mood), style = MaterialTheme.typography.bodyMedium)
                    Text(moodStat.mood.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                }
                Text(
                    "%.1f tasks".format(moodStat.avgTasksCompleted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (moodStat != retro.moodProductivity.last()) {
                HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun SubjectBreakdownCard(subjects: List<SubjectStat>) {
    OrganicCard(modifier = Modifier.fillMaxWidth()) {
        SectionKicker("By subject", color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.height(12.dp))
        subjects.forEach { stat ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stat.subject, style = MaterialTheme.typography.bodyMedium)
                Text(stat.count.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { stat.pct },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(Pill),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            if (stat != subjects.last()) Spacer(Modifier.height(10.dp))
        }
    }
}

private fun dayName(day: DayOfWeek): String =
    day.name.lowercase().replaceFirstChar { it.uppercase() }

private fun moodEmoji(mood: Mood): String = when (mood) {
    Mood.STRESSED -> "😰"
    Mood.TIRED -> "😴"
    Mood.NEUTRAL -> "😐"
    Mood.GOOD -> "😊"
    Mood.GREAT -> "😄"
}

@Composable
fun WellnessCard(score: Int) {
    val animatedScore by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOut),
        label = "wellness_score"
    )

    OrganicCard(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Wellness Score", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Based on your recent logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { animatedScore },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(Pill),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("$score%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                CircularProgressIndicator(
                    progress = { animatedScore },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Text(text = "$score%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun DeepWorkCard(summary: FocusSummary) {
    OrganicCard(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        SectionKicker("Deep work", color = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "${summary.totalMinutesThisWeek} min this week",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "${summary.sessionCountThisWeek} session${if (summary.sessionCountThisWeek != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            summary.bestDayLabel?.let {
                Text("Best: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedBarChart(
            entries = summary.minutesByDay.map { (day, mins) -> day to mins.toFloat() },
            valueLabel = { "" },
            emptyMessage = "No focus sessions yet.",
            barColor = MaterialTheme.colorScheme.secondary,
            height = 80.dp
        )
    }
}
