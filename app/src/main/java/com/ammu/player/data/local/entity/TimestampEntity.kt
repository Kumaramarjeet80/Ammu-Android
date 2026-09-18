package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timestamps",
    indices = [Index(value = ["songKey"])]
)
data class TimestampEntity(
    @PrimaryKey
    val id: String,
    val songKey: String,
    val timeMs: Long,
    val name: String
)
