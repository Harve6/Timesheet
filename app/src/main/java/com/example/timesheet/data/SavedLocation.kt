package com.example.timesheet.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_locations",
    indices = [Index(value = ["siteName", "siteAddress"], unique = true)]
)
data class SavedLocation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siteName: String,
    val siteAddress: String
)
