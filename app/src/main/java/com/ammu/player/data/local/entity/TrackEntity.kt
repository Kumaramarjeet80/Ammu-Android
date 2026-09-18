package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["name"])
    ]
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playlistId: String,
    val name: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val durationMs: Long = 0L,
    val orderIndex: Int = 0,
    val isMissing: Boolean = false,
    val downloadRestricted: Boolean = false,
    val downloadKeyHash: String = "",
    val masterKeyHash: String = "",
    val addedAt: Long = System.currentTimeMillis()
)
