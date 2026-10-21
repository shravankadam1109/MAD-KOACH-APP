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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

private val ButtonShape = RoundedCornerShape(KoachRadius.md)
private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)

/** Solid neon-green call to action with a restrained glow. One per screen, ideally. */
@Composable
fun KoachPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
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
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .shadow(
                elevation = if (enabled) 18.dp else 0.dp,
                shape = ButtonShape,
                ambientColor = colors.brand,
                spotColor = colors.brand
            )
    ) {
        ButtonContent(text, leadingIcon, loading, progressColor = colors.onBrand)
    }
}

/** Outlined secondary action on dark surfaces. */
@Composable
fun KoachSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    destructive: Boolean = false
) {
    val colors = KoachTheme.colors
    val accent = if (destructive) colors.danger else colors.textPrimary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = BorderStroke(1.dp, if (destructive) colors.danger.copy(alpha = 0.5f) else colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = accent,
            disabledContentColor = colors.textTertiary
        ),
        modifier = modifier.defaultMinSize(minHeight = 52.dp)
    ) {
        ButtonContent(text, leadingIcon, loading, progressColor = accent)
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
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(contentColor = KoachTheme.colors.brand)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ButtonContent(
    text: String,
    leadingIcon: ImageVector?,
    loading: Boolean,
    progressColor: androidx.compose.ui.graphics.Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = progressColor
            )
            Spacer(Modifier.width(10.dp))
        } else if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
