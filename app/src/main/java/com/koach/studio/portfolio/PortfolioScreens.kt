package com.koach.studio.portfolio

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.koach.studio.catalog.ui.icon
import com.koach.studio.catalog.ui.tone
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.PortfolioItem
import com.koach.studio.core.model.ServiceCategory

/** Two-column portfolio grid, embedded in the Explore tab's LazyColumn. */
fun LazyListScope.portfolioGrid(
    state: PortfolioUiState,
    onSelectCategory: (ServiceCategory?) -> Unit,
    onOpenItem: (String) -> Unit
) {
    item {
        KoachFilterChips(
            options = state.categories,
            selected = state.selectedCategory,
            onSelected = onSelectCategory,
            label = { it.displayName }
        )
    }
    item {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = KoachTheme.spacing.gutter)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = KoachTheme.colors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (state.items.any { it.isSample }) {
                    "Sample projects that show our range. Not real client work."
                } else {
                    "Selected social media work by KOACH Studio."
                },
                style = MaterialTheme.typography.bodySmall,
                color = KoachTheme.colors.textTertiary
            )
        }
    }
    if (state.items.isEmpty()) {
        item { EmptyState(Icons.Outlined.Info, "Nothing here yet", "No work in this category yet.") }
    }
    items(state.items.chunked(2), key = { row -> row.first().id }) { row ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = KoachTheme.spacing.gutter)
        ) {
            row.forEach { item ->
                PortfolioTile(item, onClick = { onOpenItem(item.id) }, modifier = Modifier.weight(1f))
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PortfolioTile(item: PortfolioItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    val cardShape = RoundedCornerShape(KoachRadius.lg)
    Column(
        modifier
            .clip(cardShape)
            .clickable(
                onClickLabel = if (item.isSample) "View ${item.title} sample project" else "View ${item.title} project",
                onClick = onClick
            )
            .testTag("portfolio_${item.id}")
    ) {
        PortfolioCover(item, aspect = 0.8f, iconSize = 48.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.categoryAndYear,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
    }
}

private val PortfolioItem.categoryAndYear: String
    get() = listOfNotNull(category.displayName, year?.toString()).joinToString(" · ")

/**
 * The piece's artwork when it has some; otherwise generated cover art (a tinted
 * gradient, light streaks and the category glyph) standing in for real stills.
 */
@Composable
fun PortfolioCover(item: PortfolioItem, aspect: Float, iconSize: Dp, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    val coverShape = RoundedCornerShape(KoachRadius.lg)
    if (item.cover != null) {
        Image(
            painter = painterResource(item.cover),
            contentDescription = "${item.title} artwork",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(coverShape)
                .border(1.dp, c.borderSubtle, coverShape)
        )
        return
    }
    val accent = item.category.tone.color()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .clip(coverShape)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.25f), c.surfaceRaised, c.background)))
            .border(1.dp, c.borderSubtle, coverShape)
            .drawBehind {
                val step = size.width / 5f
                for (i in 0..6) {
                    drawLine(
                        color = accent.copy(alpha = 0.06f),
                        start = Offset(i * step - size.height * 0.3f, size.height),
                        end = Offset(i * step + size.height * 0.4f, 0f),
                        strokeWidth = 14f
                    )
                }
            }
            .semantics { contentDescription = "${item.category.displayName} sample cover" }
    ) {
        Icon(
            item.category.icon,
            contentDescription = null,
            tint = accent.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .size(iconSize)
        )
        DemoTag(text = "Sample", modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
        if (item.year != null) {
            Text(
                item.year.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp).offset(y = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PortfolioDetailScreen(
    viewModel: PortfolioDetailViewModel,
    onBack: () -> Unit,
    onStartSimilar: (serviceId: String) -> Unit,
    onBookSimilar: (serviceId: String) -> Unit = onStartSimilar,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val item = state.item

    Scaffold(
        containerColor = c.background,
        topBar = {
            KoachTopBar(
                title = item?.title.orEmpty(),
                subtitle = if (item?.isSample == false) "Client work" else "Sample project",
                onBack = onBack
            )
        },
        bottomBar = {
            val serviceId = state.relatedServiceId
            if (serviceId != null) {
                Column(
                    Modifier
                        .background(c.background)
                        .navigationBarsPadding()
                        .padding(horizontal = gutter, vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        KoachSecondaryButton(
                            text = "Explore service",
                            onClick = { onStartSimilar(serviceId) },
                            modifier = Modifier.weight(1f).testTag("portfolio_explore_service_button")
                        )
                        KoachPrimaryButton(
                            text = "Book similar",
                            onClick = { onBookSimilar(serviceId) },
                            modifier = Modifier.weight(1f).testTag("portfolio_book_similar_button")
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            item == null -> ErrorState("This piece is no longer in the portfolio.", Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = gutter,
                    end = gutter,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { PortfolioCover(item, aspect = if (item.cover != null) 0.8f else 1.5f, iconSize = 72.dp) }
                item {
                    Eyebrow(item.categoryAndYear)
                    Spacer(Modifier.height(6.dp))
                    Text(item.summary, style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                }
                if (item.isSample) item {
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.Top) {
                            IconTile(Icons.Outlined.Info, tone = Tone.Info, size = 32.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "This is a sample project created to illustrate the kind of work KOACH Studio does. The client and results are illustrative.",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary
                            )
                        }
                    }
                }
                if (item.metrics.isNotEmpty()) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item.metrics.forEach { (value, label) ->
                            KoachCard(modifier = Modifier.weight(1f)) {
                                Text(value, style = MaterialTheme.typography.headlineSmall, color = c.brand)
                                Text(label, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                            }
                        }
                    }
                }
                if (item.brief.isNotBlank()) item { DetailSection("The brief", item.brief) }
                if (item.approach.isNotBlank()) item { DetailSection(if (item.isSample) "Our approach" else "Details", item.approach) }
                items(item.gallery) { image -> GalleryImage(image, item.title) }
                item {
                    Eyebrow("Services used", color = c.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item.services.forEach { StatusPill(it.displayName, it.tone) }
                    }
                }
            }
        }
    }
}

/** Full-width artwork at its own aspect ratio. */
@Composable
private fun GalleryImage(image: Int, title: String) {
    val c = KoachTheme.colors
    val shape = RoundedCornerShape(KoachRadius.lg)
    val painter = painterResource(image)
    val size = painter.intrinsicSize
    Image(
        painter = painter,
        contentDescription = "More $title artwork",
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (size.height > 0f) size.width / size.height else 1f)
            .clip(shape)
            .border(1.dp, c.borderSubtle, shape)
    )
}

@Composable
private fun DetailSection(title: String, body: String) {
    val c = KoachTheme.colors
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
    }
}
