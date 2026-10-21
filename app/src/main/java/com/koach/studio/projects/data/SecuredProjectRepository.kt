package com.koach.studio.projects.data

import com.koach.studio.core.auth.AccessControl
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.model.User
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.Project
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
            if (user == null) emptyList() else projects.filter { access.canAccessClient(it.clientId) }
        }

    override fun getProjectById(projectId: String): Flow<Project?> =
        combine(delegate.getProjectById(projectId), currentUser) { project, user ->
            project?.takeIf { user != null && access.canAccessClient(it.clientId) }
        }

    override suspend fun updateDeliverable(projectId: String, deliverable: Deliverable): Result<Unit> = guarded {
        access.require(Permission.EDIT_DELIVERABLES)
        requireProject(projectId)
        delegate.updateDeliverable(projectId, deliverable)
    }

    override suspend fun requestRevision(projectId: String, reason: String): Result<Unit> = guarded {
        access.require(Permission.REQUEST_REVISIONS)
        requireProject(projectId)
        delegate.requestRevision(projectId, reason)
    }

    override suspend fun startDeliverableDownload(projectId: String, deliverableId: String, simulateFailure: Boolean) {
        if (getProjectById(projectId).first() == null) return
        delegate.startDeliverableDownload(projectId, deliverableId, simulateFailure)
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
}
