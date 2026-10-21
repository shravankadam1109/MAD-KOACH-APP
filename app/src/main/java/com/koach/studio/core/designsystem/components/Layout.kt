package com.koach.studio.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KoachTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val c = KoachTheme.colors
    TopAppBar(
        title = {
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = c.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, maxLines = 1)
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = c.background,
            scrolledContainerColor = c.background
        ),
        modifier = modifier
    )
}

/** Small uppercase green label that sits above headlines. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = KoachTheme.colors.brand) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = KoachTheme.spacing.gutter)
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Eyebrow(eyebrow)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = KoachTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (actionLabel != null && onAction != null) {
            KoachTextButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun KoachDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = KoachTheme.colors.outlineSubtle)
}

/** Label/value pair used in detail screens. */
@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, textAlign = TextAlign.End)
    }
}

/** Headline number with a caption, for dashboards. */
@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Tone = Tone.Brand
) {
    KoachCard(modifier = modifier) {
        if (icon != null) {
            IconTile(icon, tone = tone, size = 34.dp)
            Spacer(Modifier.height(14.dp))
        }
        Text(value, style = MaterialTheme.typography.headlineMedium, color = KoachTheme.colors.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = KoachTheme.colors.textSecondary)
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Loading") {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(color = KoachTheme.colors.brand, strokeWidth = 3.dp)
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = KoachTheme.colors.textSecondary)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = KoachTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp)
    ) {
        Box(Modifier.koachGlow(c.brand, 0.25f)) { IconTile(icon, size = 64.dp) }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            KoachPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val c = KoachTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        IconTile(Icons.Default.ErrorOutline, tone = Tone.Danger, size = 56.dp)
        Spacer(Modifier.height(14.dp))
        Text("Something went wrong", style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Spacer(Modifier.height(4.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            KoachSecondaryButton(text = "Try again", onClick = onRetry)
        }
    }
}
