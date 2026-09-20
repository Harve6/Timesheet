package com.example.timesheet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TimesheetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: SiteTimeEntry): Long

    @Update
    suspend fun updateEntry(entry: SiteTimeEntry)

    @Delete
    suspend fun deleteEntry(entry: SiteTimeEntry)

    @Query("SELECT * FROM site_time_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<SiteTimeEntry>>

    @Query("SELECT * FROM site_time_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getEntriesForDateRange(startDate: Long, endDate: Long): Flow<List<SiteTimeEntry>>

    @Query("SELECT * FROM site_time_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, id ASC")
    suspend fun getEntriesOnce(startDate: Long, endDate: Long): List<SiteTimeEntry>

    @Query("DELETE FROM site_time_entries WHERE date >= :startDate AND date <= :endDate")
    suspend fun deleteInRange(startDate: Long, endDate: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedLocation(location: SavedLocation)

    @Query("SELECT * FROM saved_locations ORDER BY siteName ASC")
    fun getAllSavedLocations(): Flow<List<SavedLocation>>
}
