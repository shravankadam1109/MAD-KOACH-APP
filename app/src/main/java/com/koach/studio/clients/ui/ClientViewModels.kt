package com.koach.studio.clients.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.clients.data.ClientDraft
import com.koach.studio.clients.data.ClientRepository
import com.koach.studio.clients.data.ClientValidator
import com.koach.studio.clients.data.ClientValidator.Field
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.Client
import com.koach.studio.core.model.ClientStatus
import com.koach.studio.core.model.User
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClientRow(val client: Client, val projectCount: Int, val activeProjectCount: Int)

data class ClientListUiState(
    val isLoading: Boolean = true,
    val rows: List<ClientRow> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val statusFilter: ClientStatus? = null,
    val canCreate: Boolean = false,
    val activeCount: Int = 0,
    val leadCount: Int = 0
)

class ClientListViewModel(
    user: User,
    clients: ClientRepository,
    projects: ProjectRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val status = MutableStateFlow<ClientStatus?>(null)
    private val canCreate = user.can(Permission.MANAGE_CLIENTS)

    val uiState: StateFlow<ClientListUiState> =
        combine(clients.observeClients(), projects.getProjects(), query, status) { all, projectList, q, s ->
            val rows = all
                .filter { s == null || it.status == s }
                .filter { client ->
                    q.isBlank() || listOf(client.name, client.contactName, client.industry, client.city)
                        .any { it.contains(q.trim(), ignoreCase = true) }
                }
                .map { client ->
                    val linked = projectList.filter { it.clientId == client.id }
                    ClientRow(client, linked.size, linked.count { it.status != ProjectStatus.COMPLETED })
                }
            ClientListUiState(
                isLoading = false,
                rows = rows,
                totalCount = all.size,
                query = q,
                statusFilter = s,
                canCreate = canCreate,
                activeCount = all.count { it.status == ClientStatus.ACTIVE },
                leadCount = all.count { it.status == ClientStatus.LEAD }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientListUiState())

    fun onQueryChange(value: String) = query.update { value }
    fun onStatusFilter(value: ClientStatus?) = status.update { value }
}

data class ClientDetailUiState(
    val isLoading: Boolean = true,
    val client: Client? = null,
    val projects: List<Project> = emptyList(),
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val isDeleting: Boolean = false,
    val deleted: Boolean = false,
    val errorMessage: String? = null
)

class ClientDetailViewModel(
    private val clientId: String,
    user: User,
    private val clients: ClientRepository,
    projects: ProjectRepository
) : ViewModel() {

    private val action = MutableStateFlow(ClientDetailUiState(canEdit = user.can(Permission.MANAGE_CLIENTS), canDelete = user.can(Permission.DELETE_CLIENTS)))

    val uiState: StateFlow<ClientDetailUiState> =
        combine(clients.observeClient(clientId), projects.getProjects(), action) { client, projectList, a ->
            a.copy(
                isLoading = false,
                client = client,
                projects = projectList.filter { it.clientId == clientId }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientDetailUiState())

    fun delete() {
        if (action.value.isDeleting) return
        viewModelScope.launch {
            action.update { it.copy(isDeleting = true, errorMessage = null) }
            val result = clients.deleteClient(clientId)
            action.update {
                it.copy(isDeleting = false, deleted = result.isSuccess, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun errorShown() = action.update { it.copy(errorMessage = null) }
}

data class ClientFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val contactName: String = "",
    val email: String = "",
    val phone: String = "",
    val industry: String = "",
    val city: String = "",
    val status: ClientStatus = ClientStatus.LEAD,
    val notes: String = "",
    val errors: Map<Field, String> = emptyMap(),
    val isSaving: Boolean = false,
    val savedClientId: String? = null,
    val errorMessage: String? = null
) {
    fun toDraft() = ClientDraft(name, contactName, email, phone, industry, city, status, notes)
}

/** Create (clientId == null) or edit an existing client. */
class ClientFormViewModel(
    private val clientId: String?,
    private val clients: ClientRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClientFormUiState(isEditing = clientId != null, isLoading = clientId != null))
    val uiState: StateFlow<ClientFormUiState> = _uiState.asStateFlow()

    init {
        if (clientId != null) {
            viewModelScope.launch {
                val client = clients.observeClient(clientId).first()
                _uiState.update { state ->
                    if (client == null) state.copy(isLoading = false, errorMessage = "Client not found")
                    else state.copy(
                        isLoading = false,
                        name = client.name, contactName = client.contactName, email = client.email,
                        phone = client.phone, industry = client.industry, city = client.city,
                        status = client.status, notes = client.notes
                    )
                }
            }
        }
    }

    fun onName(v: String) = edit(Field.NAME) { it.copy(name = v) }
    fun onContact(v: String) = edit(Field.CONTACT) { it.copy(contactName = v) }
    fun onEmail(v: String) = edit(Field.EMAIL) { it.copy(email = v) }
    fun onPhone(v: String) = edit(Field.PHONE) { it.copy(phone = v) }
    fun onIndustry(v: String) = edit(Field.INDUSTRY) { it.copy(industry = v) }
    fun onCity(v: String) = edit(Field.CITY) { it.copy(city = v) }
    fun onStatus(v: ClientStatus) = _uiState.update { it.copy(status = v) }
    fun onNotes(v: String) = edit(Field.NOTES) { it.copy(notes = v) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val errors = ClientValidator.validate(state.toDraft())
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (clientId == null) clients.createClient(state.toDraft()) else clients.updateClient(clientId, state.toDraft())
            _uiState.update {
                it.copy(isSaving = false, savedClientId = result.getOrNull()?.id, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    /** Applies an edit and clears that field's error until the next save attempt. */
    private fun edit(field: Field, change: (ClientFormUiState) -> ClientFormUiState) =
        _uiState.update { change(it).copy(errors = it.errors - field) }
}
