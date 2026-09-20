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

/** 8.0 -> "8", 8.5 -> "8.5" */
fun formatHours(hours: Double): String =
    if (hours % 1.0 == 0.0) hours.toLong().toString() else String.format(Locale.US, "%.1f", hours)

/**
 * Plain-text week summary meant to be pasted into a text message.
 * [weekStart] is the Monday (start of day) of the week being summarized.
 */
fun buildWeekText(weekStart: Long, entries: List<SiteTimeEntry>): String {
    val rangeFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    val dayFmt = SimpleDateFormat("EEE M/d", Locale.getDefault())
    val end = Calendar.getInstance().apply {
        timeInMillis = weekStart
        add(Calendar.DAY_OF_YEAR, 6)
    }.timeInMillis

    return buildString {
        appendLine("Hours for ${rangeFmt.format(Date(weekStart))} - ${rangeFmt.format(Date(end))}")
        entries.sortedBy { it.date }.forEach { e ->
            append(dayFmt.format(Date(e.date)))
            append(": ${formatHours(e.hoursWorked)}h")
            if (e.siteName.isNotBlank()) append(" - ${e.siteName}")
            if (e.jobNumber.isNotBlank()) append(" (Job ${e.jobNumber})")
            if (e.workSummary.isNotBlank()) append(" - ${e.workSummary}")
            if (e.parkingAmount > 0) append(" - Parking $${String.format(Locale.US, "%.2f", e.parkingAmount)}")
            if (e.travelReimbursed) append(" - Travel")
            appendLine()
        }
        append("Total: ${formatHours(entries.sumOf { it.hoursWorked })}h")
    }
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

/** Material date picker works in UTC; convert its result to a local-noon timestamp. */
fun pickerUtcToLocalNoon(utcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
    }.timeInMillis
}

fun localToPickerUtc(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}
