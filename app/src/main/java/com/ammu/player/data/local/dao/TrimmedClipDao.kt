package com.ammu.player.data.local.dao

import androidx.room.*
import com.ammu.player.data.local.entity.TrimmedClipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrimmedClipDao {
    @Query("SELECT * FROM trimmed_clips WHERE songName = :songName ORDER BY id DESC")
    fun getClipsForSongFlow(songName: String): Flow<List<TrimmedClipEntity>>

    @Query("SELECT * FROM trimmed_clips WHERE songName = :songName ORDER BY id DESC")
    suspend fun getClipsForSong(songName: String): List<TrimmedClipEntity>

    @Query("SELECT * FROM trimmed_clips ORDER BY id DESC")
    fun getAllClipsFlow(): Flow<List<TrimmedClipEntity>>

    @Query("SELECT * FROM trimmed_clips ORDER BY id DESC")
    suspend fun getAllClips(): List<TrimmedClipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: TrimmedClipEntity): Long

    @Delete
    suspend fun deleteClip(clip: TrimmedClipEntity)

    @Query("DELETE FROM trimmed_clips WHERE id = :id")
    suspend fun deleteClipById(id: Long)
}
