package com.koach.studio.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.koach.studio.R
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.designsystem.theme.rememberReducedMotion

/** Real KOACH client work (from the studio's Figma file), used as brand imagery. */
object KoachArtwork {
    val posts: List<Int> = listOf(
        R.drawable.portfolio_peeyo_1,
        R.drawable.portfolio_endura_1,
        R.drawable.portfolio_boba_bob_1,
        R.drawable.portfolio_picklx_1,
        R.drawable.portfolio_batpro_1,
        R.drawable.portfolio_peeyo_2,
        R.drawable.portfolio_pitaara_1,
        R.drawable.portfolio_hydrotech_1
    )
}

/**
 * Cinematic wall of KOACH work: three tilted columns of portfolio posts drifting
 * slowly in opposite directions, dimmed and fading into the background so text
 * laid over the lower edge stays readable. Decorative, so it's hidden from
 * accessibility services; motion holds still under reduced-motion settings.
 */
@Composable
fun PortfolioMosaic(modifier: Modifier = Modifier, dim: Float = 0.52f) {
    val c = KoachTheme.colors
    val reduced = rememberReducedMotion()
    val drift = if (reduced) null else rememberInfiniteTransition(label = "mosaic").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "mosaic-drift"
    )
    BoxWithConstraints(modifier.clipToBounds()) {
        val columnWidth = maxWidth * 0.46f
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .requiredWidth(maxWidth * 1.5f)
                .align(Alignment.Center)
                .graphicsLayer { rotationZ = -9f }
        ) {
            val posts = KoachArtwork.posts
            listOf(posts.take(3), posts.drop(3).take(3), posts.drop(5) + posts.take(1)).forEachIndexed { i, column ->
                MosaicColumn(
                    images = column,
                    modifier = Modifier
                        .requiredWidth(columnWidth)
                        .wrapContentHeight(unbounded = true)
                        .graphicsLayer {
                            val t = drift?.value ?: 0.5f
                            val direction = if (i % 2 == 0) 1f else -1f
                            translationY = (if (i == 1) -120f else 60f) + direction * (t - 0.5f) * 220f
                        }
                )
            }
        }
        // Dim the artwork, then fade it into the page so overlaid text has a calm base.
        Box(Modifier.fillMaxSize().background(c.scrim.copy(alpha = dim)))
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to c.background.copy(alpha = 0.55f),
                    0.25f to Color.Transparent,
                    0.62f to c.background.copy(alpha = 0.35f),
                    1f to c.background
                )
            )
        )
        Box(Modifier.fillMaxSize().cornerGlow(c.glow, 0.18f))
    }
}

@Composable
private fun MosaicColumn(images: List<Int>, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        images.forEach { MosaicTile(it) }
    }
}

@Composable
private fun MosaicTile(@DrawableRes image: Int) {
    Image(
        painter = painterResource(image),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(18.dp))
    )
}
