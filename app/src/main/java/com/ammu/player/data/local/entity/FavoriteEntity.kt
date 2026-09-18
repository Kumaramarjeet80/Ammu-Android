package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val songKey: String, // Normalized track name (lowercase trimmed)
    val trackId: Long = 0,
    val favoritedAt: Long = System.currentTimeMillis()
)
