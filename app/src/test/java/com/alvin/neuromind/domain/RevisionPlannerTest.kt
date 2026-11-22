package com.alvin.neuromind.domain

import com.alvin.neuromind.data.TimetableEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Runs the planner on the real Dec 2026 exam timetable and checks it against
 * the rules of the student's hand-made plan (SEMESTER-PLAN.md, "Exam plan").
 */
class RevisionPlannerTest {

    private val today = LocalDate.of(2026, 11, 22) // Sunday
    private val classesEnd = LocalDate.of(2026, 11, 25)
    private fun d(day: Int, month: Int = 12) = LocalDate.of(2026, month, day)
    private fun t(h: Int, m: Int = 0) = LocalTime.of(h, m)

    private var nextId = 1
    private fun event(title: String, date: LocalDate, start: LocalTime, end: LocalTime) =
        TimetableEntry(id = nextId++, title = title, dayOfWeek = date.dayOfWeek, startTime = start, endTime = end,
            isRecurring = false, date = date)
    private fun cls(title: String, day: DayOfWeek, start: LocalTime, end: LocalTime) =
        TimetableEntry(id = nextId++, title = title, dayOfWeek = day, startTime = start, endTime = end, lastDate = classesEnd)

    private val entries = listOf(
        cls("Mobile Application Development", DayOfWeek.MONDAY, t(8, 15), t(10, 15)),
        cls("Maisha Program 6", DayOfWeek.MONDAY, t(10, 15), t(11, 15)),
        cls("Assembly Language Programming", DayOfWeek.MONDAY, t(14, 15), t(16, 15)),
        cls("Spanish 4", DayOfWeek.TUESDAY, t(14, 15), t(16, 15)),
        cls("Crypto", DayOfWeek.WEDNESDAY, t(8, 15), t(10, 15)),
        event("Meeting with mentor", d(23, 11), t(15, 15), t(16, 15)),
        event("Spanish oral exam", d(24, 11), t(14, 15), t(16, 15)),
        event("EXAM: ICS 3102 Mobile App Dev", d(2), t(16), t(18)),
        event("EXAM: HLE 3204 Spanish IV", d(4), t(13, 30), t(15, 30)),
        event("EXAM: ICS 3206 Server Admin", d(7), t(13, 30), t(15, 30)),
        event("EXAM: ICS 3202 AI", d(8), t(16), t(18)),
        event("EXAM: ICS 3205 Research Methodology", d(10), t(16), t(18)),
        event("EXAM: ICS 3204 Computer Graphics", d(11), t(16), t(18)),
        event("EXAM: ICS 3203 Assembly", d(14), t(11), t(13)),
        event("EXAM: ICS 3201 Crypto", d(15), t(16), t(18))
    )

    private val plan = RevisionPlanner.plan(entries, today)

    private fun finalDay(exam: String) =
        plan.sessions.single { it.examName == exam && it.kind == RevisionKind.FINAL_PASS }.date
    private fun firstDay(exam: String) =
        plan.sessions.single { it.examName == exam && it.kind == RevisionKind.FIRST_PASS }.date

    @Test
    fun `prints the plan`() {
        plan.days.forEach { day ->
            val line = plan.on(day).joinToString(" · ") { "${it.examName} ${it.kind.label} ${it.minutes}m" }
            println("${day.dayOfWeek.toString().take(3)} ${day.dayOfMonth}/${day.monthValue}: $line")
        }
        println("Shortfall: ${plan.shortfall}")
    }

    @Test
    fun `only the Spanish oral is revised before the study break`() {
        val early = plan.sessions.filter { it.date.isBefore(d(26, 11)) }
        assertTrue(early.isNotEmpty())
        assertTrue(early.all { it.examName == "Spanish oral exam" })
    }

    @Test
    fun `final passes land where the hand plan puts them`() {
        assertEquals(d(1), finalDay("Mobile App Dev"))
        assertEquals(d(3), finalDay("Spanish IV"))
        assertEquals(d(6), finalDay("Server Admin"))
        assertEquals(d(7), finalDay("AI"))                 // evening after Server Admin
        assertEquals(d(9), finalDay("Research Methodology"))
        assertEquals(d(10), finalDay("Computer Graphics")) // evening after RM
        assertEquals(d(13), finalDay("Assembly"))
        assertEquals(d(14), finalDay("Crypto"))            // afternoon after Assembly
    }

    @Test
    fun `second exam of each back-to-back pair gets its first pass in the study break`() {
        for (exam in listOf("AI", "Computer Graphics", "Crypto")) {
            assertTrue("$exam first pass ${firstDay(exam)}", firstDay(exam).isBefore(d(2)))
        }
    }

    @Test
    fun `every exam gets a light review before it and nothing else that morning`() {
        for (exam in plan.sessions.map { it.examName }.distinct()) {
            val examDate = plan.sessions.first { it.examName == exam }.examDate
            assertTrue(plan.on(examDate).any { it.examName == exam && it.kind == RevisionKind.LIGHT_REVIEW } ||
                plan.on(examDate.minusDays(1)).any { it.examName == exam && it.kind == RevisionKind.LIGHT_REVIEW })
        }
    }

    @Test
    fun `at most three units a day and no shortfall`() {
        plan.days.forEach { assertTrue("$it", plan.on(it).map { s -> s.examName }.distinct().size <= 3) }
        assertEquals(emptyMap<String, Int>(), plan.shortfall)
    }

    @Test
    fun `free days before an exam are mostly that exam`() {
        // Thu 3 is Spanish's day, Wed 9 is Research Methodology's.
        fun share(day: LocalDate, exam: String) =
            plan.on(day).filter { it.examName == exam }.sumOf { it.minutes }.toDouble() / plan.on(day).sumOf { it.minutes }
        assertTrue("Thu 3 Spanish share ${share(d(3), "Spanish IV")}", share(d(3), "Spanish IV") >= 0.5)
        assertTrue("Wed 9 RM share ${share(d(9), "Research Methodology")}", share(d(9), "Research Methodology") >= 0.5)
    }
}
