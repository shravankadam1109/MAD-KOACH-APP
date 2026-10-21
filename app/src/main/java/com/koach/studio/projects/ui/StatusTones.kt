package com.koach.studio.projects.ui

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

val DeliverableStatus.tone: Tone
    get() = when (this) {
        DeliverableStatus.DRAFT -> Tone.Neutral
        DeliverableStatus.IN_REVIEW -> Tone.Review
        DeliverableStatus.APPROVED -> Tone.Success
        DeliverableStatus.REVISION_REQUESTED -> Tone.Warning
    }
