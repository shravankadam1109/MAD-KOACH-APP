package com.koach.studio.projects.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachButton
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachSearchField
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.projects.model.ProjectSortOrder
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.components.ProjectListItem
import com.koach.studio.projects.ui.components.ProjectListShimmer
import com.koach.studio.projects.ui.components.staggeredEntrance

@Composable
fun ProjectListScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectListViewModel = koachViewModel { ProjectListViewModel(it.projectRepository) },
    onProjectClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp
        if (isWide) {
            WideProjectListLayout(uiState, viewModel, onProjectClick)
        } else {
            NarrowProjectListLayout(uiState, viewModel, onProjectClick)
        }
    }
}

/**
 * Narrow (phone portrait) layout — single-column project list.
 */
@Composable
private fun NarrowProjectListLayout(
    uiState: ProjectListUiState,
    viewModel: ProjectListViewModel,
    onProjectClick: (String) -> Unit
) {
    val gutter = KoachTheme.spacing.gutter
    val activeCount = uiState.projects.count { it.status != ProjectStatus.COMPLETED }

    KoachBackground(Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            ProjectListShimmer()
            return@KoachBackground
        }
        if (uiState.errorMessage != null) {
            ErrorState(
                message = uiState.errorMessage ?: "Failed to load projects",
                onRetry = viewModel::retry,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .testTag("project_list_error")
            )
            return@KoachBackground
        }
        ProjectLazyList(
            uiState = uiState,
            viewModel = viewModel,
            onProjectClick = onProjectClick,
            activeCount = activeCount,
            gutter = gutter
        )
    }
}

/**
 * Wide (tablet / landscape) layout — list on the left, "select a project" hint panel on the right.
 */
@Composable
private fun WideProjectListLayout(
    uiState: ProjectListUiState,
    viewModel: ProjectListViewModel,
    onProjectClick: (String) -> Unit
) {
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val activeCount = uiState.projects.count { it.status != ProjectStatus.COMPLETED }

    KoachBackground(Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Left pane: project list
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
            ) {
                if (uiState.isLoading) {
                    ProjectListShimmer()
                } else if (uiState.errorMessage != null) {
                    ErrorState(
                        message = uiState.errorMessage ?: "Failed to load projects",
                        onRetry = viewModel::retry,
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .testTag("project_list_error")
                    )
                } else {
                    ProjectLazyList(
                        uiState = uiState,
                        viewModel = viewModel,
                        onProjectClick = onProjectClick,
                        activeCount = activeCount,
                        gutter = gutter
                    )
                }
            }

            // Thin divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 16.dp)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(c.borderSubtle)
                )
            }

            // Right pane: placeholder to open project details
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .padding(gutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Select a project",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textSecondary
                )
                Text(
                    text = "Tap a project from the list to view its details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary
                )
            }
        }
    }
}

/**
 * The shared LazyColumn content used in both narrow and wide layouts.
 * Filter controls are accessed via a compact filter icon button next to the search bar
 * that opens a bottom sheet only when tapped.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectLazyList(
    uiState: ProjectListUiState,
    viewModel: ProjectListViewModel,
    onProjectClick: (String) -> Unit,
    activeCount: Int,
    gutter: androidx.compose.ui.unit.Dp
) {
    val c = KoachTheme.colors
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Projects",
                    style = MaterialTheme.typography.headlineMedium,
                    color = c.textPrimary
                )
                Spacer(Modifier.height(12.dp))

                // Compact search bar + filter trigger icon button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KoachSearchField(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        placeholder = "Search projects or clients",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_search")
                    )

                    val hasActiveFilters = uiState.activeFilterCount > 0
                    val filterButtonShape = RoundedCornerShape(KoachRadius.md)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(filterButtonShape)
                            .background(if (hasActiveFilters) c.brandSoft else c.surface)
                            .border(
                                width = 1.dp,
                                color = if (hasActiveFilters) c.brand.copy(alpha = 0.45f) else c.borderSubtle,
                                shape = filterButtonShape
                            )
                            .clickable(
                                role = Role.Button,
                                onClick = { showFilterSheet = true }
                            )
                            .semantics {
                                contentDescription = if (hasActiveFilters) {
                                    "Filter and sort projects, ${uiState.activeFilterCount} active"
                                } else {
                                    "Filter and sort projects"
                                }
                            }
                            .testTag("project_filter_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = if (hasActiveFilters) c.brand else c.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        if (hasActiveFilters) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-6).dp, y = 6.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(c.brand),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.activeFilterCount.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = c.onBrand,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Compact active-filter summary strip (shown only when a filter or non-default sort is active)
                if (uiState.activeFilterCount > 0) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.selectedServiceFilter?.let { service ->
                            ActiveFilterTag(
                                label = service.displayName,
                                onRemove = { viewModel.onFilterSelected(null) }
                            )
                        }
                        uiState.selectedStatusFilter?.let { status ->
                            ActiveFilterTag(
                                label = status.displayName,
                                onRemove = { viewModel.onStatusFilterSelected(null) }
                            )
                        }
                        if (uiState.sortOrder != ProjectSortOrder.NEWEST) {
                            ActiveFilterTag(
                                label = "Sort: ${uiState.sortOrder.displayName}",
                                onRemove = { viewModel.onSortOrderSelected(ProjectSortOrder.NEWEST) }
                            )
                        }
                        Text(
                            text = "Reset all",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.brand,
                            modifier = Modifier
                                .clip(RoundedCornerShape(KoachRadius.pill))
                                .clickable { viewModel.clearFilters() }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("clear_project_filters")
                        )
                    }
                }
            }
        }

        if (uiState.projects.isEmpty()) {
            item {
                val searching = uiState.searchQuery.isNotBlank() ||
                    uiState.selectedServiceFilter != null ||
                    uiState.selectedStatusFilter != null
                EmptyState(
                    icon = if (searching) Icons.Outlined.SearchOff else Icons.Outlined.Movie,
                    title = if (searching) "No matching projects" else "No projects yet",
                    message = if (searching) "Try a different search or filter." else "Projects appear here once work is booked.",
                    actionLabel = if (searching) "Clear filters" else null,
                    onAction = if (searching) { { viewModel.clearFilters() } } else null,
                    modifier = Modifier.testTag("project_list_empty")
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

    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = c.surfaceRaised,
            contentColor = c.textPrimary,
            modifier = Modifier.testTag("project_filter_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gutter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Eyebrow("Refine")
                        Text(
                            text = "Filter & Sort Projects",
                            style = MaterialTheme.typography.titleLarge,
                            color = c.textPrimary
                        )
                    }
                    if (uiState.activeFilterCount > 0) {
                        TextButton(onClick = { viewModel.clearFilters() }) {
                            Text("Reset", color = c.brand)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Service type",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                    KoachFilterChips(
                        options = ServiceType.entries,
                        selected = uiState.selectedServiceFilter,
                        onSelected = viewModel::onFilterSelected,
                        label = { it.displayName },
                        allLabel = "All services",
                        modifier = Modifier.testTag("service_filter_chips")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Project status",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                    KoachFilterChips(
                        options = ProjectStatus.entries,
                        selected = uiState.selectedStatusFilter,
                        onSelected = viewModel::onStatusFilterSelected,
                        label = { it.displayName },
                        allLabel = "All statuses",
                        modifier = Modifier.testTag("status_filter_chips")
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Sort order",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                    KoachFilterChips(
                        options = ProjectSortOrder.entries,
                        selected = uiState.sortOrder,
                        onSelected = { it?.let(viewModel::onSortOrderSelected) },
                        label = { it.displayName },
                        allLabel = null,
                        modifier = Modifier.testTag("sort_filter_chips")
                    )
                }

                KoachButton(
                    text = "Show ${uiState.projects.size} project${if (uiState.projects.size == 1) "" else "s"}",
                    onClick = { showFilterSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = gutter)
                )
            }
        }
    }
}

@Composable
private fun ActiveFilterTag(
    label: String,
    onRemove: () -> Unit
) {
    val c = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.pill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(c.brandSoft)
            .border(1.dp, c.brand.copy(alpha = 0.35f), shape)
            .clickable(onClick = onRemove)
            .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = c.textPrimary
        )
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = "Remove $label filter",
            tint = c.textSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}
