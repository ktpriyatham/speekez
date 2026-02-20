package com.speekez.data.dao

import androidx.room.*
import com.speekez.data.entity.DailyStats
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStatsDao {
    @Query("SELECT * FROM daily_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<DailyStats>>

    @Query("SELECT * FROM daily_stats WHERE date = :date")
    suspend fun getStatsByDate(date: String): DailyStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStats(stats: DailyStats)

    @Query("SELECT SUM(word_count) FROM daily_stats")
    fun getTotalWordCount(): Flow<Int?>

    @Query("SELECT SUM(recording_count) FROM daily_stats")
    fun getTotalRecordingCount(): Flow<Int?>
}
