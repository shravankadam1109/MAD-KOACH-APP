package com.koach.studio.projects.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.koach.studio.core.designsystem.theme.KoachTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.projects.model.DeliverableStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditDeliverableScreen(
    projectId: String,
    deliverableId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditDeliverableViewModel = koachViewModel(key = "deliverable-$projectId-$deliverableId") {
        EditDeliverableViewModel(projectId, deliverableId, it.projectRepository)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val darkBackground = KoachTheme.colors.background
    val cardBackground = KoachTheme.colors.surface
    val brandGreen = KoachTheme.colors.brand
    val mutedText = KoachTheme.colors.textSecondary
    val errorColor = KoachTheme.colors.danger

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = darkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Deliverable",
                        color = KoachTheme.colors.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("navigate_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = KoachTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = brandGreen)
                }
            }
            uiState.errorMessage != null && uiState.initialDeliverable == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Deliverable not found",
                        color = errorColor,
                        fontSize = 16.sp
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Deliverable Name Input with Validation
                    Text(
                        text = "File Name",
                        color = KoachTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deliverable_name_field"),
                        placeholder = { Text("e.g. Rough_Cut_v2.mp4", color = mutedText) },
                        isError = uiState.nameError != null,
                        supportingText = {
                            if (uiState.nameError != null) {
                                Text(
                                    text = uiState.nameError!!,
                                    color = errorColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.testTag("deliverable_name_error")
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = KoachTheme.colors.textPrimary,
                            unfocusedTextColor = KoachTheme.colors.textPrimary,
                            focusedBorderColor = brandGreen,
                            unfocusedBorderColor = KoachTheme.colors.outline,
                            errorBorderColor = errorColor,
                            focusedContainerColor = cardBackground,
                            unfocusedContainerColor = cardBackground,
                            errorContainerColor = cardBackground
                        )
                    )

                    // Deliverable Status Selection
                    Text(
                        text = "Delivery Status",
                        color = KoachTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.availableStatuses.forEach { status ->
                            FilterChip(
                                selected = uiState.status == status,
                                onClick = { viewModel.onStatusChanged(status) },
                                label = { Text(status.displayName) },
                                modifier = Modifier.testTag("status_chip_${status.name}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = brandGreen,
                                    selectedLabelColor = KoachTheme.colors.onBrand,
                                    containerColor = cardBackground,
                                    labelColor = KoachTheme.colors.textPrimary
                                )
                            )
                        }
                    }

                    // Notes Input with Validation
                    Text(
                        text = "Review Notes & Specifications",
                        color = KoachTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::onNotesChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("deliverable_notes_field"),
                        placeholder = { Text("Add mix notes, color feedback, or revision guidance...", color = mutedText) },
                        isError = uiState.notesError != null,
                        supportingText = {
                            if (uiState.notesError != null) {
                                Text(
                                    text = uiState.notesError!!,
                                    color = errorColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.testTag("deliverable_notes_error")
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = KoachTheme.colors.textPrimary,
                            unfocusedTextColor = KoachTheme.colors.textPrimary,
                            focusedBorderColor = brandGreen,
                            unfocusedBorderColor = KoachTheme.colors.outline,
                            errorBorderColor = errorColor,
                            focusedContainerColor = cardBackground,
                            unfocusedContainerColor = cardBackground,
                            errorContainerColor = cardBackground
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Save Button (disabled if invalid)
                    Button(
                        onClick = { viewModel.save(onSuccess = onNavigateBack) },
                        enabled = !uiState.isSaving && uiState.name.isNotBlank() && uiState.nameError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_deliverable_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandGreen,
                            contentColor = KoachTheme.colors.onBrand,
                            disabledContainerColor = KoachTheme.colors.surfaceHigh,
                            disabledContentColor = KoachTheme.colors.textTertiary
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KoachTheme.colors.onBrand,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Changes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    uiState.initialDeliverable?.let { current ->
                        com.koach.studio.core.designsystem.components.KoachSecondaryButton(
                            text = "Share as ${current.nextVersion()} for client review",
                            onClick = { viewModel.publishNewVersion(onSuccess = onNavigateBack) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_version_button")
                        )
                        Text(
                            text = "Approval and change requests come from the client, so those statuses can't be set here.",
                            color = mutedText,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
