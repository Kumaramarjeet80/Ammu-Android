package com.ammu.player.data.local.dao

import androidx.room.*
import com.ammu.player.data.local.entity.LyricsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricsDao {
    @Query("SELECT text FROM lyrics WHERE songKey = :songKey LIMIT 1")
    fun getLyricsFlow(songKey: String): Flow<String?>

    @Query("SELECT text FROM lyrics WHERE songKey = :songKey LIMIT 1")
    suspend fun getLyrics(songKey: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyrics(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE songKey = :songKey")
    suspend fun deleteLyrics(songKey: String)
}
