package com.koach.studio.clients.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.clients.data.ClientValidator
import com.koach.studio.clients.data.ClientValidator.Field
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.FormSection
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachProgressBar
import com.koach.studio.core.designsystem.components.KoachSearchField
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.ClientStatus
import com.koach.studio.core.ui.formatDate
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.ui.tone

val ClientStatus.tone: Tone
    get() = when (this) {
        ClientStatus.LEAD -> Tone.Info
        ClientStatus.ACTIVE -> Tone.Brand
        ClientStatus.PAUSED -> Tone.Warning
        ClientStatus.ARCHIVED -> Tone.Neutral
    }

@Composable
fun ClientListScreen(
    viewModel: ClientListViewModel,
    onOpenClient: (String) -> Unit,
    onAddClient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    Scaffold(
        containerColor = c.background,
        floatingActionButton = {
            if (state.canCreate) {
                ExtendedFloatingActionButton(
                    onClick = onAddClient,
                    containerColor = c.brand,
                    contentColor = c.onBrand,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Add client") },
                    modifier = Modifier.testTag("add_client")
                )
            }
        },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("client_list")
        ) {
            item {
                Column(Modifier.padding(start = gutter, end = gutter, top = 16.dp)) {
                    Eyebrow("${state.totalCount} clients · ${state.activeCount} active")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Clients",
                        style = MaterialTheme.typography.headlineMedium,
                        color = c.textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    KoachSearchField(
                        query = state.query,
                        onQueryChange = viewModel::onQueryChange,
                        placeholder = "Search name, contact, industry or city",
                        modifier = Modifier.testTag("client_search")
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            item {
                KoachFilterChips(
                    options = ClientStatus.entries,
                    selected = state.statusFilter,
                    onSelected = viewModel::onStatusFilter,
                    label = { it.displayName }
                )
            }
            if (state.rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = if (state.query.isNotBlank()) Icons.Outlined.SearchOff else Icons.Outlined.Groups,
                        title = if (state.query.isNotBlank()) "No matches" else "No clients yet",
                        message = if (state.query.isNotBlank()) "Try another name, city or industry." else "Add your first client to start tracking work.",
                        actionLabel = if (state.canCreate && state.query.isBlank()) "Add client" else null,
                        onAction = if (state.canCreate && state.query.isBlank()) onAddClient else null
                    )
                }
            }
            items(state.rows, key = { it.client.id }) { row ->
                ClientCard(
                    row = row,
                    onClick = { onOpenClient(row.client.id) },
                    modifier = Modifier.padding(horizontal = gutter)
                )
            }
        }
    }
}

@Composable
private fun ClientCard(row: ClientRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    val client = row.client
    KoachCard(
        onClick = onClick,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("client_${client.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KoachAvatar(
                initials = client.initials,
                size = 42.dp,
                highlighted = client.status == ClientStatus.ACTIVE
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${client.industry} · ${client.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(client.status.displayName, client.status.tone)
        }

        KoachDivider(padding = PaddingValues(vertical = 10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = c.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = client.contactName,
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = if (row.activeProjectCount > 0) c.brand else c.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = when (row.projectCount) {
                    0 -> "No projects"
                    1 -> "1 project"
                    else -> "${row.projectCount} projects"
                } + if (row.activeProjectCount > 0) " · ${row.activeProjectCount} active" else "",
                style = MaterialTheme.typography.labelMedium,
                color = if (row.activeProjectCount > 0) c.brand else c.textTertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ClientDetailScreen(
    viewModel: ClientDetailViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.errorShown()
        }
    }

    val client = state.client
    Scaffold(
        containerColor = c.background,
        topBar = {
            KoachTopBar(title = client?.name ?: "Client", onBack = onBack) {
                if (client != null && state.canEdit) {
                    IconButton(onClick = { onEdit(client.id) }, modifier = Modifier.testTag("edit_client")) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit client", tint = c.textPrimary)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            client == null -> ErrorState("This client no longer exists.", Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = gutter, end = gutter, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    KoachCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KoachAvatar(client.initials, size = 56.dp, highlighted = true)
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(client.name, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                                Text(client.industry, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                                Spacer(Modifier.height(8.dp))
                                StatusPill(client.status.displayName, client.status.tone)
                            }
                        }
                    }
                }
                item {
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Eyebrow("Contact", color = c.textTertiary)
                        Spacer(Modifier.height(4.dp))
                        InfoRow("Contact", client.contactName, icon = Icons.Outlined.Person)
                        KoachDivider()
                        InfoRow("Email", client.email, icon = Icons.Outlined.Email)
                        KoachDivider()
                        InfoRow("Phone", client.phone, icon = Icons.Outlined.Phone)
                        KoachDivider()
                        InfoRow("City", client.city, icon = Icons.Outlined.LocationOn)
                        KoachDivider()
                        InfoRow("Client since", formatDate(client.since), icon = Icons.Outlined.CalendarMonth)
                    }
                }
                if (client.notes.isNotBlank()) {
                    item {
                        KoachCard(modifier = Modifier.fillMaxWidth()) {
                            Eyebrow("Notes", color = c.textTertiary)
                            Spacer(Modifier.height(8.dp))
                            Text(client.notes, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        }
                    }
                }
                item {
                    Text("Projects", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                }
                if (state.projects.isEmpty()) {
                    item {
                        KoachCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(Icons.Outlined.Movie, "No projects yet", "Projects for this client will appear here.")
                        }
                    }
                }
                items(state.projects, key = { it.id }) { project ->
                    LinkedProjectRow(project, onClick = { onOpenProject(project.id) })
                }
                if (state.canDelete) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        KoachSecondaryButton(
                            text = "Delete client",
                            onClick = { confirmDelete = true },
                            destructive = true,
                            leadingIcon = Icons.Outlined.Delete,
                            loading = state.isDeleting,
                            modifier = Modifier.fillMaxWidth().testTag("delete_client")
                        )
                        if (state.projects.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Clients with projects can't be deleted. Set the status to Archived instead.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textTertiary
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete && client != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.surfaceRaised,
            icon = { IconTile(Icons.Outlined.Delete, tone = Tone.Danger) },
            title = { Text("Delete ${client.name}?", color = c.textPrimary) },
            text = { Text("This removes the client record. It can't be undone.", color = c.textSecondary) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }, modifier = Modifier.testTag("confirm_delete")) {
                    Text("Delete", color = c.danger)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = c.textSecondary) } }
        )
    }
}

@Composable
private fun LinkedProjectRow(project: Project, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(onClick = onClick, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            StatusPill(project.status.displayName, project.status.tone)
        }
        Spacer(Modifier.height(10.dp))
        KoachProgressBar(project.progress, height = 4.dp)
    }
}

@Composable
fun ClientFormScreen(
    viewModel: ClientFormViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors

    LaunchedEffect(state.savedClientId) { state.savedClientId?.let(onSaved) }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = if (state.isEditing) "Edit client" else "New client", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = KoachTheme.spacing.gutter, vertical = 8.dp)
                .testTag("client_form")
        ) {
            FormSection {
                Eyebrow("Organisation", color = c.textTertiary)
                KoachTextField(state.name, viewModel::onName, "Client name", leadingIcon = Icons.Outlined.Business, error = state.errors[Field.NAME], maxLength = ClientValidator.NAME_MAX, modifier = Modifier.testTag("client_name"))
                KoachTextField(state.industry, viewModel::onIndustry, "Industry", placeholder = "e.g. Music label", error = state.errors[Field.INDUSTRY], modifier = Modifier.testTag("client_industry"))
                KoachTextField(state.city, viewModel::onCity, "City", leadingIcon = Icons.Outlined.LocationOn, error = state.errors[Field.CITY], modifier = Modifier.testTag("client_city"))
                KoachDropdownField(
                    label = "Status",
                    options = ClientStatus.entries,
                    selected = state.status,
                    onSelected = viewModel::onStatus,
                    optionLabel = { it.displayName }
                )
                Spacer(Modifier.height(4.dp))
                Eyebrow("Main contact", color = c.textTertiary)
                KoachTextField(state.contactName, viewModel::onContact, "Contact name", leadingIcon = Icons.Outlined.Person, error = state.errors[Field.CONTACT], modifier = Modifier.testTag("client_contact"))
                KoachTextField(state.email, viewModel::onEmail, "Email", leadingIcon = Icons.Outlined.Email, error = state.errors[Field.EMAIL], keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.testTag("client_email"))
                KoachTextField(state.phone, viewModel::onPhone, "Phone", leadingIcon = Icons.Outlined.Phone, error = state.errors[Field.PHONE], placeholder = "+91 98200 00000", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.testTag("client_phone"))
                KoachTextField(state.notes, viewModel::onNotes, "Notes (optional)", error = state.errors[Field.NOTES], maxLength = ClientValidator.NOTES_MAX, singleLine = false, minLines = 3)
            }
            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            KoachPrimaryButton(
                text = if (state.isEditing) "Save changes" else "Add client",
                onClick = viewModel::save,
                loading = state.isSaving,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().testTag("save_client")
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
