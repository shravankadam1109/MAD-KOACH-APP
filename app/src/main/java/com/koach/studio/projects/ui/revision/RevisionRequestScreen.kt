package com.koach.studio.projects.ui.revision

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import com.koach.studio.core.designsystem.theme.KoachTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.projects.ui.components.ProjectStatusBadge

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RevisionRequestScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    deliverableId: String? = null,
    viewModel: RevisionRequestViewModel = koachViewModel(key = "revision-$projectId-$deliverableId") {
        RevisionRequestViewModel(projectId, it.projectRepository, deliverableId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()

    val darkBackground = KoachTheme.colors.background
    val cardBackground = KoachTheme.colors.surface
    val cardBorder = KoachTheme.colors.outlineSubtle
    val brandGreen = KoachTheme.colors.brand
    val mutedText = KoachTheme.colors.textSecondary
    val errorColor = KoachTheme.colors.danger

    val quickTags = listOf(
        "Color Grading Correction",
        "Audio Level Balancing",
        "Pacing & Trim Adjustment",
        "Typo in Lower Thirds",
        "Add Missing B-Roll Sequence"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = darkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Revision",
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
            uiState.project == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.errorMessage ?: "Project not found",
                        color = errorColor,
                        fontSize = 16.sp
                    )
                }
            }
            else -> {
                val project = uiState.project!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Project Info Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBackground),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorder))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = project.title,
                                    color = KoachTheme.colors.textPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                ProjectStatusBadge(status = project.status)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Client: ${project.clientName}",
                                color = mutedText,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Prompt Header
                    Text(
                        text = "Describe Requested Changes",
                        color = KoachTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Quick select tags
                    Text(
                        text = "Quick Suggestions:",
                        color = mutedText,
                        fontSize = 12.sp
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickTags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(KoachTheme.colors.surfaceHigh)
                                    .border(1.dp, KoachTheme.colors.outline, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.applyQuickTag(tag) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = KoachTheme.colors.textPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Reason Text Area
                    OutlinedTextField(
                        value = uiState.reason,
                        onValueChange = viewModel::onReasonChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("revision_reason_field"),
                        placeholder = {
                            Text(
                                text = "Please specify timestamp, deliverable file name, and what adjustments our creative team should make...",
                                color = mutedText
                            )
                        },
                        isError = uiState.reasonError != null,
                        supportingText = {
                            if (uiState.reasonError != null) {
                                Text(
                                    text = uiState.reasonError!!,
                                    color = errorColor,
                                    fontSize = 12.sp,
                                    modifier = Modifier.testTag("revision_reason_error")
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

                    Spacer(modifier = Modifier.height(8.dp))

                    // Submit Button
                    Button(
                        onClick = { viewModel.submit(onSuccess = onNavigateBack) },
                        enabled = !uiState.isSubmitting && uiState.reason.trim().length >= 5 && uiState.reasonError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_revision_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KoachTheme.colors.brand,
                            contentColor = KoachTheme.colors.onBrand,
                            disabledContainerColor = KoachTheme.colors.surfaceHigh,
                            disabledContentColor = KoachTheme.colors.textTertiary
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = KoachTheme.colors.onBrand,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Submit Revision Request",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
