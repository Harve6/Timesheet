package com.example.timesheet.ui.summary

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.ui.TimesheetViewModel
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

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    
    // Group entries by day and sort them chronologically within each day
    val groupedEntries = remember(entries) {
        entries.sortedBy { it.date }
            .groupBy { dateFormatter.format(Date(it.date)) }
    }
    
    val grandTotal = remember(entries) {
        entries.sumOf { it.hoursWorked }
    }

    var expandedDays by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = { Text("Weekly Overview", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = {
                            val summaryText = buildSummaryText(entries, grandTotal)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Timesheet"))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onNavigateToEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = FloatingActionButtonDefaults.largeShape
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "New Entry",
                    modifier = Modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        if (entries.isEmpty()) {
            EmptyState(onNavigateToEntry, modifier = Modifier.padding(padding))
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
                    contentPadding = PaddingValues(
                        bottom = 80.dp, // Extra space for FAB
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GrandTotalCard(grandTotal = grandTotal)
                    }

                    items(groupedEntries.keys.toList()) { dayKey ->
                        val dayEntries = groupedEntries[dayKey] ?: emptyList()
                        val dayTotal = dayEntries.sumOf { it.hoursWorked }
                        val isExpanded = expandedDays.contains(dayKey)

                        DaySummaryCard(
                            dayLabel = dayKey,
                            dayTotal = dayTotal,
                            entries = dayEntries,
                            isExpanded = isExpanded,
                            onExpandClick = {
                                expandedDays = if (isExpanded) {
                                    expandedDays - dayKey
                                } else {
                                    expandedDays + dayKey
                                }
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val summaryText = buildSummaryText(entries, grandTotal)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summaryText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Weekly Summary"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share Weekly Summary", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(onNavigateToEntry: () -> Unit, modifier: Modifier = Modifier) {
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
            Text(
                "No entries yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Start tracking your hours by adding your first entry for the week.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onNavigateToEntry,
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Add Entry")
            }
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
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "TOTAL HOURS THIS WEEK",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                String.format(Locale.getDefault(), "%.1f", grandTotal),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Black
            )
            Text(
                "HOURS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DaySummaryCard(
    dayLabel: String,
    dayTotal: Double,
    entries: List<SiteTimeEntry>,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 0.dp else 2.dp),
        border = if (isExpanded) null else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.clickable { onExpandClick() }.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(dayLabel, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        String.format(Locale.getDefault(), "%.1f hours", dayTotal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.siteName, 
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
                                if (entry.jobNumber.isNotBlank() || entry.parkingAmount > 0 || entry.travelReimbursed) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (entry.jobNumber.isNotBlank()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    "Job: ${entry.jobNumber}",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                        if (entry.parkingAmount > 0) {
                                            Text(
                                                "Parking: $${String.format(Locale.getDefault(), "%.2f", entry.parkingAmount)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (entry.travelReimbursed) {
                                            Icon(
                                                Icons.Rounded.DirectionsCar,
                                                contentDescription = "Travel Reimbursed",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                String.format(Locale.getDefault(), "%.1f h", entry.hoursWorked),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun buildSummaryText(entries: List<SiteTimeEntry>, grandTotal: Double): String {
    val dateFormatter = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    val grouped = entries.groupBy { dateFormatter.format(Date(it.date)) }
    
    // Sort keys by date of first entry in each group to maintain chronological order
    val sortedKeys = grouped.keys.sortedBy { key -> 
        grouped[key]?.firstOrNull()?.date ?: 0L 
    }

    return buildString {
        if (entries.isNotEmpty()) {
            val start = Date(entries.minOf { it.date })
            val end = Date(entries.maxOf { it.date })
            val rangeFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            appendLine("Weekly Timesheet - ${rangeFormatter.format(start)} to ${rangeFormatter.format(end)}")
        } else {
            appendLine("Weekly Timesheet Summary")
        }
        appendLine("---------------------------")
        
        sortedKeys.forEach { day ->
            val dayEntries = grouped[day] ?: emptyList()
            dayEntries.forEach { entry ->
                append("$day: ${entry.siteName} - ${String.format(Locale.getDefault(), "%.1f", entry.hoursWorked)}h")
                if (entry.jobNumber.isNotBlank()) {
                    append(" [Job: ${entry.jobNumber}]")
                }
                if (entry.workSummary.isNotBlank()) {
                    append(" (${entry.workSummary})")
                }
                if (entry.parkingAmount > 0) {
                    append(" - Parking: $${String.format(Locale.getDefault(), "%.2f", entry.parkingAmount)}")
                }
                if (entry.travelReimbursed) {
                    append(" (Travel Reimbursed)")
                }
                appendLine()
            }
        }
        
        appendLine("---------------------------")
        appendLine("Total Hours: ${String.format(Locale.getDefault(), "%.1f", grandTotal)}h")
    }
}

@Preview(showBackground = true)
@Composable
fun WeeklyOverviewPreview() {
    TimesheetTheme {
        // Mock data for preview could be added here if we had a way to provide it without the ViewModel
        // But for simplicity, we'll just preview the components
        Column {
            GrandTotalCard(grandTotal = 40.5)
            DaySummaryCard(
                dayLabel = "Mon, Aug 24",
                dayTotal = 8.5,
                entries = listOf(
                    SiteTimeEntry(siteName = "Site A", hoursWorked = 8.5, workSummary = "Concrete pour", date = 0, siteAddress = "", jobNumber = "", travelReimbursed = false, parkingAmount = 0.0)
                ),
                isExpanded = true,
                onExpandClick = {}
            )
        }
    }
}
