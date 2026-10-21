package com.koach.studio.projects.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProjectDetailUiState(
    val project: Project? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProjectDetailViewModel(
    private val projectId: String,
    private val repository: ProjectRepository
) : ViewModel() {

    val uiState: StateFlow<ProjectDetailUiState> = repository.getProjectById(projectId)
        .map { project ->
            if (project != null) {
                ProjectDetailUiState(project = project, isLoading = false)
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

    fun downloadDeliverable(deliverableId: String, simulateFailure: Boolean = false) {
        viewModelScope.launch {
            repository.startDeliverableDownload(projectId, deliverableId, simulateFailure)
        }
    }
}
