package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_eq_presets")
data class CustomEqPresetEntity(
    @PrimaryKey
    val name: String,
    val gainsJson: String, // JSON float array of 10 band gains
    val preampDb: Float = 14.1f,
    val createdAt: String
)
