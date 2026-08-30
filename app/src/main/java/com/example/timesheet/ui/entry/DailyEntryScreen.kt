package com.example.timesheet.ui.entry

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timesheet.data.SavedLocation
import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.theme.TimesheetTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    viewModel: TimesheetViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var date by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var siteName by rememberSaveable { mutableStateOf("") }
    var siteAddress by rememberSaveable { mutableStateOf("") }
    var jobNumber by rememberSaveable { mutableStateOf("") }
    var hoursWorked by rememberSaveable { mutableStateOf(8.0) }
    var workSummary by rememberSaveable { mutableStateOf("") }
    var travelReimbursed by rememberSaveable { mutableStateOf(false) }
    var parkingAmountText by rememberSaveable { mutableStateOf("") }

    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date)

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Daily Entry", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Date Selection
                val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
                OutlinedTextField(
                    value = dateFormatter.format(Date(date)),
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = "Select Date")
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                )

                // Site Autocomplete
                SiteAutocompleteField(
                    value = siteName,
                    onValueChange = { siteName = it },
                    suggestions = savedLocations,
                    onSuggestionSelected = { location ->
                        siteName = location.siteName
                        siteAddress = location.siteAddress
                    },
                    label = "Site Name"
                )

                OutlinedTextField(
                    value = siteAddress,
                    onValueChange = { siteAddress = it },
                    label = { Text("Site Address (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                // Hour Selector
                HourSelector(
                    hours = hoursWorked,
                    onHoursChange = { hoursWorked = it }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = jobNumber,
                        onValueChange = { jobNumber = it },
                        label = { Text("Job Number") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    )

                    OutlinedTextField(
                        value = parkingAmountText,
                        onValueChange = { 
                            if (it.isEmpty() || it.toDoubleOrNull() != null) {
                                parkingAmountText = it
                            }
                        },
                        label = { Text("Parking $") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                OutlinedTextField(
                    value = workSummary,
                    onValueChange = { workSummary = it },
                    label = { Text("Work Completed") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = MaterialTheme.shapes.medium
                )

                Surface(
                    onClick = { travelReimbursed = !travelReimbursed },
                    color = if (travelReimbursed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Travel Reimbursed", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text("Toggle if travel was covered", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = travelReimbursed, onCheckedChange = { travelReimbursed = it })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val entry = SiteTimeEntry(
                            date = date,
                            siteName = siteName,
                            siteAddress = siteAddress,
                            jobNumber = jobNumber,
                            hoursWorked = hoursWorked,
                            workSummary = workSummary,
                            travelReimbursed = travelReimbursed,
                            parkingAmount = parkingAmountText.toDoubleOrNull() ?: 0.0
                        )
                        viewModel.addOrUpdateEntry(entry)
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = siteName.isNotBlank(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Entry", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date = it }
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
        else suggestions.filter { it.siteName.contains(value, ignoreCase = true) }
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
            modifier = Modifier.padding(16.dp),
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
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Decrease", modifier = Modifier.size(32.dp))
                }

                Text(
                    text = String.format(Locale.getDefault(), "%.1f", hours),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                FilledIconButton(
                    onClick = { onHoursChange(hours + 0.5) },
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Increase", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
