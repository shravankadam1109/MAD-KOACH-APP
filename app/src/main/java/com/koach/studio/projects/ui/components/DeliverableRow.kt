package com.koach.studio.projects.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DeliverableStatus

@Composable
fun DeliverableRow(
    deliverable: Deliverable,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionSlot: @Composable (() -> Unit)? = null
) {
    val cardBg = Color(0xFF16181F)
    val cardBorder = Color(0xFF272A36)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF9EA3B0)

    val icon: ImageVector = when {
        deliverable.type.contains("Video", ignoreCase = true) -> Icons.Default.Videocam
        deliverable.type.contains("Audio", ignoreCase = true) || deliverable.type.contains("Stem", ignoreCase = true) -> Icons.Default.Audiotrack
        deliverable.type.contains("Archive", ignoreCase = true) || deliverable.type.contains("Zip", ignoreCase = true) -> Icons.Default.FolderZip
        else -> Icons.Default.Description
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEditClick)
            .testTag("deliverable_row_${deliverable.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF212530)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = deliverable.type,
                        tint = Color(0xFF41BA00),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and metadata
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deliverable.name,
                        color = textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${deliverable.type} • ${deliverable.size} • ${deliverable.version}",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Action slot (e.g. Download button) or Edit button
                if (actionSlot != null) {
                    actionSlot()
                } else {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_deliverable_button_${deliverable.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit deliverable",
                            tint = Color(0xFF9EA3B0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Notes and Status Row
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DeliverableStatusBadge(status = deliverable.status)

                if (deliverable.notes.isNotBlank()) {
                    Text(
                        text = deliverable.notes,
                        color = textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DeliverableStatusBadge(
    status: DeliverableStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (status) {
        DeliverableStatus.DRAFT -> Triple(Color(0xFF1E222B), Color(0xFF9EA3B0), Color(0xFF333847))
        DeliverableStatus.IN_REVIEW -> Triple(Color(0xFF2E2614), Color(0xFFFBBF24), Color(0xFF534320))
        DeliverableStatus.APPROVED -> Triple(Color(0xFF122E1A), Color(0xFF41BA00), Color(0xFF1E5229))
        DeliverableStatus.REVISION_REQUESTED -> Triple(Color(0xFF301618), Color(0xFFF87171), Color(0xFF5C262A))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
