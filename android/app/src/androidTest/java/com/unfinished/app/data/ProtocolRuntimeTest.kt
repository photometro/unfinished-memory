package com.unfinished.app.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.*
import java.util.UUID

class MutableProtocolClock(var instant: Instant) : Clock() {
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
    override fun instant(): Instant = instant
}

@RunWith(AndroidJUnit4::class)
class ProtocolRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: UnfinishedDatabase
    private lateinit var repo: MemoryRepository
    private val clock = MutableProtocolClock(Instant.parse("2026-10-06T03:00:00Z"))
    private var zone = ZoneId.of("Asia/Shanghai")
    private var failure: String? = null
    private var calls = 0
    private var noAuto = false
    private val selector = RandomEvaluator { source, candidates, _ ->
        calls++
        if (noAuto && source == "AUTO_SERENDIPITY") Evaluation.NoResurface("TEST_NO") else Evaluation.Selected(candidates.first().id)
    }

    @Before fun setup() {
        check(context.packageName == "com.unfinished.app.protocoltest") { "Only isolated package allowed" }
        db = Room.inMemoryDatabaseBuilder(context, UnfinishedDatabase::class.java).build()
        repo = repository(db)
    }
    private fun repository(database: UnfinishedDatabase, evaluator: RandomEvaluator = selector) = MemoryRepository(database, LocalCipher(), "test-scope", ProtocolTime(clock) { zone }, evaluator) {
        if (it == failure) throw IllegalStateException("Injected transaction failure")
    }
    @After fun close() { repo.closeObservations(); db.close() }
    private suspend fun seed(count: Int = 4, mode: String = "SERENDIPITY", due: Long? = null): List<String> {
        val current = clock.instant
        clock.instant = current.minus(Duration.ofDays(30))
        val ids = (1..count).map {
            repo.saveCapture(null, "", "synthetic test $it", clock.millis(), mode, due, emptyList(), true)
        }
        clock.instant = current
        return ids
    }
    private suspend fun finish(session: RecallSessionEntity) {
        val request = repo.requestReveal(session.id)
        repo.readOriginal(request.memoryId)
        repo.originalVisible(request)
        repo.complete(session.id, null)
    }

    @Test fun concurrentCommandsShareExactlyOneQuotaAndSession() = runBlocking {
        seed()
        val results = coroutineScope { (1..20).map { async(Dispatchers.IO) { repo.schedule(if (it % 2 == 0) "AUTO_SERENDIPITY" else "MANUAL_ENCOUNTER") } }.awaitAll() }
        assertEquals(1, results.map { it.encounter!!.event.id }.distinct().size)
        assertEquals(1, db.memoryDao().allEvents().size)
        assertEquals(1, calls)
        assertNotNull(db.memoryDao().daily("test-scope", "2026-10-06")!!.quotaEventId)
        assertEquals(0, repo.current().encounter!!.memory.resurfaceCount)
    }
    @Test fun autoNoCachedButManualCanSucceedAndZonesDoNotReroll() = runBlocking {
        seed(); noAuto = true
        assertEquals("TEST_NO", repo.schedule().reason)
        repeat(3) { repo.schedule() }
        assertEquals(1, calls)
        zone = ZoneId.of("Asia/Tokyo")
        repo.schedule()
        assertEquals(1, calls)
        val session = repo.schedule("MANUAL_ENCOUNTER").encounter!!.session
        val day = db.memoryDao().daily("test-scope", "2026-10-06")!!
        assertEquals("Asia/Shanghai", day.zoneId)
        assertEquals("NO_RESURFACE", day.autoResult)
        assertNotNull(day.quotaEventId)
        finish(session)
        seed(1) // Replace the just-exposed record excluded by its 30-day cooldown.
        clock.instant = clock.instant.plus(Duration.ofDays(1)); repo.schedule()
        assertEquals(3, calls)
        clock.instant = clock.instant.minus(Duration.ofDays(1)); repo.schedule()
        assertEquals(3, calls)
    }
    @Test fun manualFirstDoesNotInventAutoSelectedResult() = runBlocking {
        seed()
        val encounter = repo.schedule("MANUAL_ENCOUNTER").encounter!!
        finish(encounter.session)
        assertEquals("QUOTA_USED", repo.schedule().reason)
        assertEquals(1, db.memoryDao().allEvents().size)
        assertNull(db.memoryDao().daily("test-scope", "2026-10-06")!!.autoResult)
        assertNull(db.memoryDao().daily("test-scope", "2026-10-06")!!.evaluatedAt)
    }
    @Test fun failedAndCancelledTransactionsLeaveNoEventOrQuota() = runBlocking {
        seed(); failure = "event-created"
        assertTrue(runCatching { repo.schedule() }.isFailure)
        assertTrue(db.memoryDao().allEvents().isEmpty())
        assertNull(db.memoryDao().daily("test-scope", "2026-10-06"))
        failure = null
        val cancelling = repository(db, RandomEvaluator { _, _, _ -> throw CancellationException("test cancellation") })
        assertTrue(runCatching { cancelling.schedule() }.isFailure)
        assertTrue(db.memoryDao().allEvents().isEmpty())
        assertNull(db.memoryDao().daily("test-scope", "2026-10-06"))
        cancelling.closeObservations()
        val cancelledAfterWrite = MemoryRepository(db, LocalCipher(), "test-scope", ProtocolTime(clock) { zone }, selector) {
            if (it == "event-created") throw CancellationException("cancel after writing event/session")
        }
        assertTrue(runCatching { cancelledAfterWrite.schedule() }.isFailure)
        assertTrue(db.memoryDao().allEvents().isEmpty())
        assertNull(db.memoryDao().daily("test-scope", "2026-10-06"))
        cancelledAfterWrite.closeObservations()
        assertNotNull(repo.schedule().encounter)
    }
    @Test fun thresholdsDormancyAndUnconfiguredAreNotProductionRandom() = runBlocking {
        seed(2)
        assertEquals("POOL_TOO_SMALL", repo.schedule("MANUAL_ENCOUNTER").reason)
        seed(1)
        assertEquals("POOL_TOO_SMALL", repo.schedule().reason)
        assertNotNull(repo.schedule("MANUAL_ENCOUNTER").encounter)
        finish(repo.current().encounter!!.session)
        clock.instant = clock.instant.plus(Duration.ofDays(1)); seed(4)
        val disabled = repository(db, UnconfiguredEvaluator)
        assertEquals("MODEL_NOT_CONFIGURED", disabled.schedule().reason)
        assertNull(db.memoryDao().daily("test-scope", "2026-10-07"))
        disabled.closeObservations()
    }
    @Test fun quietAndImmatureAndDisabledRemainIneligibleEvenAfterDay21() = runBlocking {
        val ids = seed(4, "QUIET")
        assertEquals("POOL_TOO_SMALL", repo.schedule("MANUAL_ENCOUNTER").reason)
        ids.forEach {
            db.memoryDao().upsertPolicy(db.memoryDao().findPolicy(it)!!.copy(mode = "SERENDIPITY", randomAllowed = true))
            db.memoryDao().upsertMemory(db.memoryDao().findMemory(it)!!.copy(sealedAt = clock.millis() - 13 * V1Rules.DAY_MS))
        }
        assertEquals("POOL_TOO_SMALL", repo.schedule("MANUAL_ENCOUNTER").reason)
        clock.instant = clock.instant.plus(Duration.ofDays(30))
        repo.setRandomEnabled(false)
        assertEquals("DISABLED", repo.schedule("MANUAL_ENCOUNTER").reason)
    }
    @Test fun fixedQueueScansDespiteActiveAndUsesOriginalDueAndOneShot() = runBlocking {
        seed()
        val active = repo.schedule().encounter!!
        val due1 = clock.millis() - 10 * V1Rules.DAY_MS
        val due2 = clock.millis() - 5 * V1Rules.DAY_MS
        val first = seed(1, "FIXED_DATE", due1).single()
        seed(1, "FIXED_DATE", due2)
        repeat(3) { assertEquals(active.event.id, repo.schedule().encounter!!.event.id) }
        assertEquals(2, db.memoryDao().allEvents().count { it.state == "PENDING_DUE" })
        finish(active.session)
        val next = repo.schedule().encounter!!
        assertEquals(first, next.memory.id); assertEquals(due1, next.event.dueAt)
        repo.defer(next.event.id)
        assertEquals(3, db.memoryDao().allEvents().size)
        val later = repo.schedule().encounter!!
        assertEquals(due2, later.event.dueAt)
        finish(later.session)
        assertNotNull(db.memoryDao().findPolicy(later.memory.id)!!.planCompletedAt)
        clock.instant = clock.instant.plus(Duration.ofDays(8))
        val resumed = repo.schedule().encounter!!
        assertEquals(next.event.id, resumed.event.id)
        assertEquals(next.session.id, resumed.session.id)
        assertEquals(due1, resumed.event.dueAt)
        finish(resumed.session)
        assertFalse(V1Rules.eligible(db.memoryDao().findMemory(first)!!, db.memoryDao().findPolicy(first), clock.millis() + 100 * V1Rules.DAY_MS))
        repo.schedule()
        assertEquals(3, db.memoryDao().allEvents().count { it.source == "FIXED_DATE" || it.source == "AUTO_SERENDIPITY" })
    }
    @Test fun stopAndDirectRevealAreDurableNoAnswerOrFakeSnapshot() = runBlocking {
        seed(); val encounter = repo.schedule().encounter!!
        repo.stopRecall(encounter.session.id)
        assertNull(repo.nextCue(encounter.session.id))
        assertEquals("RECALL_STOPPED", repo.current().encounter!!.session.state)
        val request = repo.requestReveal(encounter.session.id)
        assertEquals(request, repo.requestReveal(encounter.session.id))
        assertTrue(db.memoryDao().snapshots(encounter.session.id).isEmpty())
        assertEquals("NO_RECALL", repo.current().encounter!!.session.recallOutcome)
        assertTrue(runCatching { repo.complete(encounter.session.id, null) }.isFailure)
        repo.readOriginal(request.memoryId)
        assertTrue(db.memoryDao().memoryExposures(request.memoryId).isEmpty())
        failure = "exposure-before-commit"
        assertTrue(runCatching { repo.originalVisible(request) }.isFailure)
        assertNull(db.memoryDao().findMemory(request.memoryId)!!.lastOriginalExposureAt)
        failure = null
        repo.originalVisible(request); repo.originalVisible(request)
        assertEquals(1, db.memoryDao().memoryExposures(request.memoryId).count { it.kind == "ORIGINAL" })
        assertEquals(clock.millis() + 30 * V1Rules.DAY_MS, db.memoryDao().findMemory(request.memoryId)!!.cooldownUntil)
        repo.complete(encounter.session.id, null)
        assertTrue(db.memoryDao().listReflections(request.memoryId).isEmpty())
    }
    @Test fun decryptionFailureAndArchivePrefetchDoNotExpose() = runBlocking {
        val ids = seed()
        val request = repo.archiveRequest(ids.first(), "archive-test")
        repo.readOriginal(ids.first())
        assertTrue(db.memoryDao().memoryExposures(ids.first()).isEmpty())
        repo.originalVisible(request); repo.originalVisible(request)
        assertTrue(db.memoryDao().allEvents().isEmpty())
        assertEquals("ARCHIVE_OPEN", db.memoryDao().memoryExposures(ids.first()).single().source)
        val encounter = repo.schedule("MANUAL_ENCOUNTER").encounter!!
        val next = repo.requestReveal(encounter.session.id)
        val original = db.memoryDao().findOriginal(next.memoryId)!!
        db.memoryDao().upsertOriginal(original.copy(encryptedContent = "invalid"))
        assertTrue(runCatching { repo.readOriginal(next.memoryId) }.isFailure)
        assertEquals("REVEAL_REQUESTED", repo.current().encounter!!.session.state)
        assertTrue(db.memoryDao().memoryExposures(next.memoryId).isEmpty())
    }
    @Test fun onlyAdoptedCueAndPhotoAuthorizationExposureSnapshotAreIndependent() = runBlocking {
        val current = clock.instant; clock.instant = current.minus(Duration.ofDays(30))
        val memory = repo.saveDraft(null, "", "", clock.millis(), "SERENDIPITY")
        repo.saveCues(memory, listOf(repo.cue("suggestion", "KEYWORD", "not adopted"), repo.cue("adopted", "KEYWORD", "adopted", confirmed = true)))
        assertEquals(listOf("adopted"), repo.cues(memory).map { it.id })
        val photo = repo.saveMediaAsset(memory, "synthetic://photo")
        val other = repo.saveMediaAsset(memory, "synthetic://photo2")
        repo.authorizePhotoCue(memory, photo, true)
        assertFalse(db.memoryDao().listMediaAssets(memory).single { it.id == other }.cueAllowed)
        repo.seal(memory); clock.instant = current; seed(3)
        val encounter = repo.schedule("MANUAL_ENCOUNTER").encounter!!
        // Resolve the selected photo record deterministically regardless of ordering ties.
        if (encounter.memory.id != memory) { finish(encounter.session) }
        val actual = if (encounter.memory.id == memory) encounter else {
            clock.instant = clock.instant.plus(Duration.ofDays(1))
            val chooser = repository(db, RandomEvaluator { _, _, _ -> Evaluation.Selected(memory) })
            val chosen = chooser.schedule("MANUAL_ENCOUNTER").encounter!!; chooser.closeObservations(); chosen
        }
        repo.saveRecallInput(actual.session.id, "blind real input", "PARTIAL")
        assertTrue(runCatching { repo.photoCueVisible(actual.session.id, other, "unauthorized") }.isFailure)
        assertTrue(db.memoryDao().sessionExposures(actual.session.id).isEmpty())
        repo.photoCueVisible(actual.session.id, photo, "photo-display")
        repo.photoCueVisible(actual.session.id, photo, "photo-display")
        val blind = db.memoryDao().snapshots(actual.session.id).single()
        assertEquals("FIRST_BLIND_RECALL", blind.recallMode)
        assertEquals("", blind.shownCueIds)
        assertEquals("POST_EXPOSURE_RECALL", repo.current().encounter!!.session.recallMode)
        assertEquals(2, db.memoryDao().sessionExposures(actual.session.id).size)
        repo.cueVisible(actual.session.id, "adopted")
        assertEquals(clock.millis() + 30 * V1Rules.DAY_MS, db.memoryDao().findMemory(memory)!!.cooldownUntil)
        repo.requestReveal(actual.session.id)
        assertEquals(blind, db.memoryDao().snapshots(actual.session.id).single { it.actionId == blind.actionId })
    }

    @Test fun stagesLockBeforeEachCueAndArchiveExtendsDeferredCooldown() = runBlocking {
        val current = clock.instant; clock.instant = current.minus(Duration.ofDays(30))
        val memory = repo.saveDraft(null, "", "original", clock.millis(), "SERENDIPITY")
        repo.saveCues(memory, listOf(repo.cue("c1", "KEYWORD", "cue1", confirmed = true), repo.cue("c2", "KEYWORD", "cue2", confirmed = true)))
        repo.seal(memory); clock.instant = current; seed(3)
        val chooser = repository(db, RandomEvaluator { _, _, _ -> Evaluation.Selected(memory) })
        val encounter = chooser.schedule("MANUAL_ENCOUNTER").encounter!!
        chooser.closeObservations()
        repo.saveRecallInput(encounter.session.id, "R0", "PARTIAL")
        repo.cueVisible(encounter.session.id, "c1")
        repo.saveRecallInput(encounter.session.id, "R1", "PARTIAL")
        repo.cueVisible(encounter.session.id, "c2")
        repo.saveRecallInput(encounter.session.id, "R2", "CLEAR")
        repo.stopRecall(encounter.session.id)
        val snapshots = db.memoryDao().snapshots(encounter.session.id)
        assertEquals(setOf("", "c1", "c1,c2"), snapshots.map { it.shownCueIds }.toSet())
        assertEquals(setOf("R0", "R1", "R2"), snapshots.map { LocalCipher().decrypt(it.encryptedContent) }.toSet())
        repo.defer(encounter.event.id); repo.defer(encounter.event.id)
        clock.instant = clock.instant.plus(Duration.ofDays(1))
        val read = repo.archiveRequest(memory, "deferred-archive")
        repo.originalVisible(read)
        clock.instant = clock.instant.plus(Duration.ofDays(7))
        assertNull(repo.schedule().encounter?.takeIf { it.memory.id == memory })
        val until = db.memoryDao().findMemory(memory)!!.cooldownUntil!!
        assertTrue(until > clock.millis())
        assertEquals(snapshots, db.memoryDao().snapshots(encounter.session.id))
    }
    @Test fun fixedPhotoCueIsExposureNotRevealAndTerminalReceiptReplayIsSafe() = runBlocking {
        val id = repo.saveDraft(null, "", "", clock.millis(), "FIXED_DATE", clock.millis())
        val asset = repo.saveMediaAsset(id, "synthetic://fixed-photo")
        repo.authorizePhotoCue(id, asset, true); repo.seal(id)
        val encounter = repo.schedule().encounter!!
        repo.photoCueVisible(encounter.session.id, asset, "fixed-photo")
        assertNull(db.memoryDao().findPolicy(id)!!.planCompletedAt)
        val read = repo.requestReveal(encounter.session.id)
        repo.originalVisible(read, "MEDIA_GROUP", listOf(asset))
        assertNotNull(db.memoryDao().findPolicy(id)!!.planCompletedAt)
        repo.complete(encounter.session.id, "real reflection")
        repo.complete(encounter.session.id, "duplicate should not be saved")
        repo.originalVisible(read, "MEDIA_GROUP", listOf(asset))
        repo.photoCueVisible(encounter.session.id, asset, "fixed-photo")
        assertEquals(1, db.memoryDao().listReflections(id).size)
    }
    @Test fun deferredArchiveSwitchesOnlySubsequentSnapshotsToPostExposure() = runBlocking {
        seed(); val encounter = repo.schedule().encounter!!
        repo.saveRecallInput(encounter.session.id, "before original", "PARTIAL")
        repo.defer(encounter.event.id)
        repo.originalVisible(repo.archiveRequest(encounter.memory.id, "deferred-view"))
        assertEquals("FIRST_BLIND_RECALL", db.memoryDao().snapshots(encounter.session.id).single().recallMode)
        clock.instant = clock.instant.plus(Duration.ofDays(31))
        val resumed = repo.schedule().encounter!!
        assertEquals(encounter.session.id, resumed.session.id)
        assertEquals("POST_EXPOSURE_RECALL", resumed.session.recallMode)
        repo.saveRecallInput(resumed.session.id, "after original", "CLEAR")
        repo.requestReveal(resumed.session.id)
        assertEquals(setOf("FIRST_BLIND_RECALL", "POST_EXPOSURE_RECALL"), db.memoryDao().snapshots(resumed.session.id).map { it.recallMode }.toSet())
    }

    @Test fun stoppedSnapshotIsNotRelabeledAfterArchiveAndDeferredResume() = runBlocking {
        val memory = seed(1, "FIXED_DATE", clock.millis()).single()
        val encounter = repo.schedule().encounter!!
        repo.saveRecallInput(encounter.session.id, "immutable blind input", "PARTIAL")
        repo.stopRecall(encounter.session.id)
        val stopped = db.memoryDao().snapshots(encounter.session.id)
        assertEquals("FIRST_BLIND_RECALL", stopped.single().recallMode)
        repo.defer(encounter.event.id)
        repo.originalVisible(repo.archiveRequest(memory, "stopped-archive"))
        clock.instant = clock.instant.plus(Duration.ofDays(31))
        val resumed = repo.schedule().encounter!!
        assertEquals(encounter.session.id, resumed.session.id)
        assertTrue(resumed.session.snapshotLocked)
        assertTrue(resumed.session.cueEngineStopped)
        repo.requestReveal(resumed.session.id)
        assertEquals(stopped, db.memoryDao().snapshots(resumed.session.id))
    }

    @Test fun unchangedBlindInputIsNotInventedAsPostExposureRecall() = runBlocking {
        val memory = seed(1, "FIXED_DATE", clock.millis()).single()
        val encounter = repo.schedule().encounter!!
        repo.saveRecallInput(encounter.session.id, "before original only", "PARTIAL")
        repo.defer(encounter.event.id)
        repo.originalVisible(repo.archiveRequest(memory, "unchanged-archive"))
        val blind = db.memoryDao().snapshots(encounter.session.id).single()
        clock.instant = clock.instant.plus(Duration.ofDays(31))
        val resumed = repo.schedule().encounter!!
        // The UI's command flush may repeat the restored value; it is not a new answer.
        repo.saveRecallInput(resumed.session.id, "before original only", "PARTIAL")
        repo.requestReveal(resumed.session.id)
        assertEquals(listOf(blind), db.memoryDao().snapshots(resumed.session.id))
    }
    @Test fun deletedMemoryCannotBeResurrectedByStaleArchiveCommand() = runBlocking {
        val memory = seed(1, "QUIET").single()
        repo.delete(memory)
        assertTrue(runCatching { repo.archive(memory) }.isFailure)
        assertEquals("DELETED", db.memoryDao().findMemory(memory)!!.state)
        assertTrue(runCatching { repo.readOriginal(memory) }.isFailure)
    }
    @Test fun v3UpgradePreservesStoppedInputAndExistingBlindSnapshot() = runBlocking {
        val name = "protocol-migration-3-${UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
        val encrypted = LocalCipher().encrypt("synthetic v3 input")
        val schema = org.json.JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.unfinished.app.data.UnfinishedDatabase/3.json").bufferedReader().use { it.readText() })
            .getJSONObject("database").getJSONArray("entities")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
            for (i in 0 until schema.length()) {
                val entity = schema.getJSONObject(i)
                val table = entity.getString("tableName")
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indexes = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (j in 0 until indexes.length()) old.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
            }
            old.execSQL("INSERT INTO recall_sessions (id,memoryId,resurfaceEventId,state,recallContent,startedAt,recallMode,cueEngineStopped,snapshotLocked) VALUES ('v3-session','v3-memory','v3-event','RECALL_STOPPED',?,1,'FIRST_BLIND_RECALL',1,1)", arrayOf(encrypted))
            old.execSQL("INSERT INTO recall_snapshots VALUES ('v3-snapshot','v3-session',?,'FIRST_BLIND_RECALL','',1)", arrayOf(encrypted))
            old.version = 3
        }
        val upgraded = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name)
            .addMigrations(UnfinishedDatabase.MIGRATION_3_4, UnfinishedDatabase.MIGRATION_4_5, UnfinishedDatabase.MIGRATION_5_6).build()
        val session = upgraded.memoryDao().findRecallSession("v3-session")!!
        assertEquals(encrypted, session.recallContent)
        assertEquals("LEGACY_UNKNOWN", session.inputRecallMode)
        assertTrue(session.snapshotLocked); assertTrue(session.cueEngineStopped)
        assertEquals(encrypted, upgraded.memoryDao().snapshots(session.id).single().encryptedContent)
        upgraded.close()
    }
    @Test fun v1AndV2UpgradePreserveCipherIdsMediaAndUnknownHistory() = runBlocking {
        for (version in listOf(1, 2)) {
            val name = "protocol-migration-$version-${UUID.randomUUID()}.db"
            val path = context.getDatabasePath(name); path.parentFile!!.mkdirs()
            val cipher = LocalCipher().encrypt("synthetic legacy original")
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                createLegacy(old, version)
                old.execSQL("INSERT INTO memories VALUES ('legacy','legacy body prefix',?,1,'SEALED','FIXED_DATE',1,100,2,1,2)", arrayOf(cipher))
                old.execSQL("INSERT INTO resurface_events VALUES ('old-event','legacy','OPENED',1,1,NULL,NULL)")
                old.execSQL("INSERT INTO recall_sessions VALUES ('old-session','legacy','old-event','REVEALED',NULL,NULL,1,NULL,2,NULL)")
                old.execSQL("INSERT INTO memory_cues VALUES ('old-cue','legacy','KEYWORD',?,'AI_SUGGESTED',1,0)", arrayOf(cipher))
                old.execSQL("INSERT INTO reflections VALUES ('old-reflection','legacy','old-session',?,1)", arrayOf(cipher))
                old.execSQL("INSERT INTO sync_operations VALUES ('old-sync','Memory','legacy','UPSERT','synthetic-hash','PENDING',2,1,NULL)")
                if (version == 2) {
                    old.execSQL("INSERT INTO memory_originals VALUES ('legacy',?,'hash',1)", arrayOf(cipher))
                    old.execSQL("INSERT INTO resurface_policies VALUES ('legacy','FIXED_DATE',7,999,0)")
                    old.execSQL("INSERT INTO media_assets VALUES ('old-photo','legacy','uri',NULL,'hash','LOCAL',NULL)")
                }
                if (version == 1) {
                    for (mode in listOf("SERENDIPITY", "FLEXIBLE", "QUIET")) old.execSQL("INSERT INTO memories VALUES (?, 'legacy raw title', ?, 1, 'SEALED', ?, 1, NULL, 0, 1, 1)", arrayOf("v1-$mode", cipher, mode))
                    old.execSQL("INSERT INTO memories VALUES ('old-draft', 'legacy unknown title', ?, 1, 'DRAFT', 'QUIET', NULL, NULL, 0, 1, 1)", arrayOf(cipher))
                }
                old.version = version
            }
            val upgraded = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name)
                .addMigrations(UnfinishedDatabase.MIGRATION_1_2, UnfinishedDatabase.MIGRATION_2_3,
                    UnfinishedDatabase.MIGRATION_3_4, UnfinishedDatabase.MIGRATION_4_5, UnfinishedDatabase.MIGRATION_5_6).build()
            val migrated = repository(upgraded)
            val memory = upgraded.memoryDao().findMemory("legacy")!!
            assertEquals(cipher, memory.encryptedOriginal)
            assertEquals("legacy body prefix", memory.title)
            assertNull(memory.userTitle)
            assertEquals("LEGACY_UNKNOWN", V1Rules.recallMode(memory))
            assertNull(memory.lastOriginalExposureAt)
            assertEquals("synthetic legacy original", migrated.readOriginal("legacy"))
            assertEquals("LEGACY_PENDING", upgraded.memoryDao().findResurfaceEvent("old-event")!!.state)
            assertEquals("REVEAL_REQUESTED", upgraded.memoryDao().findRecallSession("old-session")!!.state)
            assertEquals("LEGACY_UNKNOWN", upgraded.memoryDao().findRecallSession("old-session")!!.inputRecallMode)
            assertTrue(migrated.cues("legacy").isEmpty()) // Legacy confirmed is not explicit Cue authorization.
            assertEquals(cipher, upgraded.memoryDao().listReflections("legacy").single().content)
            assertTrue(upgraded.memoryDao().pendingSyncOperations().any { it.id == "old-sync" && it.attempts == 2 })
            if (version == 2) {
                assertEquals(999L, upgraded.memoryDao().findPolicy("legacy")!!.fixedDate)
                assertEquals("LEGACY_UNKNOWN", upgraded.memoryDao().findPolicy("legacy")!!.dateSource)
                assertFalse(upgraded.memoryDao().listMediaAssets("legacy").single().cueAllowed)
            }
            if (version == 1) {
                for (mode in listOf("SERENDIPITY", "FLEXIBLE")) assertTrue(V1Rules.eligible(upgraded.memoryDao().findMemory("v1-$mode")!!, upgraded.memoryDao().findPolicy("v1-$mode"), clock.millis()))
                assertFalse(V1Rules.eligible(upgraded.memoryDao().findMemory("v1-QUIET")!!, upgraded.memoryDao().findPolicy("v1-QUIET"), clock.millis()))
                migrated.saveDraft("old-draft", "", "only body changed", 1L, "QUIET")
                assertEquals("legacy unknown title", upgraded.memoryDao().findMemory("old-draft")!!.title)
                assertEquals("LEGACY_UNKNOWN", upgraded.memoryDao().findMemory("old-draft")!!.titleSource)
                val fresh = migrated.saveCapture(null, "", "new after upgrade", clock.millis(), "QUIET", null, emptyList(), true)
                assertEquals("new after upgrade", migrated.readOriginal(fresh))
            }
            migrated.closeObservations(); upgraded.close()
            val reopen = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name).build()
            assertEquals(cipher, reopen.memoryDao().findMemory("legacy")!!.encryptedOriginal)
            reopen.close()
        }
    }
    private fun createLegacy(db: SQLiteDatabase, version: Int) {
        listOf(
            "CREATE TABLE memories (id TEXT NOT NULL PRIMARY KEY,title TEXT NOT NULL,encryptedOriginal TEXT NOT NULL,occurredAt INTEGER NOT NULL,state TEXT NOT NULL,policy TEXT NOT NULL,sealedAt INTEGER,lastResurfacedAt INTEGER,resurfaceCount INTEGER NOT NULL,createdAt INTEGER NOT NULL,updatedAt INTEGER NOT NULL)",
            "CREATE INDEX index_memories_state ON memories(state)", "CREATE INDEX index_memories_updatedAt ON memories(updatedAt)",
            "CREATE TABLE memory_cues (id TEXT NOT NULL PRIMARY KEY,memoryId TEXT NOT NULL,type TEXT NOT NULL,value TEXT NOT NULL,source TEXT NOT NULL,confirmed INTEGER NOT NULL,orderIndex INTEGER NOT NULL)", "CREATE INDEX index_memory_cues_memoryId ON memory_cues(memoryId)",
            "CREATE TABLE resurface_events (id TEXT NOT NULL PRIMARY KEY,memoryId TEXT NOT NULL,state TEXT NOT NULL,scheduledAt INTEGER NOT NULL,openedAt INTEGER,deferredUntil INTEGER,completedAt INTEGER)",
            "CREATE INDEX index_resurface_events_memoryId ON resurface_events(memoryId)", "CREATE INDEX index_resurface_events_state ON resurface_events(state)",
            "CREATE TABLE recall_sessions (id TEXT NOT NULL PRIMARY KEY,memoryId TEXT NOT NULL,resurfaceEventId TEXT,state TEXT NOT NULL,clarity TEXT,recallContent TEXT,startedAt INTEGER NOT NULL,answeredAt INTEGER,revealedAt INTEGER,completedAt INTEGER)",
            "CREATE INDEX index_recall_sessions_memoryId ON recall_sessions(memoryId)", "CREATE INDEX index_recall_sessions_state ON recall_sessions(state)",
            "CREATE TABLE reflections (id TEXT NOT NULL PRIMARY KEY,memoryId TEXT NOT NULL,recallSessionId TEXT NOT NULL,content TEXT NOT NULL,createdAt INTEGER NOT NULL)", "CREATE INDEX index_reflections_memoryId ON reflections(memoryId)",
            "CREATE TABLE sync_operations (id TEXT NOT NULL PRIMARY KEY,entityType TEXT NOT NULL,entityId TEXT NOT NULL,operationType TEXT NOT NULL,payloadHash TEXT NOT NULL,state TEXT NOT NULL,attempts INTEGER NOT NULL,createdAt INTEGER NOT NULL,lastAttemptAt INTEGER)", "CREATE INDEX index_sync_operations_state ON sync_operations(state)"
        ).forEach(db::execSQL)
        if (version == 2) listOf(
            "CREATE TABLE memory_originals (memoryId TEXT NOT NULL PRIMARY KEY,encryptedContent TEXT NOT NULL,contentHash TEXT NOT NULL,sealedAt INTEGER)",
            "CREATE TABLE resurface_policies (memoryId TEXT NOT NULL PRIMARY KEY,mode TEXT NOT NULL,minDormantDays INTEGER NOT NULL,fixedDate INTEGER,notificationEnabled INTEGER NOT NULL)",
            "CREATE TABLE media_assets (id TEXT NOT NULL PRIMARY KEY,memoryId TEXT NOT NULL,localUri TEXT,remoteObjectId TEXT,sha256 TEXT NOT NULL,uploadState TEXT NOT NULL,deletedAt INTEGER)", "CREATE INDEX index_media_assets_memoryId ON media_assets(memoryId)"
        ).forEach(db::execSQL)
    }

    // Run these in separate instrumentation processes with force-stop between them.
    private fun processDatabaseName(kind: String): String {
        val run = requireNotNull(InstrumentationRegistry.getArguments().getString("processRunId"))
        require(run.matches(Regex("[A-Za-z0-9_-]+")))
        return "protocol-process-$kind-$run.db"
    }
    @Test fun seedProcessRestart() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("processPhase") == "seed")
        val persistent = Room.databaseBuilder(context, UnfinishedDatabase::class.java, processDatabaseName("stop")).build()
        val saved = repository(persistent)
        val draft = saved.saveDraft(null, "", "synthetic durable stop", clock.millis(), "FIXED_DATE", clock.millis())
        saved.seal(draft)
        val session = saved.schedule().encounter!!.session
        saved.stopRecall(session.id)
        assertEquals("RECALL_STOPPED", saved.current().encounter!!.session.state)
        saved.closeObservations(); persistent.close()
        val directDb = Room.databaseBuilder(context, UnfinishedDatabase::class.java, processDatabaseName("direct")).build()
        val direct = repository(directDb)
        val d = direct.saveDraft(null, "", "synthetic durable direct", clock.millis(), "FIXED_DATE", clock.millis())
        direct.seal(d)
        direct.requestReveal(direct.schedule().encounter!!.session.id)
        direct.closeObservations(); directDb.close()
    }
    @Test fun recoverProcessRestart() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("processPhase") == "recover")
        val persistent = Room.databaseBuilder(context, UnfinishedDatabase::class.java, processDatabaseName("stop")).build()
        val saved = repository(persistent)
        val session = saved.schedule().encounter!!.session
        assertEquals("RECALL_STOPPED", session.state)
        assertTrue(session.cueEngineStopped)
        assertNull(saved.nextCue(session.id))
        assertEquals(1, persistent.memoryDao().allEvents().size)
        saved.closeObservations(); persistent.close()
        val directDb = Room.databaseBuilder(context, UnfinishedDatabase::class.java, processDatabaseName("direct")).build()
        val direct = repository(directDb)
        val s = direct.schedule().encounter!!.session
        assertEquals("REVEAL_REQUESTED", s.state)
        assertEquals("DIRECT_REVEAL", s.recallOutcome)
        assertEquals(s.revealActionId, direct.requestReveal(s.id).actionId)
        assertTrue(directDb.memoryDao().snapshots(s.id).isEmpty())
        assertNull(directDb.memoryDao().findMemory(s.memoryId)!!.lastOriginalExposureAt)
        direct.closeObservations(); directDb.close()
    }
}
