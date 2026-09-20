package com.example.timesheet.reminders

import android.content.Context
import java.util.Calendar

data class ReminderSettings(
    val dailyEnabled: Boolean = false,
    val dailyHour: Int = 16,
    val dailyMinute: Int = 30,
    val dailyWeekdaysOnly: Boolean = true,
    val weeklyEnabled: Boolean = false,
    val weeklyDay: Int = Calendar.FRIDAY,
    val weeklyHour: Int = 15,
    val weeklyMinute: Int = 0
)

/** Reminder settings, kept in plain SharedPreferences on the device. */
object ReminderStore {
    private const val FILE = "reminders"

    fun load(context: Context): ReminderSettings {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val d = ReminderSettings()
        return ReminderSettings(
            dailyEnabled = p.getBoolean("daily_enabled", d.dailyEnabled),
            dailyHour = p.getInt("daily_hour", d.dailyHour),
            dailyMinute = p.getInt("daily_minute", d.dailyMinute),
            dailyWeekdaysOnly = p.getBoolean("daily_weekdays_only", d.dailyWeekdaysOnly),
            weeklyEnabled = p.getBoolean("weekly_enabled", d.weeklyEnabled),
            weeklyDay = p.getInt("weekly_day", d.weeklyDay),
            weeklyHour = p.getInt("weekly_hour", d.weeklyHour),
            weeklyMinute = p.getInt("weekly_minute", d.weeklyMinute)
        )
    }

    fun save(context: Context, s: ReminderSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putBoolean("daily_enabled", s.dailyEnabled)
            .putInt("daily_hour", s.dailyHour)
            .putInt("daily_minute", s.dailyMinute)
            .putBoolean("daily_weekdays_only", s.dailyWeekdaysOnly)
            .putBoolean("weekly_enabled", s.weeklyEnabled)
            .putInt("weekly_day", s.weeklyDay)
            .putInt("weekly_hour", s.weeklyHour)
            .putInt("weekly_minute", s.weeklyMinute)
            .apply()
    }
}
