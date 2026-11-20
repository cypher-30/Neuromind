package com.alvin.neuromind.domain

import com.alvin.neuromind.data.FeedbackLog
import com.alvin.neuromind.data.Mood
import com.alvin.neuromind.data.ToneLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class AdaptiveAppearanceTest {
    private val zone = ZoneId.of("Africa/Nairobi")
    private val today = LocalDate.of(2026, 3, 10)

    private fun at(date: LocalDate, time: LocalTime = LocalTime.NOON) =
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()

    private fun log(
        date: Long,
        mood: Mood,
        energyLevel: Int,
        toneLabel: ToneLabel? = ToneLabel.OVERWHELMED,
        sentimentScore: Float? = -0.9f,
        comment: String? = "unrelated diary text that must never be read"
    ) = FeedbackLog(
        date = date,
        mood = mood,
        energyLevel = energyLevel,
        tasksCompleted = 0,
        comment = comment,
        toneLabel = toneLabel,
        sentimentScore = sentimentScore
    )

    @Test
    fun `no logs at all resolves to BASE`() {
        val variant = AdaptiveAppearance.resolve(emptyList(), today, zone)
        assertEquals(AppearanceVariant.BASE, variant)
    }

    @Test
    fun `missing today's check-in resolves to BASE even with logs on other days`() {
        val logs = listOf(
            log(at(today.minusDays(1)), Mood.STRESSED, energyLevel = 1),
            log(at(today.plusDays(1)), Mood.GREAT, energyLevel = 5)
        )
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.BASE, variant)
    }

    @Test
    fun `stale day data is ignored — only current local calendar day counts`() {
        // A stressed check-in "yesterday" must not bleed into today's variant.
        val logs = listOf(log(at(today.minusDays(1)), Mood.STRESSED, energyLevel = 1))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.BASE, variant)
    }

    @Test
    fun `stressed mood today resolves to SUBDUED`() {
        val logs = listOf(log(at(today), Mood.STRESSED, energyLevel = 3))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.SUBDUED, variant)
    }

    @Test
    fun `tired mood today resolves to SUBDUED`() {
        val logs = listOf(log(at(today), Mood.TIRED, energyLevel = 3))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.SUBDUED, variant)
    }

    @Test
    fun `low energy alone (neutral mood) resolves to SUBDUED`() {
        val logs = listOf(log(at(today), Mood.NEUTRAL, energyLevel = 2))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.SUBDUED, variant)
    }

    @Test
    fun `good mood with high energy today resolves to WARM`() {
        val logs = listOf(log(at(today), Mood.GOOD, energyLevel = 4))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.WARM, variant)
    }

    @Test
    fun `great mood with max energy today resolves to WARM`() {
        val logs = listOf(log(at(today), Mood.GREAT, energyLevel = 5))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.WARM, variant)
    }

    @Test
    fun `neutral mood with mid energy resolves to BASE`() {
        val logs = listOf(log(at(today), Mood.NEUTRAL, energyLevel = 3))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.BASE, variant)
    }

    @Test
    fun `good mood but low energy is a conflicting signal — SUBDUED takes precedence`() {
        val logs = listOf(log(at(today), Mood.GOOD, energyLevel = 1))
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.SUBDUED, variant)
    }

    @Test
    fun `most recent check-in of the day wins over an earlier conflicting one`() {
        val logs = listOf(
            log(at(today, LocalTime.of(8, 0)), Mood.GREAT, energyLevel = 5),
            log(at(today, LocalTime.of(20, 0)), Mood.STRESSED, energyLevel = 1)
        )
        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.SUBDUED, variant)
    }

    @Test
    fun `tone label and sentiment score never influence the result`() {
        // Same mood/energy input, wildly different tone/sentiment/comment —
        // result must be identical, proving those fields are never read.
        val calmToned = log(
            at(today), Mood.GOOD, energyLevel = 4,
            toneLabel = ToneLabel.CALM, sentimentScore = 0.95f, comment = "wonderful day"
        )
        val overwhelmedToned = log(
            at(today), Mood.GOOD, energyLevel = 4,
            toneLabel = ToneLabel.OVERWHELMED, sentimentScore = -0.95f, comment = "terrible day"
        )
        assertEquals(
            AdaptiveAppearance.resolve(listOf(calmToned), today, zone),
            AdaptiveAppearance.resolve(listOf(overwhelmedToned), today, zone)
        )
    }

    @Test
    fun `a log just before local midnight does not count for the next day`() {
        val logs = listOf(log(at(today, LocalTime.of(23, 59)), Mood.STRESSED, energyLevel = 1))
        val variant = AdaptiveAppearance.resolve(logs, today.plusDays(1), zone)
        assertEquals(AppearanceVariant.BASE, variant)
    }

    @Test
    fun `calendar day boundary is evaluated in the given zone, not UTC`() {
        // 00:30 in UTC+3 (Nairobi) on `today` is still late evening UTC the
        // day before — this must be classified as "today" in Nairobi time,
        // not silently misattributed by using UTC underneath.
        val utc = ZoneId.of("UTC")
        val millis = today.atTime(LocalTime.of(0, 30)).atZone(zone).toInstant().toEpochMilli()
        val logs = listOf(log(millis, Mood.STRESSED, energyLevel = 1))

        val variant = AdaptiveAppearance.resolve(logs, today, zone)
        assertEquals(AppearanceVariant.SUBDUED, variant)

        // Same instant, interpreted in UTC, falls on the previous UTC date.
        val utcDate = java.time.Instant.ofEpochMilli(millis).atZone(utc).toLocalDate()
        assertEquals(today.minusDays(1), utcDate)
    }
}
