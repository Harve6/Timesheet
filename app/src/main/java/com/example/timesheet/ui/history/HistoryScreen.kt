package com.example.timesheet.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.timesheet.ui.PeriodTotal
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.WeekSummary
import com.example.timesheet.ui.buildWeekText
import com.example.timesheet.ui.copyToClipboard
import com.example.timesheet.ui.formatHours

private val tabs = listOf("Weeks", "Months", "Years")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TimesheetViewModel,
    onOpenWeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekHours by viewModel.totalHoursThisWeek.collectAsStateWithLifecycle()
    val monthHours by viewModel.totalHoursThisMonth.collectAsStateWithLifecycle()
    val ytdHours by viewModel.totalHoursThisYear.collectAsStateWithLifecycle()
    val pastWeeks by viewModel.pastWeeksSummary.collectAsStateWithLifecycle()
    val months by viewModel.monthTotals.collectAsStateWithLifecycle()
    val years by viewModel.yearTotals.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("History", fontWeight = FontWeight.Bold) }) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsCard("This Week", formatHours(weekHours), Modifier.weight(1f))
                        StatsCard("This Month", formatHours(monthHours), Modifier.weight(1f))
                        StatsCard("This Year", formatHours(ytdHours), Modifier.weight(1f))
                    }
                }

                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        tabs.forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                shape = SegmentedButtonDefaults.itemShape(index, tabs.size)
                            ) { Text(label) }
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        if (pastWeeks.isEmpty()) item { EmptyHistory() }
                        items(pastWeeks, key = { it.startDate }) { week ->
                            WeekSummaryItem(
                                week = week,
                                onOpen = { onOpenWeek(week.startDate) },
                                onCopy = { copyToClipboard(context, buildWeekText(week.startDate, week.entries)) }
                            )
                        }
                    }
                    1 -> {
                        if (months.isEmpty()) item { EmptyHistory() }
                        items(months, key = { it.label }) { PeriodRow(it) }
                    }
                    else -> {
                        if (years.isEmpty()) item { EmptyHistory() }
                        items(years, key = { it.label }) { PeriodRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nothing here yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp)
            )
            Text(text = "hours", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun WeekSummaryItem(
    week: WeekSummary,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(week.weekRangeText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${formatHours(week.totalHours)} h",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy this week")
            }
        }
    }
}

@Composable
private fun PeriodRow(period: PeriodTotal) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(period.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "${formatHours(period.totalHours)} h",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
