package com.koach.studio.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.PortfolioMosaic
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.designsystem.theme.entrance

/**
 * First screen: a cinematic wall of real KOACH work fills the top of the
 * screen, and the brand statement and two ways in rise over its lower edge.
 */
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
            PortfolioMosaic(Modifier.fillMaxWidth().height(maxHeight * 0.62f))
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = minHeight)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = KoachTheme.spacing.xl, vertical = KoachTheme.spacing.xl)
                    .testTag("welcome_screen")
            ) {
                Wordmark(Modifier.entrance(0))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.height(minHeight * 0.18f))
                Eyebrow("Creative studio", modifier = Modifier.entrance(1))
                Spacer(Modifier.height(12.dp))
                Text(
                    buildAnnotatedString {
                        append("Stories, shot, cut and ")
                        withStyle(SpanStyle(color = c.brand)) { append("delivered.") }
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = c.textPrimary,
                    modifier = Modifier.entrance(2)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "One place for your projects, cuts, approvals and the crew behind them.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textSecondary,
                    modifier = Modifier.entrance(3)
                )
                Spacer(Modifier.height(32.dp))
                Column(Modifier.entrance(4)) {
                    KoachPrimaryButton(
                        text = "I'm a client",
                        onClick = onClientLogin,
                        trailingIcon = Icons.Filled.NorthEast,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enter_client")
                            .semantics { contentDescription = "Client sign-in" }
                    )
                    Spacer(Modifier.height(12.dp))
                    KoachSecondaryButton(
                        text = "Agency team",
                        onClick = onTeamLogin,
                        leadingIcon = Icons.Outlined.Groups,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("enter_team")
                            .semantics { contentDescription = "Agency team sign-in" }
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Clients follow projects, review cuts and book the studio. The agency team runs clients, tasks and delivery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary
                    )
                }
                if (isDemo) {
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.entrance(5)) {
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

/**
 * Stand-in brand mark. The original KOACH logo lives in the studio's Figma file
 * but couldn't be exported (Figma MCP plan limit); swap this for the logo
 * drawable once it is imported, keeping the "KOACH Studio" description.
 */
@Composable
internal fun Wordmark(modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Start, modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "KOACH Studio" }) {
        Text("KOACH", style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.padding(bottom = 5.dp)) {
            Text("STUDIO", style = MaterialTheme.typography.labelSmall, color = c.brand)
        }
    }
}
