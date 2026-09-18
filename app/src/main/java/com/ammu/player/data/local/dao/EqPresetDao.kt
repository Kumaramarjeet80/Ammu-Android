package com.ammu.player.data.local.dao

import androidx.room.*
import com.ammu.player.data.local.entity.CustomEqPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EqPresetDao {
    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAt DESC")
    fun getAllPresetsFlow(): Flow<List<CustomEqPresetEntity>>

    @Query("SELECT * FROM custom_eq_presets ORDER BY createdAt DESC")
    suspend fun getAllPresets(): List<CustomEqPresetEntity>

    @Query("SELECT * FROM custom_eq_presets WHERE name = :name LIMIT 1")
    suspend fun getPresetByName(name: String): CustomEqPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: CustomEqPresetEntity)

    @Delete
    suspend fun deletePreset(preset: CustomEqPresetEntity)

    @Query("DELETE FROM custom_eq_presets WHERE name = :name")
    suspend fun deletePresetByName(name: String)
}
