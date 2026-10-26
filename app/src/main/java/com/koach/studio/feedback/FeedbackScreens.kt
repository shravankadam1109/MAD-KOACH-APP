package com.koach.studio.feedback

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.FormSection
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Feedback
import com.koach.studio.core.model.FeedbackStatus
import com.koach.studio.core.model.FeedbackType
import com.koach.studio.core.model.User
import com.koach.studio.core.ui.formatDate
import com.koach.studio.feedback.FeedbackValidator.Field
import com.koach.studio.projects.data.ProjectRepository
import com.koach.studio.projects.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedbackListUiState(
    val isLoading: Boolean = true,
    val items: List<Feedback> = emptyList(),
    val authorNames: Map<String, String> = emptyMap(),
    val filter: FeedbackStatus? = null,
    val canRespond: Boolean = false,
    val canSubmit: Boolean = false,
    val message: String? = null
)

class FeedbackListViewModel(user: User, private val feedback: FeedbackRepository, people: List<User>) : ViewModel() {
    private val filter = MutableStateFlow<FeedbackStatus?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val names = people.associate { it.id to it.name }
    private val canRespond = user.can(Permission.RESPOND_FEEDBACK)
    private val canSubmit = user.can(Permission.SUBMIT_FEEDBACK)

    val uiState: StateFlow<FeedbackListUiState> = combine(feedback.observeFeedback(), filter, message) { list, f, msg ->
        FeedbackListUiState(false, list.filter { f == null || it.status == f }, names, f, canRespond, canSubmit, msg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedbackListUiState())

    fun setFilter(status: FeedbackStatus?) = filter.update { status }

    fun respond(id: String, response: String, status: FeedbackStatus) {
        viewModelScope.launch {
            message.value = feedback.respond(id, response, status).fold({ "Reply sent" }, { it.message ?: "Couldn't send reply" })
        }
    }

    fun messageShown() = message.update { null }
}

data class NewFeedbackUiState(
    val type: FeedbackType = FeedbackType.FEEDBACK,
    val rating: Int? = null,
    val subject: String = "",
    val message: String = "",
    val projectId: String? = null,
    val projects: List<Project> = emptyList(),
    val errors: Map<Field, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitted: Boolean = false,
    val errorMessage: String? = null
)

class NewFeedbackViewModel(private val feedback: FeedbackRepository, projects: ProjectRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NewFeedbackUiState())
    val uiState: StateFlow<NewFeedbackUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { _uiState.update { it.copy(projects = projects.getProjects().first()) } }
    }

    fun onType(type: FeedbackType) = _uiState.update { it.copy(type = type, errors = it.errors - Field.RATING) }
    fun onRating(rating: Int) = _uiState.update { it.copy(rating = rating, errors = it.errors - Field.RATING) }
    fun onSubject(v: String) = _uiState.update { it.copy(subject = v, errors = it.errors - Field.SUBJECT) }
    fun onMessage(v: String) = _uiState.update { it.copy(message = v, errors = it.errors - Field.MESSAGE) }
    fun onProject(id: String?) = _uiState.update { it.copy(projectId = id) }

    fun submit() {
        val s = _uiState.value
        if (s.isSubmitting) return
        val draft = FeedbackDraft(s.type, s.rating, s.subject, s.message, s.projectId)
        val errors = FeedbackValidator.validate(draft)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = feedback.submit(draft)
            _uiState.update { it.copy(isSubmitting = false, submitted = result.isSuccess, errorMessage = result.exceptionOrNull()?.message) }
        }
    }
}

private val FeedbackStatus.tone: Tone
    get() = when (this) {
        FeedbackStatus.OPEN -> Tone.Warning
        FeedbackStatus.IN_PROGRESS -> Tone.Info
        FeedbackStatus.RESOLVED -> Tone.Success
    }

@Composable
fun FeedbackListScreen(viewModel: FeedbackListViewModel, onBack: () -> Unit, onNewFeedback: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val snackbar = remember { SnackbarHostState() }
    var respondingId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = if (state.canRespond) "Feedback inbox" else "Feedback & support", onBack = onBack) },
        floatingActionButton = {
            if (state.canSubmit) {
                ExtendedFloatingActionButton(
                    onClick = onNewFeedback,
                    containerColor = c.brand,
                    contentColor = c.onBrand,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Share feedback") },
                    modifier = Modifier.testTag("new_feedback")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("feedback_list")
        ) {
            item { KoachFilterChips(FeedbackStatus.entries, state.filter, viewModel::setFilter, label = { it.displayName }) }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Outlined.RateReview,
                        "Nothing yet",
                        if (state.canSubmit) "Tell us how we're doing or raise an issue. We read everything." else "No feedback matches this filter.",
                        actionLabel = if (state.canSubmit) "Share feedback" else null,
                        onAction = if (state.canSubmit) onNewFeedback else null
                    )
                }
            }
            items(state.items, key = { it.id }) { item ->
                FeedbackCard(
                    item,
                    author = state.authorNames[item.authorId] ?: "Client",
                    onRespond = if (state.canRespond) ({ respondingId = item.id }) else null,
                    modifier = Modifier.padding(horizontal = gutter)
                )
            }
        }
    }

    state.items.find { it.id == respondingId }?.let { item ->
        RespondDialog(
            item,
            onDismiss = { respondingId = null },
            onSend = { response, status -> viewModel.respond(item.id, response, status); respondingId = null }
        )
    }
}

@Composable
private fun FeedbackCard(item: Feedback, author: String, onRespond: (() -> Unit)?, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    KoachCard(onClick = onRespond, modifier = modifier.fillMaxWidth().testTag("feedback_${item.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusPill(item.type.displayName, if (item.type == FeedbackType.COMPLAINT) Tone.Danger else Tone.Brand, showDot = false)
            Spacer(Modifier.width(8.dp))
            StatusPill(item.status.displayName, item.status.tone)
            Spacer(Modifier.weight(1f))
            Text(formatDate(item.createdOn), style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        }
        Spacer(Modifier.height(12.dp))
        Text(item.subject, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        item.rating?.let { Spacer(Modifier.height(4.dp)); Stars(it) }
        Spacer(Modifier.height(6.dp))
        Text(item.message, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        Spacer(Modifier.height(8.dp))
        Text("From $author", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        item.response?.let {
            Spacer(Modifier.height(12.dp))
            KoachCard(contentPadding = PaddingValues(12.dp), modifier = Modifier.fillMaxWidth()) {
                Eyebrow("Studio reply")
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
            }
        }
    }
}

@Composable
private fun Stars(rating: Int, onRate: ((Int) -> Unit)? = null) {
    val c = KoachTheme.colors
    Row(modifier = Modifier.semantics(mergeDescendants = onRate == null) { if (onRate == null) contentDescription = "$rating out of 5 stars" }) {
        (1..5).forEach { star ->
            val filled = star <= rating
            Icon(
                if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = if (onRate != null) "$star star${if (star > 1) "s" else ""}" else null,
                tint = if (filled) c.brand else c.textTertiary,
                modifier = Modifier
                    .size(if (onRate != null) 40.dp else 18.dp)
                    .then(if (onRate != null) Modifier.selectable(selected = star == rating, role = Role.RadioButton) { onRate(star) }.testTag("star_$star") else Modifier)
                    .padding(if (onRate != null) 4.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun RespondDialog(item: Feedback, onDismiss: () -> Unit, onSend: (String, FeedbackStatus) -> Unit) {
    val c = KoachTheme.colors
    var reply by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf(if (item.status == FeedbackStatus.OPEN) FeedbackStatus.IN_PROGRESS else item.status) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceRaised,
        title = { Text("Reply to ${item.subject}", color = c.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KoachTextField(reply, { reply = it }, "Reply", singleLine = false, minLines = 3, modifier = Modifier.testTag("feedback_reply"))
                KoachDropdownField("Status", FeedbackStatus.entries, status, { status = it }, { it.displayName })
            }
        },
        confirmButton = { TextButton(onClick = { onSend(reply, status) }, modifier = Modifier.testTag("send_reply")) { Text("Send", color = c.brand) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textSecondary) } }
    )
}

@Composable
fun NewFeedbackScreen(viewModel: NewFeedbackViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors

    LaunchedEffect(state.submitted) { if (state.submitted) onBack() }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = "Share feedback", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KoachTheme.spacing.gutter, vertical = 8.dp)
                .testTag("feedback_form")
        ) {
            KoachSegmentedControl(
                options = listOf("Feedback", "Complaint"),
                selectedIndex = if (state.type == FeedbackType.FEEDBACK) 0 else 1,
                onSelect = { viewModel.onType(if (it == 0) FeedbackType.FEEDBACK else FeedbackType.COMPLAINT) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            FormSection {
                if (state.type == FeedbackType.FEEDBACK) {
                    Eyebrow("How did we do?", color = c.textTertiary)
                    Stars(state.rating ?: 0, onRate = viewModel::onRating)
                    state.errors[Field.RATING]?.let { Text(it, color = c.danger, style = MaterialTheme.typography.bodySmall) }
                } else {
                    Text(
                        "Sorry something went wrong. Tell us what happened and the studio will follow up.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary
                    )
                }
                KoachDropdownField(
                    label = "Project (optional)",
                    options = listOf<String?>(null) + state.projects.map { it.id },
                    selected = state.projectId,
                    onSelected = viewModel::onProject,
                    optionLabel = { id -> state.projects.find { it.id == id }?.title ?: "General" },
                    placeholder = "General"
                )
                KoachTextField(state.subject, viewModel::onSubject, "Subject", error = state.errors[Field.SUBJECT], maxLength = FeedbackValidator.SUBJECT_MAX, modifier = Modifier.testTag("feedback_subject"))
                KoachTextField(state.message, viewModel::onMessage, "Details", error = state.errors[Field.MESSAGE], maxLength = FeedbackValidator.MESSAGE_MAX, singleLine = false, minLines = 4, modifier = Modifier.testTag("feedback_message"))
            }
            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            KoachPrimaryButton(
                text = if (state.type == FeedbackType.FEEDBACK) "Send feedback" else "Raise complaint",
                onClick = viewModel::submit,
                loading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().testTag("submit_feedback")
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
