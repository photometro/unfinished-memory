package com.unfinished.app.ui.leave

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.unfinished.app.data.CaptureCue
import com.unfinished.app.data.CaptureDraft
import com.unfinished.app.data.CaptureGateway
import com.unfinished.app.data.SealReceipt
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val LEAVE_PHOTO_LIMIT = 20
internal enum class LeavePage { WRITING, REVIEW, POLICY, BACKGROUND, PREFERENCE, PHOTO, JOURNEY, COMPLETE }
internal data class LeaveState(
    val draft: CaptureDraft = CaptureDraft(), val ready: Boolean = false,
    val page: LeavePage = LeavePage.WRITING, val photoId: String? = null,
    val photoReturn: LeavePage = LeavePage.WRITING,
    val saving: Boolean = false, val saved: Boolean = false, val importing: Boolean = false,
    val navigating: Boolean = false, val sealing: Boolean = false,
    val error: String? = null, val receipt: SealReceipt? = null, val fixedDateInput: String = ""
) {
    val locked: Boolean get() = !ready || navigating || sealing || draft.sealed
    val operationPending: Boolean get() = locked || importing
}
/** Literal scene words only. Not AI, inference, or the V3 Cue engine. */
internal fun localCueCandidates(text: String, memoryId: String): List<CaptureCue> = Regex(
    "(?:老|旧|小)?(?:车站|站台|码头|街角|路口|书店|咖啡馆|餐馆|小店|桥边|海边)|" +
        "(?:铁皮|木)?屋檐|窗台|窗边|杯印|纸箱|路灯|树叶|新叶|雨声|滴水声|钟声|脚步声|蝉鸣|夏天|秋天|冬天|春天|清晨|傍晚"
).findAll(text).map { it.value }.distinct().take(3).map {
    CaptureCue(id = "$memoryId:local:${UUID.randomUUID()}", value = it, source = "LOCAL_EXTRACT")
}.toList()
internal fun fixedDateInput(timestamp: Long?): String = timestamp?.let {
    java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
} ?: ""
internal fun parseFixedDate(value: String): Long? = runCatching {
    LocalDate.parse(value.trim()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()
internal fun sealValidation(draft: CaptureDraft): String? = when {
    !draft.hasContent -> "写下一点内容，或留下一张照片。"
    draft.importErrors.isNotEmpty() -> "还有未处理的照片。请重新选择，或明确移除失败项。"
    draft.photos.size > LEAVE_PHOTO_LIMIT -> "这一段最多留下 20 张照片。"
    draft.photos.any { !it.available } -> "有照片尚不可用。请重新选择，或明确移除后再封存。"
    draft.policy == "FIXED_DATE" && draft.fixedDate == null -> "选一个再见的日期，或改用其他出现方式。"
    draft.policy !in setOf("SERENDIPITY", "FIXED_DATE", "QUIET") -> "出现方式未能识别，请重新选择。"
    else -> null
}

/** One retained ID and one serial queue; old autosaves never write after seal. */
internal class LeaveController(
    private val gateway: CaptureGateway,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val mutable = MutableStateFlow(LeaveState())
    val state: StateFlow<LeaveState> = mutable
    private var editVersion = 0L
    private var persistedVersion = -1L
    private var saveQueued = false
    private var sessionGeneration = 0
    init { scope.launch { for (command in commands) command() }; beginSession() }
    private fun enqueue(command: suspend () -> Unit) { commands.trySend(command) }
    private fun beginSession() {
        val generation = ++sessionGeneration
        enqueue {
            try {
                val id = savedState.get<String>("leave.draftId")
                val draft = (if (id == null) gateway.latestDraft() else gateway.loadDraft(id)) ?: CaptureDraft()
                if (generation != sessionGeneration) return@enqueue
                savedState["leave.draftId"] = draft.id
                val restored = savedState.get<String>("leave.page")?.let { name -> LeavePage.entries.firstOrNull { it.name == name } }
                val page = when {
                    draft.sealed -> LeavePage.COMPLETE
                    restored in setOf(LeavePage.REVIEW, LeavePage.POLICY, LeavePage.BACKGROUND, LeavePage.PREFERENCE) -> restored!!
                    else -> LeavePage.WRITING
                }
                mutable.value = LeaveState(draft = draft, ready = true, page = page,
                    fixedDateInput = draft.fixedDateText.ifBlank { savedState.get<String>("leave.fixedDateInput") ?: fixedDateInput(draft.fixedDate) },
                    saved = id != null || draft.hasContent)
                editVersion = 0; persistedVersion = if (draft.hasContent || draft.sealed) 0 else -1
            } catch (failure: Exception) { mutable.update { it.copy(ready = false, error = message(failure, "草稿暂时未能打开，请重试。")) } }
        }
    }
    fun retryLoad() { if (!state.value.ready) beginSession() }
    fun edit(change: (CaptureDraft) -> CaptureDraft) {
        if (state.value.locked) return
        mutable.update { it.copy(draft = change(it.draft), saved = false, error = null) }
        editVersion++; requestSave()
    }
    fun setDate(value: String) {
        if (state.value.locked) return
        mutable.update { it.copy(fixedDateInput = value) }; savedState["leave.fixedDateInput"] = value
        edit { it.copy(fixedDate = parseFixedDate(value), fixedDateText = value) }
    }
    fun adoptCue(cue: CaptureCue, adopted: Boolean) = edit { draft ->
        val others = draft.cues.filterNot { it.id == cue.id }
        draft.copy(cues = if (adopted && cue.value.isNotBlank()) others + cue.copy(value = cue.value.trim()) else others)
    }
    fun editCue(cue: CaptureCue, value: String) = edit { draft ->
        val others = draft.cues.filterNot { it.id == cue.id }
        draft.copy(cues = if (value.isBlank()) others else others + cue.copy(value = value.take(80), source = "USER_EDITED"))
    }
    fun setSelfCue(value: String) = edit { draft ->
        val others = draft.cues.filterNot { it.id == "${draft.id}:self" }
        draft.copy(cues = if (value.isBlank()) others else others + CaptureCue(id = "${draft.id}:self", value = value.take(80), source = "USER_WRITTEN"))
    }
    fun removePhoto(id: String) = edit { it.copy(photos = it.photos.filterNot { photo -> photo.id == id }) }
    fun setPhotoCueAllowed(id: String, allowed: Boolean) = edit { draft ->
        draft.copy(photos = draft.photos.map { if (it.id == id) it.copy(cueAllowed = allowed) else it })
    }
    fun movePhoto(id: String, direction: Int) = edit { draft ->
        val index = draft.photos.indexOfFirst { it.id == id }; val target = index + direction
        if (index < 0 || target !in draft.photos.indices) draft else {
            val ordered = draft.photos.toMutableList(); val photo = ordered.removeAt(index); ordered.add(target, photo); draft.copy(photos = ordered)
        }
    }
    fun dismissImportError(index: Int) = edit { draft -> draft.copy(importErrors = draft.importErrors.filterIndexed { position, _ -> position != index }) }
    fun importPhotos(uris: List<Uri>, replaceFailedItems: Boolean = false) {
        if (uris.isEmpty() || state.value.operationPending) return
        mutable.update { it.copy(importing = true, error = null) }
        enqueue {
            try {
                // A pure-photo draft must own its durable Memory row before
                // any private file can be associated with it.
                persistLatest()
            } catch (failure: Exception) {
                mutable.update { it.copy(importing = false, saving = false, error = message(failure, "照片导入前未能保存草稿，请重试。")) }
                return@enqueue
            }
            if (state.value.draft.photos.size + uris.size > LEAVE_PHOTO_LIMIT) {
                addImportError("所选照片超过剩余额度。这一段最多留下 20 张照片，请重新选择。")
            } else {
                val retried = if (replaceFailedItems) state.value.draft.importErrors.take(uris.size) else emptyList()
                val pending = uris.indices.map { index -> retried.getOrNull(index)
                    ?: "照片 ${state.value.draft.photos.size + index + 1}：导入尚未完成，请重选或移除。" }
                mutable.update { it.copy(draft = it.draft.copy(importErrors = it.draft.importErrors + pending.drop(retried.size)), saved = false) }
                editVersion++
                // Persist unfinished slots before attempting a batch. A kill
                // after photo 1 must not silently forget photos 2..20.
                try { persistLatest() } catch (failure: Exception) {
                    mutable.update { it.copy(importing = false, saving = false, error = message(failure, "照片导入队列未能保存，请重试。")) }
                    return@enqueue
                }
                for ((index, uri) in uris.withIndex()) {
                    try {
                        val photo = gateway.importPhoto(state.value.draft.id, uri)
                        mutable.update { current -> current.copy(draft = current.draft.copy(photos = current.draft.photos + photo,
                            importErrors = removeOne(current.draft.importErrors, pending[index])), saved = false) }
                        editVersion++
                    } catch (cancelled: CancellationException) { throw cancelled
                    } catch (failure: Exception) {
                        mutable.update { it.copy(draft = it.draft.copy(importErrors = removeOne(it.draft.importErrors, pending[index])), saved = false) }
                        addImportError("照片 ${index + 1}：${message(failure, "未能保存，请重新选择。")}")
                    }
                    try { persistLatest() } catch (failure: Exception) { mutable.update { it.copy(saving = false, error = message(failure, "照片草稿未能保存，请重试。")) } }
                }
            }
            try { persistLatest() } catch (failure: Exception) { mutable.update { it.copy(error = message(failure, "照片草稿未能保存，请重试。")) } }
            mutable.update { it.copy(importing = false) }
        }
    }
    private fun addImportError(error: String) { mutable.update { it.copy(draft = it.draft.copy(importErrors = it.draft.importErrors + error), saved = false) }; editVersion++ }
    private fun removeOne(errors: List<String>, message: String): List<String> {
        val index = errors.indexOf(message)
        return if (index < 0) errors else errors.filterIndexed { position, _ -> position != index }
    }
    private fun requestSave() {
        if (saveQueued) return
        saveQueued = true
        enqueue {
            saveQueued = false
            try { persistLatest() } catch (failure: Exception) { mutable.update { it.copy(saving = false, saved = false, error = message(failure, "草稿未能保存。内容仍在，请重试。")) } }
        }
    }
    private suspend fun persistLatest() {
        if (!state.value.ready || state.value.draft.sealed || persistedVersion == editVersion) return
        val snapshot = state.value.draft; val version = editVersion
        mutable.update { it.copy(saving = true) }
        val acknowledged = gateway.saveDraft(snapshot)
        persistedVersion = version
        mutable.update { it.copy(saving = false, saved = editVersion == version,
            draft = it.draft.copy(maintenanceWarning = acknowledged.maintenanceWarning)) }
    }
    suspend fun flush(): Boolean {
        val completion = CompletableDeferred<Boolean>()
        enqueue {
            try { persistLatest(); completion.complete(true) } catch (failure: Exception) {
                mutable.update { it.copy(saving = false, saved = false, error = message(failure, "草稿未能保存。内容仍在，请重试。")) }; completion.complete(false)
            }
        }
        return completion.await()
    }
    fun checkpoint() { if (state.value.ready && !state.value.draft.sealed) requestSave() }
    fun navigate(page: LeavePage, onExit: (() -> Unit)? = null, validatePolicy: Boolean = false) {
        val current = state.value
        if (current.operationPending) return
        if (page == LeavePage.REVIEW && !current.draft.hasContent) { mutable.update { it.copy(error = "写下一点内容，或留下一张照片。") }; return }
        if (validatePolicy && current.page == LeavePage.POLICY && current.draft.policy == "FIXED_DATE" && current.draft.fixedDate == null) {
            mutable.update { it.copy(error = "请输入有效的再见日期，例如 2026-12-31。") }; return
        }
        mutable.update { it.copy(navigating = true) }
        enqueue {
            try {
                persistLatest(); savedState["leave.page"] = page.name
                mutable.update { it.copy(page = page, navigating = false, error = null) }; onExit?.invoke()
            } catch (failure: Exception) { mutable.update { it.copy(navigating = false, saving = false, saved = false, error = message(failure, "草稿未能保存，请重试后再离开。")) } }
        }
    }
    fun viewPhoto(id: String) {
        if (state.value.operationPending) return
        mutable.update { it.copy(photoId = id, photoReturn = it.page, page = LeavePage.PHOTO) }
    }
    fun leavePhoto() { mutable.update { it.copy(photoId = null, page = it.photoReturn) } }
    fun seal() {
        val current = state.value
        if (current.operationPending) return
        val invalid = sealValidation(current.draft)
        if (invalid != null) { mutable.update { it.copy(error = invalid) }; return }
        mutable.update { it.copy(sealing = true, error = null) }
        enqueue {
            try {
                val receipt = gateway.seal(state.value.draft)
                mutable.update { it.copy(draft = it.draft.copy(sealed = true), receipt = receipt, sealing = false, saved = true) }
                savedState["leave.page"] = LeavePage.COMPLETE.name
            } catch (failure: Exception) {
                val committed = runCatching { gateway.loadDraft(state.value.draft.id) }.getOrNull()?.takeIf { it.sealed }
                if (committed != null) {
                    mutable.update { it.copy(draft = committed, receipt = SealReceipt(committed.id, true),
                        sealing = false, saving = false, saved = true, error = null) }
                    savedState["leave.page"] = LeavePage.COMPLETE.name
                } else mutable.update { it.copy(sealing = false, saving = false,
                    error = message(failure, "暂未能确认封存结果。内容仍在，重试会核对同一条记录。")) }
            }
        }
    }
    fun startJourney(snapshotAvailable: Boolean) {
        if (!state.value.draft.sealed) return
        mutable.update { it.copy(page = if (snapshotAvailable && it.receipt?.alreadySealed == false) LeavePage.JOURNEY else LeavePage.COMPLETE) }
    }
    fun snapshotFailed() { mutable.update { it.copy(page = LeavePage.COMPLETE, error = "已安全封存。当前设备未能准备折船画面，本次以静态完成状态显示。") } }
    fun finishJourney() { if (state.value.draft.sealed) mutable.update { it.copy(page = LeavePage.COMPLETE) } }
    fun exitComplete(onExit: () -> Unit) {
        savedState.remove<String>("leave.draftId"); savedState.remove<String>("leave.page"); savedState.remove<String>("leave.fixedDateInput")
        mutable.value = LeaveState(); onExit(); beginSession()
    }
    override fun onCleared() { commands.close(); scope.cancel() }
    private fun message(failure: Exception, fallback: String): String = failure.message?.takeIf { it.isNotBlank() }?.take(220) ?: fallback
}
