package com.koach.studio.projects.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.ui.icon
import com.koach.studio.projects.ui.tone

private val Deliverable.icon: ImageVector
    get() = when {
        type.contains("Video", ignoreCase = true) -> Icons.Default.Videocam
        type.contains("Audio", ignoreCase = true) || type.contains("Stem", ignoreCase = true) -> Icons.Default.Audiotrack
        type.contains("Archive", ignoreCase = true) || type.contains("Zip", ignoreCase = true) -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }

/**
 * One deliverable with its version, size, status and download control.
 * Actions are role-driven: pass [onEditClick] for agency users, and
 * [onApprove] / [onRequestChanges] for the client while it is in review.
 */
@Composable
fun DeliverableRow(
    deliverable: Deliverable,
    onEditClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onDownloadClick: (() -> Unit)? = null,
    onApprove: (() -> Unit)? = null,
    onRequestChanges: (() -> Unit)? = null
) {
    val c = KoachTheme.colors
    val inClientReview = deliverable.status == DeliverableStatus.IN_REVIEW && (onApprove != null || onRequestChanges != null)

    KoachCard(
        highlighted = inClientReview,
        onClick = onEditClick,
        contentPadding = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("deliverable_row_${deliverable.id}")
            .semantics(mergeDescendants = true) {
                contentDescription = "${deliverable.name}, version ${deliverable.version}, ${deliverable.type}, ${deliverable.size}, status ${deliverable.status.displayName}"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(deliverable.icon, tone = deliverable.status.tone.takeIf { it != Tone.Neutral } ?: Tone.Brand, size = 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(deliverable.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${deliverable.version} · ${deliverable.type} · ${deliverable.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onDownloadClick != null) {
                Spacer(Modifier.width(8.dp))
                DeliverableDownloadButton(deliverable = deliverable, onDownloadClick = onDownloadClick)
            }
            if (onEditClick != null) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("edit_deliverable_button_${deliverable.id}")
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit ${deliverable.name}",
                        tint = c.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (deliverable.notes.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(deliverable.notes, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            DeliverableStatusBadge(deliverable.status)
            if (deliverable.previousVersions.isNotEmpty()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    "Earlier: ${deliverable.previousVersions.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textTertiary
                )
            }
        }

        if (inClientReview) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onRequestChanges != null) {
                    KoachSecondaryButton(
                        text = "Request changes",
                        onClick = onRequestChanges,
                        leadingIcon = Icons.Outlined.RateReview,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("request_changes_${deliverable.id}")
                    )
                }
                if (onApprove != null) {
                    KoachPrimaryButton(
                        text = "Approve",
                        onClick = onApprove,
                        leadingIcon = Icons.Outlined.CheckCircle,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_${deliverable.id}")
                    )
                }
            }
        }
    }
}

@Composable
fun DeliverableStatusBadge(status: DeliverableStatus, modifier: Modifier = Modifier) {
    AnimatedStatusPill(
        text = status.displayName,
        tone = status.tone,
        icon = status.icon,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Deliverable status: ${status.displayName}"
        }
    )
}
