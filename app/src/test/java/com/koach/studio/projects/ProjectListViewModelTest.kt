package com.koach.studio.projects

import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectSortOrder
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

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
            dueDate = "2026-11-20",
            description = "Shooting video"
        ),
        Project(
            id = "proj-2",
            title = "Beta Podcast Session",
            clientName = "Spotify Studios",
            serviceType = ServiceType.RECORDING,
            status = ProjectStatus.IN_REVIEW,
            dueDate = "2026-11-10",
            description = "Studio podcast record"
        ),
        Project(
            id = "proj-3",
            title = "Gamma TV Commercial",
            clientName = "Nike",
            serviceType = ServiceType.EDITING,
            status = ProjectStatus.COMPLETED,
            dueDate = "2026-10-15",
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

    // --- New tests for status filter ---

    @Test
    fun onStatusFilterSelected_filtersProjectsByStatus() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onStatusFilterSelected(ProjectStatus.COMPLETED)
        val completedState = viewModel.uiState.value
        assertEquals(1, completedState.projects.size)
        assertEquals("Gamma TV Commercial", completedState.projects.first().title)

        viewModel.onStatusFilterSelected(ProjectStatus.IN_REVIEW)
        val inReviewState = viewModel.uiState.value
        assertEquals(1, inReviewState.projects.size)
        assertEquals("Beta Podcast Session", inReviewState.projects.first().title)

        viewModel.onStatusFilterSelected(ProjectStatus.IN_PROGRESS)
        val inProgressState = viewModel.uiState.value
        assertEquals(1, inProgressState.projects.size)
        assertEquals("Alpha Music Video", inProgressState.projects.first().title)

        viewModel.onStatusFilterSelected(null)
        val allState = viewModel.uiState.value
        assertEquals(3, allState.projects.size)

        collectJob.cancel()
    }

    @Test
    fun onSortOrderSelected_sortsByDueDateAscending() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSortOrderSelected(ProjectSortOrder.DUE_DATE)
        val state = viewModel.uiState.value
        // proj-3 due 2026-10-15 < proj-2 due 2026-11-10 < proj-1 due 2026-11-20
        assertEquals(3, state.projects.size)
        assertEquals("proj-3", state.projects[0].id)
        assertEquals("proj-2", state.projects[1].id)
        assertEquals("proj-1", state.projects[2].id)

        collectJob.cancel()
    }

    @Test
    fun onSortOrderSelected_sortsByStatusOrdinal() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSortOrderSelected(ProjectSortOrder.STATUS)
        val state = viewModel.uiState.value
        assertEquals(3, state.projects.size)
        // Verify sorted by status ordinal: all ordinals are in non-decreasing order
        val ordinals = state.projects.map { it.status.ordinal }
        assertEquals(ordinals, ordinals.sorted())

        collectJob.cancel()
    }

    @Test
    fun searchAndStatusFilter_combinedFiltering() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        // Search for "music" AND filter by IN_PROGRESS — only proj-1 matches both
        viewModel.onSearchQueryChanged("music")
        viewModel.onStatusFilterSelected(ProjectStatus.IN_PROGRESS)
        val combined = viewModel.uiState.value
        assertEquals(1, combined.projects.size)
        assertEquals("Alpha Music Video", combined.projects.first().title)

        // Search for "music" AND filter by COMPLETED — no match
        viewModel.onStatusFilterSelected(ProjectStatus.COMPLETED)
        val noMatch = viewModel.uiState.value
        assertEquals(0, noMatch.projects.size)

        collectJob.cancel()
    }

    @Test
    fun emptyResult_whenNoProjectsMatchSearch() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSearchQueryChanged("xyzzy_not_a_real_project_12345")
        val state = viewModel.uiState.value
        assertTrue(state.projects.isEmpty())

        collectJob.cancel()
    }

    @Test
    fun clearFilters_resetsAllFilters() = runTest {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSearchQueryChanged("music")
        viewModel.onFilterSelected(ServiceType.SHOOTING)
        viewModel.onStatusFilterSelected(ProjectStatus.IN_PROGRESS)
        assertEquals(1, viewModel.uiState.value.projects.size)

        viewModel.clearFilters()
        val clearedState = viewModel.uiState.value
        assertEquals(3, clearedState.projects.size)
        assertTrue(clearedState.searchQuery.isBlank())
        assertEquals(null, clearedState.selectedServiceFilter)
        assertEquals(null, clearedState.selectedStatusFilter)

        collectJob.cancel()
    }
}