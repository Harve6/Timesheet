package com.example.timesheet

import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.ui.addDays
import com.example.timesheet.ui.buildWeekText
import com.example.timesheet.ui.formatHours
import com.example.timesheet.ui.localNoon
import com.example.timesheet.ui.weekStartOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class SummaryTextTest {

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
    }

    private fun date(y: Int, m: Int, d: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, 12, 0, 0) }.timeInMillis

    private fun entry(day: Long, hours: Double, site: String = "", travel: Boolean = false, parking: Double = 0.0) =
        SiteTimeEntry(
            date = day, siteName = site, siteAddress = "", jobNumber = "", hoursWorked = hours,
            workSummary = "", travelReimbursed = travel, parkingAmount = parking
        )

    @Test
    fun formatHours_dropsTrailingZerosAndKeepsTwoDecimals() {
        assertEquals("8", formatHours(8.0))
        assertEquals("8.5", formatHours(8.5))
        assertEquals("8.25", formatHours(8.25))
        assertEquals("0", formatHours(0.0))
    }

    @Test
    fun weekStart_isSunday() {
        // Wed Sep 17 2025 -> Sun Sep 14 2025
        assertEquals(date(2025, 9, 14) - 12 * 3600_000L, weekStartOf(date(2025, 9, 17)))
        // A Sunday is its own week start; a Saturday belongs to the week before it.
        assertEquals(date(2025, 9, 14) - 12 * 3600_000L, weekStartOf(date(2025, 9, 14)))
        assertEquals(date(2025, 9, 14) - 12 * 3600_000L, weekStartOf(date(2025, 9, 20)))
    }

    @Test
    fun weekText_matchesOriginalPipePatternsFormat() {
        val start = weekStartOf(date(2025, 9, 17))
        val entries = listOf(
            entry(localNoon(addDays(start, 1)), 8.0, "Main St Hospital"),
            entry(localNoon(addDays(start, 2)), 9.5, "Main St Hospital", travel = true, parking = 10.0),
            entry(localNoon(addDays(start, 4)), 8.0, "Airport Job", parking = 4.5)
        )

        // Empty days keep their two trailing spaces, like the original screen.
        val expected = listOf(
            "Sun  ",
            "Mon  8  Main St Hospital",
            "Tue  9.5  Main St Hospital +Travel +Park \$10",
            "Wed  ",
            "Thu  8  Airport Job +Park \$4.5",
            "Fri  ",
            "Sat"
        ).joinToString("\n")

        assertEquals(expected, buildWeekText(start, entries))
    }

    @Test
    fun weekText_multipleEntriesOnOneDayAreMerged() {
        val start = weekStartOf(date(2025, 9, 17))
        val monday = localNoon(addDays(start, 1))
        val text = buildWeekText(start, listOf(entry(monday, 4.0, "Site A"), entry(monday, 4.0, "Site B")))
        assertEquals("Mon  8  Site A / Site B", text.lines()[1])
    }
}
