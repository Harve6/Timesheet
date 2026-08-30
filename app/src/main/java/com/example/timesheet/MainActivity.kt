package com.example.timesheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.timesheet.data.TimesheetDao
import com.example.timesheet.data.TimesheetDatabase
import com.example.timesheet.ui.TimesheetViewModel
import com.example.timesheet.ui.entry.DailyEntryScreen
import com.example.timesheet.ui.history.HistoryScreen
import com.example.timesheet.ui.summary.WeeklyOverviewScreen
import com.example.timesheet.ui.theme.TimesheetTheme
import kotlinx.serialization.Serializable

@Serializable
object WeeklyHoursRoute : NavKey

@Serializable
object DailyEntryRoute : NavKey

@Serializable
object HistoryRoute : NavKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TimesheetDatabase.getDatabase(this)
        val dao = database.timesheetDao()

        setContent {
            TimesheetTheme {
                val viewModel: TimesheetViewModel = viewModel(
                    factory = TimesheetViewModelFactory(dao)
                )

                val backStack = rememberNavBackStack(WeeklyHoursRoute)
                val currentKey = backStack.last()
                val isTopLevel = currentKey is WeeklyHoursRoute || currentKey is HistoryRoute

                val adaptiveInfo = currentWindowAdaptiveInfo()
                val navSuiteType = if (isTopLevel) {
                    NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
                } else {
                    NavigationSuiteType.None
                }

                NavigationSuiteScaffold(
                    layoutType = navSuiteType,
                    navigationSuiteItems = {
                        item(
                            selected = currentKey is WeeklyHoursRoute,
                            onClick = {
                                if (currentKey !is WeeklyHoursRoute) {
                                    backStack.clear()
                                    backStack.add(WeeklyHoursRoute)
                                }
                            },
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "This Week") },
                            label = { Text("This Week") }
                        )
                        item(
                            selected = currentKey is HistoryRoute,
                            onClick = {
                                if (currentKey !is HistoryRoute) {
                                    backStack.clear()
                                    backStack.add(HistoryRoute)
                                }
                            },
                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                            label = { Text("History") }
                        )
                    }
                ) {
                    NavDisplay(
                        backStack = backStack
                    ) { key ->
                        androidx.navigation3.runtime.NavEntry(key) {
                            when (key) {
                                is WeeklyHoursRoute -> {
                                    WeeklyOverviewScreen(
                                        viewModel = viewModel,
                                        onNavigateToEntry = { backStack.add(DailyEntryRoute) }
                                    )
                                }
                                is DailyEntryRoute -> {
                                    DailyEntryScreen(
                                        viewModel = viewModel,
                                        onNavigateBack = { 
                                            if (backStack.isNotEmpty()) {
                                                backStack.removeAt(backStack.size - 1)
                                            }
                                        }
                                    )
                                }
                                is HistoryRoute -> {
                                    HistoryScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class TimesheetViewModelFactory(private val dao: TimesheetDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TimesheetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TimesheetViewModel(dao, androidx.lifecycle.SavedStateHandle()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
