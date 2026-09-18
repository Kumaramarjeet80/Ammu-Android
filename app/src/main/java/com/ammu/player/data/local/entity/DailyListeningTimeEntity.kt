package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_tracking")
data class DailyListeningTimeEntity(
    @PrimaryKey
    val dateKey: String, // "dd/MM/yyyy"
    val totalSeconds: Long = 0L,
    val hoursDistributionJson: String = "{}" // Map<Int, Long> JSON
)
