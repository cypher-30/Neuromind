package com.alvin.neuromind.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TeamsMeetingHelperTest {

    @Test
    fun `parseMeeting extracts teams url`() {
        val parsed = TeamsMeetingHelper.parseMeeting(
            "Join here: https://teams.microsoft.com/l/meetup-join/abc123?context=xyz"
        )

        assertEquals("https://teams.microsoft.com/l/meetup-join/abc123?context=xyz", parsed.joinUrl)
    }

    @Test
    fun `parseMeeting extracts id and passcode from standard labels`() {
        val parsed = TeamsMeetingHelper.parseMeeting(
            "Meeting ID: 123 456 789 012\nPasscode: Abc123"
        )

        assertEquals("123456789012", parsed.meetingId)
        assertEquals("Abc123", parsed.passcode)
    }

    @Test
    fun `parseMeeting extracts id and pass from short labels`() {
        val parsed = TeamsMeetingHelper.parseMeeting(
            "ID - 987654321\nPass: q1w2e3"
        )

        assertEquals("987654321", parsed.meetingId)
        assertEquals("q1w2e3", parsed.passcode)
    }

    @Test
    fun `parseMeeting returns empty when no meeting details exist`() {
        val parsed = TeamsMeetingHelper.parseMeeting("Lecture details only")

        assertNull(parsed.joinUrl)
        assertNull(parsed.meetingId)
        assertNull(parsed.passcode)
    }
}

