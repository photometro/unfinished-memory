package com.unfinished.app.sync

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unfinished.app.data.ContentKeyEnvelope
import com.unfinished.app.data.SyncMirrorStore
import com.unfinished.app.data.UnfinishedDatabase
import com.unfinished.app.data.VaultIdentity
import com.unfinished.app.data.VaultIdentityStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SyncRuntimeTest {
    private lateinit var database: UnfinishedDatabase
    private lateinit var store: SyncMirrorStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), UnfinishedDatabase::class.java
        ).allowMainThreadQueries().build()
        store = SyncMirrorStore(database)
    }

    @After
    fun tearDown() { database.close() }

    @Test
    fun encryptedOutboxAcknowledgesOnlyCapturedBatchAndPullIsDecryptable() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val keyEnvelope = ContentKeyEnvelope(context)
        val contentKey = keyEnvelope.getOrCreateLocalContentKey()
        assertEquals(contentKey.toList(), keyEnvelope.getOrCreateLocalContentKey().toList())
        val codec = SyncPayloadCodec(contentKey, "key-test-1")
        val serverIdentity = "remote-vault-test"
        val entityId = UUID.randomUUID().toString()
        val recordId = UUID.randomUUID().toString()
        val operationId = UUID.randomUUID().toString()
        val payload = "A private shoreline note used only by the isolated sync test."
        val payloadContext = SyncPayloadContext(serverIdentity, "Memory", entityId, 1L)
        val ciphertext = codec.encrypt(payload, payloadContext)
        store.enqueue(recordId, "Memory", entityId, 1L, ciphertext, operationId = operationId)

        val laterOperationId = UUID.randomUUID().toString()
        val laterEntityId = UUID.randomUUID().toString()
        val gateway = object : SyncGateway {
            override suspend fun push(records: List<EncryptedSyncRecord>): PushResponse {
                assertEquals(listOf(operationId), records.map { it.operationId })
                // Simulate a new local write while the network request is in flight.
                store.enqueue(
                    UUID.randomUUID().toString(), "Memory", laterEntityId, 1L,
                    codec.encrypt("later", SyncPayloadContext(serverIdentity, "Memory", laterEntityId, 1L)),
                    operationId = laterOperationId
                )
                return PushResponse(records.map { PushAcknowledgement(it.operationId, "APPLIED", 7L) }, 7L)
            }

            override suspend fun pull(cursor: Long, limit: Int): PullBatch = PullBatch(
                listOf(EncryptedSyncRecord(recordId, operationId, "Memory", entityId, 1L, ciphertext,
                    SyncPayloadCodec.contentHash(ciphertext), null, 9L)),
                nextCursor = 9L,
                hasMore = false
            )
        }

        val coordinator = OutboxSyncCoordinator(store, gateway)
        assertEquals(7L, coordinator.flush())
        assertEquals(listOf(laterOperationId), store.pending().map { it.operationId })

        val pull = coordinator.pullAndApply()
        assertEquals(9L, pull.nextCursor)
        val mirrored = store.mirror("Memory", entityId)
        assertNotNull(mirrored)
        assertEquals(payload, codec.decrypt(mirrored!!.ciphertext, payloadContext))
        assertEquals(9L, store.cursor())
    }

    @Test
    fun wrongRecoverySecretAndConflictingPageDoNotAdvanceCursor() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val envelope = ContentKeyEnvelope(context)
        val key = envelope.getOrCreateLocalContentKey()
        val recovery = ByteArray(32) { (it + 13).toByte() }
        val recoveryEnvelope = envelope.createRecoveryEnvelope(key, recovery, "vault-a", "key-a")
        assertEquals(key.toList(), envelope.openRecoveryEnvelope(recoveryEnvelope, recovery, "vault-a").toList())

        val cipher = SyncPayloadCodec(key, "key-a")
        val id = UUID.randomUUID().toString()
        val contextA = SyncPayloadContext("vault-a", "Memory", id, 1L)
        val first = cipher.encrypt("first", contextA)
        val second = cipher.encrypt("different-at-same-version", contextA)
        val record = EncryptedSyncRecord(id, UUID.randomUUID().toString(), "Memory", id, 1L, first,
            SyncPayloadCodec.contentHash(first), null, 1L)
        store.applyPage(listOf(record), 1L)
        val conflict = record.copy(operationId = UUID.randomUUID().toString(), ciphertext = second,
            contentHash = SyncPayloadCodec.contentHash(second), changeSequence = 2L)

        kotlin.runCatching { store.applyPage(listOf(conflict), 2L) }
            .onSuccess { error("equal-version divergent page should be rejected") }
        assertEquals(1L, store.cursor())
        assertEquals(first, store.mirror("Memory", id)?.ciphertext)

        val badRecovery = recovery.clone().also { it[0] = (it[0].toInt() xor 1).toByte() }
        kotlin.runCatching { envelope.openRecoveryEnvelope(recoveryEnvelope, badRecovery, "vault-a") }
            .onSuccess { error("wrong recovery secret should not decrypt") }
        assertNull(store.mirror("Missing", "missing"))
    }

    @Test
    fun remoteBootstrapCredentialsAreStoredEncryptedAndSeparateFromLocalScope() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = context.getSharedPreferences("unfinished_vault", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val store = VaultIdentityStore(context)
        val local = store.getOrCreate()
        assertNull(store.remoteCredentialsOrNull())
        val issued = VaultIdentity("server-vault", "server-device", "server-issued-secret-token")
        store.saveRemoteCredentials(issued)

        assertEquals(local, store.getOrCreate())
        assertEquals(issued, store.requireRemoteCredentials())
        assertNotNull(prefs.getString("encryptedRemoteDeviceToken", null))
        org.junit.Assert.assertNotEquals("server-issued-secret-token", prefs.getString("encryptedRemoteDeviceToken", null))
    }
}
