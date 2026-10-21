package com.koach.studio.projects.data

import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.DownloadState
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.Revision
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class InMemoryProjectRepository(
    initialProjects: List<Project> = FakeProjectDataSource.createInitialProjects(),
    private val clock: Clock = Clock.systemDefaultZone()
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

    override suspend fun requestRevision(
        projectId: String,
        reason: String,
        deliverableId: String?,
        requestedBy: String?
    ): Result<Unit> {
        if (reason.isBlank()) {
            return Result.failure(IllegalArgumentException("Revision reason cannot be blank"))
        }
        val currentList = _projects.value
        val project = currentList.find { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Project not found: $projectId"))
        if (deliverableId != null && project.deliverables.none { it.id == deliverableId }) {
            return Result.failure(IllegalArgumentException("Deliverable not found: $deliverableId"))
        }

        val revision = Revision(
            id = "rev-" + UUID.randomUUID().toString().take(8),
            projectId = projectId,
            deliverableId = deliverableId,
            reason = reason.trim(),
            requestedBy = requestedBy.orEmpty(),
            requestedOn = LocalDate.now(clock)
        )
        val updatedProject = project.copy(
            status = ProjectStatus.REVISION_REQUESTED,
            revisions = listOf(revision) + project.revisions,
            deliverables = project.deliverables.map {
                if (it.id == deliverableId) it.copy(status = DeliverableStatus.REVISION_REQUESTED) else it
            }
        )
        _projects.value = currentList.map {
            if (it.id == projectId) updatedProject else it
        }

        return Result.success(Unit)
    }

    override suspend fun approveDeliverable(projectId: String, deliverableId: String): Result<Unit> {
        val deliverable = _projects.value.find { it.id == projectId }?.deliverables?.find { it.id == deliverableId }
            ?: return Result.failure(IllegalArgumentException("Deliverable not found: $deliverableId"))
        if (deliverable.status != DeliverableStatus.IN_REVIEW) {
            return Result.failure(IllegalStateException("Only deliverables in review can be approved"))
        }
        return updateProject(projectId) { p ->
            p.copy(deliverables = p.deliverables.map { if (it.id == deliverableId) it.copy(status = DeliverableStatus.APPROVED) else it })
        }
    }

    override suspend fun publishNewVersion(projectId: String, deliverableId: String, notes: String): Result<Unit> {
        val project = _projects.value.find { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Project not found: $projectId"))
        if (project.deliverables.none { it.id == deliverableId }) {
            return Result.failure(IllegalArgumentException("Deliverable not found: $deliverableId"))
        }
        return updateProject(projectId) { p ->
            val deliverables = p.deliverables.map {
                if (it.id != deliverableId) it
                else it.copy(
                    version = it.nextVersion(),
                    previousVersions = it.previousVersions + it.version,
                    status = DeliverableStatus.IN_REVIEW,
                    notes = notes.trim().ifBlank { it.notes },
                    downloadState = DownloadState.IDLE,
                    downloadProgress = 0f
                )
            }
            val revisions = p.revisions.map { if (it.deliverableId == deliverableId) it.copy(resolved = true) else it }
            val stillOpen = revisions.any { !it.resolved } || deliverables.any { it.status == DeliverableStatus.REVISION_REQUESTED }
            p.copy(
                deliverables = deliverables,
                revisions = revisions,
                status = if (p.status == ProjectStatus.REVISION_REQUESTED && !stillOpen) ProjectStatus.IN_REVIEW else p.status
            )
        }
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

    override suspend fun updateProjectStatus(projectId: String, status: ProjectStatus): Result<Unit> =
        updateProject(projectId) { it.copy(status = status) }

    override suspend fun setMilestoneCompleted(projectId: String, milestoneId: String, completed: Boolean): Result<Unit> {
        val project = _projects.value.find { it.id == projectId }
            ?: return Result.failure(IllegalArgumentException("Project not found: $projectId"))
        if (project.milestones.none { it.id == milestoneId }) {
            return Result.failure(IllegalArgumentException("Milestone not found: $milestoneId"))
        }
        return updateProject(projectId) { p ->
            p.copy(milestones = p.milestones.map { if (it.id == milestoneId) it.copy(completed = completed) else it })
        }
    }

    private fun updateProject(projectId: String, change: (Project) -> Project): Result<Unit> {
        val currentList = _projects.value
        if (currentList.none { it.id == projectId }) {
            return Result.failure(IllegalArgumentException("Project not found: $projectId"))
        }
        _projects.value = currentList.map { if (it.id == projectId) change(it) else it }
        return Result.success(Unit)
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
