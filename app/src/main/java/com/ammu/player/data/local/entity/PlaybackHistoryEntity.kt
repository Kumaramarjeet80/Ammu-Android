package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackName: String,
    val playlistName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val dateIst: String
)
