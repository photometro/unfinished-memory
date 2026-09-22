package com.unfinished.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

data class MemoryDraft(
    val id: String,
    val title: String,
    val content: String,
    val occurredAt: Long,
    val policy: String,
    val cues: List<CueEntity>
)

data class RecallResult(
    val session: RecallSessionEntity,
    val original: String?
)

class MemoryRepository(context: Context) {
    private val dao = UnfinishedDatabase.get(context).memoryDao()
    private val cipher = LocalCipher()

    suspend fun listMemories(): List<MemoryEntity> = withContext(Dispatchers.IO) { dao.listMemories() }

    suspend fun pendingSyncOperations(): List<SyncOperationEntity> = withContext(Dispatchers.IO) {
        dao.pendingSyncOperations()
    }

    suspend fun markSyncOperation(id: String, state: String) = withContext(Dispatchers.IO) {
        dao.updateSyncState(id, state, System.currentTimeMillis())
    }

    suspend fun readOriginal(memoryId: String): String? = withContext(Dispatchers.IO) {
        val encrypted = dao.findOriginal(memoryId)?.encryptedContent ?: dao.findMemory(memoryId)?.encryptedOriginal
        encrypted?.let { cipher.decrypt(it) }
    }

    suspend fun saveDraft(id: String?, title: String, content: String, occurredAt: Long, policy: String): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val memoryId = id ?: UUID.randomUUID().toString()
        val existing = dao.findMemory(memoryId)
        check(existing?.state != "SEALED") { "sealed memory is immutable" }
        val encryptedOriginal = cipher.encrypt(content)
        dao.upsertMemory(
            MemoryEntity(
                id = memoryId,
                title = title.ifBlank { "未命名记忆" },
                encryptedOriginal = encryptedOriginal,
                occurredAt = occurredAt,
                state = existing?.state ?: "DRAFT",
                policy = policy,
                sealedAt = existing?.sealedAt,
                lastResurfacedAt = existing?.lastResurfacedAt,
                resurfaceCount = existing?.resurfaceCount ?: 0,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
        dao.upsertOriginal(MemoryOriginalEntity(memoryId, encryptedOriginal, sha256Hex(content), existing?.sealedAt))
        val normalizedPolicy = if (policy == "FLEXIBLE") "SERENDIPITY" else policy
        val fixedDate = if (normalizedPolicy == "FIXED_DATE") now + 7L * 24 * 60 * 60 * 1000 else null
        dao.upsertPolicy(ResurfacePolicyEntity(memoryId, normalizedPolicy, 7, fixedDate, false))
        enqueue("Memory", memoryId, "UPSERT", content)
        memoryId
    }

    suspend fun saveCues(memoryId: String, cues: List<CueEntity>) = withContext(Dispatchers.IO) {
        dao.deleteCues(memoryId)
        cues.filter { it.value.isNotBlank() }.forEachIndexed { index, cue ->
            dao.upsertCue(cue.copy(memoryId = memoryId, value = cipher.encrypt(cue.value), orderIndex = index, confirmed = true))
        }
        enqueue("Cue", memoryId, "UPSERT", cues.joinToString("|") { it.value })
    }

    suspend fun seal(memoryId: String) = withContext(Dispatchers.IO) {
        val memory = dao.findMemory(memoryId) ?: return@withContext
        check(memory.state != "DELETED") { "deleted memory" }
        val sealedAt = System.currentTimeMillis()
        dao.updateState(memoryId, "SEALED", sealedAt, sealedAt)
        dao.markOriginalSealed(memoryId, sealedAt)
        enqueue("Memory", memoryId, "SEAL", "SEALED")
    }

    suspend fun delete(memoryId: String) = withContext(Dispatchers.IO) {
        dao.softDeleteMemory(memoryId, System.currentTimeMillis())
        enqueue("Memory", memoryId, "DELETE", "DELETED")
    }

    suspend fun saveMediaAsset(memoryId: String, localUri: String, sha256: String = "LOCAL_URI") = withContext(Dispatchers.IO) {
        dao.upsertMediaAsset(MediaAssetEntity(UUID.randomUUID().toString(), memoryId, localUri, null, sha256, "LOCAL", null))
        enqueue("MediaAsset", memoryId, "UPSERT", localUri)
    }

    suspend fun cues(memoryId: String): List<CueEntity> = withContext(Dispatchers.IO) {
        dao.listCues(memoryId).map { cue -> cue.copy(value = cipher.decrypt(cue.value)) }
    }

    suspend fun recallHistory(memoryId: String): List<RecallSessionEntity> = withContext(Dispatchers.IO) {
        dao.listRecallSessions(memoryId)
    }

    suspend fun pickEncounter(now: Long = System.currentTimeMillis()): MemoryEntity? = withContext(Dispatchers.IO) {
        val minDormant = 7L * 24 * 60 * 60 * 1000
        val eligible = buildList {
            for (memory in dao.listSealed()) {
            val dormantEnough = memory.sealedAt != null && now - memory.sealedAt >= minDormant
            val cooldownOver = memory.lastResurfacedAt == null || now - memory.lastResurfacedAt >= minDormant
            val policy = dao.findPolicy(memory.id)
            val fixedDateReady = policy?.mode != "FIXED_DATE" || (policy.fixedDate ?: Long.MAX_VALUE) <= now
                if (dormantEnough && cooldownOver && fixedDateReady) add(memory)
            }
        }
        eligible.randomOrNull()
    }

    suspend fun openEncounter(memoryId: String): Pair<ResurfaceEventEntity, RecallSessionEntity> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val event = ResurfaceEventEntity(UUID.randomUUID().toString(), memoryId, "OPENED", now, now, null, null)
        val session = RecallSessionEntity(UUID.randomUUID().toString(), memoryId, event.id, "CUEING", null, null, now, null, null, null)
        dao.upsertResurfaceEvent(event)
        dao.upsertRecallSession(session)
        dao.markResurfaced(memoryId, now, now)
        event to session
    }

    suspend fun answer(sessionId: String, recall: String, clarity: String): RecallSessionEntity = withContext(Dispatchers.IO) {
        val current = dao.findRecallSession(sessionId) ?: error("recall session not found")
        val now = System.currentTimeMillis()
        val answered = current.copy(state = "ANSWERED", clarity = clarity, recallContent = cipher.encrypt(recall), answeredAt = now)
        dao.upsertRecallSession(answered)
        answered
    }

    suspend fun reveal(sessionId: String): RecallResult = withContext(Dispatchers.IO) {
        val current = dao.findRecallSession(sessionId) ?: error("recall session not found")
        check(current.state == "ANSWERED" || current.state == "REVEALED") { "recall must be answered before reveal" }
        val memory = dao.findMemory(current.memoryId) ?: error("memory not found")
        val revealed = current.copy(state = "REVEALED", revealedAt = current.revealedAt ?: System.currentTimeMillis())
        dao.upsertRecallSession(revealed)
        RecallResult(revealed, readOriginal(current.memoryId))
    }

    suspend fun complete(sessionId: String, reflection: String?) = withContext(Dispatchers.IO) {
        val session = dao.findRecallSession(sessionId) ?: return@withContext
        val now = System.currentTimeMillis()
        dao.upsertRecallSession(session.copy(state = "COMPLETED", completedAt = now))
        dao.findResurfaceEvent(session.resurfaceEventId ?: "")?.let {
            dao.updateEvent(it.id, "COMPLETED", it.openedAt, it.deferredUntil, now)
        }
        if (!reflection.isNullOrBlank()) {
            dao.insertReflection(ReflectionEntity(UUID.randomUUID().toString(), session.memoryId, session.id, cipher.encrypt(reflection), now))
        }
    }

    suspend fun defer(eventId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.updateEvent(eventId, "DEFERRED", now, now + 24 * 60 * 60 * 1000, null)
    }

    fun cue(id: String, type: String, value: String, source: String = "MANUAL") =
        CueEntity(id, "", type, value, source, true, 0)

    private suspend fun enqueue(entityType: String, entityId: String, operationType: String, payload: String) {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        dao.upsertSyncOperation(
            SyncOperationEntity(
                id = UUID.randomUUID().toString(), entityType = entityType, entityId = entityId,
                operationType = operationType, payloadHash = hash, state = "PENDING", attempts = 0,
                createdAt = System.currentTimeMillis(), lastAttemptAt = null
            )
        )
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
