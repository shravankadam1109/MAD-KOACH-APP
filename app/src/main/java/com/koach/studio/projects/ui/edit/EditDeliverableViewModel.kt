package com.koach.studio.projects.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.data.ProjectRepositoryProvider
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditDeliverableUiState(
    val isLoading: Boolean = true,
    val initialDeliverable: Deliverable? = null,
    val name: String = "",
    val notes: String = "",
    val status: DeliverableStatus = DeliverableStatus.DRAFT,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class EditDeliverableViewModel(
    private val projectId: String,
    private val deliverableId: String,
    private val repository: ProjectRepository = ProjectRepositoryProvider.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditDeliverableUiState())
    val uiState: StateFlow<EditDeliverableUiState> = _uiState.asStateFlow()

    init {
        loadDeliverable()
    }

    private fun loadDeliverable() {
        viewModelScope.launch {
            val project = repository.getProjectById(projectId).firstOrNull()
            val deliverable = project?.deliverables?.find { it.id == deliverableId }
            if (deliverable != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        initialDeliverable = deliverable,
                        name = deliverable.name,
                        notes = deliverable.notes,
                        status = deliverable.status
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Deliverable not found"
                    )
                }
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(name = newName) }
    }

    fun onNotesChanged(newNotes: String) {
        _uiState.update { it.copy(notes = newNotes) }
    }

    fun onStatusChanged(newStatus: DeliverableStatus) {
        _uiState.update { it.copy(status = newStatus) }
    }

    fun save(onSuccess: () -> Unit) {
        val current = _uiState.value
        val original = current.initialDeliverable ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updated = original.copy(
                name = current.name.trim(),
                notes = current.notes.trim(),
                status = current.status
            )
            val result = repository.updateDeliverable(projectId, updated)
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to save deliverable"
                    )
                }
            }
        }
    }

    class Factory(
        private val projectId: String,
        private val deliverableId: String,
        private val repository: ProjectRepository = ProjectRepositoryProvider.repository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditDeliverableViewModel(projectId, deliverableId, repository) as T
        }
    }
}
