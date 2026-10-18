package com.unfinished.app.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unfinished.app.data.ContentKeyEnvelope
import com.unfinished.app.data.SyncMirrorStore
import com.unfinished.app.data.UnfinishedDatabase
import com.unfinished.app.data.VaultBootstrapClient
import com.unfinished.app.data.VaultIdentityStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Runs only when explicitly given a loopback test service; default instrumentation skips it. */
@RunWith(AndroidJUnit4::class)
class SyncEndToEndTest {
    private lateinit var database: UnfinishedDatabase
    private lateinit var store: SyncMirrorStore
    private lateinit var baseUrl: String
    private lateinit var identityStore: VaultIdentityStore
    private lateinit var keyEnvelope: ContentKeyEnvelope
    private lateinit var context: android.content.Context
    private lateinit var databaseName: String
    private lateinit var identityPreferencesName: String
    private lateinit var keyPreferencesName: String

    @Before
    fun setUp() {
        baseUrl = InstrumentationRegistry.getArguments().getString("syncBaseUrl").orEmpty()
        assumeTrue("syncBaseUrl argument is required for the loopback E2E run", baseUrl.isNotBlank())
        context = ApplicationProvider.getApplicationContext()
        val scope = UUID.randomUUID().toString()
        databaseName = "sync-e2e-$scope.db"
        identityPreferencesName = "sync-e2e-vault-$scope"
        keyPreferencesName = "sync-e2e-keys-$scope"
        database = Room.databaseBuilder(context, UnfinishedDatabase::class.java, databaseName)
            .addMigrations(UnfinishedDatabase.MIGRATION_1_2, UnfinishedDatabase.MIGRATION_2_3,
                UnfinishedDatabase.MIGRATION_3_4, UnfinishedDatabase.MIGRATION_4_5)
            .build()
        store = SyncMirrorStore(database)
        identityStore = VaultIdentityStore(context, preferencesName = identityPreferencesName)
        keyEnvelope = ContentKeyEnvelope(context, preferencesName = keyPreferencesName)
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(identityPreferencesName, android.content.Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences(keyPreferencesName, android.content.Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun bootstrapRoomOutboxPostgresPullAndDecryptDisplay() = runBlocking {
        val remote = VaultBootstrapClient(baseUrl, identityStore).bootstrap()
        assertEquals(remote, identityStore.requireRemoteCredentials())
        val contentKey = keyEnvelope.getOrCreateLocalContentKey()
        val codec = SyncPayloadCodec(contentKey, "e2e-${UUID.randomUUID()}")
        val firstId = UUID.randomUUID().toString()
        val secondId = UUID.randomUUID().toString()
        val textById = mapOf(
            firstId to "Synthetic first memory for isolated sync verification.",
            secondId to "Synthetic second memory used to verify stable pagination."
        )

        textById.forEach { (entityId, text) ->
            val context = SyncPayloadContext(remote.vaultId, "Memory", entityId, 1L)
            val ciphertext = codec.encrypt(text, context)
            store.enqueue(UUID.randomUUID().toString(), "Memory", entityId, 1L, ciphertext)
        }
        val captured = store.pending()
        assertEquals(2, captured.size)
        val gateway = HttpSyncGateway(baseUrl, identityStore)
        val coordinator = OutboxSyncCoordinator(store, gateway)
        assertTrue(coordinator.flush() > 0L)
        assertTrue(store.pending().isEmpty())

        // A lost HTTP response can safely replay the same immutable IDs and bodies.
        val replay = gateway.push(captured.map {
            EncryptedSyncRecord(it.recordId, it.operationId, it.entityType, it.entityId, it.version,
                it.ciphertext, it.contentHash, it.deletedAt)
        })
        assertEquals(2, replay.acknowledgements.size)
        assertTrue(replay.acknowledgements.all { it.outcome == "APPLIED" })

        val pageOne = coordinator.pullAndApply(limit = 1)
        assertEquals(1, pageOne.records.size)
        assertTrue(pageOne.hasMore)
        assertEquals(pageOne.records.single().changeSequence, store.cursor())
        val pageTwo = coordinator.pullAndApply(limit = 1)
        assertEquals(1, pageTwo.records.size)
        assertFalse(pageTwo.hasMore)
        assertEquals(pageTwo.records.single().changeSequence, store.cursor())

        textById.forEach { (entityId, expectedText) ->
            val mirror = store.mirror("Memory", entityId) ?: error("pulled Room mirror missing")
            val context = SyncPayloadContext(remote.vaultId, "Memory", entityId, mirror.version)
            assertEquals(expectedText, codec.decrypt(mirror.ciphertext, context))
        }
    }
}
