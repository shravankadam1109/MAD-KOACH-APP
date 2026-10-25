package com.koach.studio.projects

import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.DownloadState
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.detail.ProjectDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: InMemoryProjectRepository
    private val testProject = Project(
        id = "proj-detail-test",
        title = "Detail Test Project",
        clientName = "Client A",
        serviceType = ServiceType.EDITING,
        status = ProjectStatus.IN_PROGRESS,
        dueDate = LocalDate.of(2026, 11, 25),
        description = "Testing details",
        deliverables = listOf(
            Deliverable(
                id = "del-test-1",
                projectId = "proj-detail-test",
                name = "Video_Edit_v1.mp4",
                type = "Video",
                size = "450 MB",
                version = "v1.0",
                status = DeliverableStatus.IN_REVIEW
            )
        )
    )

    @Before
    fun setup() {
        repository = InMemoryProjectRepository(listOf(testProject))
    }

    @Test
    fun uiState_loadsProjectSuccessfully() = runTest {
        val viewModel = ProjectDetailViewModel("proj-detail-test", repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertNotNull(state.project)
        assertEquals("Detail Test Project", state.project?.title)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun uiState_nonExistentProject_emitsErrorMessage() = runTest {
        val viewModel = ProjectDetailViewModel("invalid-id", repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertNull(state.project)
        assertEquals("Project not found", state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun downloadDeliverable_triggersDownloadStateUpdate() = runTest {
        val viewModel = ProjectDetailViewModel("proj-detail-test", repository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.downloadDeliverable("del-test-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DownloadState.DONE, state.downloads["del-test-1"]?.state)

        collectJob.cancel()
    }
}
