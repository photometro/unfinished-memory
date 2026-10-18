package com.unfinished.app.ui.leave

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.unfinished.app.data.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class LeaveStateTest {
    @Test fun purePhotosAndNoCueCanSealButUnresolvedImportsCannot() {
        val photo = CapturePhoto("photo", "memory", "original", "preview")
        assertNull(sealValidation(CaptureDraft(id = "memory", photos = listOf(photo))))
        assertNotNull(sealValidation(CaptureDraft(text = "keep", importErrors = listOf("failed"))))
        assertNotNull(sealValidation(CaptureDraft(text = "keep", policy = "FIXED_DATE")))
        assertNull(sealValidation(CaptureDraft(text = "keep", policy = "QUIET")))
    }

    @Test fun localCandidatesAreLiteralOptionalAndScopedToMemory() {
        val first = localCueCandidates("那个夏天，在老车站听雨声。", "one")
        val second = localCueCandidates("那个夏天，在老车站听雨声。", "two")
        assertEquals(listOf("夏天", "老车站", "雨声"), first.map { it.value })
        assertTrue(first.none { a -> second.any { it.id == a.id } })
        assertTrue(localCueCandidates("a synthetic plain record", "one").isEmpty())
        assertTrue(CaptureDraft(text = "那个夏天").cues.isEmpty())
    }

    @Test fun invalidDateAndRawInputArePreservedRatherThanReplacedByArbitraryDate() = runBlocking {
        val gateway = FakeGateway(CaptureDraft(text = "keep"))
        val store = ViewModelStore()
        val controller = LeaveController(gateway, SavedStateHandle(), Dispatchers.Unconfined)
        store.put("leave", controller)
        try {
            controller.setDate("2030-13-xx")
            assertTrue(controller.flush())
            assertEquals("2030-13-xx", gateway.persisted!!.fixedDateText)
            assertNull(gateway.persisted!!.fixedDate)
            controller.edit { it.copy(policy = "FIXED_DATE") }
            controller.seal()
            assertEquals(0, gateway.seals)
            assertFalse(controller.state.value.draft.sealed)
        } finally { store.clear() }
    }

    @Test fun olderAutosaveCannotOverwriteNewerEditingOrRunAfterSeal() = runBlocking {
        val gateway = FakeGateway(CaptureDraft(text = "start"))
        val store = ViewModelStore()
        val controller = LeaveController(gateway, SavedStateHandle(), Dispatchers.Unconfined)
        store.put("leave", controller)
        try {
            val blocked = CompletableDeferred<Unit>()
            gateway.blockNextSave = blocked
            controller.edit { it.copy(text = "first") }
            controller.edit { it.copy(text = "latest") }
            blocked.complete(Unit)
            assertTrue(controller.flush())
            assertEquals("latest", gateway.persisted!!.text)
            val id = controller.state.value.draft.id
            controller.seal()
            controller.edit { it.copy(text = "must not edit sealed") }
            assertEquals(id, gateway.persisted!!.id)
            assertEquals("latest", gateway.persisted!!.text)
            assertEquals(1, gateway.seals)
            assertTrue(controller.state.value.draft.sealed)
        } finally { store.clear() }
    }

    @Test fun lostSealResponseRetriesSameIdentityAndShowsCompletedWithoutReplaying() = runBlocking {
        val gateway = FakeGateway(CaptureDraft(text = "keep", policy = "QUIET"))
        gateway.loseNextSealResponse = true
        val store = ViewModelStore()
        val controller = LeaveController(gateway, SavedStateHandle(), Dispatchers.Unconfined)
        store.put("leave", controller)
        try {
            val id = controller.state.value.draft.id
            controller.seal()
            assertTrue(controller.state.value.draft.sealed)
            assertEquals(id, controller.state.value.receipt!!.memoryId)
            assertTrue(controller.state.value.receipt!!.alreadySealed)
            assertEquals(1, gateway.seals)
            controller.startJourney(true)
            assertEquals(LeavePage.COMPLETE, controller.state.value.page)
        } finally { store.clear() }
    }

    @Test fun cleanupWarningDoesNotTurnCommittedDraftIntoSaveFailure() = runBlocking {
        val gateway = FakeGateway(CaptureDraft(text = "keep"))
        gateway.warning = "cleanup pending"
        val store = ViewModelStore()
        val controller = LeaveController(gateway, SavedStateHandle(), Dispatchers.Unconfined)
        store.put("leave", controller)
        try {
            controller.edit { it.copy(text = "new keep") }
            assertTrue(controller.flush())
            assertTrue(controller.state.value.saved)
            assertEquals("cleanup pending", controller.state.value.draft.maintenanceWarning)
            assertNull(controller.state.value.error)
        } finally { store.clear() }
    }

    private class FakeGateway(initial: CaptureDraft) : CaptureGateway {
        var persisted: CaptureDraft? = initial
        var seals = 0
        var loseNextSealResponse = false
        var blockNextSave: CompletableDeferred<Unit>? = null
        var warning = ""
        override suspend fun latestDraft() = persisted?.takeUnless { it.sealed }
        override suspend fun loadDraft(id: String) = persisted?.takeIf { it.id == id }
        override suspend fun saveDraft(draft: CaptureDraft): CaptureDraft {
            val delay = blockNextSave
            blockNextSave = null
            delay?.await()
            check(persisted?.sealed != true)
            persisted = draft
            return draft.copy(maintenanceWarning = warning)
        }
        override suspend fun importPhoto(memoryId: String, uri: Uri): CapturePhoto = error("not used in this seam")
        override suspend fun preview(photo: CapturePhoto): Bitmap = error("not used in this seam")
        override suspend fun seal(draft: CaptureDraft): SealReceipt {
            if (persisted?.sealed == true) return SealReceipt(draft.id, true)
            persisted = draft.copy(sealed = true)
            seals++
            if (loseNextSealResponse) { loseNextSealResponse = false; error("lost response") }
            return SealReceipt(draft.id, false)
        }
    }
}
