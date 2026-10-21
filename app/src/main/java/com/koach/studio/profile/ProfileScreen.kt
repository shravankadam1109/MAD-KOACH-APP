package com.koach.studio.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koach.studio.clients.data.ClientRepository
import com.koach.studio.core.auth.AccessPolicy
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.koachGlow
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Composable
fun ProfileScreen(
    user: User,
    clients: ClientRepository,
    isDemo: Boolean,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val organisationFlow = androidx.compose.runtime.remember(user.clientId) {
        user.clientId?.let { id -> clients.observeClient(id).map { it?.name } } ?: flowOf("KOACH Studio")
    }
    val organisation by organisationFlow.collectAsState(initial = null)

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = "Profile", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = KoachTheme.spacing.gutter, end = KoachTheme.spacing.gutter, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.testTag("profile_screen")
        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().koachGlow(c.brand, 0.18f).padding(vertical = 20.dp)
                ) {
                    KoachAvatar(user.initials, size = 88.dp, highlighted = true)
                    Spacer(Modifier.height(14.dp))
                    Text(user.name, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary, textAlign = TextAlign.Center)
                    Text(user.title, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    StatusPill(user.role.displayName, if (user.role == Role.CLIENT) Tone.Info else Tone.Brand)
                }
            }
            item {
                KoachCard(modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Details", color = c.textTertiary)
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Email", user.email, icon = Icons.Outlined.Email)
                    KoachDivider()
                    InfoRow("Phone", user.phone.ifBlank { "Not set" }, icon = Icons.Outlined.Phone)
                    KoachDivider()
                    InfoRow("Organisation", organisation ?: "…", icon = Icons.Outlined.Business)
                    KoachDivider()
                    InfoRow("Role", user.role.displayName, icon = Icons.Outlined.Badge)
                    KoachDivider()
                    InfoRow("Permissions", "${AccessPolicy.permissionsFor(user.role).size} granted", icon = Icons.Outlined.Shield)
                }
            }
            if (isDemo) {
                item {
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconTile(Icons.Outlined.Info, tone = Tone.Info, size = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "This is a sample demo account. Profile details can't be edited until real accounts are connected through Firebase Authentication.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary
                            )
                        }
                    }
                }
            }
            item {
                KoachSecondaryButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    destructive = true,
                    leadingIcon = Icons.AutoMirrored.Filled.Logout,
                    modifier = Modifier.fillMaxWidth().testTag("profile_sign_out")
                )
            }
        }
    }
}
