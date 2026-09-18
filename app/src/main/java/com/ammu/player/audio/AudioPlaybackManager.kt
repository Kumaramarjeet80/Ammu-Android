package com.ammu.player.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ammu.player.data.local.entity.TrackEntity
import com.ammu.player.data.repository.AmmuRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

enum class PlayerRepeatMode { ALL, ONE }

class AudioPlaybackManager(
    private val context: Context,
    private val repository: AmmuRepository
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(PlayerRepeatMode.ALL)
    val repeatMode: StateFlow<PlayerRepeatMode> = _repeatMode.asStateFlow()

    private val _playingQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playingQueue: StateFlow<List<TrackEntity>> = _playingQueue.asStateFlow()

    private val _playNextQueue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val playNextQueue: StateFlow<List<TrackEntity>> = _playNextQueue.asStateFlow()

    private val _sleepTimerSec = MutableStateFlow<Int?>(null)
    val sleepTimerSec: StateFlow<Int?> = _sleepTimerSec.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Tracking listening time
    private var listeningJob: Job? = null

    val speedList = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    init {
        initializeMediaController()
        startPositionTracker()
        startListeningTracker()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, AmmuMediaService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupControllerListener()
            } catch (e: Exception) {
                // Service may still be starting
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                } else if (playbackState == Player.STATE_ENDED) {
                    playNext()
                }
            }
        })
    }

    private fun startPositionTracker() {
        scope.launch {
            while (isActive) {
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _currentPositionMs.value = controller.currentPosition.coerceAtLeast(0L)
                        _durationMs.value = controller.duration.coerceAtLeast(0L)
                    }
                }
                delay(300)
            }
        }
    }

    private fun startListeningTracker() {
        listeningJob?.cancel()
        listeningJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (_isPlaying.value) {
                    repository.addListeningTime(1)
                }
            }
        }
    }

    fun playTrackFromQueue(track: TrackEntity, playlistTracks: List<TrackEntity>, playlistName: String = "All") {
        _playingQueue.value = playlistTracks
        playTrack(track, playlistName)
    }

    fun playTrack(track: TrackEntity, playlistName: String = "All") {
        val file = File(track.filePath)
        if (!file.exists() || file.length() == 0L) {
            return
        }

        _currentTrack.value = track

        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.name)
                    .setArtist("Ammu • Amarjeet Kumar")
                    .build()
            )
            .build()

        mediaController?.let { controller ->
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }

        scope.launch {
            repository.recordTrackPlayed(track, playlistName)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun playNext() {
        val nextQueue = _playNextQueue.value
        if (nextQueue.isNotEmpty()) {
            val nextTrack = nextQueue.first()
            _playNextQueue.value = nextQueue.drop(1)
            playTrack(nextTrack)
            return
        }

        if (_repeatMode.value == PlayerRepeatMode.ONE && _currentTrack.value != null) {
            playTrack(_currentTrack.value!!)
            return
        }

        val queue = _playingQueue.value
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.name == _currentTrack.value?.name }
        val nextIndex = if (_isShuffle.value) {
            (0 until queue.size).random()
        } else {
            if (currentIndex == -1 || currentIndex >= queue.size - 1) 0 else currentIndex + 1
        }

        val nextTrack = queue[nextIndex]
        playTrack(nextTrack)
    }

    fun playPrevious() {
        val queue = _playingQueue.value
        if (queue.isEmpty()) return

        val currentIndex = queue.indexOfFirst { it.name == _currentTrack.value?.name }
        val prevIndex = if (currentIndex <= 0) queue.size - 1 else currentIndex - 1
        playTrack(queue[prevIndex])
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun seekRelative(offsetMs: Long) {
        val current = _currentPositionMs.value
        val dur = _durationMs.value.coerceAtLeast(1L)
        val target = (current + offsetMs).coerceIn(0L, dur)
        seekTo(target)
    }

    fun cycleSpeed() {
        val current = _playbackSpeed.value
        val nextIdx = (speedList.indexOfFirst { it == current } + 1) % speedList.size
        val newSpeed = speedList[nextIdx]
        _playbackSpeed.value = newSpeed
        mediaController?.setPlaybackSpeed(newSpeed)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = if (_repeatMode.value == PlayerRepeatMode.ALL) PlayerRepeatMode.ONE else PlayerRepeatMode.ALL
    }

    fun setMasterVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaController?.volume = clamped
    }

    fun queuePlayNext(track: TrackEntity) {
        _playNextQueue.value = listOf(track) + _playNextQueue.value
    }

    fun removeTrackFromPlayNext(index: Int) {
        val current = _playNextQueue.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _playNextQueue.value = current
        }
    }

    // Zone A: Drag Reorder Queue
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _playingQueue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _playingQueue.value = list
        }
    }

    // Zone B: Shift Up/Down/Delete Queue
    fun shiftQueueItem(index: Int, delta: Int) {
        val target = index + delta
        val list = _playingQueue.value.toMutableList()
        if (index in list.indices && target in list.indices) {
            val temp = list[index]
            list[index] = list[target]
            list[target] = temp
            _playingQueue.value = list
        }
    }

    fun removeQueueItem(index: Int) {
        val list = _playingQueue.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _playingQueue.value = list
        }
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _sleepTimerSec.value = null
            return
        }

        var remaining = minutes * 60
        _sleepTimerSec.value = remaining

        sleepTimerJob = scope.launch {
            while (remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerSec.value = remaining

                // Smooth fade out in the last 5 seconds
                if (remaining in 1..5) {
                    setMasterVolume(remaining / 5f)
                }
            }
            mediaController?.pause()
            setMasterVolume(1.0f)
            _sleepTimerSec.value = null
        }
    }

    fun release() {
        scope.cancel()
        sleepTimerJob?.cancel()
        listeningJob?.cancel()
        mediaController?.release()
    }
}
