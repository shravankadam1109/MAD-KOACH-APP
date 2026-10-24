package com.koach.studio.projects.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectSortOrder
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.model.dueLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedServiceFilter: ServiceType? = null,
    val selectedStatusFilter: ProjectStatus? = null,
    val sortOrder: ProjectSortOrder = ProjectSortOrder.NEWEST,
    val searchQuery: String = ""
)

private data class FilterParams(
    val service: ServiceType?,
    val status: ProjectStatus?,
    val sort: ProjectSortOrder,
    val query: String
)

class ProjectListViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<ServiceType?>(null)
    private val _selectedStatusFilter = MutableStateFlow<ProjectStatus?>(null)
    private val _sortOrder = MutableStateFlow(ProjectSortOrder.NEWEST)
    private val _searchQuery = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _filterParams = combine(
        _selectedFilter,
        _selectedStatusFilter,
        _sortOrder,
        _searchQuery
    ) { service, status, sort, query ->
        FilterParams(service, status, sort, query)
    }

    val uiState: StateFlow<ProjectListUiState> = combine(
        repository.getProjects(),
        _filterParams,
        _errorMessage
    ) { allProjects, params, error ->
        val filtered = allProjects.filter { project ->
            val matchesService = params.service == null || project.serviceType == params.service
            val matchesStatus = params.status == null || project.status == params.status
            val matchesQuery = params.query.isBlank() ||
                project.title.contains(params.query, ignoreCase = true) ||
                project.clientName.contains(params.query, ignoreCase = true)
            matchesService && matchesStatus && matchesQuery
        }
        val sorted = when (params.sort) {
            ProjectSortOrder.NEWEST -> filtered.sortedByDescending { it.id }
            ProjectSortOrder.DUE_DATE -> filtered.sortedWith(compareBy(nullsLast()) { it.dueLocalDate })
            ProjectSortOrder.STATUS -> filtered.sortedBy { it.status.ordinal }
        }
        ProjectListUiState(
            projects = sorted,
            isLoading = false,
            errorMessage = error,
            selectedServiceFilter = params.service,
            selectedStatusFilter = params.status,
            sortOrder = params.sort,
            searchQuery = params.query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProjectListUiState(isLoading = true)
    )

    fun onFilterSelected(serviceType: ServiceType?) {
        _selectedFilter.value = serviceType
    }

    fun onStatusFilterSelected(status: ProjectStatus?) {
        _selectedStatusFilter.value = status
    }

    fun onSortOrderSelected(sortOrder: ProjectSortOrder) {
        _sortOrder.value = sortOrder
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun retry() {
        _errorMessage.value = null
    }

    fun clearFilters() {
        _selectedFilter.value = null
        _selectedStatusFilter.value = null
        _searchQuery.value = ""
    }
}
