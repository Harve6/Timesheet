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
 * Only days that have something in them are listed.
 */
fun buildWeekText(weekStart: Long, entries: List<SiteTimeEntry>): String {
    val rangeFmt = SimpleDateFormat("MMM d", Locale.getDefault())
    val dayFmt = SimpleDateFormat("EEE M/d", Locale.getDefault())

    return buildString {
        appendLine("Hours ${rangeFmt.format(Date(weekStart))} - ${rangeFmt.format(Date(addDays(weekStart, 6)))}")
        entries.sortedBy { it.date }.forEach { e ->
            append(dayFmt.format(Date(e.date)))
            if (e.hoursWorked > 0) append("  ${formatHours(e.hoursWorked)}")
            if (e.siteName.isNotBlank()) append("  ${e.siteName}")
            if (e.travelReimbursed) append(" +Travel")
            if (e.parkingAmount > 0) append(" +Park $${formatMoney(e.parkingAmount)}")
            appendLine()
        }
        append("Total: ${formatHours(entries.sumOf { it.hoursWorked })}")
    }
}

fun formatMoney(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)

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
