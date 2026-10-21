package com.koach.studio.app.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.koach.studio.app.navigation.Routes
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User

data class MenuEntry(val route: String, val title: String, val subtitle: String, val icon: ImageVector)

/** Menu entries a user can reach from the Account (client) or More (agency) tab. */
fun menuEntriesFor(user: User): List<MenuEntry> = buildList {
    if (user.role.isAgency) add(MenuEntry(Routes.MESSAGES, "Messages", "Project conversations", Icons.Outlined.ChatBubbleOutline))
    add(MenuEntry(Routes.NOTIFICATIONS, "Notifications", "Updates across your projects", Icons.Outlined.Notifications))
    add(
        MenuEntry(
            Routes.BOOKINGS,
            if (user.role.isAgency) "Booking inbox" else "My bookings",
            if (user.role.isAgency) "Requests from clients and the website" else "Requests and confirmations",
            Icons.AutoMirrored.Outlined.EventNote
        )
    )
    add(MenuEntry(Routes.DOCUMENTS, "Documents", "Briefs, contracts and assets", Icons.Outlined.FolderOpen))
    if (!user.role.isAgency || user.can(Permission.VIEW_ALL_PAYMENTS)) {
        add(MenuEntry(Routes.PAYMENTS, if (user.role.isAgency) "Payments" else "Invoices & payments", "Simulated payment records", Icons.AutoMirrored.Outlined.ReceiptLong))
    }
    add(
        MenuEntry(
            Routes.FEEDBACK,
            if (user.role.isAgency) "Feedback inbox" else "Feedback & support",
            if (user.role.isAgency) "Client feedback and complaints" else "Share feedback or raise an issue",
            Icons.Outlined.RateReview
        )
    )
    add(MenuEntry(Routes.PROFILE, "Profile", "Your details", Icons.Outlined.Person))
}

@Composable
fun MenuScreen(
    user: User,
    onNavigate: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    KoachBackground(modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = gutter, end = gutter, top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("menu_screen")
        ) {
            item {
                Eyebrow(if (user.role.isAgency) "Studio" else "Your account")
                Spacer(Modifier.height(6.dp))
                Text(if (user.role.isAgency) "More" else "Account", style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
                Spacer(Modifier.height(20.dp))
                KoachCard(highlighted = true, onClick = { onNavigate(Routes.PROFILE) }, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KoachAvatar(user.initials, size = 56.dp, highlighted = true)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                            Text(user.title, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                            Spacer(Modifier.height(8.dp))
                            StatusPill(user.role.displayName, tone = if (user.role == Role.CLIENT) Tone.Info else Tone.Brand)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            items(menuEntriesFor(user), key = { it.route }) { entry ->
                MenuRow(entry, onClick = { onNavigate(entry.route) })
            }
            item {
                Spacer(Modifier.height(14.dp))
                KoachSecondaryButton(
                    text = "Sign out",
                    onClick = onSignOut,
                    leadingIcon = Icons.AutoMirrored.Filled.Logout,
                    destructive = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                )
            }
        }
    }
}

@Composable
private fun MenuRow(entry: MenuEntry, onClick: () -> Unit) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_${entry.route}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(entry.icon, size = 38.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

/** Temporary stand-in for destinations that are wired up in later commits. */
@Composable
fun PendingFeatureScreen(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    KoachBackground(modifier.fillMaxSize()) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(KoachTheme.spacing.gutter)
        ) {
            KoachSecondaryButton(text = "Back", onClick = onBack)
            Spacer(Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = c.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text("This section is being built.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }
    }
}
