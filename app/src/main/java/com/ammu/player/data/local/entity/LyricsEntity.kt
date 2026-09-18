package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey
    val songKey: String, // Normalized track name
    val text: String,
    val updatedAt: Long = System.currentTimeMillis()
)
