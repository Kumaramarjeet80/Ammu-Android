package com.ammu.player.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.ammu.player.R
import com.ammu.player.ui.MainActivity
import kotlinx.coroutines.*

class AmmuMediaService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var exoPlayer: ExoPlayer
    val dspEngine = DspEngine()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var crossfadeJob: Job? = null
    var crossfadeDurationSec: Int = 2

    // A-B Looper state
    var loopPointAMs: Long? = null
    var loopPointBMs: Long? = null

    companion object {
        const val CHANNEL_ID = "ammu_playback_channel"
        const val NOTIFICATION_ID = 101

        const val ACTION_SKIP_FORWARD = "com.ammu.player.ACTION_SKIP_FORWARD"
        const val ACTION_SKIP_BACKWARD = "com.ammu.player.ACTION_SKIP_BACKWARD"
        const val ACTION_TOGGLE_FAVORITE = "com.ammu.player.ACTION_TOGGLE_FAVORITE"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Automatic audio focus handling
            .setWakeMode(C.WAKE_MODE_LOCAL)             // WakeLock protection
            .setHandleAudioBecomingNoisy(true)         // Unplugged headphones auto-pause
            .build()

        // Attach audio session to DSP Engine
        dspEngine.attachAudioSession(exoPlayer.audioSessionId)

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    dspEngine.attachAudioSession(exoPlayer.audioSessionId)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                checkAbLoop()
            }
        })

        // Periodic A-B Looper check
        serviceScope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    checkAbLoop()
                }
                delay(100)
            }
        }

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(CustomMediaSessionCallback())
            .build()
    }

    private fun checkAbLoop() {
        val a = loopPointAMs
        val b = loopPointBMs
        if (a != null && b != null && b > a) {
            val current = exoPlayer.currentPosition
            if (current >= b) {
                exoPlayer.seekTo(a)
            }
        }
    }

    fun executeCrossfadeTransition(onFinish: () -> Unit) {
        if (crossfadeDurationSec <= 0) {
            onFinish()
            return
        }

        crossfadeJob?.cancel()
        crossfadeJob = serviceScope.launch {
            val steps = 20
            val delayPerStep = (crossfadeDurationSec * 1000L) / steps
            val currentVol = exoPlayer.volume

            // Ramp down
            for (i in steps downTo 1) {
                exoPlayer.volume = currentVol * (i.toFloat() / steps)
                delay(delayPerStep)
            }

            onFinish()

            // Ramp up
            for (i in 1..steps) {
                exoPlayer.volume = currentVol * (i.toFloat() / steps)
                delay(delayPerStep)
            }
            exoPlayer.volume = currentVol
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                ACTION_SKIP_FORWARD -> {
                    val current = exoPlayer.currentPosition
                    exoPlayer.seekTo((current + 10_000L).coerceAtMost(exoPlayer.duration))
                }
                ACTION_SKIP_BACKWARD -> {
                    val current = exoPlayer.currentPosition
                    exoPlayer.seekTo((current - 10_000L).coerceAtLeast(0L))
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ammu Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls and background playback notification"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        crossfadeJob?.cancel()
        dspEngine.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
