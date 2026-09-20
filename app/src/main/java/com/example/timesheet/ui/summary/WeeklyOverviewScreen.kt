package com.example.timesheet.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.buildWeekText
import com.example.timesheet.ui.copyToClipboard
import com.example.timesheet.ui.formatHours
import com.example.timesheet.ui.shareText
import com.example.timesheet.ui.theme.TimesheetTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyOverviewScreen(
    viewModel: TimesheetViewModel,
    onNavigateToEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entries by viewModel.currentWeekEntries.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val dateFormatter = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val dayGroups = remember(entries) {
        entries.sortedBy { it.date }.groupBy { dateFormatter.format(Date(it.date)) }.toList()
    }
    val grandTotal = remember(entries) { entries.sumOf { it.hoursWorked } }

    var entryToDelete by remember { mutableStateOf<SiteTimeEntry?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("This Week", fontWeight = FontWeight.ExtraBold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.startEditing(null)
                    onNavigateToEntry()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Hours", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyState(
                onAdd = {
                    viewModel.startEditing(null)
                    onNavigateToEntry()
                },
                modifier = Modifier.padding(padding)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    contentPadding = PaddingValues(bottom = 96.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { GrandTotalCard(grandTotal = grandTotal) }

                    item {
                        val text = buildWeekText(viewModel.currentWeekStart, entries)
                        Button(
                            onClick = { copyToClipboard(context, text) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy Hours to Send", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { shareText(context, text) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Or share directly")
                        }
                    }

                    items(dayGroups) { (dayLabel, dayEntries) ->
                        DaySummaryCard(
                            dayLabel = dayLabel,
                            dayTotal = dayEntries.sumOf { it.hoursWorked },
                            entries = dayEntries,
                            onEdit = { entry ->
                                viewModel.startEditing(entry)
                                onNavigateToEntry()
                            },
                            onDelete = { entryToDelete = it }
                        )
                    }
                }
            }
        }
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete this entry?") },
            text = { Text("${formatHours(entry.hoursWorked)}h ${entry.siteName}".trim()) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entry)
                    entryToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Keep") }
            }
        )
    }
}

@Composable
fun EmptyState(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Notes,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text("No hours yet this week", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Tap Add Hours after each day. When the week is done, tap Copy and paste it into a text.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAdd, shape = MaterialTheme.shapes.medium) { Text("Add Hours") }
        }
    }
}

@Composable
fun GrandTotalCard(grandTotal: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("TOTAL THIS WEEK", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(formatHours(grandTotal), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            Text("HOURS", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DaySummaryCard(
    dayLabel: String,
    dayTotal: Double,
    entries: List<SiteTimeEntry>,
    onEdit: (SiteTimeEntry) -> Unit,
    onDelete: (SiteTimeEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dayLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(
                    "${formatHours(dayTotal)}h",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
            }

            entries.forEach { entry ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${formatHours(entry.hoursWorked)}h" + if (entry.siteName.isNotBlank()) "  ${entry.siteName}" else "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (entry.workSummary.isNotBlank()) {
                            Text(
                                entry.workSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val extras = listOfNotNull(
                            entry.jobNumber.takeIf { it.isNotBlank() }?.let { "Job $it" },
                            entry.parkingAmount.takeIf { it > 0 }?.let { "Parking $${String.format(Locale.US, "%.2f", it)}" },
                            "Travel".takeIf { entry.travelReimbursed }
                        )
                        if (extras.isNotEmpty()) {
                            Text(
                                extras.joinToString(" • "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    IconButton(onClick = { onEdit(entry) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyOverviewPreview() {
    TimesheetTheme {
        Column {
            GrandTotalCard(grandTotal = 40.5)
            DaySummaryCard(
                dayLabel = "Monday, Aug 24",
                dayTotal = 8.5,
                entries = listOf(
                    SiteTimeEntry(siteName = "Site A", hoursWorked = 8.5, workSummary = "Concrete pour", date = 0, siteAddress = "", jobNumber = "", travelReimbursed = false, parkingAmount = 0.0)
                ),
                onEdit = {},
                onDelete = {}
            )
        }
    }
}
