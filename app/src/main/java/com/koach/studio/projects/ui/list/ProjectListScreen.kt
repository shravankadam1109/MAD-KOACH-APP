package com.koach.studio.projects.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachSearchField
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.components.ProjectListItem
import com.koach.studio.projects.ui.components.staggeredEntrance

@Composable
fun ProjectListScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectListViewModel = koachViewModel { ProjectListViewModel(it.projectRepository) },
    onProjectClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val activeCount = uiState.projects.count { it.status != ProjectStatus.COMPLETED }

    KoachBackground(modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            LoadingState()
            return@KoachBackground
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("project_list")
        ) {
            item {
                Column(Modifier.padding(start = gutter, end = gutter, top = 16.dp)) {
                    Eyebrow("$activeCount active")
                    Spacer(Modifier.height(6.dp))
                    Text("Projects", style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                    Spacer(Modifier.height(16.dp))
                    KoachSearchField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        placeholder = "Search projects or clients",
                        modifier = Modifier.testTag("project_search")
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            item {
                KoachFilterChips(
                    options = ServiceType.entries,
                    selected = uiState.selectedServiceFilter,
                    onSelected = viewModel::onFilterSelected,
                    label = { it.displayName },
                    allLabel = "All services"
                )
            }
            if (uiState.projects.isEmpty()) {
                item {
                    val searching = uiState.searchQuery.isNotBlank() || uiState.selectedServiceFilter != null
                    EmptyState(
                        icon = if (searching) Icons.Outlined.SearchOff else Icons.Outlined.Movie,
                        title = if (searching) "No matching projects" else "No projects yet",
                        message = if (searching) "Try a different search or service." else "Projects appear here once work is booked."
                    )
                }
            }
            itemsIndexed(uiState.projects, key = { _, project -> project.id }) { index, project ->
                ProjectListItem(
                    project = project,
                    onClick = { onProjectClick(project.id) },
                    modifier = Modifier
                        .padding(horizontal = gutter)
                        .staggeredEntrance(index = index)
                )
            }
        }
    }
}
