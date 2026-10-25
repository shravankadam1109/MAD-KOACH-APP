package com.koach.studio.projects.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.projects.model.DeliverableStatus
import com.koach.studio.projects.model.ProjectStatus

val ProjectStatus.tone: Tone
    get() = when (this) {
        ProjectStatus.IN_PROGRESS -> Tone.Info
        ProjectStatus.IN_REVIEW -> Tone.Review
        ProjectStatus.REVISION_REQUESTED -> Tone.Warning
        ProjectStatus.COMPLETED -> Tone.Success
    }

val ProjectStatus.icon: ImageVector
    get() = when (this) {
        ProjectStatus.IN_PROGRESS -> Icons.Default.PlayArrow
        ProjectStatus.IN_REVIEW -> Icons.Default.Schedule
        ProjectStatus.REVISION_REQUESTED -> Icons.Default.Warning
        ProjectStatus.COMPLETED -> Icons.Default.CheckCircle
    }

val DeliverableStatus.tone: Tone
    get() = when (this) {
        DeliverableStatus.DRAFT -> Tone.Neutral
        DeliverableStatus.IN_REVIEW -> Tone.Review
        DeliverableStatus.APPROVED -> Tone.Success
        DeliverableStatus.REVISION_REQUESTED -> Tone.Warning
    }

val DeliverableStatus.icon: ImageVector
    get() = when (this) {
        DeliverableStatus.DRAFT -> Icons.Default.Edit
        DeliverableStatus.IN_REVIEW -> Icons.Default.Schedule
        DeliverableStatus.APPROVED -> Icons.Default.CheckCircle
        DeliverableStatus.REVISION_REQUESTED -> Icons.Default.Warning
    }
