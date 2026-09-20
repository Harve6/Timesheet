package com.example.timesheet.ui

import android.content.Context
import java.util.Calendar

/**
 * Which day the pay week starts on. Kept in memory so [weekStartOf] can read it from anywhere,
 * and saved in SharedPreferences. Call [load] before using it (activity start, receivers).
 */
object WeekSettings {
    private const val FILE = "settings"
    private const val KEY = "week_start_day"

    /** Calendar.SUNDAY, Calendar.MONDAY or Calendar.SATURDAY. */
    val choices = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.SATURDAY)

    @Volatile
    var startDay: Int = Calendar.SUNDAY
        private set

    fun load(context: Context) {
        val saved = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY, Calendar.SUNDAY)
        startDay = if (saved in choices) saved else Calendar.SUNDAY
    }

    fun save(context: Context, day: Int) {
        if (day !in choices) return
        startDay = day
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY, day).apply()
    }
}
