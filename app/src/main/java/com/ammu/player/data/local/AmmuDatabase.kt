package com.ammu.player.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ammu.player.data.local.dao.*
import com.ammu.player.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        FavoriteEntity::class,
        LyricsEntity::class,
        TimestampEntity::class,
        TrimmedClipEntity::class,
        AuditLogEntity::class,
        PlaybackHistoryEntity::class,
        StatsEntity::class,
        DailyListeningTimeEntity::class,
        CustomEqPresetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AmmuDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun timestampDao(): TimestampDao
    abstract fun trimmedClipDao(): TrimmedClipDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun statsDao(): StatsDao
    abstract fun eqPresetDao(): EqPresetDao

    companion object {
        const val DATABASE_NAME = "ammu_music.db"

        const val PLAYLIST_ALL = "all"
        const val PLAYLIST_FAVORITES = "favorites"
        const val PLAYLIST_SMART_ROTATION = "smart_rotation"
        const val PLAYLIST_SMART_RECENT = "smart_recent"
        const val PLAYLIST_SMART_UNPLAYED = "smart_unplayed"

        @Volatile
        private var INSTANCE: AmmuDatabase? = null

        fun getInstance(context: Context): AmmuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmmuDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-seed default favorites playlist
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).playlistDao().insertPlaylist(
                                    PlaylistEntity(
                                        id = PLAYLIST_FAVORITES,
                                        name = "Favorites",
                                        originalName = "Favorites",
                                        authorName = "Amarjeet Kumar",
                                        createdAt = "Local Storage"
                                    )
                                )
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
