package com.example.timesheet.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.timesheet.R
import com.example.timesheet.data.TimesheetDatabase
import com.example.timesheet.ui.WeekSettings
import com.example.timesheet.ui.addDays
import com.example.timesheet.ui.formatHours
import com.example.timesheet.ui.weekStartOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/** Fires when a reminder alarm goes off: posts the notification, then sets the next alarm. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext
        WeekSettings.load(app) // the process may have started just for this alarm
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ReminderScheduler.ACTION_DAILY -> notifyDaily(app)
                    ReminderScheduler.ACTION_WEEKLY -> notifyWeekly(app)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ReminderScheduler.scheduleAll(app)
                pending.finish()
            }
        }
    }

    private suspend fun notifyDaily(context: Context) {
        val now = System.currentTimeMillis()
        val dao = TimesheetDatabase.getDatabase(context).timesheetDao()
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val loggedToday = dao.getEntriesOnce(start, addDays(start, 1) - 1).any { it.hoursWorked > 0 }
        if (loggedToday) return // already done, no nagging
        show(context, id = 1, title = "Log today's hours", text = "Takes 10 seconds. Tap to open TradeHours.")
    }

    private suspend fun notifyWeekly(context: Context) {
        val dao = TimesheetDatabase.getDatabase(context).timesheetDao()
        val start = weekStartOf(System.currentTimeMillis())
        val total = dao.getEntriesOnce(start, addDays(start, 7) - 1).sumOf { it.hoursWorked }
        val text = if (total > 0) {
            "This week: ${formatHours(total)} hrs. Tap to open, then COPY and send."
        } else {
            "No hours logged yet this week. Tap to open TradeHours."
        }
        show(context, id = 2, title = "Time to send your hours", text = text)
    }

    @SuppressLint("MissingPermission") // checked in canNotify()
    private fun show(context: Context, id: Int, title: String, text: String) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val tap = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_clock)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    companion object {
        const val CHANNEL_ID = "reminders"

        fun canNotify(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        fun ensureChannel(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                        description = "Daily and weekly reminders to log and send your hours"
                    }
                )
            }
        }
    }
}
