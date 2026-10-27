package com.koach.studio.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.auth.LoginValidator.Field
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTextButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Role
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---- Request (signed out) ----------------------------------------------------

data class StudioAccessRequestUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val errors: Map<Field, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** Set once the sign-in exists; the person passes this ID to a studio admin. */
    val accountId: String? = null
)

/**
 * New studio staff create a sign-in here. It carries no profile, so it grants no
 * access: a studio admin activates it (Account → Studio team access). The
 * security rules only let admins write team or admin profiles.
 */
class StudioAccessRequestViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(StudioAccessRequestUiState())
    val uiState: StateFlow<StudioAccessRequestUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, errors = it.errors - Field.NAME, errorMessage = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v.trim(), errors = it.errors - Field.EMAIL, errorMessage = null) }
    fun onPasswordChange(v: String) = _uiState.update { it.copy(password = v, errors = it.errors - Field.PASSWORD, errorMessage = null) }

    fun submit() {
        val s = _uiState.value
        if (s.isLoading) return
        val errors = LoginValidator.validate(s.email, s.password).toMutableMap()
        if (s.name.isBlank()) errors[Field.NAME] = "Enter your name"
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = auth.requestStudioAccess(s.name, s.email, s.password)
            _uiState.update {
                it.copy(isLoading = false, accountId = result.getOrNull(), errorMessage = result.exceptionOrNull()?.message, password = "")
            }
        }
    }
}

@Composable
fun StudioAccessRequestScreen(
    viewModel: StudioAccessRequestViewModel,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    KoachBackground(modifier.fillMaxSize()) {
        AuthBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { KoachTopBar(title = "", onBack = onBackToSignIn, transparent = true) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KoachTheme.spacing.xl, vertical = 8.dp)
                    .testTag("studio_access_request_screen")
            ) {
                AuthHeader(
                    eyebrow = "Agency team",
                    headline = "Request studio access.",
                    body = "Create your studio sign-in. A KOACH admin activates it before you can use the studio."
                )
                Spacer(Modifier.height(28.dp))
                AnimatedContent(
                    targetState = state.accountId,
                    transitionSpec = { fadeIn(KoachMotion.enter()) togetherWith fadeOut(KoachMotion.exit()) },
                    label = "studio-request-state"
                ) { accountId ->
                    if (accountId == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            KoachTextField(
                                value = state.name, onValueChange = viewModel::onNameChange, label = "Full name",
                                leadingIcon = Icons.Outlined.Person, error = state.errors[Field.NAME],
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier.testTag("studio_request_name")
                            )
                            KoachTextField(
                                value = state.email, onValueChange = viewModel::onEmailChange, label = "Work email",
                                leadingIcon = Icons.Outlined.Email, error = state.errors[Field.EMAIL],
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier.testTag("studio_request_email")
                            )
                            KoachTextField(
                                value = state.password, onValueChange = viewModel::onPasswordChange,
                                label = "Choose a password (min ${LoginValidator.PASSWORD_MIN} chars)",
                                leadingIcon = Icons.Outlined.Lock, error = state.errors[Field.PASSWORD],
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.submit() }),
                                modifier = Modifier.testTag("studio_request_password")
                            )
                            state.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.danger) }
                            Spacer(Modifier.height(4.dp))
                            KoachPrimaryButton(
                                text = "Request access",
                                onClick = { focus.clearFocus(); viewModel.submit() },
                                loading = state.isLoading,
                                modifier = Modifier.fillMaxWidth().testTag("studio_request_submit")
                            )
                        }
                    } else {
                        KoachCard(
                            highlighted = true,
                            modifier = Modifier.fillMaxWidth().testTag("studio_request_sent").semantics { liveRegion = LiveRegionMode.Polite }
                        ) {
                            Text("Request created", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Send this account ID to a KOACH admin. Once they activate it, sign in on the Studio sign-in screen.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textSecondary
                            )
                            Spacer(Modifier.height(14.dp))
                            Eyebrow("Account ID", color = c.textTertiary)
                            Spacer(Modifier.height(4.dp))
                            SelectionContainer {
                                Text(accountId, style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace), color = c.textPrimary, modifier = Modifier.testTag("studio_request_account_id"))
                            }
                            Spacer(Modifier.height(12.dp))
                            KoachSecondaryButton(
                                text = "Copy account ID",
                                leadingIcon = Icons.Outlined.ContentCopy,
                                onClick = { clipboard.setText(AnnotatedString(accountId)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    KoachTextButton("Back to Studio sign-in", onClick = onBackToSignIn)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// ---- Activation (admins, signed in) -----------------------------------------

data class GrantStudioAccessUiState(
    val accountId: String = "",
    val name: String = "",
    val email: String = "",
    val title: String = "",
    val role: Role = Role.TEAM,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activatedName: String? = null
)

/** Admins turn a requested studio sign-in into a team or admin account. */
class GrantStudioAccessViewModel(private val auth: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(GrantStudioAccessUiState())
    val uiState: StateFlow<GrantStudioAccessUiState> = _uiState.asStateFlow()

    fun onAccountIdChange(v: String) = _uiState.update { it.copy(accountId = v.trim(), errorMessage = null, activatedName = null) }
    fun onNameChange(v: String) = _uiState.update { it.copy(name = v, errorMessage = null) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v.trim(), errorMessage = null) }
    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v, errorMessage = null) }
    fun onRoleChange(role: Role) = _uiState.update { it.copy(role = role) }

    fun activate() {
        val s = _uiState.value
        if (s.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, activatedName = null) }
            val result = auth.grantStudioAccess(s.accountId, s.name, s.email, s.title, s.role)
            _uiState.update { st ->
                result.fold(
                    onSuccess = { GrantStudioAccessUiState(activatedName = s.name.trim()) },
                    onFailure = { e -> st.copy(isLoading = false, errorMessage = e.message ?: "Couldn't activate this account.") }
                )
            }
        }
    }
}

@Composable
fun GrantStudioAccessScreen(viewModel: GrantStudioAccessViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { KoachTopBar(title = "Studio team access", subtitle = "Admins only", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = KoachTheme.spacing.xl, vertical = 12.dp))
                .testTag("grant_studio_access_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Activate a sign-in someone created with \"Request studio access\". Paste the account ID they send you. Client accounts can't be given studio access.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary
            )
            KoachTextField(value = state.accountId, onValueChange = viewModel::onAccountIdChange, label = "Account ID", leadingIcon = Icons.Outlined.Fingerprint, modifier = Modifier.testTag("grant_account_id"))
            KoachTextField(value = state.name, onValueChange = viewModel::onNameChange, label = "Full name", leadingIcon = Icons.Outlined.Person, modifier = Modifier.testTag("grant_name"))
            KoachTextField(
                value = state.email, onValueChange = viewModel::onEmailChange, label = "Work email", leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.testTag("grant_email")
            )
            KoachTextField(value = state.title, onValueChange = viewModel::onTitleChange, label = "Title (optional), e.g. Lead Editor", leadingIcon = Icons.Outlined.Badge)
            Eyebrow("Role", color = c.textTertiary)
            KoachSegmentedControl(
                options = listOf("Team", "Admin"),
                selectedIndex = if (state.role == Role.ADMIN) 1 else 0,
                onSelect = { viewModel.onRoleChange(if (it == 1) Role.ADMIN else Role.TEAM) },
                modifier = Modifier.fillMaxWidth()
            )
            state.errorMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = c.danger, modifier = Modifier.testTag("grant_error")) }
            state.activatedName?.let {
                Text("$it can now sign in on the Studio sign-in screen.", style = MaterialTheme.typography.bodyMedium, color = c.brand, modifier = Modifier.testTag("grant_success"))
            }
            KoachPrimaryButton(
                text = "Activate account",
                onClick = viewModel::activate,
                loading = state.isLoading,
                modifier = Modifier.fillMaxWidth().testTag("grant_activate")
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
