package com.koach.studio.documents

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.DocumentCategory
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.documents.data.DemoDocumentStorage
import com.koach.studio.documents.data.DocumentDraft
import com.koach.studio.documents.data.InMemoryDocumentRepository
import com.koach.studio.documents.ui.AddDocumentViewModel
import com.koach.studio.documents.ui.DocumentsViewModel
import com.koach.studio.documents.ui.PickedFile
import com.koach.studio.projects.MainDispatcherRule
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentManagementTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 22)
    private val clock = Clock.fixed(today.atStartOfDay(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"))
    private val data = DemoData(today)
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val docs = InMemoryDocumentRepository(data.documents, auth.currentUser, access, clock)
    private val projects = SecuredProjectRepository(InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects()), auth.currentUser, access)

    @Test
    fun clients_seeOnlySharedDocumentsForTheirOrganisation() = runTest {
        auth.signInAsDemo("u-maya")
        val visible = docs.observeDocuments().first()
        assertTrue(visible.isNotEmpty())
        assertTrue(visible.all { it.clientId == "cl-aetheria" && it.visibility == DocumentVisibility.SHARED })
        assertFalse(visible.any { it.id == "doc-3" }) // internal shot list
    }

    @Test
    fun clientUploads_areForcedSharedAndScopedToTheirClient_withNoStoragePath() = runTest {
        auth.signInAsDemo("u-maya")
        val added = docs.addDocument(
            DocumentDraft("Moodboard.pdf", DocumentCategory.BRIEF, "proj-1", "cl-apex", "application/pdf", 2_000, DocumentVisibility.INTERNAL)
        ).getOrThrow()
        assertEquals(DocumentVisibility.SHARED, added.visibility)
        assertEquals("cl-aetheria", added.clientId)
        assertNull(added.storagePath)
        assertFalse(DemoDocumentStorage.isAvailable)
    }

    @Test
    fun onlyAgency_changesVisibility_andClientsDeleteOnlyTheirOwn() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(docs.setVisibility("doc-1", DocumentVisibility.INTERNAL).exceptionOrNull() is PermissionDeniedException)
        assertTrue(docs.deleteDocument("doc-1").exceptionOrNull() is PermissionDeniedException)

        auth.signInAsDemo("u-zoya")
        assertTrue(docs.setVisibility("doc-1", DocumentVisibility.INTERNAL).isSuccess)
        auth.signInAsDemo("u-maya")
        assertFalse(docs.observeDocuments().first().any { it.id == "doc-1" })
    }

    @Test
    fun list_filtersByProjectCategoryAndSearch() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = DocumentsViewModel(auth.currentUser.value!!, docs, projects, data.users, DemoDocumentStorage, projectId = "proj-1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        assertTrue(state.rows.all { it.document.projectId == "proj-1" })
        assertEquals("Cyberpunk Indie Music Video", state.projectFilterTitle)

        vm.onCategory(DocumentCategory.CONTRACT)
        assertEquals(listOf("doc-2"), vm.uiState.value.rows.map { it.document.id })
        vm.onCategory(null)
        vm.onQuery("storyboard")
        assertEquals(listOf("doc-4"), vm.uiState.value.rows.map { it.document.id })
    }

    @Test
    fun addForm_usesPickedFileDetails_andGuessesCategory() = runTest {
        auth.signInAsDemo("u-zoya")
        val vm = AddDocumentViewModel(auth.currentUser.value!!, docs, projects, "proj-3")
        vm.onFilePicked(PickedFile("Invoice_Oct.pdf", "application/pdf", 48_000))
        assertEquals("Invoice_Oct.pdf", vm.uiState.value.name)
        assertEquals(DocumentCategory.INVOICE, vm.uiState.value.category)
        vm.save()
        assertTrue(vm.uiState.value.saved)
        val stored = docs.observeDocuments().first().first { it.name == "Invoice_Oct.pdf" }
        assertEquals("cl-apex", stored.clientId)
        assertEquals(48_000, stored.sizeBytes)
    }
}
