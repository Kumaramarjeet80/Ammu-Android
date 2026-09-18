package com.ammu.player.data.local.dao

import androidx.room.*
import com.ammu.player.data.local.entity.TimestampEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimestampDao {
    @Query("SELECT * FROM timestamps WHERE songKey = :songKey ORDER BY timeMs ASC")
    fun getTimestampsFlow(songKey: String): Flow<List<TimestampEntity>>

    @Query("SELECT * FROM timestamps WHERE songKey = :songKey ORDER BY timeMs ASC")
    suspend fun getTimestamps(songKey: String): List<TimestampEntity>

    @Query("SELECT * FROM timestamps ORDER BY timeMs ASC")
    suspend fun getAllTimestamps(): List<TimestampEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimestamp(timestamp: TimestampEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimestamps(timestamps: List<TimestampEntity>)

    @Query("DELETE FROM timestamps WHERE id = :id")
    suspend fun deleteTimestamp(id: String)

    @Query("DELETE FROM timestamps WHERE songKey = :songKey")
    suspend fun deleteTimestampsForSong(songKey: String)
}
