package com.unfinished.app.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/** The only owner of business transitions. Reads never imply exposure. */
class MemoryRepository(
    val database: UnfinishedDatabase,
    private val cipher: LocalCipher,
    val scopeId: String,
    private val time: ProtocolTime = ProtocolTime(),
    private val evaluator: RandomEvaluator = UnconfiguredEvaluator,
    private val fault: (String) -> Unit = {}
) {
    constructor(context: Context) : this(UnfinishedDatabase.get(context), LocalCipher(), VaultIdentityStore(context).getOrCreate().vaultId)
    private val dao = database.memoryDao()
    private val observationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val state: StateFlow<HomeProjection> = combine(
        dao.observeMemories(), dao.observeEvents(), dao.observeSessions(), dao.observeExposures(), dao.observeSettings(scopeId)
    ) { _, _, _, _, _ -> Unit }.map { database.withTransaction { projection() } }
        .distinctUntilChanged().stateIn(observationScope, SharingStarted.WhileSubscribed(5000), HomeProjection())

    fun closeObservations() = observationScope.cancel()
    private fun newId() = UUID.randomUUID().toString()
    private suspend fun <T> transaction(block: suspend () -> T): T = withContext(Dispatchers.IO) { database.withTransaction { block() } }
    suspend fun listMemories() = withContext(Dispatchers.IO) { dao.listMemories() }
    suspend fun pendingSyncOperations() = withContext(Dispatchers.IO) { dao.pendingSyncOperations() }
    suspend fun markSyncOperation(id: String, state: String) = transaction { dao.updateSyncState(id, state, time.now()) }
    suspend fun readOriginal(memoryId: String): String? = withContext(Dispatchers.IO) {
        val memory = dao.findMemory(memoryId) ?: return@withContext null
        check(memory.state != "DELETED")
        cipher.decrypt(dao.findOriginal(memoryId)?.encryptedContent ?: memory.encryptedOriginal)
    }

    suspend fun saveDraft(id: String?, title: String, content: String, occurredAt: Long, policy: String,
                          fixedDate: Long? = null, titleEdited: Boolean = false): String = transaction {
        val now = time.now()
        val memoryId = id ?: newId()
        val old = dao.findMemory(memoryId)
        check(old == null || old.state == "DRAFT") { "sealed/archived/deleted original is immutable" }
        val mode = if (policy == "FLEXIBLE") "SERENDIPITY" else policy
        require(mode in listOf("SERENDIPITY", "QUIET", "FIXED_DATE"))
        val encrypted = encrypt(content)
        val preserveLegacyTitle = old?.titleSource == "LEGACY_UNKNOWN" && !titleEdited
        val memory = old?.copy(title = if (preserveLegacyTitle) old.title else title,
            userTitle = if (preserveLegacyTitle) old.userTitle else title.takeIf { it.isNotBlank() },
            titleSource = if (preserveLegacyTitle) "LEGACY_UNKNOWN" else "USER_OR_NONE",
            encryptedOriginal = encrypted, occurredAt = occurredAt, policy = mode, updatedAt = now)
            ?: MemoryEntity(memoryId, title, encrypted, occurredAt, "DRAFT", mode, null, null, 0, now, now,
                userTitle = title.takeIf { it.isNotBlank() })
        dao.upsertMemory(memory)
        dao.upsertOriginal(MemoryOriginalEntity(memoryId, encrypted, sha256Hex(content), null))
        val previous = dao.findPolicy(memoryId)
        val due = if (mode == "FIXED_DATE") fixedDate ?: previous?.fixedDate?.takeIf { previous.dateSource == "USER" } else null
        dao.upsertPolicy(ResurfacePolicyEntity(memoryId, mode, 14, due, previous?.notificationEnabled ?: false,
            randomAllowed = mode != "QUIET", planId = if (due != null) previous?.planId ?: newId() else null,
            dateSource = if (due != null) "USER" else "USER_OR_NONE"))
        enqueue("Memory", memoryId, "UPSERT", content)
        memoryId
    }
    suspend fun saveCues(memoryId: String, cues: List<CueEntity>) = transaction {
        check(dao.findMemory(memoryId)?.state == "DRAFT")
        dao.deleteCues(memoryId)
        cues.filter { it.confirmed && it.type != "PHOTO" && it.value.isNotBlank() }.forEachIndexed { index, cue ->
            dao.upsertCue(cue.copy(memoryId = memoryId, value = encrypt(cue.value), orderIndex = index))
        }
        enqueue("Cue", memoryId, "UPSERT", cues.filter { it.confirmed }.joinToString("|") { it.value })
    }
    suspend fun policy(memoryId: String) = withContext(Dispatchers.IO) { dao.findPolicy(memoryId) }

    suspend fun captureDraft(id: String): CaptureDraft? = transaction {
        val memory = dao.findMemory(id) ?: return@transaction null
        if (memory.state == "DELETED") return@transaction null
        val metadata = dao.findCaptureDraft(id)?.let { JSONObject(cipher.decrypt(it.encryptedSnapshot)) }
        val selectedPolicy = dao.findPolicy(id)
        val adopted = cues(id).map { CaptureCue(it.id, it.value, it.type, it.source) }
        CaptureDraft(
            id = id, text = readOriginal(id).orEmpty(), title = memory.userTitle.orEmpty(),
            occurredAt = memory.occurredAt, occurredDate = metadata?.optString("occurredDate").orEmpty(),
            place = metadata?.optString("place").orEmpty(), people = metadata?.optString("people").orEmpty(),
            source = metadata?.optString("source").orEmpty(), provenance = metadata?.optString("provenance", "now") ?: "now",
            preference = metadata?.optString("preference", "inherit") ?: "inherit",
            policy = memory.policy,
            fixedDate = metadata?.takeIf { !it.isNull("fixedDate") }?.optLong("fixedDate") ?: selectedPolicy?.fixedDate,
            fixedDateText = metadata?.optString("fixedDateText").orEmpty(),
            cues = adopted, photos = dao.listMediaAssets(id).map { it.toCapturePhoto() },
            importErrors = metadata?.optJSONArray("importErrors")?.let { errors ->
                (0 until errors.length()).map { errors.getString(it) }
            }.orEmpty(), sealed = memory.state in listOf("SEALED", "ARCHIVED")
        )
    }

    suspend fun latestCaptureDraft(): CaptureDraft? = transaction {
        dao.listDrafts().firstOrNull()?.let { captureDraft(it.id) }
    }

    suspend fun saveCaptureDraft(draft: CaptureDraft): CaptureDraft = transaction {
        require(!draft.sealed) { "sealed original is immutable" }
        require(draft.photos.size <= 20) { "最多留下20张照片" }
        require(draft.photos.map { it.id }.distinct().size == draft.photos.size) { "duplicate asset identity" }
        val existing = dao.findMemory(draft.id)
        check(existing == null || existing.state == "DRAFT") { "sealed original is immutable" }
        val assets = dao.listMediaAssets(draft.id)
        val assetById = assets.associateBy { it.id }
        draft.photos.forEach { require(it.memoryId == draft.id && assetById.containsKey(it.id)) { "foreign or missing asset" } }
        saveDraft(draft.id, draft.title, draft.text, draft.occurredAt, draft.policy, draft.fixedDate,
            titleEdited = draft.title != existing?.userTitle.orEmpty())
        saveCues(draft.id, draft.cues.mapIndexed { index, cue ->
            CueEntity(if (cue.id.startsWith(draft.id + ":")) cue.id else draft.id + ":" + cue.id,
                draft.id, cue.type, cue.value, cue.source, true, index, "USER_EXPLICIT")
        })
        val kept = draft.photos.map { it.id }.toSet()
        assets.filter { it.id !in kept }.forEach { dao.upsertMediaAsset(it.copy(deletedAt = time.now())) }
        draft.photos.forEachIndexed { index, photo ->
            dao.upsertMediaAsset(assetById.getValue(photo.id).copy(orderIndex = index, cueAllowed = photo.cueAllowed))
        }
        val metadata = JSONObject().put("occurredDate", draft.occurredDate).put("place", draft.place)
            .put("people", draft.people).put("source", draft.source).put("provenance", draft.provenance)
            .put("preference", draft.preference).put("fixedDate", draft.fixedDate ?: JSONObject.NULL)
            .put("fixedDateText", draft.fixedDateText)
            .put("importErrors", JSONArray(draft.importErrors))
        dao.upsertCaptureDraft(CaptureDraftEntity(draft.id, encrypt(metadata.toString()), time.now()))
        fault("capture-draft-before-commit")
        captureDraft(draft.id)!!
    }

    suspend fun attachPrivateMedia(asset: MediaAssetEntity) = transaction {
        check(dao.findMemory(asset.memoryId)?.state == "DRAFT") { "media belongs to an editable draft" }
        val assets = dao.listMediaAssets(asset.memoryId)
        require(assets.size < 20) { "最多留下20张照片" }
        require(asset.mediaState == "READY" && asset.originalPath != null && asset.previewPath != null)
        require(assets.none { it.id == asset.id }) { "asset already registered" }
        dao.upsertMediaAsset(asset.copy(orderIndex = assets.size, cueAllowed = false, localUri = null))
        enqueue("MediaAsset", asset.id, "UPSERT", asset.sha256)
        fault("media-register-before-commit")
    }

    suspend fun upgradeLegacyMedia(asset: MediaAssetEntity) = transaction {
        val current = dao.listMediaAssets(asset.memoryId).single { it.id == asset.id }
        require(current.mediaState == "LEGACY") { "not a legacy asset" }
        dao.upsertMediaAsset(asset.copy(orderIndex = current.orderIndex, cueAllowed = current.cueAllowed, localUri = null))
        fault("legacy-media-before-commit")
    }

    suspend fun captureMedia(memoryId: String): List<MediaAssetEntity> = withContext(Dispatchers.IO) { dao.listMediaAssets(memoryId) }
    suspend fun referencedMediaPaths(): Set<String> = withContext(Dispatchers.IO) {
        dao.referencedMediaAssets().flatMap { listOfNotNull(it.originalPath, it.previewPath) }.toSet()
    }

    suspend fun sealCaptureDraft(draft: CaptureDraft): SealReceipt = transaction {
        val existing = dao.findMemory(draft.id)
        if (existing?.state in listOf("SEALED", "ARCHIVED")) return@transaction SealReceipt(draft.id, true)
        check(existing == null || existing.state == "DRAFT") { "deleted record cannot be sealed" }
        require(draft.hasContent) { "写下一点内容，或留下一张照片。" }
        require(draft.importErrors.isEmpty()) { "请重试或明确移除未导入的照片。" }
        require(draft.photos.all { it.available }) { "请移除不可用照片，或重新选择来源。" }
        saveCaptureDraft(draft)
        require(dao.listMediaAssets(draft.id).all { it.mediaState == "READY" }) { "media has not been durably imported" }
        seal(draft.id)
        SealReceipt(draft.id, false)
    }

    suspend fun saveCapture(id: String?, title: String, content: String, occurredAt: Long, policy: String,
                            fixedDate: Long?, adoptedCues: List<CueEntity>, seal: Boolean, titleEdited: Boolean = false): String = transaction {
        val memoryId = saveDraft(id, title, content, occurredAt, policy, fixedDate, titleEdited)
        saveCues(memoryId, adoptedCues)
        if (seal) seal(memoryId)
        memoryId
    }
    suspend fun seal(memoryId: String) = transaction {
        val memory = dao.findMemory(memoryId) ?: error("memory missing")
        if (memory.state == "SEALED") return@transaction
        check(memory.state == "DRAFT")
        val policy = dao.findPolicy(memoryId) ?: error("policy missing")
        if (policy.mode == "FIXED_DATE") require(policy.dateSource == "USER" && policy.fixedDate != null) { "select an actual date" }
        val now = time.now()
        dao.upsertMemory(memory.copy(state = "SEALED", sealedAt = now, updatedAt = now))
        dao.markOriginalSealed(memoryId, now)
        enqueue("Memory", memoryId, "SEAL", "SEALED")
        fault("seal-before-commit")
    }
    suspend fun delete(memoryId: String) = transaction {
        dao.softDeleteMemory(memoryId, time.now())
        closeEvents(memoryId)
        enqueue("Memory", memoryId, "DELETE", "DELETED")
    }
    suspend fun archive(memoryId: String) = transaction {
        val memory = dao.findMemory(memoryId) ?: error("missing")
        check(memory.state in listOf("SEALED", "ARCHIVED")) { "cannot archive a draft or deleted memory" }
        dao.upsertMemory(memory.copy(state = "ARCHIVED", updatedAt = time.now()))
        closeEvents(memoryId)
    }
    private suspend fun closeEvents(memoryId: String) {
        dao.allEvents().filter { it.memoryId == memoryId && it.state != "COMPLETED" }.forEach {
            dao.upsertResurfaceEvent(it.copy(state = "COMPLETED", activeScope = null, completedAt = time.now()))
            dao.eventSession(it.id)?.let { session -> dao.upsertRecallSession(session.copy(state = "ABANDONED", completedAt = time.now())) }
        }
    }
    suspend fun saveMediaAsset(memoryId: String, localUri: String, sha256: String = "LOCAL_URI"): String = transaction {
        check(dao.findMemory(memoryId)?.state == "DRAFT")
        val assetId = newId()
        dao.upsertMediaAsset(MediaAssetEntity(assetId, memoryId, localUri, null, sha256, "LOCAL", null))
        enqueue("MediaAsset", memoryId, "UPSERT", localUri)
        assetId
    }
    suspend fun authorizePhotoCue(memoryId: String, assetId: String, allowed: Boolean) = transaction {
        val asset = dao.listMediaAssets(memoryId).single { it.id == assetId }
        dao.upsertMediaAsset(asset.copy(cueAllowed = allowed))
    }
    suspend fun cues(memoryId: String): List<CueEntity> = withContext(Dispatchers.IO) {
        dao.listCues(memoryId).filter { it.type != "PHOTO" }.map { it.copy(value = cipher.decrypt(it.value)) }
    }
    suspend fun recallHistory(memoryId: String) = withContext(Dispatchers.IO) { dao.listRecallSessions(memoryId) }
    suspend fun setRandomEnabled(enabled: Boolean) = transaction { dao.saveSettings(VaultSettingsEntity(scopeId, enabled)) }
    private suspend fun projection(): HomeProjection {
        val event = dao.activeEvent(scopeId)
        return HomeProjection(dao.listMemories(), event?.let { encounter(it) }, dao.settings(scopeId)?.randomEnabled ?: true)
    }
    suspend fun current(): HomeProjection = transaction { projection() }
    private suspend fun encounter(event: ResurfaceEventEntity): EncounterProjection {
        val memory = dao.findMemory(event.memoryId) ?: error("missing memory")
        val session = dao.eventSession(event.id) ?: error("missing session")
        val shownIds = dao.sessionExposures(session.id).filter { it.kind == "CUE" }.map { it.contentScope }.toSet()
        val bank = cues(memory.id)
        return EncounterProjection(event, session, memory, bank.filter { it.id in shownIds },
            if (session.cueEngineStopped || session.snapshotLocked) emptyList() else bank.filter { it.id !in shownIds })
    }
    private suspend fun scanDue(now: Long, date: String, zone: String) {
        val plans = dao.listSealed().mapNotNull { memory -> dao.findPolicy(memory.id)?.let { memory to it } }
        for ((memory, policy) in plans) {
            if (policy.mode != "FIXED_DATE" || policy.dateSource != "USER" || policy.planCompletedAt != null ||
                policy.fixedDate == null || policy.fixedDate > now || policy.planId == null) continue
            if (dao.allEvents().any { it.fixedPlanId == policy.planId }) continue
            dao.insertEvent(ResurfaceEventEntity(newId(), memory.id, "PENDING_DUE", policy.fixedDate, null, null, null,
                source = "FIXED_DATE", createdAt = now, dueAt = policy.fixedDate, fixedPlanId = policy.planId,
                localDateKey = date, zoneId = zone))
        }
    }
    private suspend fun activate(event: ResurfaceEventEntity): ResurfaceEventEntity {
        val active = event.copy(state = "ACTIVE", activeScope = scopeId)
        dao.upsertResurfaceEvent(active)
        if (dao.eventSession(event.id) == null) {
            val memory = dao.findMemory(event.memoryId) ?: error("missing")
            dao.upsertRecallSession(RecallSessionEntity(newId(), memory.id, event.id, "RESURFACED", null, null, time.now(), null, null, null,
                source = event.source, recallMode = V1Rules.recallMode(memory)))
        }
        return active
    }
    suspend fun schedule(requestedSource: String = "AUTO_SERENDIPITY"): ScheduleResult = transaction {
        val source = V1Rules.canonicalSource(requestedSource)
        require(source == "AUTO_SERENDIPITY" || source == "MANUAL_ENCOUNTER")
        val instant = time.clock.instant()
        val zone = time.zone()
        val now = instant.toEpochMilli()
        val date = V1Rules.dateKey(instant, zone)
        scanDue(now, date, zone.id) // Must precede active / daily early returns.
        dao.activeEvent(scopeId)?.let { return@transaction ScheduleResult(encounter(it), "RESTORED") }
        val pending = dao.allEvents().filter {
            val memory = dao.findMemory(it.memoryId)
            it.state in listOf("PENDING_DUE", "DEFERRED", "LEGACY_PENDING") &&
                (it.deferredUntil ?: Long.MIN_VALUE) <= now && memory?.state == "SEALED" &&
                (it.state != "DEFERRED" || maxOf(memory.cooldownUntil ?: 0, memory.deferUntil ?: 0) <= now)
        }
            .sortedWith(compareBy<ResurfaceEventEntity> { if (it.source == "FIXED_DATE") 0 else 1 }
                .thenBy { it.dueAt ?: it.scheduledAt }.thenBy { it.createdAt }.thenBy { it.id })
        pending.firstOrNull()?.let { return@transaction ScheduleResult(encounter(activate(it)), "QUEUED") }
        val previous = dao.daily(scopeId, date)
        if (previous?.quotaEventId != null) return@transaction ScheduleResult(null, "QUOTA_USED")
        if (source == "AUTO_SERENDIPITY" && previous?.evaluatedAt != null) return@transaction ScheduleResult(null, previous.autoResult ?: "NO_RESURFACE")
        if (dao.settings(scopeId)?.randomEnabled == false) return@transaction ScheduleResult(null, "DISABLED")
        val candidates = dao.listSealed().filter { V1Rules.eligible(it, dao.findPolicy(it.id), now) }
        val minimum = if (source == "AUTO_SERENDIPITY") 4 else 3
        val result = if (candidates.size < minimum) Evaluation.NoResurface("POOL_TOO_SMALL") else evaluator.evaluate(source, candidates, instant)
        if (result == Evaluation.Unavailable) return@transaction ScheduleResult(null, "MODEL_NOT_CONFIGURED")
        var day = previous ?: DailyDecisionEntity(scopeId, date, zone.id, null, null, null, null, null, V1Rules.VERSION)
        var active: ResurfaceEventEntity? = null
        if (result is Evaluation.Selected) {
            require(candidates.any { it.id == result.memoryId }) { "evaluator selected an ineligible memory" }
            active = activate(ResurfaceEventEntity(newId(), result.memoryId, "ACTIVE", now, null, null, null,
                source = source, createdAt = now, activeScope = scopeId, localDateKey = date, zoneId = zone.id))
            fault("event-created")
            day = day.copy(quotaEventId = active.id, quotaConsumedAt = now)
        }
        if (source == "AUTO_SERENDIPITY") day = day.copy(evaluatedAt = now, autoResult = if (active != null) "SELECTED" else "NO_RESURFACE", chosenMemoryId = active?.memoryId)
        if (source == "AUTO_SERENDIPITY" || active != null) {
            if (previous == null) dao.insertDaily(day) else dao.updateDaily(day)
        }
        fault("decision-before-commit")
        ScheduleResult(active?.let { encounter(it) }, (result as? Evaluation.NoResurface)?.reason ?: "SELECTED")
    }
    suspend fun beginRecall(sessionId: String) = transaction {
        val session = requireActiveSession(sessionId)
        if (session.state == "RESURFACED") dao.upsertRecallSession(session.copy(state = "RECALLING"))
    }
    private suspend fun requireActiveSession(sessionId: String): RecallSessionEntity {
        val session = dao.findRecallSession(sessionId) ?: error("missing session")
        check(dao.activeEvent(scopeId)?.id == session.resurfaceEventId) { "session not active" }
        return session
    }
    suspend fun saveRecallInput(sessionId: String, input: String, clarity: String) = transaction {
        val session = requireActiveSession(sessionId)
        check(!session.snapshotLocked && !session.cueEngineStopped)
        val changed = session.recallContent?.let(cipher::decrypt).orEmpty() != input
        dao.upsertRecallSession(session.copy(recallContent = if (changed) encrypt(input) else session.recallContent,
            clarity = clarity, inputRecallMode = if (changed) session.recallMode else session.inputRecallMode))
    }
    suspend fun recallInput(sessionId: String): String = withContext(Dispatchers.IO) {
        dao.findRecallSession(sessionId)?.recallContent?.let(cipher::decrypt).orEmpty()
    }
    suspend fun answer(sessionId: String, recall: String, clarity: String): RecallSessionEntity = transaction {
        val current = requireActiveSession(sessionId)
        check(!current.snapshotLocked && !current.cueEngineStopped)
        require(recall.isNotBlank()) { "do not fabricate an empty snapshot" }
        dao.upsertRecallSession(current.copy(state = "RECALLING", clarity = clarity, recallContent = encrypt(recall),
            inputRecallMode = current.recallMode, answeredAt = time.now()))
        dao.findRecallSession(sessionId)!!
    }
    private suspend fun lockSnapshot(session: RecallSessionEntity) {
        if (session.snapshotLocked) return
        // Restoring/flushing an old answer after exposure does not make it a new post-exposure answer.
        if (session.inputRecallMode != session.recallMode) return
        val content = session.recallContent?.let(cipher::decrypt)
        val shown = dao.sessionExposures(session.id).filter { it.kind == "CUE" }.map { it.contentScope }.sorted().joinToString(",")
        val action = "snapshot:${session.id}:${session.recallMode}:${sha256Hex(shown)}"
        if (!content.isNullOrBlank() && dao.snapshot(action) == null) dao.insertSnapshot(RecallSnapshotEntity(action, session.id,
            session.recallContent!!, session.recallMode, shown, time.now()))
    }
    suspend fun stopRecall(sessionId: String) = transaction {
        val session = requireActiveSession(sessionId)
        if (session.state == "REVEALED") return@transaction
        lockSnapshot(session)
        val outcome = if (session.recallContent?.let(cipher::decrypt).isNullOrBlank()) "NO_RECALL" else "CONTENT_PROVIDED"
        dao.upsertRecallSession(session.copy(state = "RECALL_STOPPED", cueEngineStopped = true, snapshotLocked = true, recallOutcome = outcome))
    }
    suspend fun requestReveal(sessionId: String): ReadRequest = transaction {
        val session = requireActiveSession(sessionId)
        lockSnapshot(session)
        val action = session.revealActionId ?: newId()
        val trulyRevealed = dao.sessionExposures(session.id).any { it.kind == "ORIGINAL" }
        dao.upsertRecallSession(session.copy(state = if (trulyRevealed) "REVEALED" else "REVEAL_REQUESTED",
            snapshotLocked = true, revealActionId = action,
            recallOutcome = session.recallOutcome ?: if (session.answeredAt == null) "DIRECT_REVEAL" else "CONTENT_PROVIDED"))
        ReadRequest(session.memoryId, session.id, action, "ENCOUNTER_REVEAL")
    }
    suspend fun reveal(sessionId: String): RecallResult {
        val request = requestReveal(sessionId)
        return RecallResult(dao.findRecallSession(sessionId)!!, readOriginal(request.memoryId))
    }
    fun archiveRequest(memoryId: String, actionId: String = newId()) = ReadRequest(memoryId, null, actionId, "ARCHIVE_OPEN")
    private suspend fun record(action: String, kind: String, contentScope: String, memoryId: String, sessionId: String?, source: String): Boolean {
        val prior = dao.exposure(action, kind, contentScope)
        if (prior != null) {
            check(prior.memoryId == memoryId && prior.sessionId == sessionId) { "action ID reused for different content" }
            return false
        }
        val instant = time.clock.instant(); val zone = time.zone()
        dao.insertExposure(ExposureEntity(action, kind, contentScope, memoryId, sessionId, source, instant.toEpochMilli(), V1Rules.dateKey(instant, zone), zone.id))
        return true
    }
    suspend fun shoreVisible(eventId: String) = transaction {
        val event = dao.activeEvent(scopeId) ?: return@transaction
        check(event.id == eventId)
        val session = dao.eventSession(eventId) ?: error("missing")
        if (record("shore:$eventId", "SHORE", "metadata", event.memoryId, session.id, event.source)) {
            dao.upsertResurfaceEvent(event.copy(openedAt = time.now()))
            dao.markResurfaced(event.memoryId, time.now(), time.now())
        }
    }
    suspend fun nextCue(sessionId: String): CueEntity? = transaction {
        val session = requireActiveSession(sessionId)
        if (session.cueEngineStopped || session.snapshotLocked) return@transaction null
        encounter(dao.activeEvent(scopeId)!!).remainingCues.firstOrNull()
    }
    suspend fun cueVisible(sessionId: String, cueId: String) = transaction {
        if (dao.exposure("cue:$sessionId:$cueId", "CUE", cueId) != null) return@transaction
        val session = requireActiveSession(sessionId)
        check(!session.cueEngineStopped && !session.snapshotLocked)
        check(dao.listCues(session.memoryId).any { it.id == cueId && it.type != "PHOTO" })
        if (dao.exposure("cue:$sessionId:$cueId", "CUE", cueId) == null) lockSnapshot(session)
        if (record("cue:$sessionId:$cueId", "CUE", cueId, session.memoryId, session.id, session.source)) {
            val memory = dao.findMemory(session.memoryId)!!
            dao.upsertMemory(memory.copy(cooldownUntil = maxOf(memory.cooldownUntil ?: 0, time.now() + 7 * V1Rules.DAY_MS)))
        }
    }
    suspend fun originalVisible(request: ReadRequest, contentScope: String = "TEXT", assetIds: List<String> = emptyList()) = transaction {
        val existing = dao.exposure(request.actionId, "ORIGINAL", contentScope)
        if (existing != null) {
            check(existing.memoryId == request.memoryId && existing.sessionId == request.sessionId)
            if (assetIds.all { dao.exposure(request.actionId, "ORIGINAL_MEDIA", it) != null }) return@transaction
        }
        val memory = dao.findMemory(request.memoryId) ?: error("missing")
        check(memory.state in listOf("SEALED", "ARCHIVED"))
        val session = request.sessionId?.let { requireActiveSession(it).also { s ->
            check(s.memoryId == memory.id && s.revealActionId == request.actionId && s.state in listOf("REVEAL_REQUESTED", "REVEALED"))
        } }
        if (assetIds.isNotEmpty()) check(dao.listMediaAssets(memory.id).map { it.id }.containsAll(assetIds))
        val originalFresh = record(request.actionId, "ORIGINAL", contentScope, memory.id, session?.id, request.source)
        var mediaFresh = false
        assetIds.forEach { if (record(request.actionId, "ORIGINAL_MEDIA", it, memory.id, session?.id, request.source)) mediaFresh = true }
        if (originalFresh || mediaFresh) {
            val now = time.now()
            dao.upsertMemory(memory.copy(lastOriginalExposureAt = now, cooldownUntil = maxOf(memory.cooldownUntil ?: 0, now + 30 * V1Rules.DAY_MS)))
            session?.let { dao.upsertRecallSession(it.copy(state = "REVEALED", revealedAt = now)) }
            if (session == null) dao.listRecallSessions(memory.id).filter { it.state !in listOf("COMPLETED", "ABANDONED") }.forEach { ongoing ->
                    lockSnapshot(ongoing)
                    dao.upsertRecallSession(ongoing.copy(recallMode = "POST_EXPOSURE_RECALL"))
            }
            val policy = dao.findPolicy(memory.id)
            if (policy?.mode == "FIXED_DATE" && session?.source == "FIXED_DATE" && policy.planCompletedAt == null)
                dao.upsertPolicy(policy.copy(planCompletedAt = now))
            fault("exposure-before-commit")
        }
    }
    suspend fun photoCueVisible(sessionId: String, assetId: String, actionId: String) = transaction {
        val prior = dao.exposure(actionId, "CUE", "photo:$assetId")
        if (prior != null) { check(prior.sessionId == sessionId); return@transaction }
        val session = requireActiveSession(sessionId)
        check(!session.cueEngineStopped && !session.snapshotLocked)
        val asset = dao.listMediaAssets(session.memoryId).single { it.id == assetId }
        check(asset.cueAllowed)
        if (dao.exposure(actionId, "CUE", "photo:$assetId") == null) lockSnapshot(session)
        val fresh = record(actionId, "CUE", "photo:$assetId", session.memoryId, session.id, session.source)
        record(actionId, "ORIGINAL_MEDIA", assetId, session.memoryId, session.id, "PHOTO_CUE")
        if (fresh) {
            val memory = dao.findMemory(session.memoryId)!!
            dao.upsertMemory(memory.copy(lastOriginalExposureAt = time.now(), cooldownUntil = maxOf(memory.cooldownUntil ?: 0, time.now() + 30 * V1Rules.DAY_MS)))
            dao.upsertRecallSession(session.copy(recallMode = "POST_EXPOSURE_RECALL"))
        }
    }
    suspend fun complete(sessionId: String, reflection: String?) = transaction {
        if (dao.findRecallSession(sessionId)?.state == "COMPLETED") return@transaction
        val session = requireActiveSession(sessionId)
        check(session.state == "REVEALED") { "original not yet visible" }
        val now = time.now()
        if (!reflection.isNullOrBlank()) dao.insertReflection(ReflectionEntity(newId(), session.memoryId, session.id, encrypt(reflection), now))
        dao.upsertRecallSession(session.copy(state = "COMPLETED", completedAt = now))
        val event = dao.activeEvent(scopeId)!!
        dao.upsertResurfaceEvent(event.copy(state = "COMPLETED", activeScope = null, completedAt = now))
    }
    suspend fun defer(eventId: String) = transaction {
        if (dao.findResurfaceEvent(eventId)?.state == "DEFERRED") return@transaction
        val event = dao.activeEvent(scopeId) ?: error("no active event")
        check(event.id == eventId)
        val memory = dao.findMemory(event.memoryId)!!
        val until = maxOf(time.now() + 7 * V1Rules.DAY_MS, memory.cooldownUntil ?: 0)
        dao.upsertResurfaceEvent(event.copy(state = "DEFERRED", activeScope = null, deferredUntil = until))
        dao.upsertMemory(memory.copy(deferUntil = maxOf(until, memory.deferUntil ?: 0)))
    }
    fun cue(id: String, type: String, value: String, source: String = "MANUAL", confirmed: Boolean = false) = CueEntity(id, "", type, value, source, confirmed, 0)
    private suspend fun enqueue(entityType: String, entityId: String, operationType: String, payload: String) {
        dao.upsertSyncOperation(SyncOperationEntity(newId(), entityType, entityId, operationType, sha256Hex(payload), "PENDING", 0, time.now(), null))
    }
    private suspend fun encrypt(value: String) = cipher.encrypt(value, allowCreate = !dao.hasTextCiphertext())
    private fun sha256Hex(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
