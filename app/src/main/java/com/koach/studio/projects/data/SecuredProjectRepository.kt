package com.koach.studio.projects.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.PermissionDeniedException
import com.koach.studio.core.model.User
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Applies role rules on top of any [ProjectRepository]: clients only ever see
 * and act on their own organisation's projects, and each write checks the
 * caller's permission. The in-memory store stays rule-free and can be replaced
 * by a Firestore implementation without losing these checks.
 */
class SecuredProjectRepository(
    private val delegate: ProjectRepository,
    private val currentUser: StateFlow<User?>,
    private val access: AccessControl
) : ProjectRepository {

    override fun getProjects(): Flow<List<Project>> =
        combine(delegate.getProjects(), currentUser) { projects, user ->
            if (user == null) emptyList()
            else projects.filter { access.canAccessClient(it.clientId) }.map { it.visibleTo(user) }
        }

    override fun getProjectById(projectId: String): Flow<Project?> =
        combine(delegate.getProjectById(projectId), currentUser) { project, user ->
            project?.takeIf { user != null && access.canAccessClient(it.clientId) }?.visibleTo(user!!)
        }

    /** Clients don't see work-in-progress drafts; those stay with the agency until shared for review. */
    private fun Project.visibleTo(user: User): Project =
        if (user.role.isAgency) this else copy(deliverables = deliverables.filter { it.status != DeliverableStatus.DRAFT })

    override suspend fun updateDeliverable(projectId: String, deliverable: Deliverable): Result<Unit> = guarded {
        access.require(Permission.EDIT_DELIVERABLES)
        val existing = requireProject(projectId).deliverables.find { it.id == deliverable.id }
            ?: throw IllegalArgumentException("Deliverable not found: ${deliverable.id}")
        // Approval and change requests are the client's decisions, never set by the agency.
        if (deliverable.status != existing.status && deliverable.status in CLIENT_DECISIONS) {
            throw PermissionDeniedException("Only the client can approve a deliverable or request changes")
        }
        delegate.updateDeliverable(projectId, deliverable)
    }

    override suspend fun requestRevision(
        projectId: String,
        reason: String,
        deliverableId: String?,
        requestedBy: String?
    ): Result<Unit> = guarded {
        val user = access.require(Permission.REQUEST_REVISIONS)
        requireProject(projectId)
        delegate.requestRevision(projectId, reason, deliverableId, requestedBy = user.id)
    }

    override suspend fun approveDeliverable(projectId: String, deliverableId: String): Result<Unit> = guarded {
        access.require(Permission.REVIEW_DELIVERABLES)
        requireProject(projectId)
        delegate.approveDeliverable(projectId, deliverableId)
    }

    override suspend fun publishNewVersion(projectId: String, deliverableId: String, notes: String): Result<Unit> = guarded {
        access.require(Permission.EDIT_DELIVERABLES)
        requireProject(projectId)
        delegate.publishNewVersion(projectId, deliverableId, notes)
    }

    override suspend fun startDeliverableDownload(projectId: String, deliverableId: String, simulateFailure: Boolean) {
        if (getProjectById(projectId).first() == null) return
        delegate.startDeliverableDownload(projectId, deliverableId, simulateFailure)
    }

    override suspend fun updateProjectStatus(projectId: String, status: ProjectStatus): Result<Unit> = guarded {
        access.require(Permission.MANAGE_PROJECTS)
        requireProject(projectId)
        delegate.updateProjectStatus(projectId, status)
    }

    override suspend fun setMilestoneCompleted(projectId: String, milestoneId: String, completed: Boolean): Result<Unit> = guarded {
        access.require(Permission.MANAGE_PROJECTS)
        requireProject(projectId)
        delegate.setMilestoneCompleted(projectId, milestoneId, completed)
    }

    private suspend fun requireProject(projectId: String): Project {
        val project = delegate.getProjectById(projectId).first()
            ?: throw IllegalArgumentException("Project not found: $projectId")
        access.requireClientAccess(project.clientId)
        return project
    }

    private inline fun guarded(block: () -> Result<Unit>): Result<Unit> =
        try {
            block()
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        }

    private companion object {
        val CLIENT_DECISIONS = setOf(DeliverableStatus.APPROVED, DeliverableStatus.REVISION_REQUESTED)
    }
}
