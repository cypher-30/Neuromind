package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The three ground/container palette variants Adaptive Organic can resolve to. */
enum class AppearanceVariant { BASE, SUBDUED, WARM }

/**
 * Pure, local resolver for Adaptive Organic (opt-in, off by default — see
 * UserPreferencesRepository.adaptiveAppearanceEnabled). Deliberately reads
 * only explicit [FeedbackLog.mood] / [FeedbackLog.energyLevel] check-ins from
 * the *current local calendar day* — never [FeedbackLog.toneLabel],
 * sentimentScore, or journal comment text, and never anything from a
 * previous day (so a stale, unlogged day always falls back to BASE rather
 * than freezing on yesterday's mood).
 *
 * Semantic colors (priority/error/category — see ui/theme/PriorityColors.kt)
 * are untouched by this; only decorative background/surface ground shifts.
 */
object AdaptiveAppearance {

    fun resolve(
        logs: List<FeedbackLog>,
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault()
    ): AppearanceVariant {
        val todaysLogs = logs.filter {
            Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() == today
        }
        if (todaysLogs.isEmpty()) return AppearanceVariant.BASE

        // Most recent check-in of the day wins if there are several.
        val latest = todaysLogs.maxByOrNull { it.date } ?: return AppearanceVariant.BASE

        val isLow = latest.mood == Mood.STRESSED || latest.mood == Mood.TIRED || latest.energyLevel <= 2
        val isHigh = (latest.mood == Mood.GOOD || latest.mood == Mood.GREAT) && latest.energyLevel >= 4

        // Stressed/low-energy takes precedence over a simultaneous high signal —
        // a subdued ground is the safer default when signals conflict.
        return when {
            isLow -> AppearanceVariant.SUBDUED
            isHigh -> AppearanceVariant.WARM
            else -> AppearanceVariant.BASE
        }
    }
}
