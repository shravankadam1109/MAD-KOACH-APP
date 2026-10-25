package com.koach.studio.projects.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.koach.studio.core.auth.NotSignedInException
import com.koach.studio.core.firebase.FirestoreMappers
import com.koach.studio.core.firebase.observe
import com.koach.studio.core.firebase.whenSignedIn
import com.koach.studio.core.model.User
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.Revision
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate

/**
 * Projects in Firestore: `projects/{id}` with `deliverables` and `revisions`
 * subcollections, milestones kept as an array on the project. Wrap it in
 * [SecuredProjectRepository] for the in-app permission checks; the security
 * rules enforce the same rules on the server.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreProjectRepository(
    private val firestore: FirebaseFirestore,
    private val currentUser: StateFlow<User?>,
    private val clock: Clock
) : ProjectRepository {

    private val projects get() = firestore.collection(COLLECTION)
    private fun deliverablesOf(projectId: String) = projects.document(projectId).collection(DELIVERABLES)
    private fun revisionsOf(projectId: String) = projects.document(projectId).collection(REVISIONS)

    override fun getProjects(): Flow<List<Project>> =
        currentUser.whenSignedIn(emptyList()) { user ->
            val query = if (user.role.isAgency) projects else projects.whereEqualTo("clientId", user.clientId)
            query.observe { snap -> snap.documents.map { it.id to it.data.orEmpty() } }
                .flatMapLatest { docs ->
                    if (docs.isEmpty()) flowOf(emptyList())
                    else combine(docs.map { (id, data) -> withChildren(user, id, data) }) { it.filterNotNull().sortedBy { p -> p.dueDate } }
                }
        }

    override fun getProjectById(projectId: String): Flow<Project?> =
        currentUser.whenSignedIn(null) { user ->
            projects.document(projectId).observe { it.data.orEmpty() }
                .flatMapLatest { data -> if (data == null) flowOf(null) else withChildren(user, projectId, data) }
        }

    /** Joins a project document with live deliverables and revisions. */
    private fun withChildren(user: User, id: String, data: Map<String, Any?>): Flow<Project?> {
        // Clients may not read drafts, so their query has to exclude them for the rules to allow it.
        val deliverableQuery = if (user.role.isAgency) deliverablesOf(id)
        else deliverablesOf(id).whereIn("status", CLIENT_VISIBLE.map { it.name })
        val deliverables = deliverableQuery.observe { snap ->
            snap.documents.mapNotNull { FirestoreMappers.deliverable(it.id, id, it.data.orEmpty()) }.sortedBy { it.name }
        }
        val revisions = revisionsOf(id).observe { snap -> snap.documents.mapNotNull { FirestoreMappers.revision(it.id, id, it.data.orEmpty()) } }
        return combine(deliverables, revisions) { d, r -> FirestoreMappers.project(id, data, d, r) }
    }

    override suspend fun updateDeliverable(projectId: String, deliverable: Deliverable): Result<Unit> = runCatching {
        val ref = deliverablesOf(projectId).document(deliverable.id)
        require(ref.get().await().exists()) { "Deliverable not found: ${deliverable.id}" }
        ref.set(FirestoreMappers.deliverableFields(deliverable) + ("updatedAt" to FieldValue.serverTimestamp())).await()
        Unit
    }

    override suspend fun requestRevision(
        projectId: String,
        reason: String,
        deliverableId: String?,
        requestedBy: String?
    ): Result<Unit> = runCatching {
        require(reason.isNotBlank()) { "Revision reason cannot be blank" }
        val uid = requestedBy ?: currentUser.value?.id ?: throw NotSignedInException()
        val deliverable = deliverableId?.let { loadDeliverable(projectId, it) }

        val revisionRef = revisionsOf(projectId).document()
        val revision = Revision(revisionRef.id, projectId, deliverableId, reason.trim(), uid, LocalDate.now(clock))
        val batch = firestore.batch()
        batch.set(revisionRef, FirestoreMappers.revisionFields(revision))
        batch.update(projects.document(projectId), mapOf("status" to ProjectStatus.REVISION_REQUESTED.name, "updatedAt" to FieldValue.serverTimestamp()))
        // A change request can only be made on something that is in review.
        if (deliverable != null && deliverable.status == DeliverableStatus.IN_REVIEW) {
            batch.update(
                deliverablesOf(projectId).document(deliverable.id),
                mapOf("status" to DeliverableStatus.REVISION_REQUESTED.name, "updatedAt" to FieldValue.serverTimestamp())
            )
        }
        batch.commit().await()
        Unit
    }

    override suspend fun approveDeliverable(projectId: String, deliverableId: String): Result<Unit> = runCatching {
        val deliverable = loadDeliverable(projectId, deliverableId)
        check(deliverable.status == DeliverableStatus.IN_REVIEW) { "Only deliverables in review can be approved" }
        deliverablesOf(projectId).document(deliverableId)
            .update(mapOf("status" to DeliverableStatus.APPROVED.name, "updatedAt" to FieldValue.serverTimestamp())).await()
        Unit
    }

    override suspend fun publishNewVersion(projectId: String, deliverableId: String, notes: String): Result<Unit> = runCatching {
        val project = loadProject(projectId)
        val deliverable = project.deliverables.find { it.id == deliverableId }
            ?: throw IllegalArgumentException("Deliverable not found: $deliverableId")
        val updated = deliverable.copy(
            version = deliverable.nextVersion(),
            previousVersions = deliverable.previousVersions + deliverable.version,
            status = DeliverableStatus.IN_REVIEW,
            notes = notes.trim().ifBlank { deliverable.notes }
        )
        val resolving = project.revisions.filter { it.deliverableId == deliverableId && !it.resolved }
        val stillOpen = project.revisions.any { !it.resolved && it !in resolving } ||
            project.deliverables.any { it.id != deliverableId && it.status == DeliverableStatus.REVISION_REQUESTED }

        val batch = firestore.batch()
        batch.set(deliverablesOf(projectId).document(deliverableId), FirestoreMappers.deliverableFields(updated) + ("updatedAt" to FieldValue.serverTimestamp()))
        resolving.forEach { batch.update(revisionsOf(projectId).document(it.id), "resolved", true) }
        if (project.status == ProjectStatus.REVISION_REQUESTED && !stillOpen) {
            batch.update(projects.document(projectId), mapOf("status" to ProjectStatus.IN_REVIEW.name, "updatedAt" to FieldValue.serverTimestamp()))
        }
        batch.commit().await()
        Unit
    }

    override suspend fun updateProjectStatus(projectId: String, status: ProjectStatus): Result<Unit> = runCatching {
        loadProject(projectId)
        projects.document(projectId).update(mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp())).await()
        Unit
    }

    override suspend fun setMilestoneCompleted(projectId: String, milestoneId: String, completed: Boolean): Result<Unit> = runCatching {
        val project = loadProject(projectId)
        require(project.milestones.any { it.id == milestoneId }) { "Milestone not found: $milestoneId" }
        val milestones = project.milestones.map { if (it.id == milestoneId) it.copy(completed = completed) else it }
        projects.document(projectId)
            .update(mapOf("milestones" to FirestoreMappers.milestones(milestones), "updatedAt" to FieldValue.serverTimestamp())).await()
        Unit
    }

    /** One-off read of a project with its children, used before writes that depend on current state. */
    private suspend fun loadProject(projectId: String): Project {
        val user = currentUser.value ?: throw NotSignedInException()
        val snap = projects.document(projectId).get().await()
        if (!snap.exists()) throw IllegalArgumentException("Project not found: $projectId")
        val deliverableQuery = if (user.role.isAgency) deliverablesOf(projectId)
        else deliverablesOf(projectId).whereIn("status", CLIENT_VISIBLE.map { it.name })
        val deliverables = deliverableQuery.get().await().documents
            .mapNotNull { FirestoreMappers.deliverable(it.id, projectId, it.data.orEmpty()) }
        val revisions = revisionsOf(projectId).get().await().documents
            .mapNotNull { FirestoreMappers.revision(it.id, projectId, it.data.orEmpty()) }
        return FirestoreMappers.project(projectId, snap.data.orEmpty(), deliverables, revisions)
            ?: throw IllegalStateException("Project $projectId has invalid data")
    }

    private suspend fun loadDeliverable(projectId: String, deliverableId: String): Deliverable {
        val snap = deliverablesOf(projectId).document(deliverableId).get().await()
        return snap.takeIf { it.exists() }?.let { FirestoreMappers.deliverable(it.id, projectId, it.data.orEmpty()) }
            ?: throw IllegalArgumentException("Deliverable not found: $deliverableId")
    }

    companion object {
        const val COLLECTION = "projects"
        const val DELIVERABLES = "deliverables"
        const val REVISIONS = "revisions"
        private val CLIENT_VISIBLE = DeliverableStatus.entries.filter { it != DeliverableStatus.DRAFT }
    }
}
