package com.koach.studio.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.pressScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

/** Standardised button geometry: pill-shaped, 50dp tall — comfortable to hit without dominating. */
private val ButtonShape = RoundedCornerShape(50)
private val ButtonPadding = PaddingValues(horizontal = 22.dp, vertical = 13.dp)
private val ButtonMinHeight = 50.dp

/**
 * The screen's main action: solid KOACH green with a soft pool of green light
 * beneath it. It settles under the finger and cross-fades into its loading state.
 */
@Composable
fun KoachPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val colors = KoachTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val lit = enabled && !loading
    val glow by animateFloatAsState(if (lit) 1f else 0f, KoachMotion.enter(KoachMotion.Medium), label = "glow")
    Button(
        onClick = onClick,
        enabled = lit,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        interactionSource = interaction,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.brand,
            contentColor = colors.onBrand,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier
            .pressScale(interaction)
            .drawBehind {
                if (glow > 0f) {
                    drawRoundRect(
                        Brush.radialGradient(
                            listOf(colors.glow.copy(alpha = 0.32f * glow), Color.Transparent),
                            center = Offset(size.width / 2f, size.height * 0.9f),
                            radius = size.width * 0.6f
                        ),
                        topLeft = Offset(-size.width * 0.1f, size.height * 0.2f),
                        size = Size(size.width * 1.2f, size.height * 1.1f),
                        cornerRadius = CornerRadius(size.height)
                    )
                }
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = ButtonMinHeight)
    ) {
        ButtonContent(text, leadingIcon, loading, progressColor = colors.onBrand, trailingIcon = trailingIcon)
    }
}

/** Secondary action button using solid elevated surface and theme-aware borders. */
@Composable
fun KoachSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false,
    trailingIcon: ImageVector? = null
) {
    val colors = KoachTheme.colors
    val accent = if (destructive) colors.danger else colors.textPrimary
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        interactionSource = interaction,
        border = BorderStroke(
            width = 1.dp,
            color = if (destructive) colors.danger.copy(alpha = 0.4f) else colors.glassHighlight.copy(alpha = 0.14f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (destructive) colors.danger.copy(alpha = 0.08f) else colors.glassRaised,
            contentColor = accent,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier
            .pressScale(interaction)
            .defaultMinSize(minWidth = 48.dp, minHeight = ButtonMinHeight)
    ) {
        ButtonContent(text, leadingIcon, loading, progressColor = accent, trailingIcon = trailingIcon)
    }
}

@Composable
fun KoachTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = KoachTheme.colors.brand)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Round icon-only action, e.g. play, send or next. Always pass a [contentDescription]. */
@Composable
fun KoachCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true
) {
    val colors = KoachTheme.colors
    val interaction = remember { MutableInteractionSource() }
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(50),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (filled) colors.brand else colors.glassRaised,
            contentColor = if (filled) colors.onBrand else colors.textPrimary,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier.pressScale(interaction, 0.92f).size(48.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    loading: Boolean,
    progressColor: Color,
    trailingIcon: ImageVector? = null
) {
    AnimatedContent(
        targetState = loading,
        transitionSpec = { fadeIn(KoachMotion.enter(KoachMotion.Medium)) togetherWith fadeOut(KoachMotion.exit()) },
        label = "button-content"
    ) { isLoading ->
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = progressColor
            )
            Spacer(Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailingIcon != null && !isLoading) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
    }
}
