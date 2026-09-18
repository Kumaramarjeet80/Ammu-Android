package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val originalName: String? = null,
    val coverUri: String? = null,
    val originalCoverUri: String? = null,
    val authorName: String = "Amarjeet Kumar",
    val authorAvatarUri: String? = null,
    val isSmart: Boolean = false,
    val isImported: Boolean = false,
    val isAuthorLocked: Boolean = false,
    val isUnlockedLocally: Boolean = false,
    val isPermanentAdminUnlocked: Boolean = false,
    val passkeyHash: String = "",
    val masterKeyHash: String = "",
    val downloadRestricted: Boolean = false,
    val downloadKeyHash: String = "",
    val isDownloadUnlocked: Boolean = false,
    val createdAt: String = ""
)
