package com.koach.studio.catalog.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.model.ServiceCategory

val ServiceCategory.icon: ImageVector
    get() = when (this) {
        ServiceCategory.VIDEO_EDITING -> Icons.Outlined.ContentCut
        ServiceCategory.CONTENT_CREATION -> Icons.Outlined.AutoAwesome
        ServiceCategory.SOCIAL_MEDIA -> Icons.Outlined.Campaign
        ServiceCategory.BRANDING -> Icons.Outlined.Palette
        ServiceCategory.FILMING -> Icons.Outlined.Videocam
        ServiceCategory.MOTION_GRAPHICS -> Icons.Outlined.Animation
        ServiceCategory.VFX -> Icons.Outlined.AutoFixHigh
    }

/** Green leads; a few categories get a secondary accent so the grid isn't monotone. */
val ServiceCategory.tone: Tone
    get() = when (this) {
        ServiceCategory.SOCIAL_MEDIA, ServiceCategory.MOTION_GRAPHICS -> Tone.Info
        ServiceCategory.BRANDING, ServiceCategory.VFX -> Tone.Review
        else -> Tone.Brand
    }
