package com.koach.studio.documents.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.FormSection
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSearchField
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.KoachSnackbarHost
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.documents.data.DocumentValidator
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.DocumentCategory
import com.koach.studio.core.model.DocumentVisibility
import com.koach.studio.core.model.StudioDocument
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatFileSize
import com.koach.studio.core.ui.formatRelative

private val StudioDocument.icon: ImageVector
    get() = when {
        mimeType == "application/pdf" -> Icons.Outlined.PictureAsPdf
        mimeType.contains("sheet") || mimeType.contains("excel") -> Icons.Outlined.TableChart
        mimeType.startsWith("image/") -> Icons.Outlined.Image
        mimeType.startsWith("video/") -> Icons.Outlined.Videocam
        mimeType.startsWith("audio/") -> Icons.Outlined.Audiotrack
        else -> Icons.Outlined.Description
    }

private val DocumentCategory.tone: Tone
    get() = when (this) {
        DocumentCategory.CONTRACT, DocumentCategory.INVOICE -> Tone.Review
        DocumentCategory.BRIEF, DocumentCategory.SCRIPT, DocumentCategory.STORYBOARD -> Tone.Info
        DocumentCategory.ASSET -> Tone.Brand
        DocumentCategory.REPORT -> Tone.Neutral
    }

@Composable
fun DocumentsScreen(
    viewModel: DocumentsViewModel,
    onBack: () -> Unit,
    onAddDocument: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val snackbar = remember { SnackbarHostState() }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemove by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = "Documents", subtitle = state.projectFilterTitle, onBack = onBack) },
        floatingActionButton = {
            if (state.canUpload) {
                ExtendedFloatingActionButton(
                    onClick = onAddDocument,
                    containerColor = c.brand,
                    contentColor = c.onBrand,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add document") },
                    modifier = Modifier.testTag("add_document")
                )
            }
        },
        snackbarHost = { KoachSnackbarHost(snackbar, isError = state.messageIsError) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.testTag("documents_list")
        ) {
            item {
                Column(Modifier.padding(horizontal = gutter)) {
                    KoachSearchField(state.query, viewModel::onQuery, "Search documents or projects")
                    if (!state.storageAvailable) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = c.textTertiary, modifier = Modifier.padding(end = 6.dp))
                            Text(
                                "Demo build: documents are listed as metadata only. Files aren't uploaded or stored.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textTertiary
                            )
                        }
                    }
                }
            }
            item {
                KoachFilterChips(DocumentCategory.entries, state.category, viewModel::onCategory, label = { it.displayName })
            }
            if (state.rows.isEmpty()) {
                item {
                    val filtering = state.query.isNotBlank() || state.category != null
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = if (filtering) "No matching documents" else "No documents yet",
                        message = when {
                            filtering -> "Try a different search or category."
                            state.isAgency -> "Add the first document to keep project files in one place."
                            else -> "Documents your studio shares with you will appear here."
                        },
                        actionLabel = if (state.canUpload) "Add document" else null,
                        onAction = if (state.canUpload) onAddDocument else null
                    )
                }
            }
            items(state.rows, key = { it.document.id }) { row ->
                DocumentCard(row, showVisibility = state.isAgency, onClick = { selectedId = row.document.id }, modifier = Modifier.padding(horizontal = gutter))
            }
        }
    }

    val selected = state.rows.find { it.document.id == selectedId }
    if (selected != null) {
        DocumentDetailsDialog(
            row = selected,
            isAgency = state.isAgency,
            onDismiss = {
                selectedId = null
                confirmRemove = false
            },
            onToggleVisibility = {
                val next = if (selected.document.visibility == DocumentVisibility.SHARED) DocumentVisibility.INTERNAL else DocumentVisibility.SHARED
                viewModel.setVisibility(selected.document.id, next)
            },
            onDelete = { confirmRemove = true }
        )
        if (confirmRemove) {
            AlertDialog(
                onDismissRequest = { confirmRemove = false },
                containerColor = c.surfaceRaised,
                title = { Text("Remove this document?", color = c.textPrimary) },
                text = {
                    Text(
                        "\"${selected.document.name}\" will be removed from the list. This can't be undone.",
                        color = c.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.delete(selected.document.id)
                            confirmRemove = false
                            selectedId = null
                        },
                        modifier = Modifier.testTag("confirm_delete_document")
                    ) { Text("Remove", color = c.danger) }
                },
                dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel", color = c.textSecondary) } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DocumentCard(row: DocumentRow, showVisibility: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    val doc = row.document
    KoachCard(onClick = onClick, contentPadding = PaddingValues(16.dp), modifier = modifier.fillMaxWidth().testTag("document_${doc.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(doc.icon, tone = doc.category.tone, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(doc.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(row.projectTitle, formatFileSize(doc.sizeBytes).takeIf { doc.sizeBytes > 0 }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatusPill(doc.category.displayName, doc.category.tone, showDot = false)
            if (showVisibility) {
                StatusPill(
                    if (doc.visibility == DocumentVisibility.SHARED) "Shared" else "Internal",
                    if (doc.visibility == DocumentVisibility.SHARED) Tone.Brand else Tone.Neutral
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${row.uploaderName.substringBefore(" ")} · ${formatRelative(doc.uploadedAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun DocumentDetailsDialog(
    row: DocumentRow,
    isAgency: Boolean,
    onDismiss: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    val c = KoachTheme.colors
    val doc = row.document
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceRaised,
        icon = { IconTile(doc.icon, tone = doc.category.tone) },
        title = { Text(doc.name, color = c.textPrimary, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("Category", doc.category.displayName)
                KoachDivider()
                InfoRow("Project", row.projectTitle ?: "None")
                KoachDivider()
                InfoRow("Size", if (doc.sizeBytes > 0) formatFileSize(doc.sizeBytes) else "Unknown")
                KoachDivider()
                InfoRow("Added by", row.uploaderName)
                KoachDivider()
                InfoRow("Added", formatDate(doc.uploadedAt.toLocalDate()))
                if (isAgency) {
                    KoachDivider()
                    InfoRow("Visibility", doc.visibility.displayName)
                }
                if (doc.storagePath == null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "This file isn't stored in the demo build, so it can't be opened.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary
                    )
                }
                if (isAgency) {
                    Spacer(Modifier.height(12.dp))
                    KoachSecondaryButton(
                        text = if (doc.visibility == DocumentVisibility.SHARED) "Make internal" else "Share with client",
                        onClick = onToggleVisibility,
                        leadingIcon = if (doc.visibility == DocumentVisibility.SHARED) Icons.Outlined.Lock else Icons.Outlined.Public,
                        modifier = Modifier.fillMaxWidth().testTag("toggle_visibility")
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.brand) } },
        dismissButton = {
            if (row.canDelete) {
                TextButton(onClick = onDelete, modifier = Modifier.testTag("delete_document")) {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = c.danger)
                    Spacer(Modifier.width(4.dp))
                    Text("Remove", color = c.danger)
                }
            }
        }
    )
}

@Composable
fun AddDocumentScreen(viewModel: AddDocumentViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { readFileInfo(context, it) }?.let(viewModel::onFilePicked)
    }

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = "Add document", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KoachTheme.spacing.gutter, vertical = 8.dp)
                .testTag("add_document_form")
        ) {
            KoachCard(
                highlighted = state.picked != null,
                onClick = { picker.launch(arrayOf("*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription = if (state.picked == null) {
                            "Choose a file"
                        } else {
                            "Selected file ${state.picked!!.name}, tap to change"
                        }
                    }
                    .testTag("pick_file")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.AttachFile, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(state.picked?.name ?: "Choose a file", style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            state.picked?.let { listOf(it.mimeType, formatFileSize(it.sizeBytes)).joinToString(" · ") } ?: "Optional: fills in the name, type and size",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Only the file's details are saved. The demo build doesn't upload files anywhere.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary
            )
            Spacer(Modifier.height(18.dp))
            FormSection {
                KoachTextField(state.name, viewModel::onName, "Document name", error = state.nameError, maxLength = DocumentValidator.NAME_MAX, modifier = Modifier.testTag("document_name"))
                KoachDropdownField("Category", DocumentCategory.entries, state.category, viewModel::onCategory, { it.displayName })
                KoachDropdownField(
                    label = "Project",
                    options = listOf<String?>(null) + state.projects.map { it.id },
                    selected = state.projectId,
                    onSelected = viewModel::onProject,
                    optionLabel = { id -> state.projects.find { it.id == id }?.title ?: "No project" }
                )
                if (state.isAgency) {
                    Eyebrow("Visibility", color = c.textTertiary)
                    KoachSegmentedControl(
                        options = listOf("Shared with client", "Internal"),
                        selectedIndex = if (state.visibility == DocumentVisibility.SHARED) 0 else 1,
                        onSelect = { viewModel.onVisibility(if (it == 0) DocumentVisibility.SHARED else DocumentVisibility.INTERNAL) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    it,
                    color = c.danger,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Spacer(Modifier.height(24.dp))
            KoachPrimaryButton(
                text = "Save document",
                onClick = viewModel::save,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().testTag("save_document")
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Reads display name, size and type for a picked file without copying its contents. */
private fun readFileInfo(context: Context, uri: Uri): PickedFile {
    var name = uri.lastPathSegment ?: "Document"
    var size = 0L
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
            cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { if (!cursor.isNull(it)) size = cursor.getLong(it) }
        }
    }
    return PickedFile(name, context.contentResolver.getType(uri) ?: "application/octet-stream", size)
}
