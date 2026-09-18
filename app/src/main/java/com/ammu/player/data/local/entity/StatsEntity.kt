package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stats")
data class StatsEntity(
    @PrimaryKey
    val songKey: String, // Normalized track name
    val playCount: Int = 0,
    val totalTimeSeconds: Long = 0L
)
