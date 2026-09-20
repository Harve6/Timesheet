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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Pay weeks run Sunday to Saturday. */
const val WEEK_START_DAY = Calendar.SUNDAY

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

/** What the grid shows for one day. */
data class DayData(
    val hours: Double,
    val site: String,
    val travel: Boolean,
    val parking: Double
)

/** Start of the week containing [millis] (midnight on [WEEK_START_DAY]). */
fun weekStartOf(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val daysSinceStart = (cal.get(Calendar.DAY_OF_WEEK) - WEEK_START_DAY + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysSinceStart)
    return cal.timeInMillis
}

fun addDays(millis: Long, days: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

private fun dayRange(millis: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return start to addDays(start, 1) - 1
}

class TimesheetViewModel(
    private val dao: TimesheetDao,
    @Suppress("UNUSED_PARAMETER") private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Single writer thread so saves land in the order they were made. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val writer = Dispatchers.IO.limitedParallelism(1)

    val allEntries: StateFlow<List<SiteTimeEntry>> = dao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLocations: StateFlow<List<SavedLocation>> = dao.getAllSavedLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _weekStart = MutableStateFlow(weekStartOf(System.currentTimeMillis()))
    val weekStart: StateFlow<Long> = _weekStart

    fun shiftWeek(delta: Int) {
        _weekStart.value = addDays(_weekStart.value, 7 * delta)
    }

    fun goToWeek(start: Long) {
        _weekStart.value = weekStartOf(start)
    }

    fun goToThisWeek() = goToWeek(System.currentTimeMillis())

    val totalHoursThisWeek: StateFlow<Double> = allEntries.map { entries ->
        val start = weekStartOf(System.currentTimeMillis())
        entries.filter { weekStartOf(it.date) == start }.sumOf { it.hoursWorked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalHoursThisMonth: StateFlow<Double> = allEntries.map { entries ->
        val now = Calendar.getInstance()
        entries.filter { entry ->
            val c = Calendar.getInstance().apply { timeInMillis = entry.date }
            c.get(Calendar.MONTH) == now.get(Calendar.MONTH) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
        }.sumOf { it.hoursWorked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalHoursThisYear: StateFlow<Double> = allEntries.map { entries ->
        val year = Calendar.getInstance().get(Calendar.YEAR)
        entries.filter { Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.YEAR) == year }
            .sumOf { it.hoursWorked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val pastWeeksSummary: StateFlow<List<WeekSummary>> = allEntries.map { entries ->
        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
        entries.groupBy { weekStartOf(it.date) }.map { (start, weekEntries) ->
            WeekSummary(
                weekRangeText = "${sdf.format(Date(start))} - ${sdf.format(Date(addDays(start, 6)))}",
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

    /**
     * Loads the seven days of the week starting at [weekStart], keyed by the day's
     * noon timestamp. If a day somehow holds several entries they are merged.
     */
    suspend fun loadWeek(weekStart: Long): Map<Long, DayData> {
        val entries = dao.getEntriesOnce(weekStart, addDays(weekStart, 7) - 1)
        return entries.groupBy { localNoon(it.date) }.mapValues { (_, list) ->
            DayData(
                hours = list.sumOf { it.hoursWorked },
                site = list.map { it.siteName }.filter { it.isNotBlank() }.distinct().joinToString(" / "),
                travel = list.any { it.travelReimbursed },
                parking = list.sumOf { it.parkingAmount }
            )
        }
    }

    /** Saves the day as a single entry, or removes it when everything is blank. */
    fun saveDay(day: Long, data: DayData) {
        viewModelScope.launch(writer) {
            try {
                val (start, end) = dayRange(day)
                val existing = dao.getEntriesOnce(start, end).firstOrNull()
                dao.deleteInRange(start, end)
                val blank = data.hours <= 0 && data.site.isBlank() && !data.travel && data.parking <= 0
                if (blank) return@launch

                dao.insertEntry(
                    SiteTimeEntry(
                        date = localNoon(day),
                        siteName = data.site.trim(),
                        siteAddress = existing?.siteAddress ?: "",
                        jobNumber = existing?.jobNumber ?: "",
                        hoursWorked = data.hours,
                        workSummary = existing?.workSummary ?: "",
                        travelReimbursed = data.travel,
                        parkingAmount = data.parking
                    )
                )
                if (data.site.isNotBlank()) {
                    dao.insertSavedLocation(SavedLocation(siteName = data.site.trim(), siteAddress = existing?.siteAddress ?: ""))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearWeek(weekStart: Long) {
        viewModelScope.launch(writer) {
            dao.deleteInRange(weekStart, addDays(weekStart, 7) - 1)
        }
    }
}
