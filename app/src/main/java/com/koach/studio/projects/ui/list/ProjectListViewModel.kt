package com.koach.studio.projects.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.data.ProjectRepositoryProvider
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ServiceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ProjectListUiState(
    val projects: List<Project> = emptyList(),
    val isLoading: Boolean = false,
    val selectedServiceFilter: ServiceType? = null,
    val searchQuery: String = ""
)

class ProjectListViewModel(
    private val repository: ProjectRepository = ProjectRepositoryProvider.repository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow<ServiceType?>(null)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ProjectListUiState> = combine(
        repository.getProjects(),
        _selectedFilter,
        _searchQuery
    ) { allProjects, filter, query ->
        val filtered = allProjects.filter { project ->
            val matchesFilter = filter == null || project.serviceType == filter
            val matchesQuery = query.isBlank() ||
                project.title.contains(query, ignoreCase = true) ||
                project.clientName.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
        ProjectListUiState(
            projects = filtered,
            isLoading = false,
            selectedServiceFilter = filter,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProjectListUiState(isLoading = true)
    )

    fun onFilterSelected(serviceType: ServiceType?) {
        _selectedFilter.value = serviceType
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }
}
