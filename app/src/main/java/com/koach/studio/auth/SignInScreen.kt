package com.koach.studio.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.koachViewModel
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    viewModel: SignInViewModel = koachViewModel { SignInViewModel(it.authRepository) }
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    KoachBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = gutter, vertical = 24.dp)
                .testTag("sign_in_screen")
        ) {
            Wordmark()
            Spacer(Modifier.height(48.dp))
            Text(
                buildAnnotatedString {
                    append("Where your stories get ")
                    withStyle(SpanStyle(color = c.brand)) { append("made.") }
                },
                style = MaterialTheme.typography.displayMedium,
                color = c.textPrimary
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Track projects, review cuts and talk to your crew, all in one place.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary
            )

            if (state.isDemo) {
                Spacer(Modifier.height(28.dp))
                DemoNotice()
            }

            Spacer(Modifier.height(32.dp))
            Eyebrow("Client accounts")
            Spacer(Modifier.height(12.dp))
            AccountList(state.clientAccounts, state.signingInId, viewModel::signIn)

            Spacer(Modifier.height(28.dp))
            Eyebrow("Agency team")
            Spacer(Modifier.height(12.dp))
            AccountList(state.agencyAccounts, state.signingInId, viewModel::signIn)

            state.errorMessage?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Email sign-in will be available once Firebase Authentication is connected.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary
            )
        }
    }
}

@Composable
private fun Wordmark() {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.Bottom) {
        Text("KOACH", style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
        Spacer(Modifier.width(8.dp))
        Text(
            "STUDIO",
            style = MaterialTheme.typography.labelSmall,
            color = c.brand,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun DemoNotice() {
    val c = KoachTheme.colors
    KoachCard {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(Icons.Outlined.Info, tone = Tone.Info, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Demo mode", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Pick a sample account to explore. There are no real logins, and all data is sample data that resets when the app closes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary
                )
            }
        }
    }
}

@Composable
private fun AccountList(accounts: List<User>, signingInId: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        accounts.forEach { user ->
            AccountCard(user, loading = signingInId == user.id, onClick = { onSelect(user.id) })
        }
    }
}

@Composable
private fun AccountCard(user: User, loading: Boolean, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        highlighted = user.role == Role.ADMIN,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("demo_account_${user.id}")
            .semantics(mergeDescendants = true) {
                role = SemanticsRole.Button
                contentDescription = "Sign in as ${user.name}, ${user.role.displayName}"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KoachAvatar(user.initials, size = 44.dp, highlighted = user.role == Role.ADMIN)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(user.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(user.title, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                Spacer(Modifier.height(8.dp))
                StatusPill(
                    user.role.displayName,
                    tone = when (user.role) {
                        Role.CLIENT -> Tone.Info
                        Role.TEAM -> Tone.Neutral
                        Role.ADMIN -> Tone.Brand
                    }
                )
            }
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), color = c.brand, strokeWidth = 2.dp)
            } else {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.textTertiary)
            }
        }
    }
}
