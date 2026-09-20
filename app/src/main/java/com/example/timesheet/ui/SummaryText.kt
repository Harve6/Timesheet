package com.example.timesheet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.timesheet.data.SiteTimeEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 8.0 -> "8", 8.5 -> "8.5", 8.25 -> "8.25" */
fun formatHours(hours: Double): String = trimNumber(hours)

fun formatMoney(amount: Double): String = trimNumber(amount)

private fun trimNumber(value: Double): String =
    java.math.BigDecimal(value).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/**
 * Plain-text week for pasting into a text message. One line per day, all seven
 * days listed, e.g. "Tue  9.5  Main St +Travel +Park $10".
 */
fun buildWeekText(weekStart: Long, entries: List<SiteTimeEntry>): String {
    val dayFmt = SimpleDateFormat("EEE", Locale.getDefault())
    val byDay = entries.groupBy { localNoon(it.date) }

    return buildString {
        for (i in 0..6) {
            val date = localNoon(addDays(weekStart, i))
            val day = byDay[date].orEmpty()
            val hours = day.sumOf { it.hoursWorked }
            val site = day.map { it.siteName }.filter { it.isNotBlank() }.distinct().joinToString(" / ")
            val parking = day.sumOf { it.parkingAmount }

            append(dayFmt.format(Date(date)))
            append("  ")
            if (hours > 0) append(formatHours(hours))
            if (hours > 0 && site.isNotEmpty()) append("  ")
            append(site)
            if (day.any { it.travelReimbursed }) append(" +Travel")
            if (parking > 0) append(" +Park $${formatMoney(parking)}")
            appendLine()
        }
    }.trimEnd()
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Timesheet", text))
    Toast.makeText(context, "Copied! Paste it into a text.", Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Send hours"))
}

/** Noon of the local day, so a date never drifts across a day boundary. */
fun localNoon(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 12)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
