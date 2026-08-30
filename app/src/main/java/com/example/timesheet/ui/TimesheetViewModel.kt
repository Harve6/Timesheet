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
    val startDate: Long
)

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
        if (entries.isEmpty()) return@map emptyList()

        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        val groupedByWeek = entries.groupBy { entry ->
            val cal = Calendar.getInstance()
            cal.timeInMillis = entry.date
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (Calendar.getInstance().apply { timeInMillis = entry.date }.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                cal.add(Calendar.WEEK_OF_YEAR, -1)
            }
            cal.timeInMillis
        }

        groupedByWeek.map { (startMillis, weekEntries) ->
            val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
            val endCal = Calendar.getInstance().apply {
                timeInMillis = startMillis
                add(Calendar.DAY_OF_WEEK, 6)
            }
            val rangeText = "${sdf.format(Date(startCal.timeInMillis))} - ${sdf.format(Date(endCal.timeInMillis))}"
            WeekSummary(
                weekRangeText = rangeText,
                totalHours = weekEntries.sumOf { it.hoursWorked },
                startDate = startMillis
            )
        }.sortedByDescending { it.startDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Set to start of the week (Monday)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        // If current day is before Monday (Sunday), go back to previous Monday
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
        }
        val startDate = calendar.timeInMillis

        // Set to end of the week (Sunday)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}
