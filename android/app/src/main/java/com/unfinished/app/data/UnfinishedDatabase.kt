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
        CaptureDraftEntity::class,
        SyncOperationEntity::class,
        DailyDecisionEntity::class,
        ExposureEntity::class,
        RecallSnapshotEntity::class,
        VaultSettingsEntity::class,
        SyncOutboxEntity::class,
        SyncMirrorEntity::class,
        SyncStateEntity::class
    ],
    version = 6,
    exportSchema = true
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
            }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS capture_drafts (memoryId TEXT NOT NULL PRIMARY KEY, encryptedSnapshot TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN originalPath TEXT")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN previewPath TEXT")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN mimeType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN width INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN height INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN originalBytes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN previewCipherHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE media_assets ADD COLUMN mediaState TEXT NOT NULL DEFAULT 'LEGACY'")
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (operationId TEXT NOT NULL PRIMARY KEY, recordId TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, version INTEGER NOT NULL, ciphertext TEXT NOT NULL, contentHash TEXT NOT NULL, deletedAt INTEGER, state TEXT NOT NULL, attempts INTEGER NOT NULL, createdAt INTEGER NOT NULL, lastAttemptAt INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_state ON sync_outbox(state)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_outbox_entityType_entityId ON sync_outbox(entityType, entityId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_mirror (recordId TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, version INTEGER NOT NULL, ciphertext TEXT NOT NULL, contentHash TEXT NOT NULL, deletedAt INTEGER, changeSequence INTEGER NOT NULL, PRIMARY KEY(entityType, entityId))")
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_state (\"key\" TEXT NOT NULL PRIMARY KEY, cursor INTEGER NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recall_sessions ADD COLUMN inputRecallMode TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'")
            }
        }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS memory_originals (memoryId TEXT NOT NULL PRIMARY KEY, encryptedContent TEXT NOT NULL, contentHash TEXT NOT NULL, sealedAt INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS resurface_policies (memoryId TEXT NOT NULL PRIMARY KEY, mode TEXT NOT NULL, minDormantDays INTEGER NOT NULL, fixedDate INTEGER, notificationEnabled INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS media_assets (id TEXT NOT NULL PRIMARY KEY, memoryId TEXT NOT NULL, localUri TEXT, remoteObjectId TEXT, sha256 TEXT NOT NULL, uploadState TEXT NOT NULL, deletedAt INTEGER)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_assets_memoryId ON media_assets(memoryId)")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                listOf(
                    "ALTER TABLE memories ADD COLUMN userTitle TEXT",
                    "ALTER TABLE memories ADD COLUMN titleSource TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE memories ADD COLUMN exposureHistory TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE memories ADD COLUMN lastOriginalExposureAt INTEGER",
                    "ALTER TABLE memories ADD COLUMN cooldownUntil INTEGER",
                    "ALTER TABLE memories ADD COLUMN deferUntil INTEGER",
                    "ALTER TABLE memory_cues ADD COLUMN authorizationSource TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE resurface_policies ADD COLUMN randomAllowed INTEGER NOT NULL DEFAULT 1",
                    "ALTER TABLE resurface_policies ADD COLUMN planId TEXT",
                    "ALTER TABLE resurface_policies ADD COLUMN dateSource TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE resurface_policies ADD COLUMN planCompletedAt INTEGER",
                    "ALTER TABLE resurface_policies ADD COLUMN allowSerendipityAfterFixedDate INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE resurface_events ADD COLUMN source TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE resurface_events ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE resurface_events ADD COLUMN dueAt INTEGER",
                    "ALTER TABLE resurface_events ADD COLUMN fixedPlanId TEXT",
                    "ALTER TABLE resurface_events ADD COLUMN activeScope TEXT",
                    "ALTER TABLE resurface_events ADD COLUMN localDateKey TEXT",
                    "ALTER TABLE resurface_events ADD COLUMN zoneId TEXT",
                    "ALTER TABLE resurface_events ADD COLUMN legacyState TEXT",
                    "ALTER TABLE recall_sessions ADD COLUMN source TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE recall_sessions ADD COLUMN recallMode TEXT NOT NULL DEFAULT 'LEGACY_UNKNOWN'",
                    "ALTER TABLE recall_sessions ADD COLUMN cueEngineStopped INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE recall_sessions ADD COLUMN snapshotLocked INTEGER NOT NULL DEFAULT 0",
                    "ALTER TABLE recall_sessions ADD COLUMN revealActionId TEXT",
                    "ALTER TABLE recall_sessions ADD COLUMN recallOutcome TEXT",
                    "ALTER TABLE recall_sessions ADD COLUMN legacyState TEXT",
                    "ALTER TABLE media_assets ADD COLUMN cueAllowed INTEGER NOT NULL DEFAULT 0",
                    "UPDATE resurface_policies SET minDormantDays = MAX(minDormantDays, 14), randomAllowed = CASE WHEN mode = 'QUIET' THEN 0 ELSE 1 END",
                    "INSERT INTO resurface_policies (memoryId,mode,minDormantDays,fixedDate,notificationEnabled,randomAllowed,dateSource) SELECT id,CASE WHEN policy = 'FLEXIBLE' THEN 'SERENDIPITY' ELSE policy END,14,NULL,0,CASE WHEN policy = 'QUIET' THEN 0 ELSE 1 END,'LEGACY_UNKNOWN' FROM memories WHERE id NOT IN (SELECT memoryId FROM resurface_policies)",
                    "UPDATE resurface_events SET legacyState = state, createdAt = scheduledAt, state = CASE WHEN state IN ('OPENED','AVAILABLE') THEN 'LEGACY_PENDING' ELSE state END",
                    "UPDATE recall_sessions SET legacyState = state, state = CASE WHEN state = 'REVEALED' THEN 'REVEAL_REQUESTED' WHEN state = 'ANSWERED' THEN 'RECALLING' WHEN state IN ('CREATED','CUEING') THEN 'RESURFACED' ELSE state END",
                    "CREATE UNIQUE INDEX index_resurface_events_activeScope ON resurface_events(activeScope)",
                    "CREATE UNIQUE INDEX index_resurface_events_fixedPlanId ON resurface_events(fixedPlanId)",
                    "CREATE TABLE daily_decisions (scopeId TEXT NOT NULL, localDateKey TEXT NOT NULL, zoneId TEXT NOT NULL, evaluatedAt INTEGER, autoResult TEXT, chosenMemoryId TEXT, quotaEventId TEXT, quotaConsumedAt INTEGER, ruleVersion TEXT NOT NULL, PRIMARY KEY(scopeId, localDateKey))",
                    "CREATE TABLE exposures (actionId TEXT NOT NULL, kind TEXT NOT NULL, contentScope TEXT NOT NULL, memoryId TEXT NOT NULL, sessionId TEXT, source TEXT NOT NULL, observedAt INTEGER NOT NULL, localDateKey TEXT NOT NULL, zoneId TEXT NOT NULL, PRIMARY KEY(actionId, kind, contentScope))",
                    "CREATE INDEX index_exposures_memoryId ON exposures(memoryId)",
                    "CREATE INDEX index_exposures_sessionId ON exposures(sessionId)",
                    "CREATE TABLE recall_snapshots (actionId TEXT NOT NULL PRIMARY KEY, sessionId TEXT NOT NULL, encryptedContent TEXT NOT NULL, recallMode TEXT NOT NULL, shownCueIds TEXT NOT NULL, createdAt INTEGER NOT NULL)",
                    "CREATE INDEX index_recall_snapshots_sessionId ON recall_snapshots(sessionId)",
                    "CREATE TABLE vault_settings (scopeId TEXT NOT NULL PRIMARY KEY, randomEnabled INTEGER NOT NULL)"
                ).forEach(db::execSQL)
            }
        }
    }
}
