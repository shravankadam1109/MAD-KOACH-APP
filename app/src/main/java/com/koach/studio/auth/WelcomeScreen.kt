package com.koach.studio.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme

/** First screen: the brand, then two clearly separate ways in. */
@Composable
fun WelcomeScreen(
    isDemo: Boolean,
    onClientLogin: () -> Unit,
    onTeamLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    KoachBackground(modifier.fillMaxSize()) {
      // Fill at least the screen so the entry cards sit at the bottom, but scroll on small screens.
      BoxWithConstraints(Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = KoachTheme.spacing.gutter, vertical = 24.dp)
                .testTag("welcome_screen")
        ) {
            Wordmark()
            Spacer(Modifier.height(64.dp))
            Text(
                buildAnnotatedString {
                    append("Stories, shot, cut and ")
                    withStyle(SpanStyle(color = c.brand)) { append("delivered.") }
                },
                style = MaterialTheme.typography.displayLarge,
                color = c.textPrimary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "One place for your projects, cuts, approvals and the crew behind them.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textSecondary
            )
            Spacer(Modifier.height(48.dp))
            Spacer(Modifier.weight(1f))

            EntryCard(
                icon = Icons.Outlined.Movie,
                title = "I'm a client",
                subtitle = "Follow your projects, review deliverables and book the studio.",
                highlighted = true,
                tone = Tone.Brand,
                onClick = onClientLogin,
                tag = "enter_client"
            )
            Spacer(Modifier.height(12.dp))
            EntryCard(
                icon = Icons.Outlined.Groups,
                title = "Agency team",
                subtitle = "Run clients, projects, tasks and delivery for the studio.",
                highlighted = false,
                tone = Tone.Info,
                onClick = onTeamLogin,
                tag = "enter_team"
            )

            if (isDemo) {
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DemoTag(text = "Demo mode")
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sample accounts and data. Nothing is sent to a server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary
                    )
                }
            }
        }
      }
    }
}

@Composable
internal fun Wordmark(modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "KOACH Studio" }) {
        Text("KOACH", style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
        Spacer(Modifier.width(8.dp))
        Text("STUDIO", style = MaterialTheme.typography.labelSmall, color = c.brand, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
private fun EntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    tone: Tone,
    onClick: () -> Unit,
    tag: String
) {
    val c = KoachTheme.colors
    KoachCard(
        onClick = onClick,
        highlighted = highlighted,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "$title. $subtitle"
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconTile(icon, tone = tone, size = 48.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (highlighted) c.brand else c.textTertiary)
        }
    }
}
