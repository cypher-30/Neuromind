package com.alvin.neuromind.domain

import com.alvin.neuromind.data.TimetableEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

enum class RevisionKind(val label: String, val hint: String) {
    FIRST_PASS("First pass", "Cold past-paper Q1 first (no notes), mark it, then work on the weak topics it shows"),
    PRACTICE("Practice", "A timed past paper or blank-page recall: test yourself, don't re-read"),
    FINAL_PASS("Final pass", "Weak areas only: redo what you got wrong, recite the bankers"),
    LIGHT_REVIEW("Light review", "Skim your notes and summaries. No new material")
}

/** One block of revision for one exam on one day. */
data class RevisionSession(
    val date: LocalDate,
    val examName: String,
    val examEntryId: Int,
    val examDate: LocalDate,
    val kind: RevisionKind,
    val minutes: Int
)

data class RevisionPlan(
    val sessions: List<RevisionSession>,
    /** Minutes per exam that didn't fit before it: a sign of overload. */
    val shortfall: Map<String, Int>
) {
    fun on(date: LocalDate): List<RevisionSession> = sessions.filter { it.date == date }
    val days: List<LocalDate> get() = sessions.map { it.date }.distinct().sorted()
}

/**
 * Tunable knobs. Defaults follow the student's own exam plan: ~6 h on a study
 * day, 2–3 units a day, a light review before each exam and the next unit in
 * the evening after it.
 */
data class RevisionSettings(
    val dayStart: LocalTime = LocalTime.of(8, 0),
    val dayEnd: LocalTime = LocalTime.of(21, 0),
    /** Study cap on a day with no exam. */
    val studyDayMinutes: Int = 360,
    /** Total revision per exam in the study window. */
    val minutesPerExam: Int = 480,
    /** Short exams (orals, CATs) before classes end get this much, in small sessions. */
    val earlyExamMinutes: Int = 120,
    val maxUnitsPerDay: Int = 3,
    /** Travel/settling time kept free before and after an exam. */
    val examBufferMinutes: Int = 60,
    val preExamReviewMinutes: Int = 90,
    val eveningAfterExamMinutes: Int = 120,
    /** A free day is topped up to about this much revision. */
    val topUpMinutes: Int = 240
)

/**
 * Offline revision planner: spaced, interleaved, deadline-aware.
 *
 * 1. Every exam gets a light review on its own day (before it), or the
 *    evening before when the exam is too early in the morning.
 * 2. A final pass on the latest free time before the exam (often the
 *    evening after the previous exam when two exams sit back to back).
 * 3. A first pass as early as possible, ideally 2+ days before the final pass
 *    (spacing), earliest exam first.
 * 4. The rest as practice sessions, day by day, to the exam under most
 *    pressure (minutes still needed ÷ free minutes left before it), at most
 *    [RevisionSettings.maxUnitsPerDay] units a day (interleaving). A unit can
 *    fill a day on its own only right before its exam.
 *
 * Revision for exams after the last day of classes starts the day after it
 * (the study break); exams before that (orals, CATs) get short sessions in
 * the days just before them.
 */
object RevisionPlanner {

    private class Day(val date: LocalDate, val exam: UpcomingExam?) {
        var pre = 0   // before today's exam, for that exam only
        var main = 0  // the rest of the day (after the exam on an exam day)
        val units = linkedSetOf<String>()
    }

    private class Need(val exam: UpcomingExam, var remaining: Int) {
        var firstDay: LocalDate? = null
        var finalDay: LocalDate? = null
    }

    fun plan(
        entries: List<TimetableEntry>,
        today: LocalDate,
        settings: RevisionSettings = RevisionSettings()
    ): RevisionPlan {
        val exams = WidgetInsights.upcomingExams(entries, today.atStartOfDay())
        if (exams.isEmpty()) return RevisionPlan(emptyList(), emptyMap())

        val classesEnd = entries.filter { it.isRecurring }.mapNotNull { it.lastDate }.maxOrNull()
        val studyStart = if (classesEnd != null && !classesEnd.isBefore(today)) classesEnd.plusDays(1) else today
        val lastExam = exams.maxOf { it.date }

        val days = generateSequence(today) { it.plusDays(1) }
            .takeWhile { !it.isAfter(lastExam) }
            .map { date -> Day(date, exams.firstOrNull { it.date == date }).also { fillCapacity(it, entries, settings) } }
            .associateBy { it.date }

        val sessions = mutableListOf<RevisionSession>()
        // Share most of the free study time across the exams (85%, leaving
        // slack for a bad day), but never less than the default per exam.
        val studyExamCount = exams.count { !it.date.isBefore(studyStart) }.coerceAtLeast(1)
        val studyCapacity = days.values.filter { !it.date.isBefore(studyStart) }.sumOf { it.main + it.pre }
        val perExam = (maxOf(settings.minutesPerExam, (studyCapacity * 0.8 / studyExamCount).toInt()) / 60 * 60)
            .coerceAtMost(900) // whole hours, so sessions don't end in odd scraps
        val needs = exams.map { exam ->
            val early = exam.date.isBefore(studyStart)
            Need(exam, if (early) settings.earlyExamMinutes else perExam)
        }

        fun add(day: Day, need: Need, kind: RevisionKind, minutes: Int, fromPre: Boolean = false) {
            if (minutes <= 0) return
            sessions += RevisionSession(day.date, need.exam.name, need.exam.entry.id, need.exam.date, kind, minutes)
            if (fromPre) day.pre -= minutes else day.main -= minutes
            day.units += need.exam.name
            need.remaining -= minutes
        }

        /** A practice block: up to 2 h, but a small remainder joins this block instead of trailing on its own. */
        fun practiceMinutes(day: Day, need: Need): Int =
            if (need.remaining <= 150) minOf(need.remaining, day.main) else minOf(120, day.main)

        fun canTake(day: Day, need: Need, min: Int): Boolean =
            day.main >= min && (need.exam.name in day.units || day.units.size < settings.maxUnitsPerDay) &&
                day.date.isBefore(need.exam.date) && (day.exam == null || day.exam.name != need.exam.name)

        // 1. Light review on the exam day, else the evening before.
        for (need in needs) {
            val day = days.getValue(need.exam.date)
            // Short early exams (an oral) keep most of their time for practice beforehand.
            val cap = if (need.exam.date.isBefore(studyStart)) 30 else 60
            val minutes = minOf(day.pre, cap, need.remaining)
            if (minutes >= 30) add(day, need, RevisionKind.LIGHT_REVIEW, minutes, fromPre = true)
            else days[need.exam.date.minusDays(1)]?.takeIf { canTake(it, need, 30) }?.let {
                add(it, need, RevisionKind.LIGHT_REVIEW, minOf(60, it.main, need.remaining))
            }
        }

        // Early exams (before the study break): short sessions on the days before.
        for (need in needs.filter { it.exam.date.isBefore(studyStart) }) {
            var date = need.exam.date.minusDays(1)
            while (need.remaining >= 15 && !date.isBefore(today)) {
                days[date]?.takeIf { canTake(it, need, 15) }?.let {
                    add(it, need, RevisionKind.PRACTICE, minOf(45, it.main, need.remaining))
                }
                date = date.minusDays(1)
            }
        }
        val study = needs.filter { !it.exam.date.isBefore(studyStart) }
        val studyDays = days.values.filter { !it.date.isBefore(studyStart) }.sortedBy { it.date }

        // 2. Final pass: the latest free time before each exam.
        for (need in study.sortedBy { it.exam.date }) {
            val day = studyDays.filter { it.date.isBefore(need.exam.date) }.lastOrNull { canTake(it, need, 60) } ?: continue
            add(day, need, RevisionKind.FINAL_PASS, minOf(90, day.main, need.remaining))
            need.finalDay = day.date
        }

        // 3. First pass: as early as possible, ideally 2+ days before the final pass.
        for (need in study.sortedBy { it.exam.date }) {
            val latest = need.finalDay ?: need.exam.date
            val spaced = studyDays.firstOrNull { it.date.isBefore(latest.minusDays(1)) && canTake(it, need, 60) }
            val day = spaced ?: studyDays.firstOrNull { it.date.isBefore(latest) && canTake(it, need, 60) } ?: continue
            add(day, need, RevisionKind.FIRST_PASS, minOf(90, day.main, need.remaining))
            need.firstDay = day.date
        }

        // 4a. Practice close to each exam: walk back from its final pass, one
        // session a day (two on the last free day before it), earliest exam
        // first. This keeps timed papers near the exam they're for.
        for (need in study.sortedBy { it.exam.date }) {
            val first = need.firstDay ?: continue
            val from = need.finalDay ?: need.exam.date
            for (day in studyDays.filter { it.date.isAfter(first) && !it.date.isAfter(from) }.reversed()) {
                if (need.remaining < 30) break
                val lastFreeDay = ChronoUnit.DAYS.between(day.date, need.exam.date) <= 1
                val already = sessions.count { it.date == day.date && it.examName == need.exam.name }
                if (already >= (if (lastFreeDay) 2 else 1) || !canTake(day, need, 45)) continue
                add(day, need, RevisionKind.PRACTICE, practiceMinutes(day, need))
            }
        }

        // 4b. Whatever is left: day by day, to the exam under the most pressure.
        for (day in studyDays) {
            while (day.main >= 45) {
                val open = study.filter { need ->
                    val first = need.firstDay
                    need.remaining >= 30 && first != null && !first.isAfter(day.date) && canTake(day, need, 45)
                }
                // Prefer a unit not yet studied today (spacing), unless its exam is
                // tomorrow; only when none fits does a unit get a second block.
                val spaced = open.filter { need ->
                    (need.exam.name !in day.units || ChronoUnit.DAYS.between(day.date, need.exam.date) <= 1) &&
                        need.firstDay!!.isBefore(day.date)
                }
                val candidates = spaced.ifEmpty { open }
                val pick = candidates.maxByOrNull { need ->
                    val left = studyDays.filter { !it.date.isBefore(day.date) && it.date.isBefore(need.exam.date) }.sumOf { it.main }
                    need.remaining.toDouble() / left.coerceAtLeast(1)
                } ?: break
                add(day, pick, RevisionKind.PRACTICE, practiceMinutes(day, pick))
            }
        }

        // 5. Top-up: a free (non-exam) day shouldn't sit nearly empty while exams
        // loom. Bring it to ~4 h with extra practice for units already started,
        // nearest exam first, one block per unit per day.
        for (day in studyDays.filter { it.exam == null }) {
            var planned = sessions.filter { it.date == day.date }.sumOf { it.minutes }
            while (planned < settings.topUpMinutes && day.main >= 45) {
                val pick = study
                    .filter { need ->
                        val first = need.firstDay
                        first != null && first.isBefore(day.date) && need.exam.name !in day.units && canTake(day, need, 45)
                    }
                    .minByOrNull { it.exam.date } ?: break
                val minutes = minOf(120, day.main, (settings.topUpMinutes - planned).coerceAtLeast(60))
                add(day, pick, RevisionKind.PRACTICE, minutes)
                planned += minutes
            }
        }

        val shortfall = needs.filter { it.remaining >= 30 }.associate { it.exam.name to it.remaining }
        // Within a day: the pre-exam review first, then the rest in exam order.
        val ordered = sessions.sortedWith(
            compareBy<RevisionSession>({ it.date }, { if (it.date == it.examDate) 0 else 1 }, { it.examDate })
        )
        return RevisionPlan(ordered, shortfall)
    }

    /** Free minutes in the day window around timetable blocks, split around a same-day exam. */
    private fun fillCapacity(day: Day, entries: List<TimetableEntry>, s: RevisionSettings) {
        val todays = entries.filter { it.occursOn(day.date) }
        // An all-day event (a trip, a careers fair) takes the whole day.
        if (todays.any { it.isAllDay && !it.isRecurring }) return
        val busy = todays.filter { !it.isAllDay }.map { it.startTime to it.endTime }
        val exam = day.exam
        if (exam == null) {
            // Long commitments tire you out: fixed time beyond 2 h comes off the
            // study day (an 8 h workshop leaves an evening hour), never below 1 h.
            val committed = (ChronoUnit.MINUTES.between(s.dayStart, s.dayEnd).toInt() - free(s.dayStart, s.dayEnd, busy))
            val energy = (s.studyDayMinutes + 120 - committed).coerceAtLeast(60)
            day.main = minOf(s.studyDayMinutes, energy, free(s.dayStart, s.dayEnd, busy))
        } else {
            val preEnd = exam.entry.startTime.minusMinutes(s.examBufferMinutes.toLong())
            val postStart = exam.entry.endTime.plusMinutes(s.examBufferMinutes.toLong())
            day.pre = if (preEnd.isAfter(s.dayStart)) minOf(s.preExamReviewMinutes, free(s.dayStart, preEnd, busy)) else 0
            day.main = if (postStart.isBefore(s.dayEnd) && postStart.isAfter(exam.entry.endTime))
                minOf(s.eveningAfterExamMinutes, free(postStart, s.dayEnd, busy)) else 0
        }
    }

    private fun free(from: LocalTime, to: LocalTime, busy: List<Pair<LocalTime, LocalTime>>): Int {
        if (!to.isAfter(from)) return 0
        val clipped = busy.mapNotNull { (a, b) ->
            val s = maxOf(a, from); val e = minOf(b, to)
            if (e.isAfter(s)) s to e else null
        }.sortedBy { it.first }
        var taken = 0L
        var cursor = from
        for ((s, e) in clipped) {
            val start = maxOf(s, cursor)
            if (e.isAfter(start)) { taken += ChronoUnit.MINUTES.between(start, e); cursor = e }
        }
        return (ChronoUnit.MINUTES.between(from, to) - taken).toInt()
    }
}
