package com.koach.studio.projects.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ServiceType
import com.koach.studio.projects.ui.components.ProjectListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    modifier: Modifier = Modifier,
    viewModel: ProjectListViewModel = koachViewModel { ProjectListViewModel(it.projectRepository) },
    onProjectClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val darkBackground = Color(0xFF0D0E11)
    val cardBackground = Color(0xFF16181F)
    val brandGreen = Color(0xFF41BA00)
    val mutedText = Color(0xFF9EA3B0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Client Projects",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Deliverables & Active Engagements",
                            color = mutedText,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("project_search_field"),
                placeholder = { Text("Search projects or clients...", color = mutedText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = mutedText
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = mutedText
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = brandGreen,
                    unfocusedBorderColor = Color(0xFF272A36),
                    focusedContainerColor = cardBackground,
                    unfocusedContainerColor = cardBackground
                )
            )

            // Service Type Filters
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.selectedServiceFilter == null,
                        onClick = { viewModel.onFilterSelected(null) },
                        label = { Text("All Services") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = brandGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = cardBackground,
                            labelColor = Color.White
                        )
                    )
                }
                items(ServiceType.entries) { service ->
                    FilterChip(
                        selected = uiState.selectedServiceFilter == service,
                        onClick = { viewModel.onFilterSelected(service) },
                        label = { Text(service.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = brandGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = cardBackground,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content List
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("loading_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = brandGreen)
                    }
                }
                uiState.projects.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .testTag("empty_projects_view"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No projects found matching the criteria.",
                            color = mutedText,
                            fontSize = 15.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("project_list"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.projects, key = { it.id }) { project ->
                            ProjectListItem(
                                project = project,
                                onClick = { onProjectClick(project.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
