# Project Plan

Timesheet MVP (Worker-First) with added History/Stats.

## Project Brief

# Project Brief: Timesheet MVP (Worker-First)

## Features
- **Punch-In/Out Tracking**: A simple, worker-focused interface for recording start and end times, including manual entry for shift corrections.
- **Weekly Overview**: A primary dashboard that breaks down hours logged in the current week for quick review.
- **History & Stats**: A comprehensive reporting section that tracks total hours across past weekly, monthly, and Year-to-Date (YTD) timeframes.
- **Shift Management**: Tools to view, edit, or delete historical time entries to maintain accurate records.

## High-Level Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Navigation**: Jetpack Navigation 3 (State-driven)
- **Adaptive Strategy**: Compose Material Adaptive library for optimized phone and tablet layouts.
- **Concurrency**: Kotlin Coroutines & Flow for reactive state management.
- **Architecture**: MVVM (Model-View-ViewModel) with a focus on unidirectional data flow.

---
> [!NOTE]
> This MVP focuses on local state management and immediate worker utility, adhering to modern Android development standards using Navigation 3 and Adaptive components.

## Implementation Steps
**Total Duration:** 24m 26s

### Task_1_Data_Layer_and_ViewModel: Implement the Room database layer including SiteTimeEntry and SavedLocation entities, DAOs for site history and weekly filtering, and a ViewModel to expose UI state via StateFlow.
- **Status:** COMPLETED
- **Updates:** Implemented Room entities (SiteTimeEntry, SavedLocation), DAO, and Database. Created TimesheetViewModel with StateFlow for managing entries and location suggestions. Auto-saving of locations is implemented. Compilation errors in the existing WeeklyHoursScreen.kt were also resolved to ensure a clean build.
- **Acceptance Criteria:**
  - Room database and DAOs implemented
  - ViewModel manages state for time entries and site suggestions
  - Offline-first persistence confirmed
- **Duration:** 2m 27s

### Task_2_Daily_Entry_UI: Develop the Daily Entry Screen featuring a DatePicker, an autocomplete field for site names/addresses, and tactile +/- 0.5h buttons for quick hour logging.
- **Status:** COMPLETED
- **Updates:** Implemented the DailyEntryScreen.kt with DatePicker, site autocomplete, quick hour adjustment (+/- 0.5h), and fields for travel/parking. Integrated the screen using Navigation 3 in MainActivity.kt. Added a FloatingActionButton to the WeeklyHoursScreen for navigating to the entry screen. Generated a new adaptive app icon.
- **Acceptance Criteria:**
  - Date picker defaults to today
  - Autocomplete suggests previous sites
  - Hour adjustment buttons work correctly
  - Entries save to Room DB
- **Duration:** 4m 43s

### Task_3_Weekly_Overview_and_Sharing: Complete the Weekly Overview Screen to group entries by day, display daily and weekly totals, and implement the 'Share to Boss' SMS export functionality.
- **Status:** COMPLETED
- **Updates:** WeeklyOverviewScreen.kt is fully implemented with Material 3 styling, daily/weekly totals, sharing functionality, and edge-to-edge support. Task 3 is verified.
- **Acceptance Criteria:**
  - Entries grouped by day with correct totals
  - Weekly grand total is accurate
  - Share button generates a formatted SMS summary
- **Duration:** 1m 44s

### Task_4_History_and_Stats_Screen: Develop the History & Stats Screen to display summaries for previous weeks, current month, and Year-to-Date (YTD) work hours.
- **Status:** COMPLETED
- **Updates:** Implemented HistoryScreen.kt with Month/YTD stats and a list of past weeks. Updated TimesheetViewModel with aggregation logic. Integrated a Bottom Navigation Bar in MainActivity.kt to toggle between Home (Weekly Overview) and History. Navigation 3 routing updated. Verified build.
- **Acceptance Criteria:**
  - History screen shows breakdown for Month and YTD
  - Calculations for total hours are correct
  - User can navigate to this screen from the main dashboard
- **Duration:** 2m 22s

### Task_5_Navigation_and_Final_Verification: Refine Navigation 3 routes, ensure adaptive UI patterns for different screen sizes, and perform a final run and verify of the entire application for stability and requirements.
- **Status:** COMPLETED
- **Updates:** Final verification completed. Fractional hours rounding bug fixed (Double handling verified). Crash on save resolved (navigation and DB conflict strategy fixed). Adaptive UI and Navigation 3 verified across Phone emulator. All requirements met.
- **Acceptance Criteria:**
  - Navigation between all screens is seamless
  - UI adapts to phone and tablet sizes
  - App does not crash
  - Build passes and all requirements are met
  - Make sure all existing tests pass
- **Duration:** 13m 10s

