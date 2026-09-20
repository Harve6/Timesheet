package com.example.timesheet

import com.example.timesheet.reminders.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min, 0) }.timeInMillis

    private fun dow(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_WEEK)

    // Sep 17 2025 is a Wednesday.
    @Test
    fun daily_laterTodayFires_today() {
        val now = at(2025, 9, 17, 10, 0)
        assertEquals(at(2025, 9, 17, 16, 30), ReminderScheduler.nextDailyTrigger(now, 16, 30, weekdaysOnly = true))
    }

    @Test
    fun daily_alreadyPassedToday_firesTomorrow() {
        val now = at(2025, 9, 17, 17, 0)
        assertEquals(at(2025, 9, 18, 16, 30), ReminderScheduler.nextDailyTrigger(now, 16, 30, weekdaysOnly = true))
    }

    @Test
    fun daily_weekdaysOnly_skipsWeekend() {
        // Friday evening after the time -> Monday.
        val now = at(2025, 9, 19, 17, 0)
        val next = ReminderScheduler.nextDailyTrigger(now, 16, 30, weekdaysOnly = true)
        assertEquals(at(2025, 9, 22, 16, 30), next)
        assertEquals(Calendar.MONDAY, dow(next))
    }

    @Test
    fun daily_everyDay_includesWeekend() {
        val now = at(2025, 9, 19, 17, 0)
        assertEquals(at(2025, 9, 20, 16, 30), ReminderScheduler.nextDailyTrigger(now, 16, 30, weekdaysOnly = false))
    }

    @Test
    fun weekly_picksNextMatchingDay() {
        val now = at(2025, 9, 17, 10, 0) // Wednesday
        val next = ReminderScheduler.nextWeeklyTrigger(now, Calendar.FRIDAY, 15, 0)
        assertEquals(at(2025, 9, 19, 15, 0), next)
    }

    @Test
    fun weekly_sameDayButPassed_goesToNextWeek() {
        val now = at(2025, 9, 19, 16, 0) // Friday after 3pm
        val next = ReminderScheduler.nextWeeklyTrigger(now, Calendar.FRIDAY, 15, 0)
        assertEquals(at(2025, 9, 26, 15, 0), next)
    }
}
