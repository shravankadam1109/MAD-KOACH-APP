package com.koach.studio.core.designsystem.components

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

/** Standardised button geometry: sleek 48dp accessible height with refined rounded corners. */
private val ButtonShape = RoundedCornerShape(KoachRadius.md)
private val ButtonPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
private val ButtonMinHeight = 48.dp

/** Solid KOACH-green call to action without excessive drop-shadow glow. */
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
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.brand,
            contentColor = colors.onBrand,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = ButtonMinHeight)
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
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = BorderStroke(
            width = 1.dp,
            color = if (destructive) colors.danger.copy(alpha = 0.4f) else colors.outline
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (destructive) colors.danger.copy(alpha = 0.08f) else colors.surfaceRaised,
            contentColor = accent,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = ButtonMinHeight)
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
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (filled) colors.brand else colors.surfaceRaised,
            contentColor = if (filled) colors.onBrand else colors.textPrimary,
            disabledContainerColor = colors.surfaceHigh,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier.size(48.dp)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
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
        if (trailingIcon != null && !loading) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
