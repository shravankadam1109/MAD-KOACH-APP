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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.projects.model.Project
import com.koach.studio.projects.model.ProjectStatus
import com.koach.studio.projects.model.ServiceType

@Composable
fun ProjectListItem(
    project: Project,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF16181F)
    val cardBorder = Color(0xFF272A36)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF9EA3B0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("project_item_${project.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardBorder))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Badges row: Service type and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ServiceTypeBadge(serviceType = project.serviceType)
                ProjectStatusBadge(status = project.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Project Title & Client
            Text(
                text = project.title,
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = project.clientName,
                color = textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer metadata row: Due date and Deliverables count + Arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Due Date",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Due: ${project.dueDate}",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Deliverables",
                            tint = textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.deliverables.size} Files",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "Open project details",
                    tint = textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun ServiceTypeBadge(
    serviceType: ServiceType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (serviceType) {
        ServiceType.SHOOTING -> Color(0xFF1E2838) to Color(0xFF64B5F6)
        ServiceType.RECORDING -> Color(0xFF33213A) to Color(0xFFCE93D8)
        ServiceType.EDITING -> Color(0xFF332B1C) to Color(0xFFFFB74D)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = serviceType.displayName.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ProjectStatusBadge(
    status: ProjectStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (status) {
        ProjectStatus.IN_PROGRESS -> Triple(Color(0xFF10281F), Color(0xFF4ADE80), Color(0xFF1B4D3E))
        ProjectStatus.IN_REVIEW -> Triple(Color(0xFF2E2614), Color(0xFFFBBF24), Color(0xFF534320))
        ProjectStatus.REVISION_REQUESTED -> Triple(Color(0xFF301618), Color(0xFFF87171), Color(0xFF5C262A))
        ProjectStatus.COMPLETED -> Triple(Color(0xFF122E1A), Color(0xFF41BA00), Color(0xFF1E5229))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
