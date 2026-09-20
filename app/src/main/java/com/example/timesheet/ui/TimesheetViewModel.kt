package com.example.timesheet.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timesheet.data.SavedLocation
import com.example.timesheet.data.SiteTimeEntry
import com.example.timesheet.data.TimesheetDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WeekSummary(
    val weekRangeText: String,
    val totalHours: Double,
    val startDate: Long,
    val entries: List<SiteTimeEntry> = emptyList()
)

data class PeriodTotal(
    val label: String,
    val totalHours: Double
)

/** Start of the Monday-based week containing [millis]. */
fun weekStartOf(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysSinceMonday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
    return cal.timeInMillis
}

class TimesheetViewModel(
    private val dao: TimesheetDao,
    @Suppress("UNUSED_PARAMETER") private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val allEntries: StateFlow<List<SiteTimeEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLocations: StateFlow<List<SavedLocation>> = dao.getAllSavedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weekRange = MutableStateFlow(getCurrentWeekRange())
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentWeekEntries: StateFlow<List<SiteTimeEntry>> = _weekRange.flatMapLatest { range ->
        dao.getEntriesForDateRange(range.first, range.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalHoursThisMonth: StateFlow<Double> = allEntries.map { entries ->
        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)
        
        entries.filter { entry ->
            val entryCal = Calendar.getInstance().apply { timeInMillis = entry.date }
            entryCal.get(Calendar.MONTH) == currentMonth && entryCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.hoursWorked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalHoursThisYear: StateFlow<Double> = allEntries.map { entries ->
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        
        entries.filter { entry ->
            val entryCal = Calendar.getInstance().apply { timeInMillis = entry.date }
            entryCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.hoursWorked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pastWeeksSummary: StateFlow<List<WeekSummary>> = allEntries.map { entries ->
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        entries.groupBy { weekStartOf(it.date) }.map { (start, weekEntries) ->
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                add(Calendar.DAY_OF_YEAR, 6)
            }.timeInMillis
            WeekSummary(
                weekRangeText = "${sdf.format(Date(start))} - ${sdf.format(Date(end))}",
                totalHours = weekEntries.sumOf { it.hoursWorked },
                startDate = start,
                entries = weekEntries
            )
        }.sortedByDescending { it.startDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthTotals: StateFlow<List<PeriodTotal>> = allEntries.map { entries ->
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        entries.groupBy { e ->
            Calendar.getInstance().apply { timeInMillis = e.date }.let { it.get(Calendar.YEAR) * 12 + it.get(Calendar.MONTH) }
        }.toSortedMap(compareByDescending { it }).values.map { list ->
            PeriodTotal(fmt.format(Date(list.first().date)), list.sumOf { it.hoursWorked })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearTotals: StateFlow<List<PeriodTotal>> = allEntries.map { entries ->
        entries.groupBy { Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.YEAR) }
            .toSortedMap(compareByDescending { it })
            .map { (year, list) -> PeriodTotal(year.toString(), list.sumOf { it.hoursWorked }) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Entry being edited on the entry screen, or null when adding a new one. */
    private val _editingEntry = MutableStateFlow<SiteTimeEntry?>(null)
    val editingEntry: StateFlow<SiteTimeEntry?> = _editingEntry

    fun startEditing(entry: SiteTimeEntry?) {
        _editingEntry.value = entry
    }

    val currentWeekStart: Long get() = _weekRange.value.first

    fun addOrUpdateEntry(entry: SiteTimeEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (entry.id == 0L) {
                    dao.insertEntry(entry)
                } else {
                    dao.updateEntry(entry)
                }
                
                // Automatically save location to SavedLocation
                if (entry.siteName.isNotBlank()) {
                    dao.insertSavedLocation(
                        SavedLocation(
                            siteName = entry.siteName,
                            siteAddress = entry.siteAddress
                        )
                    )
                }
            } catch (e: Exception) {
                // Log error
                e.printStackTrace()
            }
        }
    }

    fun deleteEntry(entry: SiteTimeEntry) {
        viewModelScope.launch {
            dao.deleteEntry(entry)
        }
    }

    /**
     * Updates the current week range for filtering entries.
     */
    fun getEntriesForWeek(startDate: Long, endDate: Long) {
        _weekRange.value = Pair(startDate, endDate)
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val start = weekStartOf(System.currentTimeMillis())
        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.DAY_OF_YEAR, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis
        return Pair(start, end)
    }
}
