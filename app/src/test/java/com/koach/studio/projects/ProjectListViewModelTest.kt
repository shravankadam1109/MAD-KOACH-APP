package com.koach.studio.projects

import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.list.ProjectListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: InMemoryProjectRepository
    private lateinit var viewModel: ProjectListViewModel

    private val sampleProjects = listOf(
        Project(
            id = "proj-1",
            title = "Alpha Music Video",
            clientName = "Warner Music",
            serviceType = ServiceType.SHOOTING,
            status = ProjectStatus.IN_PROGRESS,
            dueDate = LocalDate.of(2026, 11, 20),
            description = "Shooting video"
        ),
        Project(
            id = "proj-2",
            title = "Beta Podcast Session",
            clientName = "Spotify Studios",
            serviceType = ServiceType.RECORDING,
            status = ProjectStatus.IN_REVIEW,
            dueDate = LocalDate.of(2026, 11, 10),
            description = "Studio podcast record"
        ),
        Project(
            id = "proj-3",
            title = "Gamma TV Commercial",
            clientName = "Nike",
            serviceType = ServiceType.EDITING,
            status = ProjectStatus.COMPLETED,
            dueDate = LocalDate.of(2026, 10, 15),
            description = "Edit commercial reel"
        )
    )

    @Before
    fun setup() {
        repository = InMemoryProjectRepository(sampleProjects)
        viewModel = ProjectListViewModel(repository)
    }

    @Test
    fun uiState_loadsAllProjectsInitially() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.projects.size)

        collectJob.cancel()
    }

    @Test
    fun onFilterSelected_filtersProjectsByServiceType() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onFilterSelected(ServiceType.RECORDING)
        val recordingState = viewModel.uiState.value
        assertEquals(1, recordingState.projects.size)
        assertEquals("Beta Podcast Session", recordingState.projects.first().title)

        viewModel.onFilterSelected(ServiceType.SHOOTING)
        val shootingState = viewModel.uiState.value
        assertEquals(1, shootingState.projects.size)
        assertEquals("Alpha Music Video", shootingState.projects.first().title)

        viewModel.onFilterSelected(null)
        val allState = viewModel.uiState.value
        assertEquals(3, allState.projects.size)

        collectJob.cancel()
    }

    @Test
    fun onSearchQueryChanged_filtersByTitleAndClient() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSearchQueryChanged("nike")
        val state1 = viewModel.uiState.value
        assertEquals(1, state1.projects.size)
        assertEquals("Gamma TV Commercial", state1.projects.first().title)

        viewModel.onSearchQueryChanged("podcast")
        val state2 = viewModel.uiState.value
        assertEquals(1, state2.projects.size)
        assertEquals("Beta Podcast Session", state2.projects.first().title)

        viewModel.onSearchQueryChanged("nonexistent")
        val state3 = viewModel.uiState.value
        assertEquals(0, state3.projects.size)

        collectJob.cancel()
    }
}
