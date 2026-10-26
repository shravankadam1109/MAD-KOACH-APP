package com.koach.studio.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
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
                Spacer(Modifier.height(36.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    buildAnnotatedString {
                        append("Stories, shot, cut and ")
                        withStyle(SpanStyle(color = c.brand)) { append("delivered.") }
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = c.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "One place for your projects, cuts, approvals and the crew behind them.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(36.dp))

                Text(
                    "Clients follow projects, review cuts and book the studio. The agency team runs clients, tasks and delivery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    KoachSecondaryButton(
                        text = "Agency team",
                        onClick = onTeamLogin,
                        leadingIcon = Icons.Outlined.Groups,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("enter_team")
                            .semantics { contentDescription = "Agency team sign-in" }
                    )
                    KoachPrimaryButton(
                        text = "Client",
                        onClick = onClientLogin,
                        trailingIcon = Icons.Filled.NorthEast,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("enter_client")
                            .semantics { contentDescription = "Client sign-in" }
                    )
                }
                if (isDemo) {
                    Spacer(Modifier.height(20.dp))
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
        Text("STUDIO", style = MaterialTheme.typography.labelSmall, color = c.brand, modifier = Modifier.padding(bottom = 5.dp))
    }
}
