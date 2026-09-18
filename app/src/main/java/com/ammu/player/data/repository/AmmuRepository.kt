package com.ammu.player.data.repository

import android.content.Context
import android.net.Uri
import com.ammu.player.crypto.SecuritySuite
import com.ammu.player.data.local.AmmuDatabase
import com.ammu.player.data.local.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class AmmuRepository(private val context: Context) {

    private val db = AmmuDatabase.getInstance(context)

    // Playlists
    fun getAllPlaylistsFlow(): Flow<List<PlaylistEntity>> = db.playlistDao().getAllPlaylistsFlow()

    suspend fun getAllPlaylists(): List<PlaylistEntity> = db.playlistDao().getAllPlaylists()

    suspend fun savePlaylist(playlist: PlaylistEntity) = db.playlistDao().insertPlaylist(playlist)

    suspend fun deletePlaylist(playlistId: String) {
        db.playlistDao().deletePlaylistById(playlistId)
        db.trackDao().deleteTracksByPlaylist(playlistId)
    }

    // Tracks
    fun getTracksForPlaylistFlow(playlistId: String): Flow<List<TrackEntity>> {
        return when (playlistId) {
            AmmuDatabase.PLAYLIST_ALL -> db.trackDao().getAllTracksFlow()
            else -> db.trackDao().getTracksByPlaylistFlow(playlistId)
        }
    }

    suspend fun getTracksForPlaylist(playlistId: String): List<TrackEntity> = withContext(Dispatchers.IO) {
        when (playlistId) {
            AmmuDatabase.PLAYLIST_ALL -> {
                val all = db.trackDao().getAllTracks()
                val uniqueMap = linkedMapOf<String, TrackEntity>()
                all.forEach { trk ->
                    val cleanKey = trk.name.trim().lowercase()
                    if (!uniqueMap.containsKey(cleanKey)) {
                        uniqueMap[cleanKey] = trk
                    }
                }
                uniqueMap.values.toList()
            }
            AmmuDatabase.PLAYLIST_SMART_ROTATION -> {
                val all = db.trackDao().getAllTracks()
                val stats = db.statsDao().getAllStats().associateBy { it.songKey }
                all.sortedByDescending { stats[it.name.trim().lowercase()]?.playCount ?: 0 }.take(20)
            }
            AmmuDatabase.PLAYLIST_SMART_RECENT -> {
                val all = db.trackDao().getAllTracks()
                all.sortedByDescending { it.id }.take(20)
            }
            AmmuDatabase.PLAYLIST_SMART_UNPLAYED -> {
                val all = db.trackDao().getAllTracks()
                val playedKeys = db.statsDao().getAllStats()
                    .filter { it.playCount > 0 }
                    .map { it.songKey }
                    .toSet()
                all.filter { !playedKeys.contains(it.name.trim().lowercase()) }
            }
            else -> db.trackDao().getTracksByPlaylist(playlistId)
        }
    }

    suspend fun saveTrack(track: TrackEntity): Long = db.trackDao().insertTrack(track)

    suspend fun updateTrack(track: TrackEntity) = db.trackDao().updateTrack(track)

    suspend fun deleteTrack(trackId: Long) = db.trackDao().deleteTrackById(trackId)

    // Import file from Uri into app storage
    suspend fun importAudioFile(uri: Uri, targetPlaylistId: String): TrackEntity? = withContext(Dispatchers.IO) {
        val fileName = getFileNameFromUri(uri) ?: "song_${System.currentTimeMillis()}.mp3"
        val musicDir = File(context.filesDir, "Ammu_Audio").apply { if (!exists()) mkdirs() }
        val destFile = File(musicDir, fileName)

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val track = TrackEntity(
                playlistId = targetPlaylistId,
                name = fileName,
                filePath = destFile.absolutePath,
                fileSize = destFile.length(),
                isMissing = false,
                orderIndex = (db.trackDao().getAllTracks().size)
            )
            val id = db.trackDao().insertTrack(track)
            track.copy(id = id)
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }

    // Clean title clutter tags ("✨ Clean" song titles)
    suspend fun cleanSongTitles(): Int = withContext(Dispatchers.IO) {
        val allTracks = db.trackDao().getAllTracks()
        var cleanedCount = 0

        for (track in allTracks) {
            val clean = track.name
                .replace(Regex("\\.(mp3|wav|flac|m4a|aac|ogg|opus)$", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\[.*?\\]|\\(.*?\\)|_|-"), " ")
                .replace(Regex("\\b(128kbps|320kbps|download|pagalworld|songs|audio|mp3)\\b", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (clean.isNotBlank() && clean != track.name) {
                db.trackDao().updateTrack(track.copy(name = clean))
                cleanedCount++
            }
        }
        cleanedCount
    }

    // Favorites
    fun isFavoriteFlow(songKey: String): Flow<Boolean> = db.favoriteDao().isFavoriteFlow(songKey.trim().lowercase())

    suspend fun toggleFavorite(track: TrackEntity): Boolean = withContext(Dispatchers.IO) {
        val key = track.name.trim().lowercase()
        val isFav = db.favoriteDao().isFavorite(key)
        if (isFav) {
            db.favoriteDao().removeFavorite(key)
            false
        } else {
            db.favoriteDao().insertFavorite(FavoriteEntity(songKey = key, trackId = track.id))
            // Also ensure it's in the favorites playlist
            db.trackDao().insertTrack(
                track.copy(
                    id = 0,
                    playlistId = AmmuDatabase.PLAYLIST_FAVORITES,
                    orderIndex = System.currentTimeMillis().toInt()
                )
            )
            true
        }
    }

    // Lyrics
    fun getLyricsFlow(songName: String): Flow<String?> = db.lyricsDao().getLyricsFlow(songName.trim().lowercase())
    suspend fun saveLyrics(songName: String, text: String) = db.lyricsDao().insertLyrics(
        LyricsEntity(songKey = songName.trim().lowercase(), text = text)
    )

    // Timestamps
    fun getTimestampsFlow(songName: String): Flow<List<TimestampEntity>> = db.timestampDao().getTimestampsFlow(songName.trim().lowercase())
    suspend fun saveTimestamp(songName: String, timeMs: Long, name: String) = db.timestampDao().insertTimestamp(
        TimestampEntity(
            id = "ts_${System.currentTimeMillis()}",
            songKey = songName.trim().lowercase(),
            timeMs = timeMs,
            name = name
        )
    )
    suspend fun deleteTimestamp(id: String) = db.timestampDao().deleteTimestamp(id)

    // Trimmed Clips
    fun getClipsForSongFlow(songName: String): Flow<List<TrimmedClipEntity>> = db.trimmedClipDao().getClipsForSongFlow(songName)
    suspend fun saveTrimmedClip(clip: TrimmedClipEntity) = db.trimmedClipDao().insertClip(clip)
    suspend fun deleteTrimmedClip(id: Long) = db.trimmedClipDao().deleteClipById(id)

    // Analytics: Play Count & History
    suspend fun recordTrackPlayed(track: TrackEntity, playlistName: String) = withContext(Dispatchers.IO) {
        val key = track.name.trim().lowercase()
        val currentStats = db.statsDao().getStats(key)
        val newCount = (currentStats?.playCount ?: 0) + 1
        db.statsDao().insertStats(StatsEntity(songKey = key, playCount = newCount))

        db.statsDao().insertHistory(
            PlaybackHistoryEntity(
                trackName = track.name,
                playlistName = playlistName,
                dateIst = SecuritySuite.getIndianStandardTime()
            )
        )
    }

    fun getPlaybackHistoryFlow(): Flow<List<PlaybackHistoryEntity>> = db.statsDao().getHistoryFlow()
    fun getTopStatsFlow(): Flow<List<StatsEntity>> = db.statsDao().getAllStatsFlow()

    suspend fun addListeningTime(seconds: Long) = withContext(Dispatchers.IO) {
        val dateKey = SecuritySuite.getIndianStandardDateOnly()
        val existing = db.statsDao().getDailyTime(dateKey)
        val currentTotal = (existing?.totalSeconds ?: 0L) + seconds

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val distObj = try {
            JSONObject(existing?.hoursDistributionJson ?: "{}")
        } catch (e: Exception) {
            JSONObject()
        }
        val currentHourSecs = distObj.optLong(hour.toString(), 0L) + seconds
        distObj.put(hour.toString(), currentHourSecs)

        db.statsDao().insertDailyTime(
            DailyListeningTimeEntity(
                dateKey = dateKey,
                totalSeconds = currentTotal,
                hoursDistributionJson = distObj.toString()
            )
        )
    }

    fun getDailyListeningTimesFlow(): Flow<List<DailyListeningTimeEntity>> = db.statsDao().getAllDailyTimesFlow()

    // EQ Presets
    fun getEqPresetsFlow(): Flow<List<CustomEqPresetEntity>> = db.eqPresetDao().getAllPresetsFlow()
    suspend fun saveEqPreset(preset: CustomEqPresetEntity) = db.eqPresetDao().insertPreset(preset)
    suspend fun deleteEqPreset(name: String) = db.eqPresetDao().deletePresetByName(name)
}
