package com.example.timesheet.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.timesheet.reminders.ReminderReceiver
import com.example.timesheet.reminders.ReminderScheduler
import com.example.timesheet.reminders.ReminderSettings
import com.example.timesheet.reminders.ReminderStore
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.WeekSettings
import com.example.timesheet.ui.theme.AppearanceSettings
import com.example.timesheet.ui.theme.TextSize
import com.example.timesheet.ui.theme.ThemeMode
import com.example.timesheet.ui.theme.bar
import com.example.timesheet.ui.theme.onBar
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

private enum class Which { DAILY, WEEKLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: TimesheetViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var weekStartDay by remember { mutableIntStateOf(WeekSettings.startDay) }
    var settings by remember { mutableStateOf(ReminderStore.load(context)) }
    var notificationsOk by remember { mutableStateOf(ReminderReceiver.canNotify(context)) }
    var editingTime by remember { mutableStateOf<Which?>(null) }
    var pendingEnable by remember { mutableStateOf<Which?>(null) }

    fun update(new: ReminderSettings) {
        settings = new
        ReminderStore.save(context, new)
        ReminderScheduler.scheduleAll(context)
    }

    fun enable(which: Which) {
        update(
            when (which) {
                Which.DAILY -> settings.copy(dailyEnabled = true)
                Which.WEEKLY -> settings.copy(weeklyEnabled = true)
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOk = ReminderReceiver.canNotify(context)
        val which = pendingEnable
        pendingEnable = null
        if (granted && which != null) {
            enable(which)
        } else if (!granted) {
            Toast.makeText(context, "Turn on notifications for TradeHours to get reminders.", Toast.LENGTH_LONG).show()
        }
    }

    fun toggle(which: Which, on: Boolean) {
        if (!on) {
            update(
                when (which) {
                    Which.DAILY -> settings.copy(dailyEnabled = false)
                    Which.WEEKLY -> settings.copy(weeklyEnabled = false)
                }
            )
            return
        }
        if (Build.VERSION.SDK_INT >= 33 && !ReminderReceiver.canNotify(context)) {
            pendingEnable = which
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enable(which)
        }
    }

    // Re-check when returning from the system notification settings.
    LaunchedEffect(settings) { notificationsOk = ReminderReceiver.canNotify(context) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.bar,
                    titleContentColor = MaterialTheme.colorScheme.onBar
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Look", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Text size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            TextSize.entries.forEachIndexed { index, size ->
                                SegmentedButton(
                                    selected = AppearanceSettings.textSize == size,
                                    onClick = { AppearanceSettings.setTextSize(context, size) },
                                    shape = SegmentedButtonDefaults.itemShape(index, TextSize.entries.size)
                                ) { Text(size.label, maxLines = 1) }
                            }
                        }
                        Text(
                            "Choose the size that's easiest to read. The whole app changes right away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text("Light or dark", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            ThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = AppearanceSettings.themeMode == mode,
                                    onClick = { AppearanceSettings.setThemeMode(context, mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size)
                                ) { Text(mode.label, maxLines = 1) }
                            }
                        }
                        Text(
                            "Auto follows your phone's setting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text("Pay week", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Week starts on", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val names = remember { DateFormatSymbols(Locale.getDefault()).weekdays } // index 1 = Sunday
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            WeekSettings.choices.forEachIndexed { index, day ->
                                SegmentedButton(
                                    selected = weekStartDay == day,
                                    onClick = {
                                        weekStartDay = day
                                        WeekSettings.save(context, day)
                                        viewModel.onWeekStartChanged()
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, WeekSettings.choices.size)
                                ) { Text(names[day].take(3)) }
                            }
                        }
                        Text(
                            "Your week, its total, and your history all follow this. Saved hours don't move.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text("Reminders", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                if ((settings.dailyEnabled || settings.weeklyEnabled) && !notificationsOk) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Notifications are turned off for TradeHours, so reminders won't show.",
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }) { Text("Open notification settings") }
                        }
                    }
                }

                // Daily
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow(
                            title = "Daily reminder",
                            subtitle = "A nudge to log your hours. Skipped if you already logged today.",
                            checked = settings.dailyEnabled,
                            onChange = { toggle(Which.DAILY, it) }
                        )
                        if (settings.dailyEnabled) {
                            ValueRow("Time", timeText(settings.dailyHour, settings.dailyMinute)) { editingTime = Which.DAILY }
                            SwitchRow(
                                title = "Weekdays only",
                                subtitle = null,
                                checked = settings.dailyWeekdaysOnly,
                                onChange = { update(settings.copy(dailyWeekdaysOnly = it)) }
                            )
                        }
                    }
                }

                // Weekly
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SwitchRow(
                            title = "Weekly reminder",
                            subtitle = "Shows your hours so far and reminds you to copy and send them.",
                            checked = settings.weeklyEnabled,
                            onChange = { toggle(Which.WEEKLY, it) }
                        )
                        if (settings.weeklyEnabled) {
                            DayPicker(
                                day = settings.weeklyDay,
                                onPick = { update(settings.copy(weeklyDay = it)) }
                            )
                            ValueRow("Time", timeText(settings.weeklyHour, settings.weeklyMinute)) { editingTime = Which.WEEKLY }
                        }
                    }
                }

                Text(
                    "Reminders are set on your phone and never leave it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    editingTime?.let { which ->
        val initialHour = if (which == Which.DAILY) settings.dailyHour else settings.weeklyHour
        val initialMinute = if (which == Which.DAILY) settings.dailyMinute else settings.weeklyMinute
        val state = rememberTimePickerState(initialHour, initialMinute, DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { editingTime = null },
            title = { Text(if (which == Which.DAILY) "Daily reminder time" else "Weekly reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    update(
                        when (which) {
                            Which.DAILY -> settings.copy(dailyHour = state.hour, dailyMinute = state.minute)
                            Which.WEEKLY -> settings.copy(weeklyHour = state.hour, weeklyMinute = state.minute)
                        }
                    )
                    editingTime = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { editingTime = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayPicker(day: Int, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val names = remember { DateFormatSymbols(Locale.getDefault()).weekdays } // index 1 = Sunday
    Box {
        ValueRow("Day", names[day]) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            (Calendar.SUNDAY..Calendar.SATURDAY).forEach { d ->
                DropdownMenuItem(text = { Text(names[d]) }, onClick = {
                    onPick(d)
                    open = false
                })
            }
        }
    }
}

private fun timeText(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(cal.time)
}
