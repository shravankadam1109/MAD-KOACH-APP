package com.koach.studio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.koach.studio.BuildConfig
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
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Role as UserRole
import com.koach.studio.core.model.User
import com.koach.studio.core.settings.AppPreferences
import com.koach.studio.core.settings.ThemeMode
import com.koach.studio.core.settings.ThemePreferencesStore
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(
    private val store: ThemePreferencesStore
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = store.preferences

    fun setThemeMode(mode: ThemeMode) = store.setThemeMode(mode)
    fun setProjectNotifications(enabled: Boolean) = store.setProjectNotifications(enabled)
    fun setDeadlineReminders(enabled: Boolean) = store.setDeadlineReminders(enabled)
    fun setShareCrashDiagnostics(enabled: Boolean) = store.setShareCrashDiagnostics(enabled)
}

@Composable
fun SettingsScreen(
    user: User,
    isDemo: Boolean,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenContact: () -> Unit,
    onOpenFeedback: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferences.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    Scaffold(
        containerColor = c.background,
        topBar = {
            KoachTopBar(
                title = "Settings",
                subtitle = "Appearance, notifications, account & about",
                onBack = onBack
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = 40.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_screen")
        ) {
            // 1. Appearance section
            item {
                KoachCard(modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Appearance")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Theme preference",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Switch between KOACH Studio's signature dark look, light mode, or your device setting. Saved across restarts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemeOptionRow(
                                mode = mode,
                                selected = prefs.themeMode == mode,
                                onSelect = { viewModel.setThemeMode(mode) }
                            )
                        }
                    }
                }
            }

            // 2. Notifications & Privacy section
            item {
                KoachCard(modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Notifications & Privacy", color = c.textTertiary)
                    Spacer(Modifier.height(6.dp))
                    SettingsToggleRow(
                        icon = Icons.Outlined.NotificationsActive,
                        tone = Tone.Brand,
                        title = "Project & deliverable updates",
                        subtitle = "In-app alerts when cuts, revisions or messages arrive",
                        checked = prefs.projectNotificationsEnabled,
                        onCheckedChange = viewModel::setProjectNotifications,
                        tag = "settings_project_notifications"
                    )
                    KoachDivider()
                    SettingsToggleRow(
                        icon = Icons.Outlined.Schedule,
                        tone = Tone.Warning,
                        title = "Deadline & milestone reminders",
                        subtitle = "Highlight tasks and milestones due within 48 hours",
                        checked = prefs.deadlineRemindersEnabled,
                        onCheckedChange = viewModel::setDeadlineReminders,
                        tag = "settings_deadline_reminders"
                    )
                    KoachDivider()
                    SettingsToggleRow(
                        icon = Icons.Outlined.PrivacyTip,
                        tone = Tone.Info,
                        title = "Share anonymous diagnostics",
                        subtitle = "Help improve stability without sharing project or client content",
                        checked = prefs.shareCrashDiagnostics,
                        onCheckedChange = viewModel::setShareCrashDiagnostics,
                        tag = "settings_crash_diagnostics"
                    )
                }
            }

            // 3. Account & Session section
            item {
                KoachCard(modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Account & Session", color = c.textTertiary)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KoachAvatar(user.initials, size = 46.dp, highlighted = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                            Text(user.email, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                        }
                        StatusPill(
                            text = user.role.displayName,
                            tone = if (user.role == UserRole.CLIENT) Tone.Info else Tone.Brand
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    KoachDivider()
                    SettingsActionRow(
                        icon = Icons.Outlined.Person,
                        tone = Tone.Brand,
                        title = "Profile & permissions",
                        subtitle = "View role details, organisation and access level",
                        onClick = onOpenProfile,
                        tag = "settings_open_profile"
                    )
                }
            }

            // 4. Support & Help section
            item {
                KoachCard(modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("Support & Help", color = c.textTertiary)
                    Spacer(Modifier.height(6.dp))
                    SettingsActionRow(
                        icon = Icons.Outlined.Phone,
                        tone = Tone.Brand,
                        title = "Contact KOACH Studio desk",
                        subtitle = "Bandra West HQ address, production hotline & direct email",
                        onClick = onOpenContact,
                        tag = "settings_open_contact"
                    )
                    KoachDivider()
                    SettingsActionRow(
                        icon = Icons.Outlined.RateReview,
                        tone = Tone.Review,
                        title = if (user.role.isAgency) "Client feedback inbox" else "Send feedback or report an issue",
                        subtitle = if (user.role.isAgency) "Review and respond to client submissions" else "Share project feedback or raise a support ticket",
                        onClick = onOpenFeedback,
                        tag = "settings_open_feedback"
                    )
                    KoachDivider()
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                    ) {
                        IconTile(Icons.Outlined.HelpOutline, tone = Tone.Info, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("How reviews & approvals work", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Clients can approve deliverables or request changes with revision notes. The studio team updates project milestones, tasks, and new cut versions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary
                            )
                        }
                    }
                }
            }

            // 5. About the App & Version Info
            item {
                KoachCard(modifier = Modifier.fillMaxWidth()) {
                    Eyebrow("About KOACH Studio", color = c.textTertiary)
                    Spacer(Modifier.height(6.dp))
                    InfoRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", icon = Icons.Outlined.Info)
                    KoachDivider()
                    InfoRow("Application ID", BuildConfig.APPLICATION_ID, icon = Icons.Outlined.VerifiedUser)
                    KoachDivider()
                    InfoRow(
                        label = "Backend mode",
                        value = if (isDemo) "Demo (device in-memory)" else "Firebase Auth & Firestore"
                    )
                    KoachDivider()
                    InfoRow("Active theme", prefs.themeMode.displayName)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (isDemo) {
                            "Demo build note: Portfolio entries display real KOACH Studio social media artwork. Deliverable downloads, document file storage, and invoice payments are simulated locally."
                        } else {
                            "Connected to Firebase Auth & Cloud Firestore. Deliverable file transfers and invoice payments remain simulated in this release."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary
                    )
                }
            }

            // Sign-out button
            item {
                KoachSecondaryButton(
                    text = "Sign out of ${user.firstName}'s session",
                    onClick = onSignOut,
                    leadingIcon = Icons.AutoMirrored.Filled.Logout,
                    destructive = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_out")
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val c = KoachTheme.colors
    val icon: ImageVector = when (mode) {
        ThemeMode.DARK -> Icons.Outlined.DarkMode
        ThemeMode.LIGHT -> Icons.Outlined.LightMode
        ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
    }
    val shape = RoundedCornerShape(KoachRadius.md)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.brandContainer else c.surfaceRaised)
            .border(1.dp, if (selected) c.brand else c.outlineSubtle, shape)
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("theme_mode_${mode.name}")
    ) {
        IconTile(icon, tone = if (selected) Tone.Brand else Tone.Neutral, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                mode.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = c.textPrimary
            )
            Text(
                mode.description,
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) c.brand else c.outline, CircleShape)
                .semantics { contentDescription = if (selected) "Selected" else "Not selected" }
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(c.brand)
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    tone: Tone,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KoachRadius.sm))
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(vertical = 10.dp)
            .testTag(tag)
    ) {
        IconTile(icon, tone = tone, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onBrand,
                checkedTrackColor = c.brand,
                uncheckedThumbColor = c.textSecondary,
                uncheckedTrackColor = c.surfaceHigh,
                uncheckedBorderColor = c.outline
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    tone: Tone,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tag: String
) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KoachRadius.sm))
            .selectable(selected = false, onClick = onClick, role = Role.Button)
            .padding(vertical = 10.dp)
            .testTag(tag)
    ) {
        IconTile(icon, tone = tone, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = c.textTertiary)
    }
}
