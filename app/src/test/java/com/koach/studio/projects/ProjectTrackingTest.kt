package com.koach.studio.projects

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.DemoAuthRepository
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.data.DemoData
import com.koach.studio.projects.data.FakeProjectDataSource
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SecuredProjectRepository
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.ui.detail.ProjectDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectTrackingTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val data = DemoData(LocalDate.of(2026, 10, 22))
    private val auth = DemoAuthRepository(data.users)
    private val access = AccessControl { auth.currentUser.value }
    private val repository = SecuredProjectRepository(
        InMemoryProjectRepository(FakeProjectDataSource.createInitialProjects()), auth.currentUser, access
    )

    @Test
    fun progress_isShareOfCompletedMilestones_andFullWhenCompleted() = runTest {
        auth.signInAsDemo("u-zoya")
        assertEquals(0.5f, repository.getProjectById("proj-1").first()!!.progress)
        assertEquals(1f, repository.getProjectById("proj-4").first()!!.progress)
    }

    @Test
    fun agency_togglesMilestones_andChangesStatus() = runTest {
        auth.signInAsDemo("u-zoya")
        assertTrue(repository.setMilestoneCompleted("proj-1", "ms-13", true).isSuccess)
        assertEquals(0.75f, repository.getProjectById("proj-1").first()!!.progress)
        assertTrue(repository.updateProjectStatus("proj-1", ProjectStatus.IN_REVIEW).isSuccess)
        assertEquals(ProjectStatus.IN_REVIEW, repository.getProjectById("proj-1").first()!!.status)
        assertTrue(repository.setMilestoneCompleted("proj-1", "missing", true).isFailure)
    }

    @Test
    fun clients_cannotChangeMilestonesOrStatus() = runTest {
        auth.signInAsDemo("u-maya")
        assertTrue(repository.setMilestoneCompleted("proj-1", "ms-13", true).exceptionOrNull() is PermissionDeniedException)
        assertTrue(repository.updateProjectStatus("proj-1", ProjectStatus.COMPLETED).exceptionOrNull() is PermissionDeniedException)
    }

    @Test
    fun detailViewModel_offersActionsByRole_andResolvesCrew() = runTest {
        auth.signInAsDemo("u-maya")
        val clientVm = ProjectDetailViewModel("proj-1", repository, auth.currentUser.value, data.users)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { clientVm.uiState.collect {} }
        val clientState = clientVm.uiState.first { !it.isLoading && it.project != null }
        assertTrue(clientState.canRequestRevision)
        assertFalse(clientState.canManage)
        assertFalse(clientState.canEditDeliverables)
        assertEquals(listOf("u-rohan", "u-zoya", "u-isha"), clientState.team.map { it.id })

        auth.signInAsDemo("u-zoya")
        val agencyVm = ProjectDetailViewModel("proj-1", repository, auth.currentUser.value, data.users)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { agencyVm.uiState.collect {} }
        val agencyState = agencyVm.uiState.first { !it.isLoading && it.project != null }
        assertTrue(agencyState.canManage)
        assertTrue(agencyState.canEditDeliverables)
        assertFalse(agencyState.canRequestRevision)
    }
}
