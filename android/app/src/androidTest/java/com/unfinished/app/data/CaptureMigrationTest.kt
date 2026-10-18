package com.unfinished.app.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unfinished.app.data.media.LocalMediaStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CaptureMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), UnfinishedDatabase::class.java
    )

    @Test fun v5MigrationRetainsOriginalAndExplicitAuthorizationAndHandlesLostSource() = runBlocking {
        val suffix = UUID.randomUUID().toString()
        val name = "capture-migration-$suffix.db"
        val memoryId = UUID.randomUUID().toString()
        val firstPhoto = UUID.randomUUID().toString()
        val lostPhoto = UUID.randomUUID().toString()
        val corruptPhoto = UUID.randomUUID().toString()
        val source = File(context.noBackupFilesDir, "capture-source-$suffix.png")
        val corruptSource = File(context.noBackupFilesDir, "capture-corrupt-$suffix.png")
        val mediaRoot = File(context.noBackupFilesDir, "capture-migration-media-$suffix")
        val bitmap = Bitmap.createBitmap(5, 7, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        corruptSource.writeBytes(byteArrayOf(1, 2, 3, 4))
        val cipher = LocalCipher("unfinished.test.text.$suffix")
        val encrypted = cipher.encrypt("synthetic v5 original")
        helper.createDatabase(name, 5).apply {
            execSQL("INSERT INTO memories (id,title,encryptedOriginal,occurredAt,state,policy,sealedAt,resurfaceCount,createdAt,updatedAt,userTitle,titleSource,exposureHistory) VALUES (?, '', ?, 123, 'SEALED', 'QUIET', 123, 0, 123, 123, NULL, 'USER_OR_NONE', 'KNOWN')", arrayOf(memoryId, encrypted))
            execSQL("INSERT INTO memory_originals VALUES (?, ?, 'synthetic-v5-hash', 123)", arrayOf(memoryId, encrypted))
            execSQL("INSERT INTO resurface_policies (memoryId,mode,minDormantDays,fixedDate,notificationEnabled,randomAllowed,dateSource,allowSerendipityAfterFixedDate) VALUES (?, 'QUIET', 14, NULL, 0, 0, 'USER_OR_NONE', 0)", arrayOf(memoryId))
            execSQL("INSERT INTO memory_cues VALUES ('synthetic-cue', ?, 'KEYWORD', ?, 'USER_WRITTEN', 1, 0, 'USER_EXPLICIT')", arrayOf(memoryId, cipher.encrypt("chosen cue")))
            execSQL("INSERT INTO media_assets VALUES (?, ?, ?, NULL, 'legacy-uri', 'LOCAL', NULL, 1)", arrayOf(firstPhoto, memoryId, Uri.fromFile(source).toString()))
            execSQL("INSERT INTO media_assets VALUES (?, ?, ?, NULL, 'legacy-uri', 'LOCAL', NULL, 0)", arrayOf(lostPhoto, memoryId, "content://synthetic.invalid/missing"))
            execSQL("INSERT INTO media_assets VALUES (?, ?, ?, NULL, 'legacy-uri', 'LOCAL', NULL, 0)", arrayOf(corruptPhoto, memoryId, Uri.fromFile(corruptSource).toString()))
            close()
        }
        helper.runMigrationsAndValidate(name, 6, true, UnfinishedDatabase.MIGRATION_5_6).close()
        val db = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name)
            .addMigrations(UnfinishedDatabase.MIGRATION_5_6).build()
        val repo = MemoryRepository(db, cipher, UUID.randomUUID().toString())
        val media = LocalMediaStore(context, repo.scopeId, rootDirectory = mediaRoot,
            keyAlias = "unfinished.test.media.$suffix")
        try {
            val gateway = LocalCaptureGateway(context, repo, media)
            val loaded = gateway.loadDraft(memoryId)!!
            assertTrue(loaded.sealed)
            assertEquals("synthetic v5 original", loaded.text)
            assertEquals("QUIET", loaded.policy)
            assertEquals(listOf("chosen cue"), loaded.cues.map { it.value })
            assertEquals(3, loaded.photos.size)
            val valid = loaded.photos.single { it.id == firstPhoto }
            val missing = loaded.photos.single { it.id == lostPhoto }
            assertFalse(loaded.photos.single { it.id == corruptPhoto }.available)
            assertTrue(valid.available)
            assertTrue(valid.cueAllowed)
            assertFalse(missing.available)
            assertFalse(missing.cueAllowed)
            assertTrue(source.delete())
            val decoded = gateway.preview(valid)
            assertEquals(5, decoded.width)
            assertEquals(7, decoded.height)
            decoded.recycle()
            assertEquals("synthetic v5 original", repo.readOriginal(memoryId))
        } finally {
            repo.closeObservations()
            db.close()
            context.deleteDatabase(name)
            source.delete()
            corruptSource.delete()
            media.recover(emptySet())
            mediaRoot.delete()
        }
    }
}
