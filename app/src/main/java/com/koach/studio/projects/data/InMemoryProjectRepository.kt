package com.koach.studio.projects.data

import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DownloadState
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class InMemoryProjectRepository(
    initialProjects: List<Project> = FakeProjectDataSource.createInitialProjects()
) : ProjectRepository {

    private val _projects = MutableStateFlow(initialProjects)

    override fun getProjects(): Flow<List<Project>> = _projects.asStateFlow()

    override fun getProjectById(projectId: String): Flow<Project?> {
        return _projects.map { list ->
            list.find { it.id == projectId }
        }
    }

    override suspend fun updateDeliverable(projectId: String, deliverable: Deliverable): Result<Unit> {
        val currentList = _projects.value
        val project = currentList.find { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Project not found: $projectId"))

        val deliverableIndex = project.deliverables.indexOfFirst { it.id == deliverable.id }
        if (deliverableIndex == -1) {
            return Result.failure(IllegalArgumentException("Deliverable not found: ${deliverable.id}"))
        }

        val updatedDeliverables = project.deliverables.toMutableList().apply {
            set(deliverableIndex, deliverable)
        }
        val updatedProject = project.copy(deliverables = updatedDeliverables)

        _projects.value = currentList.map {
            if (it.id == projectId) updatedProject else it
        }

        return Result.success(Unit)
    }

    override suspend fun requestRevision(projectId: String, reason: String): Result<Unit> {
        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Revision reason cannot be blank"))
        }
        val currentList = _projects.value
        val project = currentList.find { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Project not found: $projectId"))

        val updatedProject = project.copy(status = ProjectStatus.REVISION_REQUESTED)
        _projects.value = currentList.map {
            if (it.id == projectId) updatedProject else it
        }

        return Result.success(Unit)
    }

    override suspend fun startDeliverableDownload(
        projectId: String,
        deliverableId: String,
        simulateFailure: Boolean
    ) {
        val currentList = _projects.value
        val project = currentList.find { it.id == projectId } ?: return
        val deliverable = project.deliverables.find { it.id == deliverableId } ?: return

        // Step 1: Start downloading
        updateDeliverableState(
            projectId,
            deliverable.copy(downloadState = DownloadState.DOWNLOADING, downloadProgress = 0.2f)
        )
        delay(120)

        // Step 2: Mid progress
        updateDeliverableState(
            projectId,
            deliverable.copy(downloadState = DownloadState.DOWNLOADING, downloadProgress = 0.6f)
        )
        delay(120)

        // Step 3: Finish or Fail
        if (simulateFailure) {
            updateDeliverableState(
                projectId,
                deliverable.copy(downloadState = DownloadState.FAILED, downloadProgress = 0f)
            )
        } else {
            updateDeliverableState(
                projectId,
                deliverable.copy(downloadState = DownloadState.DONE, downloadProgress = 1.0f)
            )
        }
    }

    private fun updateDeliverableState(projectId: String, deliverable: Deliverable) {
        val currentList = _projects.value
        val project = currentList.find { it.id == projectId } ?: return
        val updatedDeliverables = project.deliverables.map {
            if (it.id == deliverable.id) deliverable else it
        }
        val updatedProject = project.copy(deliverables = updatedDeliverables)
        _projects.value = currentList.map {
            if (it.id == projectId) updatedProject else it
        }
    }
}
