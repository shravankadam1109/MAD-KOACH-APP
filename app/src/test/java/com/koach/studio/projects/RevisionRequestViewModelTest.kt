package com.koach.studio.projects

import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.revision.RevisionRequestViewModel
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RevisionRequestViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: InMemoryProjectRepository
    private val testProject = Project(
        id = "proj-rev-test",
        title = "Revision Test Project",
        clientName = "Client C",
        serviceType = ServiceType.SHOOTING,
        status = ProjectStatus.IN_REVIEW,
        dueDate = LocalDate.of(2026, 11, 30),
        description = "Description"
    )

    @Before
    fun setup() {
        repository = InMemoryProjectRepository(listOf(testProject))
    }

    @Test
    fun init_loadsProjectDetails() = runTest {
        val viewModel = RevisionRequestViewModel("proj-rev-test", repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Revision Test Project", state.project?.title)
    }

    @Test
    fun onReasonChanged_validatesInput() = runTest {
        val viewModel = RevisionRequestViewModel("proj-rev-test", repository)
        advanceUntilIdle()

        viewModel.onReasonChanged("ab")
        assertNotNull(viewModel.uiState.value.reasonError)

        viewModel.onReasonChanged("Valid revision details for audio")
        assertNull(viewModel.uiState.value.reasonError)
    }

    @Test
    fun applyQuickTag_appendsTagText() = runTest {
        val viewModel = RevisionRequestViewModel("proj-rev-test", repository)
        advanceUntilIdle()

        viewModel.applyQuickTag("Audio Level Balancing")
        assertEquals("Audio Level Balancing", viewModel.uiState.value.reason)

        viewModel.applyQuickTag("Color Correction")
        assertEquals("Audio Level Balancing, Color Correction", viewModel.uiState.value.reason)
    }

    @Test
    fun submit_validReason_updatesRepositoryProjectStatus() = runTest {
        val viewModel = RevisionRequestViewModel("proj-rev-test", repository)
        advanceUntilIdle()

        viewModel.onReasonChanged("Please adjust vocal mix and drums eq.")
        var successCalled = false
        viewModel.submit {
            successCalled = true
        }
        advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(viewModel.uiState.value.isSubmitted)

        // Verify repository project status is updated
        val updatedProject = repository.getProjectById("proj-rev-test").first()
        assertEquals(ProjectStatus.REVISION_REQUESTED, updatedProject?.status)
    }

    @Test
    fun submit_invalidReason_doesNotSubmit() = runTest {
        val viewModel = RevisionRequestViewModel("proj-rev-test", repository)
        advanceUntilIdle()

        viewModel.onReasonChanged("bad")
        var successCalled = false
        viewModel.submit {
            successCalled = true
        }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertFalse(viewModel.uiState.value.isSubmitted)
    }
}
