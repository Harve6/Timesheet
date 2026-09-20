package com.example.timesheet.ui.entry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timesheet.data.SavedLocation
import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.formatHours
import com.example.timesheet.ui.localNoon
import com.example.timesheet.ui.localToPickerUtc
import com.example.timesheet.ui.pickerUtcToLocalNoon
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    viewModel: TimesheetViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val editing = remember { viewModel.editingEntry.value }

    var date by rememberSaveable { mutableLongStateOf(editing?.date ?: localNoon(System.currentTimeMillis())) }
    var siteName by rememberSaveable { mutableStateOf(editing?.siteName ?: "") }
    var siteAddress by rememberSaveable { mutableStateOf(editing?.siteAddress ?: "") }
    var jobNumber by rememberSaveable { mutableStateOf(editing?.jobNumber ?: "") }
    var hoursWorked by rememberSaveable { mutableStateOf(editing?.hoursWorked ?: 8.0) }
    var workSummary by rememberSaveable { mutableStateOf(editing?.workSummary ?: "") }
    var travelReimbursed by rememberSaveable { mutableStateOf(editing?.travelReimbursed ?: false) }
    var parkingAmountText by rememberSaveable {
        mutableStateOf(editing?.parkingAmount?.takeIf { it > 0 }?.let { formatHours(it) } ?: "")
    }
    var showMore by rememberSaveable {
        mutableStateOf(
            editing != null && (editing.siteAddress.isNotBlank() || editing.jobNumber.isNotBlank() ||
                editing.travelReimbursed || editing.parkingAmount > 0)
        )
    }

    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }

    fun leave() {
        viewModel.startEditing(null)
        onNavigateBack()
    }

    BackHandler { leave() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (editing == null) "Add Hours" else "Edit Hours", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { leave() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.addOrUpdateEntry(
                        SiteTimeEntry(
                            id = editing?.id ?: 0L,
                            date = date,
                            siteName = siteName.trim(),
                            siteAddress = siteAddress.trim(),
                            jobNumber = jobNumber.trim(),
                            hoursWorked = hoursWorked,
                            workSummary = workSummary.trim(),
                            travelReimbursed = travelReimbursed,
                            parkingAmount = parkingAmountText.toDoubleOrNull() ?: 0.0
                        )
                    )
                    leave()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(64.dp),
                enabled = hoursWorked > 0,
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
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
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DateRow(date = date, onDateChange = { date = it }, onPickDate = { showDatePicker = true })

                HourSelector(hours = hoursWorked, onHoursChange = { hoursWorked = it })

                SiteAutocompleteField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    suggestions = savedLocations,
                    onSuggestionSelected = { location ->
                        siteName = location.siteName
                        siteAddress = location.siteAddress
                    },
                    label = "Where? (site or job name)"
                )

                OutlinedTextField(
                    value = workSummary,
                    onValueChange = { workSummary = it },
                    label = { Text("What did you do? (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium
                )

                TextButton(onClick = { showMore = !showMore }) {
                    Text(if (showMore) "Hide extras" else "Add job #, parking, travel...")
                    Icon(
                        if (showMore) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }

                AnimatedVisibility(visible = showMore) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = siteAddress,
                            onValueChange = { siteAddress = it },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = jobNumber,
                                onValueChange = { jobNumber = it },
                                label = { Text("Job #") },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            )
                            OutlinedTextField(
                                value = parkingAmountText,
                                onValueChange = {
                                    if (it.isEmpty() || it.toDoubleOrNull() != null) parkingAmountText = it
                                },
                                label = { Text("Parking $") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = MaterialTheme.shapes.medium
                            )
                        }
                        Surface(
                            onClick = { travelReimbursed = !travelReimbursed },
                            color = if (travelReimbursed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Travel time paid", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Switch(checked = travelReimbursed, onCheckedChange = { travelReimbursed = it })
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = localToPickerUtc(date))
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = pickerUtcToLocalNoon(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateRow(date: Long, onDateChange: (Long) -> Unit, onPickDate: () -> Unit) {
    val today = remember { localNoon(System.currentTimeMillis()) }
    val yesterday = remember {
        Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
    }
    val fmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val isToday = localNoon(date) == today
    val isYesterday = localNoon(date) == yesterday

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(selected = isToday, onClick = { onDateChange(today) }, label = { Text("Today") })
        FilterChip(selected = isYesterday, onClick = { onDateChange(yesterday) }, label = { Text("Yesterday") })
        FilterChip(
            selected = !isToday && !isYesterday,
            onClick = onPickDate,
            label = { Text(if (!isToday && !isYesterday) fmt.format(Date(date)) else "Other day") },
            leadingIcon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
fun SiteAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<SavedLocation>,
    onSuggestionSelected: (SavedLocation) -> Unit,
    label: String
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value, suggestions) {
        if (value.isBlank()) emptyList()
        else suggestions.filter { it.siteName.contains(value, ignoreCase = true) && !it.siteName.equals(value, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            singleLine = true,
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            }
        )

        if (expanded && filteredSuggestions.isNotEmpty()) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f),
                properties = androidx.compose.ui.window.PopupProperties(focusable = false)
            ) {
                filteredSuggestions.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(location.siteName, fontWeight = FontWeight.Bold)
                                if (location.siteAddress.isNotEmpty()) {
                                    Text(location.siteAddress, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        onClick = {
                            onSuggestionSelected(location)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HourSelector(
    hours: Double,
    onHoursChange: (Double) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Hours Worked", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                FilledIconButton(
                    onClick = { if (hours >= 0.5) onHoursChange(hours - 0.5) },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(36.dp))
                }

                Text(
                    text = formatHours(hours),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                FilledIconButton(
                    onClick = { if (hours < 24) onHoursChange(hours + 0.5) },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(36.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4.0, 6.0, 8.0, 10.0).forEach { preset ->
                    FilterChip(
                        selected = hours == preset,
                        onClick = { onHoursChange(preset) },
                        label = { Text("${formatHours(preset)}h") }
                    )
                }
            }
        }
    }
}
