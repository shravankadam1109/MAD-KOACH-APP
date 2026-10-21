package com.koach.studio.projects.data

import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun getProjects(): Flow<List<Project>>
    fun getProjectById(projectId: String): Flow<Project?>
    suspend fun updateDeliverable(projectId: String, deliverable: Deliverable): Result<Unit>
    /**
     * Records a client's change request in the project's revision history and
     * flags the project (and the deliverable, if given) as revision requested.
     */
    suspend fun requestRevision(
        projectId: String,
        reason: String,
        deliverableId: String? = null,
        requestedBy: String? = null
    ): Result<Unit>

    /** Client sign-off on a deliverable that is in review. */
    suspend fun approveDeliverable(projectId: String, deliverableId: String): Result<Unit>

    /** Agency shares a new version: bumps the version, puts it in review and resolves its open revisions. */
    suspend fun publishNewVersion(projectId: String, deliverableId: String, notes: String): Result<Unit>
    suspend fun startDeliverableDownload(projectId: String, deliverableId: String, simulateFailure: Boolean = false)
    suspend fun updateProjectStatus(projectId: String, status: ProjectStatus): Result<Unit>
    suspend fun setMilestoneCompleted(projectId: String, milestoneId: String, completed: Boolean): Result<Unit>
}
