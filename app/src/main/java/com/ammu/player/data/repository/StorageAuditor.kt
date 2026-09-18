package com.ammu.player.data.repository

import android.content.Context
import com.ammu.player.data.local.AmmuDatabase
import com.ammu.player.data.local.entity.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

data class StorageAuditSummary(
    val totalSizeBytes: Long,
    val duplicateCount: Int,
    val duplicateTracks: List<DuplicateTrackItem>
)

data class DuplicateTrackItem(
    val trackId: Long,
    val name: String,
    val sizeBytes: Long,
    val filePath: String
)

class StorageAuditor(private val context: Context) {

    private val db = AmmuDatabase.getInstance(context)

    suspend fun scanDuplicates(): StorageAuditSummary = withContext(Dispatchers.IO) {
        val allTracks = db.trackDao().getAllTracks()
        var totalBytes = 0L
        val signatureMap = mutableMapOf<String, Long>() // signature -> first track id
        val duplicates = mutableListOf<DuplicateTrackItem>()

        for (track in allTracks) {
            val file = File(track.filePath)
            if (file.exists() && file.length() > 0) {
                val size = file.length()
                totalBytes += size

                // Signature: cleanName_size
                val signature = "${track.name.trim().lowercase()}_$size"
                if (signatureMap.containsKey(signature)) {
                    duplicates.add(
                        DuplicateTrackItem(
                            trackId = track.id,
                            name = track.name,
                            sizeBytes = size,
                            filePath = track.filePath
                        )
                    )
                } else {
                    signatureMap[signature] = track.id
                }
            }
        }

        StorageAuditSummary(
            totalSizeBytes = totalBytes,
            duplicateCount = duplicates.size,
            duplicateTracks = duplicates
        )
    }

    suspend fun purgeDuplicates(duplicateTrackIds: Set<Long>): Int = withContext(Dispatchers.IO) {
        var purgedCount = 0
        for (id in duplicateTrackIds) {
            val track = db.trackDao().getTrackById(id)
            if (track != null) {
                // Delete database record
                db.trackDao().deleteTrackById(id)
                purgedCount++
            }
        }
        purgedCount
    }
}
