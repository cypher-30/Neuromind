package com.alvin.neuromind.domain

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

private const val TEAMS_PACKAGE = "com.microsoft.teams"

data class ParsedTeamsMeeting(
    val joinUrl: String? = null,
    val meetingId: String? = null,
    val passcode: String? = null
) {
    val hasLaunchAction: Boolean
        get() = !joinUrl.isNullOrBlank() || !meetingId.isNullOrBlank()
}

object TeamsMeetingHelper {
    private val teamsUrlRegex = Regex("https?://teams\\.microsoft\\.com/\\S+", RegexOption.IGNORE_CASE)
    private val meetingIdRegex = Regex(
        """(?im)\b(?:meeting\s*(?:id|number)|id)\b\s*[:=\-]?\s*([a-z0-9][a-z0-9\s-]{5,})"""
    )
    private val passcodeRegex = Regex(
        """(?im)\b(?:pass\s*code|passcode|password|pwd|pass)\b\s*[:=\-]?\s*([a-z0-9][a-z0-9-]{2,})"""
    )

    fun parseMeeting(vararg textBlocks: String?): ParsedTeamsMeeting {
        val combined = textBlocks
            .filterNotNull()
            .joinToString("\n")
            .trim()

        if (combined.isBlank()) return ParsedTeamsMeeting()

        val joinUrl = teamsUrlRegex.find(combined)?.value?.trim()?.trimEnd(')', '.', ',')
        val meetingId = normalizeMeetingId(meetingIdRegex.find(combined)?.groupValues?.getOrNull(1))
        val passcode = normalizePasscode(passcodeRegex.find(combined)?.groupValues?.getOrNull(1))

        return ParsedTeamsMeeting(joinUrl = joinUrl, meetingId = meetingId, passcode = passcode)
    }

    fun launchMeeting(context: Context, meeting: ParsedTeamsMeeting) {
        if (!meeting.joinUrl.isNullOrBlank()) {
            val teamsDeepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meeting.joinUrl)).apply {
                setPackage(TEAMS_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(teamsDeepLinkIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // Teams app may be missing; try generic intent next.
            }

            val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(meeting.joinUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(deepLinkIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // Fall through to app-launch fallback.
            }
        }

        if (!meeting.meetingId.isNullOrBlank()) {
            copyCredentials(context, meeting)
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TEAMS_PACKAGE)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                context.startActivity(launchIntent)
                Toast.makeText(context, "Meeting details copied. Paste in Teams Join screen.", Toast.LENGTH_LONG).show()
                return
            }
        }

        Toast.makeText(context, "No Teams link found for this class.", Toast.LENGTH_SHORT).show()
    }

    private fun copyCredentials(context: Context, meeting: ParsedTeamsMeeting) {
        if (meeting.meetingId.isNullOrBlank()) return
        val payload = buildString {
            append("Meeting ID: ")
            append(meeting.meetingId)
            if (!meeting.passcode.isNullOrBlank()) {
                append("\nPasscode: ")
                append(meeting.passcode)
            }
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Teams meeting", payload))
    }

    private fun normalizeMeetingId(raw: String?): String? {
        val cleaned = raw
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            ?.replace(Regex("[^A-Za-z0-9]"), "")
            .orEmpty()
        return cleaned.takeIf { it.length >= 6 }
    }

    private fun normalizePasscode(raw: String?): String? {
        val cleaned = raw
            ?.lineSequence()
            ?.firstOrNull()
            ?.trim()
            ?.replace(Regex("[^A-Za-z0-9]"), "")
            .orEmpty()
        return cleaned.takeIf { it.length >= 3 }
    }
}

