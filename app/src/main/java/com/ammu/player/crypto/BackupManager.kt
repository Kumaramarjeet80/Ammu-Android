package com.ammu.player.crypto

import android.content.Context
import android.util.Base64
import com.ammu.player.data.local.AmmuDatabase
import com.ammu.player.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ExportOptions(
    val masterKey: String = "",
    val encryptionKey: String = "",
    val creatorPasskey: String = "",
    val downloadKey: String = "",
    val selectedPlaylistIds: Set<String>? = null,
    val allowedDownloadTrackNames: Set<String> = emptySet(),
    val includeAudio: Boolean = true,
    val includeTrimmedClips: Boolean = true,
    val includeTimestamps: Boolean = true,
    val includeImages: Boolean = true
)

data class ImportVerificationResult(
    val authorName: String,
    val totalTracks: Int,
    val totalPlaylists: Int,
    val availableTracksCount: Int,
    val missingTracksCount: Int,
    val trackMatchList: List<TrackMatchItem>,
    val playlists: List<PlaylistImportMeta>,
    val parsedPayload: JSONObject
)

data class TrackMatchItem(
    val name: String,
    val isAvailable: Boolean,
    val isDownloadRestricted: Boolean
)

data class PlaylistImportMeta(
    val id: String,
    val name: String
)

data class ImportPermissions(
    val isMasterUnlocked: Boolean,
    val isDownloadUnlocked: Boolean,
    val isAuthorUnlocked: Boolean,
    val isPermanentAdmin: Boolean
)

class BackupManager(private val context: Context) {

    private val db = AmmuDatabase.getInstance(context)

    // Scoped Selective Export Pipeline
    suspend fun executeExport(options: ExportOptions, onProgress: (Float, String) -> Unit): String = withContext(Dispatchers.IO) {
        onProgress(0.1f, "Gathering database records...")
        val allPlaylists = db.playlistDao().getAllPlaylists()
        val allTracks = db.trackDao().getAllTracks()

        val masterKeyHash = if (options.masterKey.isNotBlank()) SecuritySuite.hashPasskey(options.masterKey) else ""
        val passkeyHash = if (options.creatorPasskey.isNotBlank()) SecuritySuite.hashPasskey(options.creatorPasskey) else ""
        val downloadKeyHash = if (options.downloadKey.isNotBlank()) SecuritySuite.hashPasskey(options.downloadKey) else ""

        val scopedPlaylists = allPlaylists.filter { options.selectedPlaylistIds == null || options.selectedPlaylistIds.contains(it.id) }
        val scopedTracks = allTracks.filter { options.selectedPlaylistIds == null || options.selectedPlaylistIds.contains(it.playlistId) || it.playlistId == "all" }

        val playlistsArray = JSONArray()
        scopedPlaylists.forEach { pl ->
            val plObj = JSONObject()
            plObj.put("id", pl.id)
            plObj.put("name", pl.originalName ?: pl.name)
            plObj.put("cover", if (options.includeImages) pl.coverUri ?: "" else "")
            plObj.put("authorName", pl.authorName)
            plObj.put("isAuthorLocked", options.creatorPasskey.isNotBlank() || options.masterKey.isNotBlank())
            plObj.put("passkeyHash", if (pl.passkeyHash.isNotBlank()) pl.passkeyHash else passkeyHash)
            plObj.put("masterKeyHash", masterKeyHash)
            plObj.put("downloadRestricted", options.downloadKey.isNotBlank() || options.masterKey.isNotBlank())
            plObj.put("downloadKeyHash", downloadKeyHash)
            plObj.put("createdAt", pl.createdAt)
            playlistsArray.put(plObj)
        }

        val tracksArray = JSONArray()
        scopedTracks.forEachIndexed { index, trk ->
            val pct = 0.2f + (index.toFloat() / scopedTracks.size.coerceAtLeast(1)) * 0.6f
            onProgress(pct, "Packing track: ${trk.name} (${index + 1}/${scopedTracks.size})")

            val trkObj = JSONObject()
            trkObj.put("id", trk.id)
            trkObj.put("name", trk.name)
            trkObj.put("playlistId", trk.playlistId)
            trkObj.put("orderIndex", trk.orderIndex)

            // Audio Base64 encoding if requested
            var audioBase64: String? = null
            if (options.includeAudio) {
                try {
                    val file = File(trk.filePath)
                    if (file.exists() && file.length() > 0) {
                        val bytes = file.readBytes()
                        audioBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    audioBase64 = null
                }
            }
            trkObj.put("audioBase64", audioBase64 ?: JSONObject.NULL)

            val lyrics = db.lyricsDao().getLyrics(trk.name.trim().lowercase())
            trkObj.put("lyrics", lyrics ?: "")

            val markers = if (options.includeTimestamps) db.timestampDao().getTimestamps(trk.name.trim().lowercase()) else emptyList()
            val markersArray = JSONArray()
            markers.forEach { m ->
                val mObj = JSONObject()
                mObj.put("id", m.id)
                mObj.put("name", m.name)
                mObj.put("timeMs", m.timeMs)
                markersArray.put(mObj)
            }
            trkObj.put("markers", markersArray)

            val isFav = db.favoriteDao().isFavorite(trk.name.trim().lowercase())
            trkObj.put("isFavorite", isFav)

            val isDownloadRestricted = !options.allowedDownloadTrackNames.contains(trk.name)
            trkObj.put("downloadRestricted", isDownloadRestricted)
            trkObj.put("downloadKeyHash", if (isDownloadRestricted) downloadKeyHash else "")
            trkObj.put("masterKeyHash", masterKeyHash)

            tracksArray.put(trkObj)
        }

        // Trimmed Clips
        val clipsArray = JSONArray()
        if (options.includeTrimmedClips) {
            val allClips = db.trimmedClipDao().getAllClips()
            allClips.forEach { clip ->
                val cObj = JSONObject()
                cObj.put("songName", clip.songName)
                cObj.put("clipName", clip.clipName)
                cObj.put("duration", clip.durationSec)
                cObj.put("createdAt", clip.createdAt)

                var clipB64: String? = null
                try {
                    val cFile = File(clip.filePath)
                    if (cFile.exists()) {
                        clipB64 = Base64.encodeToString(cFile.readBytes(), Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    clipB64 = null
                }
                cObj.put("clipBase64", clipB64 ?: JSONObject.NULL)
                clipsArray.put(cObj)
            }
        }

        val rootObj = JSONObject()
        rootObj.put("version", "19.0")
        rootObj.put("exportedAt", SecuritySuite.getIndianStandardTime())
        rootObj.put("generator", "Ammu Native Android")
        rootObj.put("authorName", "Amarjeet Kumar")
        rootObj.put("isAuthorLocked", options.creatorPasskey.isNotBlank() || options.masterKey.isNotBlank())
        rootObj.put("passkeyHash", passkeyHash)
        rootObj.put("masterKeyHash", masterKeyHash)
        rootObj.put("downloadKeyHash", downloadKeyHash)
        rootObj.put("hasMedia", options.includeAudio)
        rootObj.put("playlists", playlistsArray)
        rootObj.put("tracks", tracksArray)
        rootObj.put("trimmedClips", clipsArray)

        onProgress(0.9f, "Applying encryption rules...")
        var finalContent = rootObj.toString(2)
        if (options.encryptionKey.isNotBlank()) {
            finalContent = SecuritySuite.encryptPayloadAES(finalContent, options.encryptionKey)
        }

        // Save export file in app's export directory
        val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Ammu_Backups")
        if (!exportDir.exists()) exportDir.mkdirs()

        val fileName = "Ammu_${if (options.includeAudio) "FULL" else "META"}_Amarjeet's music taste backup.json"
        val targetFile = File(exportDir, fileName)
        targetFile.writeText(finalContent)

        // Log audit event
        db.auditLogDao().insertLog(
            AuditLogEntity(
                type = "EXPORT",
                description = "Exported: \"$fileName\"",
                keysDetail = "Master: ${options.masterKey.isNotBlank()} | Encryption: ${options.encryptionKey.isNotBlank()} | Passkey: ${options.creatorPasskey.isNotBlank()} | DownloadKey: ${options.downloadKey.isNotBlank()}",
                timestampIst = SecuritySuite.getIndianStandardTime()
            )
        )

        onProgress(1.0f, "Export finished!")
        targetFile.absolutePath
    }

    // Storage Matcher & Verification Audit
    suspend fun verifyImportPayload(payload: JSONObject): ImportVerificationResult = withContext(Dispatchers.IO) {
        val tracksArray = payload.optJSONArray("tracks") ?: JSONArray()
        val playlistsArray = payload.optJSONArray("playlists") ?: JSONArray()
        val authorName = payload.optString("authorName", "External Curator")

        val localTracks = db.trackDao().getAllTracks()
        val localTrackNames = localTracks.filter {
            val f = File(it.filePath)
            f.exists() && f.length() > 0
        }.map { it.name.trim().lowercase() }.toSet()

        val trackMatches = mutableListOf<TrackMatchItem>()
        var availableCount = 0
        var missingCount = 0

        for (i in 0 until tracksArray.length()) {
            val trk = tracksArray.getJSONObject(i)
            val name = trk.getString("name")
            val hasPayloadAudio = !trk.isNull("audioBase64")
            val existsInStorage = localTrackNames.contains(name.trim().lowercase())
            val isAvailable = hasPayloadAudio || existsInStorage

            if (isAvailable) availableCount++ else missingCount++

            trackMatches.add(
                TrackMatchItem(
                    name = name,
                    isAvailable = isAvailable,
                    isDownloadRestricted = trk.optBoolean("downloadRestricted", false)
                )
            )
        }

        val playlistList = mutableListOf<PlaylistImportMeta>()
        for (i in 0 until playlistsArray.length()) {
            val pl = playlistsArray.getJSONObject(i)
            playlistList.add(
                PlaylistImportMeta(
                    id = pl.getString("id"),
                    name = pl.getString("name")
                )
            )
        }

        ImportVerificationResult(
            authorName = authorName,
            totalTracks = tracksArray.length(),
            totalPlaylists = playlistsArray.length(),
            availableTracksCount = availableCount,
            missingTracksCount = missingCount,
            trackMatchList = trackMatches,
            playlists = playlistList,
            parsedPayload = payload
        )
    }

    // Final Import Execution
    suspend fun executeImport(
        payload: JSONObject,
        selectedPlaylistIds: Set<String>,
        permissions: ImportPermissions,
        importMarkers: Boolean = true,
        importLyrics: Boolean = true,
        importClips: Boolean = true,
        onProgress: (Float, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        onProgress(0.1f, "Writing playlists...")
        val authorName = payload.optString("authorName", "External Curator")
        val passkeyHash = payload.optString("passkeyHash", "")
        val masterKeyHash = payload.optString("masterKeyHash", "")
        val downloadKeyHash = payload.optString("downloadKeyHash", "")

        val playlistsArray = payload.optJSONArray("playlists") ?: JSONArray()
        for (i in 0 until playlistsArray.length()) {
            val pl = playlistsArray.getJSONObject(i)
            val id = pl.getString("id")
            if (selectedPlaylistIds.contains(id)) {
                val entity = PlaylistEntity(
                    id = id,
                    name = pl.getString("name"),
                    originalName = pl.getString("name"),
                    authorName = authorName,
                    isImported = true,
                    isAuthorLocked = !permissions.isAuthorUnlocked && !permissions.isPermanentAdmin,
                    isUnlockedLocally = permissions.isAuthorUnlocked,
                    isPermanentAdminUnlocked = permissions.isPermanentAdmin,
                    passkeyHash = passkeyHash,
                    masterKeyHash = masterKeyHash,
                    downloadKeyHash = downloadKeyHash,
                    downloadRestricted = pl.optBoolean("downloadRestricted", false) && !permissions.isDownloadUnlocked && !permissions.isPermanentAdmin,
                    isDownloadUnlocked = permissions.isDownloadUnlocked || permissions.isPermanentAdmin,
                    createdAt = SecuritySuite.getIndianStandardDateOnly()
                )
                db.playlistDao().insertPlaylist(entity)
            }
        }

        val localTracks = db.trackDao().getAllTracks()
        val localFilesMap = localTracks.filter { File(it.filePath).exists() }
            .associateBy { it.name.trim().lowercase() }

        val musicDir = File(context.filesDir, "Ammu_Audio").apply { if (!exists()) mkdirs() }

        val tracksArray = payload.optJSONArray("tracks") ?: JSONArray()
        for (i in 0 until tracksArray.length()) {
            val trk = tracksArray.getJSONObject(i)
            val trkPlId = trk.optString("playlistId", "favorites")
            if (selectedPlaylistIds.contains(trkPlId) || trkPlId == "all") {
                val name = trk.getString("name")
                val cleanName = name.trim().lowercase()
                val pct = 0.2f + (i.toFloat() / tracksArray.length().coerceAtLeast(1)) * 0.7f
                onProgress(pct, "Importing: $name (${i + 1}/${tracksArray.length()})")

                var finalPath = ""
                var isMissing = true

                if (!trk.isNull("audioBase64")) {
                    try {
                        val bytes = Base64.decode(trk.getString("audioBase64"), Base64.DEFAULT)
                        val songFile = File(musicDir, name)
                        songFile.writeBytes(bytes)
                        finalPath = songFile.absolutePath
                        isMissing = false
                    } catch (e: Exception) {
                        isMissing = true
                    }
                } else if (localFilesMap.containsKey(cleanName)) {
                    finalPath = localFilesMap[cleanName]!!.filePath
                    isMissing = false
                }

                val trackEntity = TrackEntity(
                    playlistId = trkPlId,
                    name = name,
                    filePath = finalPath,
                    isMissing = isMissing,
                    orderIndex = trk.optInt("orderIndex", i),
                    downloadRestricted = trk.optBoolean("downloadRestricted", false) && !permissions.isDownloadUnlocked && !permissions.isPermanentAdmin,
                    downloadKeyHash = trk.optString("downloadKeyHash", ""),
                    masterKeyHash = trk.optString("masterKeyHash", "")
                )
                val trackId = db.trackDao().insertTrack(trackEntity)

                if (importLyrics && trk.has("lyrics")) {
                    val lyricsText = trk.optString("lyrics", "")
                    if (lyricsText.isNotBlank()) {
                        db.lyricsDao().insertLyrics(LyricsEntity(songKey = cleanName, text = lyricsText))
                    }
                }

                if (importMarkers && trk.has("markers")) {
                    val markersArray = trk.getJSONArray("markers")
                    for (mIdx in 0 until markersArray.length()) {
                        val m = markersArray.getJSONObject(mIdx)
                        db.timestampDao().insertTimestamp(
                            TimestampEntity(
                                id = m.optString("id", "ts_${System.currentTimeMillis()}_$mIdx"),
                                songKey = cleanName,
                                timeMs = m.getLong("timeMs"),
                                name = m.getString("name")
                            )
                        )
                    }
                }

                if (trk.optBoolean("isFavorite", false)) {
                    db.favoriteDao().insertFavorite(FavoriteEntity(songKey = cleanName, trackId = trackId))
                }
            }
        }

        // Import Trimmed Clips
        if (importClips && payload.has("trimmedClips")) {
            val clipsDir = File(context.filesDir, "Ammu_Clips").apply { if (!exists()) mkdirs() }
            val clipsArray = payload.getJSONArray("trimmedClips")
            for (cIdx in 0 until clipsArray.length()) {
                val c = clipsArray.getJSONObject(cIdx)
                val clipName = c.getString("clipName")
                var clipPath = ""
                if (!c.isNull("clipBase64")) {
                    try {
                        val cBytes = Base64.decode(c.getString("clipBase64"), Base64.DEFAULT)
                        val cFile = File(clipsDir, clipName)
                        cFile.writeBytes(cBytes)
                        clipPath = cFile.absolutePath
                    } catch (e: Exception) {}
                }

                db.trimmedClipDao().insertClip(
                    TrimmedClipEntity(
                        songName = c.getString("songName"),
                        clipName = clipName,
                        filePath = clipPath,
                        durationSec = c.optDouble("duration", 30.0).toFloat(),
                        createdAt = c.optString("createdAt", SecuritySuite.getIndianStandardTime())
                    )
                )
            }
        }

        db.auditLogDao().insertLog(
            AuditLogEntity(
                type = "IMPORT",
                description = "Imported payload curated by $authorName",
                keysDetail = "Permissions: Master=${permissions.isMasterUnlocked}, Download=${permissions.isDownloadUnlocked}, Author=${permissions.isAuthorUnlocked}, SuperAdmin=${permissions.isPermanentAdmin}",
                timestampIst = SecuritySuite.getIndianStandardTime()
            )
        )

        onProgress(1.0f, "Import completed!")
    }
}
