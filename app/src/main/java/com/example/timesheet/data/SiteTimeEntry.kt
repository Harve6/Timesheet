package com.example.timesheet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "site_time_entries")
data class SiteTimeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val siteName: String,
    val siteAddress: String,
    val jobNumber: String,
    val hoursWorked: Double,
    val workSummary: String,
    val travelReimbursed: Boolean,
    val parkingAmount: Double
)
