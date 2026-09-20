package com.example.timesheet.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object ReminderScheduler {
    const val ACTION_DAILY = "com.harve6.timesheet.REMINDER_DAILY"
    const val ACTION_WEEKLY = "com.harve6.timesheet.REMINDER_WEEKLY"

    /** Next time at [hour]:[minute] after [now], on a weekday if [weekdaysOnly]. */
    fun nextDailyTrigger(now: Long, hour: Int, minute: Int, weekdaysOnly: Boolean): Long {
        val cal = timeOn(now, hour, minute)
        if (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, 1)
        while (weekdaysOnly && cal.get(Calendar.DAY_OF_WEEK).let { it == Calendar.SATURDAY || it == Calendar.SUNDAY }) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /** Next [dayOfWeek] (Calendar.SUNDAY..SATURDAY) at [hour]:[minute] after [now]. */
    fun nextWeeklyTrigger(now: Long, dayOfWeek: Int, hour: Int, minute: Int): Long {
        val cal = timeOn(now, hour, minute)
        while (cal.timeInMillis <= now || cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun timeOn(millis: Long, hour: Int, minute: Int): Calendar =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    /** Sets or cancels both alarms to match the saved settings. Safe to call any time. */
    fun scheduleAll(context: Context) {
        val s = ReminderStore.load(context)
        val now = System.currentTimeMillis()
        if (s.dailyEnabled) {
            set(context, ACTION_DAILY, nextDailyTrigger(now, s.dailyHour, s.dailyMinute, s.dailyWeekdaysOnly))
        } else {
            cancel(context, ACTION_DAILY)
        }
        if (s.weeklyEnabled) {
            set(context, ACTION_WEEKLY, nextWeeklyTrigger(now, s.weeklyDay, s.weeklyHour, s.weeklyMinute))
        } else {
            cancel(context, ACTION_WEEKLY)
        }
    }

    private fun pendingIntent(context: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            if (action == ACTION_DAILY) 1 else 2,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun set(context: Context, action: String, triggerAt: Long) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Inexact on purpose: a reminder a few minutes late is fine and needs no special permission.
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context, action))
    }

    private fun cancel(context: Context, action: String) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarms.cancel(pendingIntent(context, action))
    }
}
