package com.ammu.player.data.local.dao

import androidx.room.*
import com.ammu.player.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY id DESC")
    fun getAllLogsFlow(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log ORDER BY id DESC")
    suspend fun getAllLogs(): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity): Long

    @Query("DELETE FROM audit_log")
    suspend fun clearLogs()
}
