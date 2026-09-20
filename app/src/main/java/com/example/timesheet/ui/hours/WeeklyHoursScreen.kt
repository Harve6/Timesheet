package com.example.timesheet.ui.hours

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
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
import com.example.timesheet.ui.theme.bar
import com.example.timesheet.ui.theme.onBar
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
    // At the biggest text sizes there is no room to put the site beside the hours, so it drops underneath.
    val stacked = LocalDensity.current.fontScale > 1.3f
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
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (isThisWeek) {
                            Text("THIS WEEK", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
                        } else {
                            Text(
                                "Back to this week",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickableNoRipple { viewModel.goToThisWeek() }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.shiftWeek(-1) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous week", modifier = Modifier.size(44.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shiftWeek(1) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next week", modifier = Modifier.size(44.dp))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.bar,
                    titleContentColor = MaterialTheme.colorScheme.onBar,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBar,
                    actionIconContentColor = MaterialTheme.colorScheme.onBar
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
                            .heightIn(min = 64.dp),
                        enabled = rows != null,
                        onClick = { showClearConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("CLEAR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                    Button(
                        modifier = Modifier
                            .weight(1.4f)
                            .heightIn(min = 64.dp),
                        enabled = rows != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.bar,
                            contentColor = MaterialTheme.colorScheme.onBar
                        ),
                        onClick = { rows?.let { copyToClipboard(context, weekText(weekStart, it)) } }
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("COPY", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL HOURS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(formatHours(totalHours), fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineLarge)
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
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (!stacked) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("DAY", modifier = Modifier.width(DAY_WIDTH), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("HRS", modifier = Modifier.width(HOURS_WIDTH), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Text("JOBSITE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                        rows.forEach { day ->
                            DayRow(
                                stacked = stacked,
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

private val DAY_WIDTH = 60.dp
private val HOURS_WIDTH = 74.dp

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)

/** Outlined text box with tight, adjustable side padding so every pixel goes to the text. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    keyboardOptions: KeyboardOptions,
    placeholder: (@Composable () -> Unit)?,
    singleLine: Boolean,
    maxLines: Int,
    horizontalPadding: Dp,
    verticalPadding: Dp = 9.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = fieldColors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        maxLines = maxLines,
        interactionSource = interaction,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = inner,
                enabled = true,
                singleLine = singleLine,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                placeholder = placeholder,
                colors = colors,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = verticalPadding,
                    bottom = verticalPadding
                ),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = false,
                        interactionSource = interaction,
                        colors = colors
                    )
                }
            )
        }
    )
}

@Composable
private fun DayRow(
    stacked: Boolean,
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

    val dayChip: @Composable () -> Unit = {
        Surface(
            onClick = onOptions,
            color = when {
                day.hasExtras() -> MaterialTheme.colorScheme.tertiaryContainer
                isToday -> MaterialTheme.colorScheme.bar
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = when {
                day.hasExtras() -> MaterialTheme.colorScheme.onTertiaryContainer
                isToday -> MaterialTheme.colorScheme.onBar
                else -> MaterialTheme.colorScheme.onSurface
            },
            shape = MaterialTheme.shapes.small,
            modifier = if (stacked) Modifier.widthIn(min = DAY_WIDTH) else Modifier.width(DAY_WIDTH)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tight line heights keep the row as short as the text boxes beside it.
                Text(
                    dayFmt.format(Date(day.date)),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 24.sp)
                )
                Text(
                    dateFmt.format(Date(day.date)),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp)
                )
            }
        }
    }

    val hoursField: @Composable (Modifier) -> Unit = { mod ->
        GridField(
            value = day.hours,
            onValueChange = { day.hours = filterDecimal(it) },
            modifier = mod,
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Black),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            // No column header in the stacked layout, so say what the box is for.
            placeholder = if (stacked) {
                { Text("Hours", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium) }
            } else null,
            singleLine = true,
            maxLines = 1,
            horizontalPadding = 4.dp,
            verticalPadding = if (stacked) 5.dp else 9.dp
        )
    }

    val extras: @Composable () -> Unit = {
        if (day.hasExtras()) {
            val tags: @Composable () -> Unit = {
                if (day.travel) Text("+Travel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                if (day.parking.isNotEmpty()) Text("+Park $${day.parking}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            // Beside the hours box at Biggest (stacked to fit); under the site otherwise.
            if (stacked) Column { tags() } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { tags() }
        }
    }

    if (stacked) {
        // Biggest text: the day label sits on the left; hours (with any travel/parking tags beside
        // it) and the site stack on the right. Two rows per day instead of three.
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayChip()
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        hoursField(Modifier.width(132.dp))
                        extras()
                    }
                    SiteField(
                        value = day.site,
                        onValueChange = { day.site = it },
                        suggestions = suggestions,
                        verticalPadding = 5.dp
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 4.dp))
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dayChip()
            hoursField(Modifier.width(HOURS_WIDTH))
            Column(modifier = Modifier.weight(1f)) {
                SiteField(value = day.site, onValueChange = { day.site = it }, suggestions = suggestions)
                extras()
            }
        }
    }
}

@Composable
private fun SiteField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    verticalPadding: Dp = 9.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions) {
        if (value.isBlank()) emptyList()
        else suggestions.filter { it.contains(value, ignoreCase = true) && !it.equals(value, ignoreCase = true) }.take(5)
    }
    Box {
        GridField(
            value = value,
            onValueChange = {
                onValueChange(it.replace('\n', ' '))
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            placeholder = { Text("Site", style = MaterialTheme.typography.bodyLarge) },
            // Long names wrap onto a second line rather than scrolling the start out of view.
            singleLine = false,
            maxLines = 2,
            horizontalPadding = 10.dp,
            verticalPadding = verticalPadding
        )
        DropdownMenu(
            expanded = expanded && matches.isNotEmpty(),
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
        ) {
            matches.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name, style = MaterialTheme.typography.bodyLarge) },
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
