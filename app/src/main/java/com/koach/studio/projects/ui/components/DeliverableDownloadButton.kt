package com.koach.studio.projects.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.koach.studio.core.designsystem.theme.KoachTheme
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.koach.studio.projects.model.Deliverable
import com.koach.studio.projects.model.DownloadState

@Composable
fun DeliverableDownloadButton(
    deliverable: Deliverable,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandGreen = KoachTheme.colors.brand
    val errorColor = KoachTheme.colors.danger

    when (deliverable.downloadState) {
        DownloadState.IDLE -> {
            IconButton(
                onClick = onDownloadClick,
                modifier = modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(KoachTheme.colors.surfaceHigh)
                    .testTag("download_button_${deliverable.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Download ${deliverable.name}",
                    tint = brandGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DownloadState.DOWNLOADING -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KoachTheme.colors.brandContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("downloading_progress_${deliverable.id}")
            ) {
                CircularProgressIndicator(
                    progress = { deliverable.downloadProgress },
                    modifier = Modifier.size(14.dp),
                    color = brandGreen,
                    strokeWidth = 2.dp,
                    trackColor = KoachTheme.colors.brandOutline
                )
                Spacer(modifier = Modifier.width(6.dp))
                val percentage = (deliverable.downloadProgress * 100).toInt()
                Text(
                    text = "$percentage%",
                    color = brandGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        DownloadState.DONE -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KoachTheme.colors.brandContainer)
                    .border(1.dp, KoachTheme.colors.brandOutline, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("download_done_${deliverable.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = brandGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Saved",
                    color = brandGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        DownloadState.FAILED -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KoachTheme.colors.danger.copy(alpha = 0.12f))
                    .border(1.dp, KoachTheme.colors.danger.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .clickable(onClick = onDownloadClick)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("download_retry_button_${deliverable.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Retry download",
                    tint = errorColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Retry",
                    color = errorColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
