package com.koach.studio.projects

import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.edit.EditDeliverableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditDeliverableViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: InMemoryProjectRepository
    private val testDeliverable = Deliverable(
        id = "del-edit-1",
        projectId = "proj-edit-1",
        name = "Initial_Name.mp4",
        type = "Video",
        size = "100 MB",
        version = "v1.0",
        notes = "Initial notes",
        status = DeliverableStatus.DRAFT
    )

    private val testProject = Project(
        id = "proj-edit-1",
        title = "Edit Test Project",
        clientName = "Client B",
        serviceType = ServiceType.EDITING,
        status = ProjectStatus.IN_PROGRESS,
        dueDate = "2026-11-28",
        description = "Description",
        deliverables = listOf(testDeliverable)
    )

    @Before
    fun setup() {
        repository = InMemoryProjectRepository(listOf(testProject))
    }

    @Test
    fun init_loadsInitialDeliverableData() = runTest {
        val viewModel = EditDeliverableViewModel("proj-edit-1", "del-edit-1", repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Initial_Name.mp4", state.name)
        assertEquals("Initial notes", state.notes)
        assertEquals(DeliverableStatus.DRAFT, state.status)
    }

    @Test
    fun validateName_blankName_returnsError() = runTest {
        val viewModel = EditDeliverableViewModel("proj-edit-1", "del-edit-1", repository)
        advanceUntilIdle()

        viewModel.onNameChanged("   ")
        assertNotNull(viewModel.uiState.value.nameError)
        assertEquals("Deliverable name cannot be blank", viewModel.uiState.value.nameError)
    }

    @Test
    fun validateName_tooShort_returnsError() = runTest {
        val viewModel = EditDeliverableViewModel("proj-edit-1", "del-edit-1", repository)
        advanceUntilIdle()

        viewModel.onNameChanged("ab")
        assertEquals("Deliverable name must be at least 3 characters", viewModel.uiState.value.nameError)
    }

    @Test
    fun validateName_valid_clearsError() = runTest {
        val viewModel = EditDeliverableViewModel("proj-edit-1", "del-edit-1", repository)
        advanceUntilIdle()

        viewModel.onNameChanged("Valid_File_v2.mp4")
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun save_validChanges_updatesRepositoryAndTriggersSuccess() = runTest {
        val viewModel = EditDeliverableViewModel("proj-edit-1", "del-edit-1", repository)
        advanceUntilIdle()

        viewModel.onNameChanged("Final_Cut_v2.mp4")
        viewModel.onNotesChanged("Color grading confirmed")
        viewModel.onStatusChanged(DeliverableStatus.APPROVED)

        var successCallbackCalled = false
        viewModel.save {
            successCallbackCalled = true
        }
        advanceUntilIdle()

        assertTrue(successCallbackCalled)
        assertTrue(viewModel.uiState.value.isSaved)

        // Verify repository data
        val project = repository.getProjectById("proj-edit-1").first()
        val deliverable = project?.deliverables?.find { it.id == "del-edit-1" }
        assertEquals("Final_Cut_v2.mp4", deliverable?.name)
        assertEquals("Color grading confirmed", deliverable?.notes)
        assertEquals(DeliverableStatus.APPROVED, deliverable?.status)
    }

    @Test
    fun save_invalidName_doesNotSave() = runTest {
        val viewModel = EditDeliverableViewModel("proj-edit-1", "del-edit-1", repository)
        advanceUntilIdle()

        viewModel.onNameChanged("")
        var successCallbackCalled = false
        viewModel.save {
            successCallbackCalled = true
        }
        advanceUntilIdle()

        assertFalse(successCallbackCalled)
        assertFalse(viewModel.uiState.value.isSaved)
    }
}
