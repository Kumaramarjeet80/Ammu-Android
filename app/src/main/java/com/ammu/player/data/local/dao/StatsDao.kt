package com.ammu.player.data.local.dao

import androidx.room.*
import com.ammu.player.data.local.entity.DailyListeningTimeEntity
import com.ammu.player.data.local.entity.PlaybackHistoryEntity
import com.ammu.player.data.local.entity.StatsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {
    // Stats
    @Query("SELECT * FROM stats WHERE songKey = :songKey LIMIT 1")
    suspend fun getStats(songKey: String): StatsEntity?

    @Query("SELECT * FROM stats ORDER BY playCount DESC")
    fun getAllStatsFlow(): Flow<List<StatsEntity>>

    @Query("SELECT * FROM stats ORDER BY playCount DESC")
    suspend fun getAllStats(): List<StatsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: StatsEntity)

    // Playback History
    @Query("SELECT * FROM playback_history ORDER BY id DESC LIMIT 50")
    fun getHistoryFlow(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history ORDER BY id DESC")
    suspend fun getAllHistory(): List<PlaybackHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlaybackHistoryEntity): Long

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    // Daily Listening Time
    @Query("SELECT * FROM time_tracking WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyTime(dateKey: String): DailyListeningTimeEntity?

    @Query("SELECT * FROM time_tracking ORDER BY dateKey DESC")
    fun getAllDailyTimesFlow(): Flow<List<DailyListeningTimeEntity>>

    @Query("SELECT * FROM time_tracking ORDER BY dateKey DESC")
    suspend fun getAllDailyTimes(): List<DailyListeningTimeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyTime(dailyTime: DailyListeningTimeEntity)
}
