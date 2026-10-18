package com.unfinished.app.data

import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unfinished.app.MainActivity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Verifies the local-first boundary in the isolated .protocoltest application only. */
@RunWith(AndroidJUnit4::class)
class OfflineBoundaryRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun mainActivityStartsWithoutRemoteCredentials() {
        assertEquals("com.unfinished.app.protocoltest", context.packageName)
        val prefsName = "offline-start-${UUID.randomUUID()}"
        val identityStore = VaultIdentityStore(context, preferencesName = prefsName)

        assertNull(identityStore.remoteCredentialsOrNull())
        assertNotNull(identityStore.getOrCreate())
        assertNull(identityStore.remoteCredentialsOrNull())

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(context.packageName, activity.packageName)
            }
        }

        context.getSharedPreferences(prefsName, android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    @Test
    fun localSaveSurvivesRoomReopenAndRemainsPendingWithoutBackend() = runBlocking {
        assertEquals("com.unfinished.app.protocoltest", context.packageName)
        val suffix = UUID.randomUUID().toString()
        val databaseName = "offline-boundary-$suffix.db"
        val preferencesName = "offline-boundary-$suffix"
        val identityStore = VaultIdentityStore(context, preferencesName = preferencesName)
        val localVault = identityStore.getOrCreate()
        val memoryId: String

        val firstDatabase = openDatabase(databaseName)
        val firstRepository = MemoryRepository(firstDatabase, LocalCipher(), localVault.vaultId)
        try {
            memoryId = firstRepository.saveCapture(
                id = null,
                title = "",
                content = "synthetic offline boundary record",
                occurredAt = System.currentTimeMillis(),
                policy = "QUIET",
                fixedDate = null,
                adoptedCues = emptyList(),
                seal = true
            )
            val pending = firstRepository.pendingSyncOperations()
            assertTrue("local save should be durable even when no remote exists", pending.isNotEmpty())
            assertTrue("no operation may be acknowledged without a backend", pending.all { it.state == "PENDING" })
            assertNull(identityStore.remoteCredentialsOrNull())
        } finally {
            firstRepository.closeObservations()
            firstDatabase.close()
        }

        val reopenedDatabase = openDatabase(databaseName)
        val reopenedRepository = MemoryRepository(reopenedDatabase, LocalCipher(), localVault.vaultId)
        try {
            val saved = reopenedRepository.listMemories().single { it.id == memoryId }
            assertEquals("SEALED", saved.state)
            assertEquals("synthetic offline boundary record", reopenedRepository.readOriginal(memoryId))
            val pendingAfterReopen = reopenedRepository.pendingSyncOperations()
            assertTrue(pendingAfterReopen.isNotEmpty())
            assertFalse(pendingAfterReopen.any { it.state == "SYNCED" })
            assertNull(identityStore.remoteCredentialsOrNull())
        } finally {
            reopenedRepository.closeObservations()
            reopenedDatabase.close()
            context.deleteDatabase(databaseName)
            context.getSharedPreferences(preferencesName, android.content.Context.MODE_PRIVATE)
                .edit().clear().commit()
        }
    }

    private fun openDatabase(name: String): UnfinishedDatabase =
        Room.databaseBuilder(context, UnfinishedDatabase::class.java, name)
            .addMigrations(
                UnfinishedDatabase.MIGRATION_1_2,
                UnfinishedDatabase.MIGRATION_2_3,
                UnfinishedDatabase.MIGRATION_3_4,
                UnfinishedDatabase.MIGRATION_4_5,
                UnfinishedDatabase.MIGRATION_5_6
            )
            .build()
}
