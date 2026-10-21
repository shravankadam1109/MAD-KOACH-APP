package com.koach.studio.projects.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.User
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.Revision
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RevisionEntry(val revision: Revision, val requesterName: String, val deliverableName: String?)

data class ProjectDetailUiState(
    val project: Project? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Agency: change status and tick off milestones. */
    val canManage: Boolean = false,
    /** Agency: edit deliverable details. */
    val canEditDeliverables: Boolean = false,
    /** Client: ask for changes. */
    val canRequestRevision: Boolean = false,
    /** Client: approve deliverables that are in review. */
    val canReviewDeliverables: Boolean = false,
    /** Revision history, newest first, with the requester's name resolved. */
    val revisionHistory: List<RevisionEntry> = emptyList(),
    val team: List<User> = emptyList(),
    val actionMessage: String? = null
)

class ProjectDetailViewModel(
    private val projectId: String,
    private val repository: ProjectRepository,
    /** Signed-in user; decides which actions are offered. Null shows a read-only view. */
    private val user: User? = null,
    /** Directory used to show the project's crew. */
    private val people: List<User> = emptyList()
) : ViewModel() {

    private val actionMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProjectDetailUiState> = combine(repository.getProjectById(projectId), actionMessage) { project, message ->
        if (project != null) {
            ProjectDetailUiState(
                project = project,
                isLoading = false,
                canManage = user?.can(Permission.MANAGE_PROJECTS) == true,
                canEditDeliverables = user?.can(Permission.EDIT_DELIVERABLES) == true,
                canRequestRevision = user?.can(Permission.REQUEST_REVISIONS) == true && project.status != ProjectStatus.COMPLETED,
                canReviewDeliverables = user?.can(Permission.REVIEW_DELIVERABLES) == true,
                team = project.teamIds.mapNotNull { id -> people.find { it.id == id } },
                revisionHistory = project.revisions.sortedByDescending { it.requestedOn }.map { revision ->
                    RevisionEntry(
                        revision = revision,
                        requesterName = people.find { it.id == revision.requestedBy }?.name ?: "Client",
                        deliverableName = project.deliverables.find { it.id == revision.deliverableId }?.name
                    )
                },
                actionMessage = message
            )
        } else {
            ProjectDetailUiState(isLoading = false, errorMessage = "Project not found")
        }
    }
        .catch { error ->
            emit(ProjectDetailUiState(isLoading = false, errorMessage = error.message ?: "Failed to load project"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProjectDetailUiState(isLoading = true)
        )

    fun updateStatus(status: ProjectStatus) {
        viewModelScope.launch {
            actionMessage.value = repository.updateProjectStatus(projectId, status)
                .fold({ "Status set to ${status.displayName}" }, { it.message ?: "Couldn't update status" })
        }
    }

    fun setMilestoneCompleted(milestoneId: String, completed: Boolean) {
        viewModelScope.launch {
            repository.setMilestoneCompleted(projectId, milestoneId, completed)
                .onFailure { actionMessage.value = it.message ?: "Couldn't update milestone" }
        }
    }

    fun approveDeliverable(deliverableId: String) {
        viewModelScope.launch {
            actionMessage.value = repository.approveDeliverable(projectId, deliverableId)
                .fold({ "Approved. Thanks for signing off!" }, { it.message ?: "Couldn't approve" })
        }
    }

    fun actionMessageShown() {
        actionMessage.value = null
    }

    fun downloadDeliverable(deliverableId: String, simulateFailure: Boolean = false) {
        viewModelScope.launch {
            repository.startDeliverableDownload(projectId, deliverableId, simulateFailure)
        }
    }
}
