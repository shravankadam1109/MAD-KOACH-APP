package com.koach.studio.projects.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.data.ProjectRepositoryProvider
import com.koach.studio.projects.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RevisionRequestUiState(
    val isLoading: Boolean = true,
    val project: Project? = null,
    val reason: String = "",
    val reasonError: String? = null,
    val isSubmitting: Boolean = false,
    val isSubmitted: Boolean = false,
    val errorMessage: String? = null
)

class RevisionRequestViewModel(
    private val projectId: String,
    private val repository: ProjectRepository = ProjectRepositoryProvider.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisionRequestUiState())
    val uiState: StateFlow<RevisionRequestUiState> = _uiState.asStateFlow()

    init {
        loadProject()
    }

    private fun loadProject() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId).firstOrNull()
            if (project != null) {
                _uiState.update { it.copy(isLoading = false, project = project) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Project not found") }
            }
        }
    }

    fun onReasonChanged(newReason: String) {
        val error = validateReason(newReason)
        _uiState.update { it.copy(reason = newReason, reasonError = error) }
    }

    fun validateReason(reason: String): String? {
        val trimmed = reason.trim()
        return when {
            trimmed.isEmpty() -> "Please provide details for the revision request"
            trimmed.length < 5 -> "Revision reason must be at least 5 characters"
            else -> null
        }
    }

    fun applyQuickTag(tag: String) {
        val current = _uiState.value.reason
        val newReason = if (current.isBlank()) tag else "$current, $tag"
        onReasonChanged(newReason)
    }

    fun submit(onSuccess: () -> Unit) {
        val currentReason = _uiState.value.reason
        val error = validateReason(currentReason)
        if (error != null) {
            _uiState.update { it.copy(reasonError = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            val result = repository.requestRevision(projectId, currentReason.trim())
            if (result.isSuccess) {
                _uiState.update { it.copy(isSubmitting = false, isSubmitted = true) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit revision request"
                    )
                }
            }
        }
    }

    class Factory(
        private val projectId: String,
        private val repository: ProjectRepository = ProjectRepositoryProvider.repository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RevisionRequestViewModel(projectId, repository) as T
        }
    }
}
