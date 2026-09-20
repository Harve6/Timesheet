package com.example.timesheet.ui.hours

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.ui.DayData
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.addDays
import com.example.timesheet.ui.buildWeekText
import com.example.timesheet.ui.copyToClipboard
import com.example.timesheet.ui.formatHours
import com.example.timesheet.ui.formatMoney
import com.example.timesheet.ui.localNoon
import com.example.timesheet.ui.weekStartOf
import kotlinx.coroutines.flow.drop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Editable state for one row of the grid. */
private class DayState(val date: Long, initial: DayData?) {
    var hours by mutableStateOf(initial?.hours?.takeIf { it > 0 }?.let { formatHours(it) } ?: "")
    var site by mutableStateOf(initial?.site ?: "")
    var travel by mutableStateOf(initial?.travel ?: false)
    var parking by mutableStateOf(initial?.parking?.takeIf { it > 0 }?.let { formatMoney(it) } ?: "")

    fun toData() = DayData(
        hours = hours.toDoubleOrNull() ?: 0.0,
        site = site.trim(),
        travel = travel,
        parking = parking.toDoubleOrNull() ?: 0.0
    )

    fun hasExtras() = travel || parking.isNotEmpty()

    fun clear() {
        hours = ""; site = ""; travel = false; parking = ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyHoursScreen(
    viewModel: TimesheetViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weekStart by viewModel.weekStart.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()

    // Rows are (re)loaded from the database whenever the week changes. The grid is
    // only shown once loaded, so a blank grid can never overwrite saved hours.
    var days by remember { mutableStateOf<List<DayState>?>(null) }
    LaunchedEffect(weekStart) {
        days = null
        val saved = viewModel.loadWeek(weekStart)
        days = (0..6).map { i ->
            val date = localNoon(addDays(weekStart, i))
            DayState(date, saved[date])
        }
    }

    val rows = days
    val totalHours = rows?.sumOf { it.hours.toDoubleOrNull() ?: 0.0 } ?: 0.0
    val isThisWeek = weekStart == weekStartOf(System.currentTimeMillis())
    val rangeFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    var showClearConfirm by rememberSaveable { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<DayState?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${rangeFmt.format(Date(weekStart))} - ${rangeFmt.format(Date(addDays(weekStart, 6)))}",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        if (isThisWeek) {
                            Text("THIS WEEK", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                        } else {
                            Text(
                                "Back to this week",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.clickableNoRipple { viewModel.goToThisWeek() }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.shiftWeek(-1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week", modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shiftWeek(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week", modifier = Modifier.size(32.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = rows != null,
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("CLEAR", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp),
                        enabled = rows != null,
                        onClick = { rows?.let { copyToClipboard(context, weekText(weekStart, it)) } }
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("COPY", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL HOURS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text(formatHours(totalHours), fontWeight = FontWeight.Black, fontSize = 28.sp)
                }
            }

            Card(modifier = Modifier.weight(1f)) {
                if (rows == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("DAY", modifier = Modifier.width(52.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("HRS", modifier = Modifier.width(76.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("JOBSITE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        rows.forEach { day ->
                            DayRow(
                                day = day,
                                isToday = day.date == localNoon(System.currentTimeMillis()),
                                suggestions = savedLocations.map { it.siteName }.distinct(),
                                onOptions = { optionsFor = day },
                                onSave = { viewModel.saveDay(day.date, day.toData()) }
                            )
                        }
                    }
                }
            }
        }
    }

    optionsFor?.let { day ->
        val dayFmt = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { optionsFor = null },
            title = { Text(dayFmt.format(Date(day.date))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Travel paid", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = day.travel, onCheckedChange = { day.travel = it })
                    }
                    OutlinedTextField(
                        value = day.parking,
                        onValueChange = { day.parking = filterDecimal(it) },
                        label = { Text("Parking") },
                        prefix = { Text("$") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = { TextButton(onClick = { optionsFor = null }) { Text("DONE") } }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear this week?") },
            text = { Text("This erases all hours and jobsites for ${rangeFmt.format(Date(weekStart))} - ${rangeFmt.format(Date(addDays(weekStart, 6)))}.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    rows?.forEach { it.clear() }
                    viewModel.clearWeek(weekStart)
                }) { Text("CLEAR", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("CANCEL") } }
        )
    }
}

@Composable
private fun DayRow(
    day: DayState,
    isToday: Boolean,
    suggestions: List<String>,
    onOptions: () -> Unit,
    onSave: () -> Unit
) {
    // Save on every change (skipping the initial load); the ViewModel writes in order.
    val currentOnSave by rememberUpdatedState(onSave)
    LaunchedEffect(day) {
        snapshotFlow { listOf(day.hours, day.site, day.travel.toString(), day.parking) }
            .drop(1)
            .collect { currentOnSave() }
    }

    val dayFmt = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("M/d", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            onClick = onOptions,
            color = when {
                day.hasExtras() -> MaterialTheme.colorScheme.tertiaryContainer
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.width(52.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(dayFmt.format(Date(day.date)), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(dateFmt.format(Date(day.date)), style = MaterialTheme.typography.labelSmall)
            }
        }

        OutlinedTextField(
            modifier = Modifier.width(76.dp),
            value = day.hours,
            onValueChange = { day.hours = filterDecimal(it) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next)
        )

        Column(modifier = Modifier.weight(1f)) {
            SiteField(value = day.site, onValueChange = { day.site = it }, suggestions = suggestions)
            if (day.hasExtras()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (day.travel) Text("+Travel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (day.parking.isNotEmpty()) Text("+Park $${day.parking}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun SiteField(value: String, onValueChange: (String) -> Unit, suggestions: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions) {
        if (value.isBlank()) emptyList()
        else suggestions.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }.take(5)
    }
    Box {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            singleLine = true,
            placeholder = { Text("Site", fontSize = 14.sp) },
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        DropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            matches.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onValueChange(name)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun weekText(weekStart: Long, rows: List<DayState>): String {
    val entries = rows.mapNotNull { day ->
        val d = day.toData()
        if (d.hours <= 0 && d.site.isBlank() && !d.travel && d.parking <= 0) null
        else SiteTimeEntry(
            date = day.date,
            siteName = d.site,
            siteAddress = "",
            jobNumber = "",
            hoursWorked = d.hours,
            workSummary = "",
            travelReimbursed = d.travel,
            parkingAmount = d.parking
        )
    }
    return buildWeekText(weekStart, entries)
}

/** Digits and at most one dot, max two decimals. */
private fun filterDecimal(input: String): String {
    var dotUsed = false
    val sb = StringBuilder()
    for (ch in input) {
        when {
            ch.isDigit() -> sb.append(ch)
            ch == '.' && !dotUsed -> { dotUsed = true; sb.append(ch) }
        }
    }
    val s = sb.toString()
    val dot = s.indexOf('.')
    return if (dot >= 0) s.substring(0, dot + 1) + s.substring(dot + 1).take(2) else s
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
