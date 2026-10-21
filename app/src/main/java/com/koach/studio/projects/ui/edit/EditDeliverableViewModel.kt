package com.koach.studio.projects.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
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
    val nameError: String? = null,
    val notesError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val isFormValid: Boolean
        get() = nameError == null && notesError == null && name.isNotBlank()
}

class EditDeliverableViewModel(
    private val projectId: String,
    private val deliverableId: String,
    private val repository: ProjectRepository
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
                        status = deliverable.status,
                        nameError = null,
                        notesError = null
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
        val error = validateName(newName)
        _uiState.update { it.copy(name = newName, nameError = error) }
    }

    fun onNotesChanged(newNotes: String) {
        val error = validateNotes(newNotes)
        _uiState.update { it.copy(notes = newNotes, notesError = error) }
    }

    fun onStatusChanged(newStatus: DeliverableStatus) {
        _uiState.update { it.copy(status = newStatus) }
    }

    fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Deliverable name cannot be blank"
            trimmed.length < 3 -> "Deliverable name must be at least 3 characters"
            trimmed.length > 120 -> "Deliverable name cannot exceed 120 characters"
            else -> null
        }
    }

    fun validateNotes(notes: String): String? {
        return when {
            notes.length > 500 -> "Notes cannot exceed 500 characters"
            else -> null
        }
    }

    fun save(onSuccess: () -> Unit) {
        val current = _uiState.value
        val nameErr = validateName(current.name)
        val notesErr = validateNotes(current.notes)

        if (nameErr != null || notesErr != null) {
            _uiState.update { it.copy(nameError = nameErr, notesError = notesErr) }
            return
        }

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
}
