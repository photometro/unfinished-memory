package com.unfinished.app.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object V1Rules {
    const val VERSION = "V1-R1"
    const val DAILY_SERENDIPITY_QUOTA = 1
    const val DAY_MS = 86_400_000L
    const val DORMANT_DAYS = 14L
    const val ORIGINAL_COOLDOWN_DAYS = 30L
    const val CUE_COOLDOWN_DAYS = 7L
    const val DEFER_DAYS = 7L
    fun canonicalSource(source: String) = if (source == "AUTO_RESURFACE") "AUTO_SERENDIPITY" else source
    fun dateKey(instant: Instant, zone: ZoneId): String = instant.atZone(zone).toLocalDate().toString()
    fun eligible(memory: MemoryEntity, policy: ResurfacePolicyEntity?, now: Long): Boolean =
        memory.state == "SEALED" && policy != null && policy.randomAllowed && policy.mode != "QUIET" &&
            (policy.mode != "FIXED_DATE" || (policy.planCompletedAt != null && policy.allowSerendipityAfterFixedDate)) &&
            memory.sealedAt?.let { now - it >= maxOf(DORMANT_DAYS, policy.minDormantDays.toLong()) * DAY_MS } == true &&
            (memory.cooldownUntil ?: Long.MIN_VALUE) <= now && (memory.deferUntil ?: Long.MIN_VALUE) <= now

    fun recallMode(memory: MemoryEntity): String = when {
        memory.lastOriginalExposureAt != null -> "POST_EXPOSURE_RECALL"
        memory.exposureHistory == "KNOWN" -> "FIRST_BLIND_RECALL"
        else -> "LEGACY_UNKNOWN"
    }

    fun displayLabel(memory: MemoryEntity, zone: ZoneId = ZoneId.systemDefault()): String =
        memory.userTitle?.takeIf { memory.titleSource == "USER_OR_NONE" && it.isNotBlank() }
            ?: DateTimeFormatter.ofPattern("yyyy年M月d日").format(Instant.ofEpochMilli(memory.occurredAt).atZone(zone))
}

sealed interface Evaluation {
    data class Selected(val memoryId: String) : Evaluation
    data class NoResurface(val reason: String) : Evaluation
    data object Unavailable : Evaluation
}

/** Eligibility and atomic persistence belong to the repository, not to the evaluator. */
fun interface RandomEvaluator {
    suspend fun evaluate(source: String, candidates: List<MemoryEntity>, instant: Instant): Evaluation
}

object UnconfiguredEvaluator : RandomEvaluator {
    override suspend fun evaluate(source: String, candidates: List<MemoryEntity>, instant: Instant) = Evaluation.Unavailable
}

data class EncounterProjection(
    val event: ResurfaceEventEntity, val session: RecallSessionEntity,
    val memory: MemoryEntity, val shownCues: List<CueEntity>, val remainingCues: List<CueEntity>
)
data class HomeProjection(
    val memories: List<MemoryEntity> = emptyList(), val encounter: EncounterProjection? = null,
    val randomEnabled: Boolean = true
)
data class ScheduleResult(val encounter: EncounterProjection?, val reason: String)
data class ReadRequest(val memoryId: String, val sessionId: String?, val actionId: String, val source: String)
data class RecallResult(val session: RecallSessionEntity, val original: String?)

class ProtocolTime(val clock: Clock = Clock.systemUTC(), val zone: () -> ZoneId = { ZoneId.systemDefault() }) {
    fun now() = clock.millis()
    fun date() = V1Rules.dateKey(clock.instant(), zone())
}
