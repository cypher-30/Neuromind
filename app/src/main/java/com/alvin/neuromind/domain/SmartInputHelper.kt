package com.alvin.neuromind.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Turns a sentence ("go to the bank on Tuesday at 10am", "essay due Dec 4 3pm")
 * into a task title, due date/time and whether it's an errand. Offline and
 * rule-based: each phrase it understands is matched, applied and cut from the
 * title.
 */
object SmartInputHelper {

    private const val DEFAULT_HOUR = 9

    private val months = listOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december"
    )
    private val monthPattern = "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"

    private fun monthNumber(token: String): Int =
        months.indexOfFirst { it.startsWith(token.lowercase().take(3)) } + 1

    fun parseInput(input: String, now: LocalDateTime = LocalDateTime.now()): ParsedTask {
        var title = input
        var date: LocalDate? = null
        var time: LocalTime? = null
        val today = now.toLocalDate()

        /** Finds [regex] in what's left of the title; if found, runs [apply] and cuts the match out. */
        fun take(regex: Regex, apply: (MatchResult) -> Unit): Boolean {
            val m = regex.find(title) ?: return false
            apply(m)
            title = title.removeRange(m.range).let { it.substring(0, m.range.first) + " " + it.substring(m.range.first) }
            return true
        }

        // Relative times: "in 2 hours", "in an hour", "in 30 minutes".
        take(Regex("(?i)\\bin\\s+(an|a|\\d+)\\s+(hours?|hrs?|minutes?|mins?)\\b")) { m ->
            val n = m.groupValues[1].toLongOrNull() ?: 1L
            val at = if (m.groupValues[2].startsWith("h")) now.plusHours(n) else now.plusMinutes(n)
            date = at.toLocalDate(); time = at.toLocalTime().withSecond(0).withNano(0)
        }

        // Parts of today.
        take(Regex("(?i)\\b(tonight|this evening|this afternoon|this morning)\\b")) { m ->
            date = today
            time = when (m.groupValues[1].lowercase()) {
                "tonight" -> LocalTime.of(20, 0)
                "this evening" -> LocalTime.of(18, 0)
                "this afternoon" -> LocalTime.of(14, 0)
                else -> LocalTime.of(DEFAULT_HOUR, 0)
            }
        }

        // "today" / "tomorrow".
        if (date == null) take(Regex("(?i)\\btoday\\b")) { date = today }
        if (date == null) take(Regex("(?i)\\btomorrow\\b")) { date = today.plusDays(1) }

        // "next week" = next Monday.
        if (date == null) take(Regex("(?i)\\bnext\\s+week\\b")) {
            date = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        }

        // "in 3 days", "in 2 weeks".
        if (date == null) take(Regex("(?i)\\bin\\s+(\\d+)\\s+(days?|weeks?)\\b")) { m ->
            val n = m.groupValues[1].toLong()
            date = if (m.groupValues[2].startsWith("w")) today.plusWeeks(n) else today.plusDays(n)
        }

        // "next monday" (the one after this week's), then a bare "monday" (this week's or today).
        if (date == null) take(Regex("(?i)\\bnext\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")) { m ->
            date = today.with(TemporalAdjusters.next(DayOfWeek.valueOf(m.groupValues[1].uppercase())))
        }
        if (date == null) take(Regex("(?i)\\b(?:on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")) { m ->
            date = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.valueOf(m.groupValues[1].uppercase())))
        }

        // Calendar dates: "Dec 4", "December 4th", "4 Dec", "the 4th of December".
        if (date == null) take(Regex("(?i)\\b(?:on\\s+)?$monthPattern\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b")) { m ->
            date = calendarDate(today, monthNumber(m.groupValues[1]), m.groupValues[2].toInt())
        }
        if (date == null) take(Regex("(?i)\\b(?:on\\s+)?(?:the\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?$monthPattern\\b")) { m ->
            date = calendarDate(today, monthNumber(m.groupValues[2]), m.groupValues[1].toInt())
        }
        // "on the 30th": the next time that day of the month comes round.
        if (date == null) take(Regex("(?i)\\b(?:on\\s+)?the\\s+(\\d{1,2})(?:st|nd|rd|th)\\b")) { m ->
            val day = m.groupValues[1].toInt()
            var month = today.withDayOfMonth(1)
            repeat(12) {
                if (day <= month.lengthOfMonth() && !month.withDayOfMonth(day).isBefore(today)) {
                    date = month.withDayOfMonth(day); return@take
                }
                month = month.plusMonths(1)
            }
        }

        // Times: "at 2:45pm", "3pm", "at 14:30", "noon".
        take(Regex("(?i)\\b(?:at\\s+)?noon\\b")) { time = LocalTime.NOON }
        if (time == null) take(Regex("(?i)\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b")) { m ->
            var hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            val amPm = m.groupValues[3].lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) time = LocalTime.of(hour, minute)
        }
        if (time == null) take(Regex("(?i)\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\b")) { m ->
            val hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].toIntOrNull() ?: 0
            if (hour in 0..23 && minute in 0..59) time = LocalTime.of(hour, minute)
        }

        // A time with no day means today (or tomorrow if that time has passed).
        val dueDate: LocalDateTime? = when {
            date != null -> date!!.atTime(time ?: LocalTime.of(DEFAULT_HOUR, 0))
            time != null -> today.atTime(time).let { if (it.isBefore(now)) it.plusDays(1) else it }
            else -> null
        }

        // Clean up common prefixes ("add task:", "I need to", …) and words left
        // dangling where a date was cut out ("go to Upperhill on", "essay due").
        title = title
            .replace(Regex("(?i)^\\s*(remind me to |add task:? |add a task:? |new task:? |task:? |i need to |i have to |i've got to |i gotta |i must )+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(Regex("(?i)(\\s+(on|by|this|at|for|due|the))+$"), "")
            .trim()

        val finalDueDate = dueDate?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val finalTitle = title.ifBlank { "New Task" }

        return ParsedTask(
            title = finalTitle,
            dueDate = finalDueDate,
            // Only meaningful with a day to do it on.
            onDueDay = finalDueDate != null && looksLikeErrand(finalTitle)
        )
    }

    /** This year's date, or next year's if it has passed. Null for impossible dates. */
    private fun calendarDate(today: LocalDate, month: Int, day: Int): LocalDate? {
        if (month !in 1..12) return null
        for (year in listOf(today.year, today.year + 1)) {
            val first = LocalDate.of(year, month, 1)
            if (day in 1..first.lengthOfMonth()) {
                val d = first.withDayOfMonth(day)
                if (!d.isBefore(today)) return d
            }
        }
        return null
    }

    // Errands and appointments: things you go and do on the day rather than
    // work you can start early. Verbs must start the title; places and
    // appointment words can be anywhere.
    private val errandVerbs = Regex(
        "(?i)^(go( to)?|visit|pick up|collect|drop off|sign|meet|see|attend|buy|pay|book|call|return|renew|get my|fetch)\\b"
    )
    private val errandWords = Regex(
        "(?i)\\b(appointment|errand|doctor|dentist|hospital|clinic|medical|bank|salon|barber|interview|meeting|pharmacy|embassy|office hours)\\b"
    )

    fun looksLikeErrand(title: String): Boolean =
        errandVerbs.containsMatchIn(title.trim()) || errandWords.containsMatchIn(title)
}

/** [onDueDay]: the text reads like an errand/appointment for that day (see [SmartInputHelper.looksLikeErrand]). */
data class ParsedTask(val title: String, val dueDate: Long?, val onDueDay: Boolean = false)
