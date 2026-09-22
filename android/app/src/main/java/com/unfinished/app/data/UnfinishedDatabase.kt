package com.unfinished.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MemoryEntity::class,
        MemoryOriginalEntity::class,
        ResurfacePolicyEntity::class,
        CueEntity::class,
        ResurfaceEventEntity::class,
        RecallSessionEntity::class,
        ReflectionEntity::class,
        MediaAssetEntity::class,
        SyncOperationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class UnfinishedDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var instance: UnfinishedDatabase? = null

        fun get(context: Context): UnfinishedDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    UnfinishedDatabase::class.java,
                    "unfinished.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS memory_originals (memoryId TEXT NOT NULL PRIMARY KEY, encryptedContent TEXT NOT NULL, contentHash TEXT NOT NULL, sealedAt INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS resurface_policies (memoryId TEXT NOT NULL PRIMARY KEY, mode TEXT NOT NULL, minDormantDays INTEGER NOT NULL, fixedDate INTEGER, notificationEnabled INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS media_assets (id TEXT NOT NULL PRIMARY KEY, memoryId TEXT NOT NULL, localUri TEXT, remoteObjectId TEXT, sha256 TEXT NOT NULL, uploadState TEXT NOT NULL, deletedAt INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_assets_memoryId ON media_assets(memoryId)")
            }
        }
    }
}
