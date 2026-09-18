package com.ammu.player

import android.app.Application
import com.ammu.player.audio.AudioPlaybackManager
import com.ammu.player.crypto.BackupManager
import com.ammu.player.data.local.AmmuDatabase
import com.ammu.player.data.repository.AmmuRepository
import com.ammu.player.data.repository.StorageAuditor

class AmmuApplication : Application() {

    lateinit var database: AmmuDatabase
        private set

    lateinit var repository: AmmuRepository
        private set

    lateinit var playbackManager: AudioPlaybackManager
        private set

    lateinit var backupManager: BackupManager
        private set

    lateinit var storageAuditor: StorageAuditor
        private set

    override fun onCreate() {
        super.onCreate()
        database = AmmuDatabase.getInstance(this)
        repository = AmmuRepository(this)
        playbackManager = AudioPlaybackManager(this, repository)
        backupManager = BackupManager(this)
        storageAuditor = StorageAuditor(this)
    }
}
