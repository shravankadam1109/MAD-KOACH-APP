package com.koach.studio.contact

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.SectionHeader
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme

@Composable
fun ContactScreen(
    viewModel: ContactViewModel,
    onBack: () -> Unit,
    onBookService: () -> Unit,
    onSendMessage: () -> Unit,
    onSendFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val gutter = KoachTheme.spacing.gutter
    val c = KoachTheme.colors

    LaunchedEffect(state.notificationMessage) {
        state.notificationMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.dismissNotification()
        }
    }

    KoachBackground(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            KoachTopBar(
                title = "Contact Studio",
                subtitle = "KOACH HQ & Production Desk",
                onBack = onBack,
                modifier = Modifier.statusBarsPadding()
            )

            LazyColumn(
                contentPadding = PaddingValues(start = gutter, end = gutter, top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("contact_screen")
            ) {
                item {
                    KoachCard(highlighted = true, modifier = Modifier.fillMaxWidth().testTag("agency_info_card")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill("Mumbai HQ", tone = Tone.Brand)
                            Spacer(Modifier.weight(1f))
                            Text("Full-Service Agency", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.contactInfo.studioName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = c.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            state.contactInfo.tagline,
                            style = MaterialTheme.typography.titleSmall,
                            color = c.brand
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${state.contactInfo.addressLine1}, ${state.contactInfo.addressLine2}, ${state.contactInfo.cityAndPin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KoachSecondaryButton(
                                text = "Call",
                                leadingIcon = Icons.Outlined.Phone,
                                onClick = { dialPhone(context, state.contactInfo.deskPhone) },
                                modifier = Modifier.weight(1f).testTag("contact_action_call")
                            )
                            KoachSecondaryButton(
                                text = "Email",
                                leadingIcon = Icons.Outlined.Email,
                                onClick = { sendEmail(context, state.contactInfo.generalEmail, "KOACH Studio — Project Inquiry") },
                                modifier = Modifier.weight(1f).testTag("contact_action_email")
                            )
                            KoachSecondaryButton(
                                text = "Map",
                                leadingIcon = Icons.Outlined.LocationOn,
                                onClick = {
                                    openMap(
                                        context,
                                        "${state.contactInfo.addressLine1}, ${state.contactInfo.addressLine2}, ${state.contactInfo.cityAndPin}"
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("contact_action_map")
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        KoachPrimaryButton(
                            text = "Book service",
                            leadingIcon = Icons.Outlined.VideoCall,
                            onClick = onBookService,
                            modifier = Modifier.fillMaxWidth().testTag("contact_book_button")
                        )
                    }
                }

                item {
                    SectionHeader("Studio Location", eyebrow = "Physical HQ")
                    Spacer(Modifier.height(8.dp))
                    KoachCard(modifier = Modifier.fillMaxWidth().testTag("contact_location_card")) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconTile(Icons.Outlined.LocationOn, tone = Tone.Brand, size = 42.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(state.contactInfo.studioName, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(state.contactInfo.addressLine1, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                                Text(state.contactInfo.addressLine2, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                                Text("${state.contactInfo.cityAndPin}, ${state.contactInfo.stateAndCountry}", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                            }
                            IconButton(
                                onClick = {
                                    val fullAddress = "${state.contactInfo.addressLine1}, ${state.contactInfo.addressLine2}, ${state.contactInfo.cityAndPin}"
                                    clipboardManager.setText(AnnotatedString(fullAddress))
                                    viewModel.onInfoCopied("Studio address")
                                }
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy address", tint = c.textTertiary)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        KoachSecondaryButton(
                            text = "Open in Maps",
                            leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                            onClick = {
                                openMap(
                                    context,
                                    "${state.contactInfo.addressLine1}, ${state.contactInfo.addressLine2}, ${state.contactInfo.cityAndPin}"
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("contact_map_button")
                        )
                    }
                }

                item {
                    SectionHeader("Hours & Availability", eyebrow = "Operating Schedule")
                    Spacer(Modifier.height(8.dp))
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconTile(Icons.Outlined.AccessTime, tone = Tone.Info, size = 42.dp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Post-Production Suites & Office", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                                Text(state.contactInfo.operatingHours, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                                Spacer(Modifier.height(10.dp))
                                Text("Production & Camera Crews", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                                Text(state.contactInfo.shootAvailability, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                            }
                        }
                    }
                }

                item {
                    SectionHeader("Direct Lines", eyebrow = "Hotlines & Inquiries")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ContactChannelRow(
                            icon = Icons.Outlined.Phone,
                            title = "Studio Desk (General)",
                            value = state.contactInfo.deskPhone,
                            actionLabel = "Call",
                            tone = Tone.Brand,
                            onAction = { dialPhone(context, state.contactInfo.deskPhone) },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(state.contactInfo.deskPhone))
                                viewModel.onInfoCopied("Desk phone")
                            }
                        )
                        ContactChannelRow(
                            icon = Icons.Outlined.Phone,
                            title = "Urgent Production Hotline",
                            value = state.contactInfo.productionHotline,
                            actionLabel = "Call",
                            tone = Tone.Warning,
                            onAction = { dialPhone(context, state.contactInfo.productionHotline) },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(state.contactInfo.productionHotline))
                                viewModel.onInfoCopied("Hotline")
                            }
                        )
                        ContactChannelRow(
                            icon = Icons.Outlined.Email,
                            title = "General & Press Email",
                            value = state.contactInfo.generalEmail,
                            actionLabel = "Email",
                            tone = Tone.Review,
                            onAction = { sendEmail(context, state.contactInfo.generalEmail, "KOACH Studio — General Inquiry") },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(state.contactInfo.generalEmail))
                                viewModel.onInfoCopied("Email")
                            }
                        )
                        ContactChannelRow(
                            icon = Icons.Outlined.Email,
                            title = "Bookings & Production Desk",
                            value = state.contactInfo.bookingsEmail,
                            actionLabel = "Email",
                            tone = Tone.Brand,
                            onAction = { sendEmail(context, state.contactInfo.bookingsEmail, "KOACH Studio — Project Production Inquiry") },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(state.contactInfo.bookingsEmail))
                                viewModel.onInfoCopied("Production email")
                            }
                        )
                    }
                }

                item {
                    SectionHeader("Client Fast Channels", eyebrow = "In-App Actions")
                    Spacer(Modifier.height(8.dp))
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Already working with us on an active project?",
                            style = MaterialTheme.typography.titleSmall,
                            color = c.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Message your assigned director, editor and producer directly within your project's conversation thread.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KoachSecondaryButton(
                                text = "Project messages",
                                leadingIcon = Icons.AutoMirrored.Outlined.Chat,
                                onClick = onSendMessage,
                                modifier = Modifier.weight(1f).testTag("contact_messages_button")
                            )
                            KoachSecondaryButton(
                                text = "Send feedback",
                                leadingIcon = Icons.Outlined.RateReview,
                                onClick = onSendFeedback,
                                modifier = Modifier.weight(1f).testTag("contact_feedback_button")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactChannelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    actionLabel: String,
    tone: Tone,
    onAction: () -> Unit,
    onCopy: () -> Unit
) {
    val c = KoachTheme.colors
    KoachCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, tone = tone, size = 38.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                Text(value, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
            }
            IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy $title", tint = c.textTertiary)
            }
            Spacer(Modifier.width(4.dp))
            KoachSecondaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

private fun dialPhone(context: Context, phone: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open dialer for $phone", Toast.LENGTH_SHORT).show()
    }
}

private fun sendEmail(context: Context, email: String, subject: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Cannot open email client for $email", Toast.LENGTH_SHORT).show()
    }
}

private fun openMap(context: Context, query: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to web browser
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${Uri.encode(query)}"))
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Cannot open maps location", Toast.LENGTH_SHORT).show()
        }
    }
}
