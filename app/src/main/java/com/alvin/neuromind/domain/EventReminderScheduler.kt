package com.alvin.neuromind.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alvin.neuromind.data.TimetableEntry
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules one AlarmManager alarm per dated event. Exact alarms are used when
 * Android grants "Alarms & reminders" access; otherwise the alarm is still set
 * (inexact, may arrive late) and the editor tells the user so.
 */
class EventReminderScheduler(context: Context) {

    enum class Status { SCHEDULED_EXACT, SCHEDULED_INEXACT, NONE, IN_PAST }

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** What [schedule] would do for this trigger, without touching AlarmManager. */
    fun preview(trigger: LocalDateTime?, now: LocalDateTime = LocalDateTime.now()): Status = when {
        trigger == null -> Status.NONE
        !trigger.isAfter(now) -> Status.IN_PAST
        canScheduleExact() -> Status.SCHEDULED_EXACT
        else -> Status.SCHEDULED_INEXACT
    }

    fun schedule(entry: TimetableEntry): Status {
        if (entry.id <= 0) return Status.NONE
        val trigger = EventReminderPolicy.triggerAt(entry)
        val status = preview(trigger)
        if (status == Status.NONE || status == Status.IN_PAST) {
            cancel(entry.id)
            return status
        }
        val triggerMillis = trigger!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = firePendingIntent(entry.id, triggerMillis)
        try {
            if (status == Status.SCHEDULED_EXACT) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            }
        } catch (_: SecurityException) {
            // Exact access was revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
            return Status.SCHEDULED_INEXACT
        }
        return status
    }

    fun cancel(entryId: Int) {
        val intent = Intent(appContext, EventReminderReceiver::class.java).setAction(EventReminderReceiver.ACTION_FIRE)
        val existing = PendingIntent.getBroadcast(
            appContext,
            REQUEST_BASE + entryId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        existing?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    fun rescheduleAll(entries: List<TimetableEntry>) {
        entries.forEach { schedule(it) }
    }

    /** Wakes (inexactly) just after midnight so "today" widgets flip to the new day. */
    fun scheduleMidnightRefresh() {
        val zone = ZoneId.systemDefault()
        val nextMidnight = LocalDate.now().plusDays(1).atStartOfDay(zone).plusMinutes(1).toInstant().toEpochMilli()
        val intent = Intent(appContext, EventReminderReceiver::class.java).setAction(EventReminderReceiver.ACTION_MIDNIGHT)
        val pending = PendingIntent.getBroadcast(
            appContext,
            MIDNIGHT_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.set(AlarmManager.RTC, nextMidnight, pending)
    }

    private fun firePendingIntent(entryId: Int, triggerMillis: Long): PendingIntent {
        val intent = Intent(appContext, EventReminderReceiver::class.java)
            .setAction(EventReminderReceiver.ACTION_FIRE)
            .putExtra(EventReminderReceiver.EXTRA_ENTRY_ID, entryId)
            .putExtra(EventReminderReceiver.EXTRA_TRIGGER_AT, triggerMillis)
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_BASE + entryId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_BASE = 400_000
        private const val MIDNIGHT_REQUEST = 399_999
    }
}
