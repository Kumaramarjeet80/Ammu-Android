package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trimmed_clips",
    indices = [Index(value = ["songName"])]
)
data class TrimmedClipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songName: String,
    val clipName: String,
    val filePath: String,
    val durationSec: Float,
    val createdAt: String
)
