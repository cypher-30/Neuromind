package com.alvin.neuromind.navigation

import android.content.Context
import android.content.Intent
import com.alvin.neuromind.MainActivity

data class AppRouteRequest(
    val route: String,
    val autoStartVoiceCapture: Boolean = false,
    val launchToken: Long = 0L,
    val prefillNote: String? = null,
    val entryId: Int? = null
)

object AppRouteIntents {
    const val EXTRA_ROUTE = "com.alvin.neuromind.extra.ROUTE"
    const val EXTRA_AUTO_VOICE_CAPTURE = "com.alvin.neuromind.extra.AUTO_VOICE_CAPTURE"
    const val EXTRA_LAUNCH_TOKEN = "com.alvin.neuromind.extra.LAUNCH_TOKEN"
    const val EXTRA_PREFILL_NOTE = "com.alvin.neuromind.extra.PREFILL_NOTE"
    const val EXTRA_ENTRY_ID = "com.alvin.neuromind.extra.ROUTE_ENTRY_ID"

    /** Opens the Timetable view with the given event's editor on top (event reminder taps). */
    fun openEvent(context: Context, entryId: Int): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, Screen.Timetable.route)
            putExtra(EXTRA_ENTRY_ID, entryId)
            putExtra(EXTRA_LAUNCH_TOKEN, System.currentTimeMillis())
        }
    }

    fun openFeedback(
        context: Context,
        autoStartVoiceCapture: Boolean,
        prefillNote: String? = null
    ): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, Screen.Feedback.route)
            putExtra(EXTRA_AUTO_VOICE_CAPTURE, autoStartVoiceCapture)
            putExtra(EXTRA_LAUNCH_TOKEN, System.currentTimeMillis())
            putExtra(EXTRA_PREFILL_NOTE, prefillNote)
        }
    }

    fun parse(intent: Intent?): AppRouteRequest? {
        val targetRoute = intent?.getStringExtra(EXTRA_ROUTE) ?: return null
        return AppRouteRequest(
            route = targetRoute,
            autoStartVoiceCapture = intent.getBooleanExtra(EXTRA_AUTO_VOICE_CAPTURE, false),
            launchToken = intent.getLongExtra(EXTRA_LAUNCH_TOKEN, 0L),
            prefillNote = intent.getStringExtra(EXTRA_PREFILL_NOTE),
            entryId = intent.getIntExtra(EXTRA_ENTRY_ID, -1).takeIf { it > 0 }
        )
    }
}

