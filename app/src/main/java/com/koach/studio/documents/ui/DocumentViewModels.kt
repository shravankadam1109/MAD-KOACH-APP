package com.koach.studio.documents.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.DocumentCategory
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.core.model.StudioDocument
import com.koach.studio.core.model.User
import com.koach.studio.documents.data.DocumentDraft
import com.koach.studio.documents.data.DocumentRepository
import com.koach.studio.documents.data.DocumentStorage
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentRow(val document: StudioDocument, val projectTitle: String?, val uploaderName: String, val canDelete: Boolean)

data class DocumentsUiState(
    val isLoading: Boolean = true,
    val rows: List<DocumentRow> = emptyList(),
    val query: String = "",
    val category: DocumentCategory? = null,
    val projectFilterTitle: String? = null,
    val isAgency: Boolean = false,
    val canUpload: Boolean = false,
    val storageAvailable: Boolean = false,
    val message: String? = null
)

private data class DocFilters(val query: String, val category: DocumentCategory?)

class DocumentsViewModel(
    private val user: User,
    private val documents: DocumentRepository,
    projects: ProjectRepository,
    private val people: List<User>,
    private val storage: DocumentStorage,
    /** Optional: only show one project's documents. */
    private val projectId: String? = null
) : ViewModel() {

    private val filters = MutableStateFlow(DocFilters("", null))
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DocumentsUiState> =
        combine(documents.observeDocuments(), projects.getProjects(), filters, message) { docs, projectList, f, msg ->
            val q = f.query.trim()
            DocumentsUiState(
                isLoading = false,
                rows = docs
                    .filter { projectId == null || it.projectId == projectId }
                    .filter { f.category == null || it.category == f.category }
                    .filter { doc ->
                        q.isEmpty() || doc.name.contains(q, ignoreCase = true) ||
                            projectList.find { it.id == doc.projectId }?.title?.contains(q, ignoreCase = true) == true
                    }
                    .map { it.toRow(projectList) },
                query = f.query,
                category = f.category,
                projectFilterTitle = projectId?.let { id -> projectList.find { it.id == id }?.title },
                isAgency = user.role.isAgency,
                canUpload = user.can(Permission.UPLOAD_DOCUMENTS),
                storageAvailable = storage.isAvailable,
                message = msg
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentsUiState())

    fun onQuery(value: String) = filters.update { it.copy(query = value) }
    fun onCategory(value: DocumentCategory?) = filters.update { it.copy(category = value) }

    fun setVisibility(documentId: String, visibility: DocumentVisibility) {
        viewModelScope.launch {
            message.value = documents.setVisibility(documentId, visibility)
                .fold({ "Now ${visibility.displayName.lowercase()}" }, { it.message ?: "Couldn't change visibility" })
        }
    }

    fun delete(documentId: String) {
        viewModelScope.launch {
            message.value = documents.deleteDocument(documentId).fold({ "Document removed" }, { it.message ?: "Couldn't remove document" })
        }
    }

    fun messageShown() = message.update { null }

    private fun StudioDocument.toRow(projectList: List<Project>) = DocumentRow(
        document = this,
        projectTitle = projectList.find { it.id == projectId }?.title,
        uploaderName = people.find { it.id == uploadedBy }?.name ?: "Unknown",
        canDelete = user.can(Permission.VIEW_INTERNAL_DOCUMENTS) || uploadedBy == user.id
    )
}

/** File details read from the system picker. */
data class PickedFile(val name: String, val mimeType: String, val sizeBytes: Long)

data class AddDocumentUiState(
    val projects: List<Project> = emptyList(),
    val picked: PickedFile? = null,
    val name: String = "",
    val category: DocumentCategory = DocumentCategory.BRIEF,
    val projectId: String? = null,
    val visibility: DocumentVisibility = DocumentVisibility.SHARED,
    val isAgency: Boolean = false,
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null
)

class AddDocumentViewModel(
    user: User,
    private val documents: DocumentRepository,
    private val projects: ProjectRepository,
    prefillProjectId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDocumentUiState(isAgency = user.role.isAgency, projectId = prefillProjectId))
    val uiState: StateFlow<AddDocumentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val list = projects.getProjects().first()
            _uiState.update { it.copy(projects = list, projectId = it.projectId ?: list.firstOrNull()?.id) }
        }
    }

    fun onFilePicked(file: PickedFile) = _uiState.update {
        it.copy(picked = file, name = it.name.ifBlank { file.name }, nameError = null, category = guessCategory(file))
    }

    fun onName(value: String) = _uiState.update { it.copy(name = value, nameError = null) }
    fun onCategory(value: DocumentCategory) = _uiState.update { it.copy(category = value) }
    fun onProject(value: String?) = _uiState.update { it.copy(projectId = value) }
    fun onVisibility(value: DocumentVisibility) = _uiState.update { it.copy(visibility = value) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Give the document a name") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val project = state.projects.find { it.id == state.projectId }
            val result = documents.addDocument(
                DocumentDraft(
                    name = state.name,
                    category = state.category,
                    projectId = state.projectId,
                    clientId = project?.clientId?.ifBlank { null },
                    mimeType = state.picked?.mimeType.orEmpty(),
                    sizeBytes = state.picked?.sizeBytes ?: 0,
                    visibility = state.visibility
                )
            )
            _uiState.update { it.copy(isSaving = false, saved = result.isSuccess, errorMessage = result.exceptionOrNull()?.message) }
        }
    }

    private fun guessCategory(file: PickedFile): DocumentCategory = when {
        file.name.contains("invoice", true) -> DocumentCategory.INVOICE
        file.name.contains("contract", true) || file.name.contains("agreement", true) -> DocumentCategory.CONTRACT
        file.name.contains("script", true) -> DocumentCategory.SCRIPT
        file.name.contains("storyboard", true) -> DocumentCategory.STORYBOARD
        file.mimeType.startsWith("image/") || file.mimeType.startsWith("video/") || file.mimeType.startsWith("audio/") -> DocumentCategory.ASSET
        else -> _uiState.value.category
    }
}
