package com.ammu.player.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ammu.player.AmmuApplication
import com.ammu.player.crypto.ExportOptions
import com.ammu.player.crypto.ImportPermissions
import com.ammu.player.crypto.ImportVerificationResult
import com.ammu.player.crypto.SecuritySuite
import com.ammu.player.data.local.AmmuDatabase
import com.ammu.player.data.local.entity.*
import com.ammu.player.data.repository.StorageAuditSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class SortMode { DEFAULT, AZ, ZA, SIZE_DESC, SIZE_ASC }
enum class PlayerDrawer { VOLUME, EQ, TIMESTAMPS, LOOPER, LYRICS, QUEUE, CLIPS }

sealed interface AppDialog {
    data object Settings : AppDialog
    data object Export : AppDialog
    data class Verification(val result: ImportVerificationResult) : AppDialog
    data object Auditor : AppDialog
    data class Trimmer(val track: TrackEntity) : AppDialog
    data object Insights : AppDialog
    data class EditPlaylist(val playlist: PlaylistEntity) : AppDialog
    data object AdminAuth : AppDialog
}

data class UndoToastAction(
    val message: String,
    val onUndo: () -> Unit,
    val onCommit: () -> Unit
)

class AmmuMainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AmmuApplication
    private val repository = app.repository
    val playbackManager = app.playbackManager
    val dspEngine = app.playbackManager.let { 
        // Access service's DSP engine via DspEngine
        com.ammu.player.audio.DspEngine()
    }

    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists.asStateFlow()

    private val _activePlaylistId = MutableStateFlow(AmmuDatabase.PLAYLIST_ALL)
    val activePlaylistId: StateFlow<String> = _activePlaylistId.asStateFlow()

    private val _rawTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val rawTracks: StateFlow<List<TrackEntity>> = _rawTracks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.DEFAULT)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _multiSelectMode = MutableStateFlow(false)
    val multiSelectMode: StateFlow<Boolean> = _multiSelectMode.asStateFlow()

    private val _selectedTrackIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTrackIds: StateFlow<Set<Long>> = _selectedTrackIds.asStateFlow()

    private val _activeDrawer = MutableStateFlow<PlayerDrawer?>(null)
    val activeDrawer: StateFlow<PlayerDrawer?> = _activeDrawer.asStateFlow()

    private val _isAmoledMode = MutableStateFlow(true)
    val isAmoledMode: StateFlow<Boolean> = _isAmoledMode.asStateFlow()

    private val _isVinylMode = MutableStateFlow(false)
    val isVinylMode: StateFlow<Boolean> = _isVinylMode.asStateFlow()

    private val _dialogState = MutableStateFlow<AppDialog?>(null)
    val dialogState: StateFlow<AppDialog?> = _dialogState.asStateFlow()

    private val _undoAction = MutableStateFlow<UndoToastAction?>(null)
    val undoAction: StateFlow<UndoToastAction?> = _undoAction.asStateFlow()
    private var undoTimerJob: Job? = null

    // Heart Particle & Emotional Reaction Triggers
    private val _heartBurstKey = MutableStateFlow(0L)
    val heartBurstKey: StateFlow<Long> = _heartBurstKey.asStateFlow()

    private val _isLastActionLiked = MutableStateFlow(false)
    val isLastActionLiked: StateFlow<Boolean> = _isLastActionLiked.asStateFlow()

    private val _emotionalToastKey = MutableStateFlow(0L)
    val emotionalToastKey: StateFlow<Long> = _emotionalToastKey.asStateFlow()

    // Storage Auditor
    private val _storageAuditSummary = MutableStateFlow<StorageAuditSummary?>(null)
    val storageAuditSummary: StateFlow<StorageAuditSummary?> = _storageAuditSummary.asStateFlow()

    private val _isScanningStorage = MutableStateFlow(false)
    val isScanningStorage: StateFlow<Boolean> = _isScanningStorage.asStateFlow()

    // Trimmer
    private val _isTrimming = MutableStateFlow(false)
    val isTrimming: StateFlow<Boolean> = _isTrimming.asStateFlow()

    // EQ Presets
    val customPresets: StateFlow<List<CustomEqPresetEntity>> = repository.getEqPresetsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Filtered and Sorted Tracks Flow
    val displayedTracks: StateFlow<List<TrackEntity>> = combine(
        _rawTracks,
        _searchQuery,
        _sortMode
    ) { tracks, query, sort ->
        var list = if (query.isBlank()) tracks else tracks.filter { it.name.contains(query, ignoreCase = true) }
        list = when (sort) {
            SortMode.AZ -> list.sortedBy { it.name.lowercase() }
            SortMode.ZA -> list.sortedByDescending { it.name.lowercase() }
            SortMode.SIZE_DESC -> list.sortedByDescending { it.fileSize }
            SortMode.SIZE_ASC -> list.sortedBy { it.fileSize }
            SortMode.DEFAULT -> list.sortedBy { it.orderIndex }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadPlaylists()
        loadTracks()
    }

    private fun loadPlaylists() {
        viewModelScope.launch {
            repository.getAllPlaylistsFlow().collect { userPlaylists ->
                val smartPlaylists = listOf(
                    PlaylistEntity(id = AmmuDatabase.PLAYLIST_ALL, name = "All", authorName = "Amarjeet Kumar"),
                    PlaylistEntity(id = AmmuDatabase.PLAYLIST_SMART_ROTATION, name = "🔥 Heavy Rotation", isSmart = true),
                    PlaylistEntity(id = AmmuDatabase.PLAYLIST_SMART_RECENT, name = "🕒 Recently Added", isSmart = true),
                    PlaylistEntity(id = AmmuDatabase.PLAYLIST_SMART_UNPLAYED, name = "💤 Unplayed", isSmart = true)
                )
                _playlists.value = smartPlaylists + userPlaylists
            }
        }
    }

    fun selectPlaylist(playlistId: String) {
        _activePlaylistId.value = playlistId
        loadTracks()
    }

    private fun loadTracks() {
        viewModelScope.launch {
            val tracks = repository.getTracksForPlaylist(_activePlaylistId.value)
            _rawTracks.value = tracks
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: SortMode) {
        _sortMode.value = mode
    }

    fun toggleAmoledMode() {
        _isAmoledMode.value = !_isAmoledMode.value
    }

    fun toggleVinylMode() {
        _isVinylMode.value = !_isVinylMode.value
    }

    fun toggleDrawer(drawer: PlayerDrawer) {
        _activeDrawer.value = if (_activeDrawer.value == drawer) null else drawer
    }

    fun openDialog(dialog: AppDialog) {
        _dialogState.value = dialog
    }

    fun dismissDialog() {
        _dialogState.value = null
    }

    // Playback
    fun playTrack(track: TrackEntity) {
        val playlistName = _playlists.value.find { it.id == _activePlaylistId.value }?.name ?: "All"
        playbackManager.playTrackFromQueue(track, _rawTracks.value, playlistName)
    }

    // Favorites
    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            val isNowFav = repository.toggleFavorite(track)
            val now = System.currentTimeMillis()
            _isLastActionLiked.value = isNowFav
            _heartBurstKey.value = now
            _emotionalToastKey.value = now
        }
    }

    // Single Track Delete with 5s Undo Toast
    fun deleteTrackWithUndo(track: TrackEntity) {
        viewModelScope.launch {
            val originalList = _rawTracks.value
            _rawTracks.value = originalList.filter { it.id != track.id }

            showUndoToast(
                message = "Removed \"${track.name}\"",
                onUndo = {
                    _rawTracks.value = originalList
                },
                onCommit = {
                    viewModelScope.launch {
                        repository.deleteTrack(track.id)
                    }
                }
            )
        }
    }

    // Multi-Select Batch Actions
    fun toggleSelectTrack(trackId: Long) {
        val current = _selectedTrackIds.value.toMutableSet()
        if (current.contains(trackId)) current.remove(trackId) else current.add(trackId)
        _selectedTrackIds.value = current
        if (current.isEmpty()) _multiSelectMode.value = false
    }

    fun startMultiSelect(trackId: Long) {
        _multiSelectMode.value = true
        _selectedTrackIds.value = setOf(trackId)
    }

    fun selectAllTracks() {
        val allIds = _rawTracks.value.map { it.id }.toSet()
        if (_selectedTrackIds.value.size == allIds.size) {
            _selectedTrackIds.value = emptySet()
            _multiSelectMode.value = false
        } else {
            _selectedTrackIds.value = allIds
        }
    }

    fun deleteSelectedTracksWithUndo() {
        val idsToDelete = _selectedTrackIds.value
        val originalList = _rawTracks.value
        val tracksToDelete = originalList.filter { idsToDelete.contains(it.id) }

        _rawTracks.value = originalList.filter { !idsToDelete.contains(it.id) }
        _selectedTrackIds.value = emptySet()
        _multiSelectMode.value = false

        showUndoToast(
            message = "Deleted ${tracksToDelete.size} tracks",
            onUndo = {
                _rawTracks.value = originalList
            },
            onCommit = {
                viewModelScope.launch {
                    tracksToDelete.forEach { repository.deleteTrack(it.id) }
                }
            }
        )
    }

    // Clean clutter tags from titles
    fun cleanSongTitles(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.cleanSongTitles()
            loadTracks()
            onResult(count)
        }
    }

    // Import Audio files
    fun importAudioFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val targetId = if (_activePlaylistId.value == AmmuDatabase.PLAYLIST_ALL || _activePlaylistId.value.startsWith("smart_")) {
                AmmuDatabase.PLAYLIST_FAVORITES
            } else {
                _activePlaylistId.value
            }

            uris.forEach { uri ->
                repository.importAudioFile(uri, targetId)
            }
            loadTracks()
        }
    }

    // Undo Toast timer
    private fun showUndoToast(message: String, onUndo: () -> Unit, onCommit: () -> Unit) {
        undoTimerJob?.cancel()
        _undoAction.value?.onCommit?.invoke()

        val action = UndoToastAction(message, onUndo, onCommit)
        _undoAction.value = action

        undoTimerJob = viewModelScope.launch {
            delay(5000)
            if (_undoAction.value == action) {
                action.onCommit()
                _undoAction.value = null
            }
        }
    }

    fun triggerUndo() {
        undoTimerJob?.cancel()
        _undoAction.value?.onUndo?.invoke()
        _undoAction.value = null
    }

    // Storage Auditor
    fun runStorageAudit() {
        viewModelScope.launch {
            _isScanningStorage.value = true
            val summary = app.storageAuditor.scanDuplicates()
            _storageAuditSummary.value = summary
            _isScanningStorage.value = false
        }
    }

    fun purgeStorageDuplicates(duplicateIds: Set<Long>) {
        viewModelScope.launch {
            app.storageAuditor.purgeDuplicates(duplicateIds)
            runStorageAudit()
            loadTracks()
        }
    }

    // Real MP3 Audio Trimmer
    fun trimAudio(track: TrackEntity, startSec: Float, endSec: Float, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isTrimming.value = true
            try {
                val clipFileName = "${track.name.substringBeforeLast(".")}_clip_${startSec.toInt()}-${endSec.toInt()}.mp3"
                val outFile = com.ammu.player.audio.Mp3AudioTrimmer.trimAudio(
                    context = getApplication(),
                    sourceFilePath = track.filePath,
                    outputFileName = clipFileName,
                    startSec = startSec,
                    endSec = endSec
                ) {}

                repository.saveTrimmedClip(
                    TrimmedClipEntity(
                        songName = track.name,
                        clipName = clipFileName,
                        filePath = outFile.absolutePath,
                        durationSec = endSec - startSec,
                        createdAt = SecuritySuite.getIndianStandardTime()
                    )
                )
                _isTrimming.value = false
                dismissDialog()
                onResult(true)
            } catch (e: Exception) {
                _isTrimming.value = false
                onResult(false)
            }
        }
    }

    // Backup & Export
    fun executeExport(options: ExportOptions, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val path = app.backupManager.executeExport(options) { _, _ -> }
            dismissDialog()
            onResult(path)
        }
    }

    fun verifyBackupPayload(jsonStr: String, onVerified: (ImportVerificationResult) -> Unit) {
        viewModelScope.launch {
            val jsonObj = JSONObject(jsonStr)
            val result = app.backupManager.verifyImportPayload(jsonObj)
            onVerified(result)
        }
    }

    fun executeImport(
        payload: JSONObject,
        selectedPlaylists: Set<String>,
        permissions: ImportPermissions,
        importMarkers: Boolean,
        importLyrics: Boolean,
        importClips: Boolean
    ) {
        viewModelScope.launch {
            app.backupManager.executeImport(
                payload = payload,
                selectedPlaylistIds = selectedPlaylists,
                permissions = permissions,
                importMarkers = importMarkers,
                importLyrics = importLyrics,
                importClips = importClips
            ) { _, _ -> }
            dismissDialog()
            loadPlaylists()
            loadTracks()
        }
    }

    // Playlists CRUD
    fun createPlaylist(name: String, author: String) {
        viewModelScope.launch {
            val entity = PlaylistEntity(
                id = "pl_${System.currentTimeMillis()}",
                name = name,
                originalName = name,
                authorName = author,
                createdAt = SecuritySuite.getIndianStandardDateOnly()
            )
            repository.savePlaylist(entity)
            selectPlaylist(entity.id)
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity, newName: String, newAuthor: String) {
        viewModelScope.launch {
            repository.savePlaylist(playlist.copy(name = newName, authorName = newAuthor))
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist.id)
            selectPlaylist(AmmuDatabase.PLAYLIST_ALL)
        }
    }

    // EQ Presets
    fun saveCustomEqPreset(name: String, gains: FloatArray) {
        viewModelScope.launch {
            val jsonGains = gains.joinToString(prefix = "[", postfix = "]")
            repository.saveEqPreset(
                CustomEqPresetEntity(
                    name = name,
                    gainsJson = jsonGains,
                    createdAt = SecuritySuite.getIndianStandardTime()
                )
            )
        }
    }

    fun deleteCustomEqPreset(name: String) {
        viewModelScope.launch {
            repository.deleteEqPreset(name)
        }
    }
}
