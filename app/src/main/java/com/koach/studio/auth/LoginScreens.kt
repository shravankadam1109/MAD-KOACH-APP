package com.koach.studio.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.koach.studio.auth.LoginValidator.Field
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachTextButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.User
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Movie

/** Extra links under the form; each is optional so a portal can omit it. */
data class LoginLinks(
    val onForgotPassword: (() -> Unit)? = null,
    val onCreateAccount: (() -> Unit)? = null
)

/** Client portal sign-in. */
@Composable
fun ClientLoginScreen(viewModel: LoginViewModel, onBack: () -> Unit, links: LoginLinks = LoginLinks(), modifier: Modifier = Modifier) {
    LoginScreen(
        viewModel = viewModel,
        onBack = onBack,
        links = links,
        copy = LoginCopy(
            eyebrow = "Client portal",
            headline = "Welcome back.",
            body = "Sign in to see your projects, review cuts and talk to your crew.",
            icon = Icons.Outlined.Movie,
            tone = Tone.Brand,
            createAccountLabel = "New to KOACH? Request access",
            screenTag = "client_login_screen"
        ),
        modifier = modifier
    )
}

/** Agency team sign-in. */
@Composable
fun TeamLoginScreen(viewModel: LoginViewModel, onBack: () -> Unit, links: LoginLinks = LoginLinks(), modifier: Modifier = Modifier) {
    LoginScreen(
        viewModel = viewModel,
        onBack = onBack,
        links = links.copy(onCreateAccount = null),
        copy = LoginCopy(
            eyebrow = "Agency team",
            headline = "Studio sign-in.",
            body = "For KOACH crew and admins. Client accounts can't sign in here.",
            icon = Icons.Outlined.Groups,
            tone = Tone.Info,
            createAccountLabel = null,
            screenTag = "team_login_screen"
        ),
        modifier = modifier
    )
}

private data class LoginCopy(
    val eyebrow: String,
    val headline: String,
    val body: String,
    val icon: ImageVector,
    val tone: Tone,
    val createAccountLabel: String?,
    val screenTag: String
)

@Composable
private fun LoginScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    links: LoginLinks,
    copy: LoginCopy,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val focus = LocalFocusManager.current

    KoachBackground(modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { KoachTopBar(title = "", onBack = onBack, transparent = true) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = KoachTheme.spacing.gutter)
                .testTag(copy.screenTag)
        ) {
            Column(Modifier.fillMaxWidth()) {
                IconTile(copy.icon, tone = copy.tone, size = 52.dp)
                Spacer(Modifier.height(20.dp))
                Eyebrow(copy.eyebrow)
                Spacer(Modifier.height(6.dp))
                Text(copy.headline, style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text(copy.body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
            }
            Spacer(Modifier.height(28.dp))

            KoachTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = "Email",
                leadingIcon = Icons.Outlined.Email,
                error = state.errors[Field.EMAIL],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.testTag("login_email")
            )
            Spacer(Modifier.height(14.dp))
            KoachTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = "Password",
                leadingIcon = Icons.Outlined.Lock,
                error = state.errors[Field.PASSWORD],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.signIn() }),
                visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = viewModel::togglePasswordVisibility, modifier = Modifier.testTag("toggle_password")) {
                        Icon(
                            if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (state.passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.testTag("login_password")
            )

            if (links.onForgotPassword != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    KoachTextButton("Forgot password?", onClick = links.onForgotPassword, modifier = Modifier.testTag("forgot_password"))
                }
            } else {
                Spacer(Modifier.height(16.dp))
            }

            state.errorMessage?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.danger,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                        .testTag("login_error")
                )
            }

            KoachPrimaryButton(
                text = "Sign in",
                onClick = { focus.clearFocus(); viewModel.signIn() },
                loading = state.isLoading && state.signingInDemoId == null,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().testTag("login_submit")
            )

            if (copy.createAccountLabel != null && links.onCreateAccount != null) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    KoachTextButton(copy.createAccountLabel, onClick = links.onCreateAccount, modifier = Modifier.testTag("create_account"))
                }
            }

            if (state.isDemo && state.demoAccounts.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                DemoAccounts(state, onSelect = viewModel::signInWithDemo)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    }
}

@Composable
private fun DemoAccounts(state: LoginUiState, onSelect: (String) -> Unit) {
    val c = KoachTheme.colors
    KoachCard(modifier = Modifier.fillMaxWidth().testTag("demo_accounts")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Demo accounts", style = MaterialTheme.typography.titleSmall, color = c.textPrimary, modifier = Modifier.weight(1f))
            DemoTag()
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Sample people for trying the app. Not real logins.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary
        )
        Spacer(Modifier.height(8.dp))
        state.demoAccounts.forEachIndexed { index, user ->
            if (index > 0) KoachDivider()
            DemoAccountRow(user, loading = state.signingInDemoId == user.id, enabled = !state.isLoading, onClick = { onSelect(user.id) })
        }
    }
}

@Composable
private fun DemoAccountRow(user: User, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp)
            .padding(vertical = 12.dp)
            .testTag("demo_account_${user.id}")
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Sign in as ${user.name}, ${user.role.displayName}, demo account"
            }
    ) {
        KoachAvatar(user.initials, size = 38.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text("${user.title} · ${user.role.displayName}", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = c.brand, strokeWidth = 2.dp)
        } else {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
