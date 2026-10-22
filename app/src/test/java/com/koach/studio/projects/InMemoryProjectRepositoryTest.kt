package com.koach.studio.projects

import com.koach.studio.projects.data.DeliverableDownloader
import com.koach.studio.projects.data.InMemoryProjectRepository
import com.koach.studio.projects.data.SimulatedDeliverableDownloader
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.DownloadState
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class InMemoryProjectRepositoryTest {

    private lateinit var repository: InMemoryProjectRepository
    private val testProject = Project(
        id = "test-proj-1",
        title = "Test Music Shoot",
        clientName = "Indie Label",
        serviceType = ServiceType.SHOOTING,
        status = ProjectStatus.IN_PROGRESS,
        dueDate = LocalDate.of(2026, 12, 1),
        description = "Shooting test footage",
        deliverables = listOf(
            Deliverable(
                id = "test-del-1",
                projectId = "test-proj-1",
                name = "Cut_v1.mp4",
                type = "Video",
                size = "500 MB",
                version = "v1.0",
                notes = "Initial cut",
                status = DeliverableStatus.DRAFT
            )
        )
    )

    @Before
    fun setup() {
        repository = InMemoryProjectRepository(listOf(testProject))
    }

    @Test
    fun getProjects_returnsInitialProjects() = runTest {
        val projects = repository.getProjects().first()
        assertEquals(1, projects.size)
        assertEquals("test-proj-1", projects.first().id)
    }

    @Test
    fun getProjectById_existingId_returnsProject() = runTest {
        val project = repository.getProjectById("test-proj-1").first()
        assertNotNull(project)
        assertEquals("Test Music Shoot", project?.title)
    }

    @Test
    fun getProjectById_unknownId_returnsNull() = runTest {
        val project = repository.getProjectById("unknown-id").first()
        assertNull(project)
    }

    @Test
    fun updateDeliverable_success_updatesDeliverableAndEmitsNewState() = runTest {
        val updatedDeliverable = testProject.deliverables.first().copy(
            name = "Cut_v2_Final.mp4",
            notes = "Updated per client feedback",
            status = DeliverableStatus.APPROVED
        )

        val result = repository.updateDeliverable("test-proj-1", updatedDeliverable)
        assertTrue(result.isSuccess)

        val project = repository.getProjectById("test-proj-1").first()
        val deliverable = project?.deliverables?.find { it.id == "test-del-1" }
        assertEquals("Cut_v2_Final.mp4", deliverable?.name)
        assertEquals("Updated per client feedback", deliverable?.notes)
        assertEquals(DeliverableStatus.APPROVED, deliverable?.status)
    }

    @Test
    fun updateDeliverable_unknownProject_returnsFailure() = runTest {
        val updated = testProject.deliverables.first()
        val result = repository.updateDeliverable("non-existent-proj", updated)
        assertTrue(result.isFailure)
    }

    @Test
    fun requestRevision_validReason_updatesProjectStatusToRevisionRequested() = runTest {
        val result = repository.requestRevision("test-proj-1", "Needs color grading adjustments")
        assertTrue(result.isSuccess)

        val project = repository.getProjectById("test-proj-1").first()
        assertEquals(ProjectStatus.REVISION_REQUESTED, project?.status)
    }

    @Test
    fun requestRevision_blankReason_fails() = runTest {
        val result = repository.requestRevision("test-proj-1", "   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun startDeliverableDownload_success_completesWithDoneState() = runTest {
        // Download progress moved out of the shared data into a device-local downloader.
        val downloader = SimulatedDeliverableDownloader()
        val deliverable = repository.getProjectById("test-proj-1").first()!!.deliverables.first { it.id == "test-del-1" }
        downloader.download(deliverable, simulateFailure = false)

        val progress = downloader.downloads.value[DeliverableDownloader.key(deliverable)]
        assertEquals(DownloadState.DONE, progress?.state)
        assertEquals(1.0f, progress?.progress ?: 0f, 0.01f)
    }

    @Test
    fun startDeliverableDownload_failureSimulation_completesWithFailedState() = runTest {
        val downloader = SimulatedDeliverableDownloader()
        val deliverable = repository.getProjectById("test-proj-1").first()!!.deliverables.first { it.id == "test-del-1" }
        downloader.download(deliverable, simulateFailure = true)

        assertEquals(DownloadState.FAILED, downloader.downloads.value[DeliverableDownloader.key(deliverable)]?.state)
    }

    @Test
    fun downloadProgress_isNotStoredInProjectData_andNewVersionStartsFresh() = runTest {
        val downloader = SimulatedDeliverableDownloader()
        val v1 = repository.getProjectById("test-proj-1").first()!!.deliverables.first { it.id == "test-del-1" }
        downloader.download(v1)
        repository.publishNewVersion("test-proj-1", "test-del-1", "")
        val v2 = repository.getProjectById("test-proj-1").first()!!.deliverables.first { it.id == "test-del-1" }

        assertEquals(DownloadState.DONE, downloader.downloads.value[DeliverableDownloader.key(v1)]?.state)
        assertEquals(null, downloader.downloads.value[DeliverableDownloader.key(v2)])
    }
}
