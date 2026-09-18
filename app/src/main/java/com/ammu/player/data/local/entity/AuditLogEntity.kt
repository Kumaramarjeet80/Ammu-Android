package com.ammu.player.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPORT" or "IMPORT"
    val description: String,
    val keysDetail: String = "",
    val timestampIst: String
)
