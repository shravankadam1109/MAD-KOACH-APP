package com.koach.studio.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.koach.studio.catalog.ui.icon
import com.koach.studio.catalog.ui.tone
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSegmentedControl
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.color
import com.koach.studio.core.designsystem.components.koachGlow
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.ServiceCategory
import com.koach.studio.core.model.ServicePackage
import com.koach.studio.core.ui.formatInr
import com.koach.studio.portfolio.PortfolioViewModel
import com.koach.studio.portfolio.portfolioGrid

/** Services list content, embedded in the Explore tab's LazyColumn. */
fun LazyListScope.servicesCatalog(
    state: ServicesUiState,
    onSelectCategory: (ServiceCategory?) -> Unit,
    onOpenService: (String) -> Unit
) {
    item {
        KoachFilterChips(
            options = state.categories,
            selected = state.selectedCategory,
            onSelected = onSelectCategory,
            label = { it.displayName },
            modifier = Modifier.testTag("service_filters")
        )
    }
    items(state.services, key = { it.id }) { service ->
        ServiceCard(
            service,
            onClick = { onOpenService(service.id) },
            modifier = Modifier.padding(horizontal = KoachTheme.spacing.gutter)
        )
    }
}

@Composable
private fun ServiceCard(service: CreativeService, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    KoachCard(onClick = onClick, modifier = modifier.fillMaxWidth().testTag("service_${service.id}")) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(service.category.icon, tone = service.category.tone, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(service.name, style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(service.tagline, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        KoachDivider()
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("From ", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            Text(formatInr(service.startingPriceInr), style = MaterialTheme.typography.titleMedium, color = c.brand)
            Spacer(Modifier.width(8.dp))
            Text("· ${service.packages.size} packages", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ServiceDetailScreen(
    viewModel: ServiceDetailViewModel,
    onBack: () -> Unit,
    onEnquire: (serviceId: String, packageId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val service = state.service

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = service?.name.orEmpty(), onBack = onBack) },
        bottomBar = {
            if (service != null) {
                val pkg = service.packages.find { it.id == state.selectedPackageId }
                Column(
                    Modifier
                        .background(c.background)
                        .navigationBarsPadding()
                        .padding(horizontal = gutter, vertical = 12.dp)
                ) {
                    KoachPrimaryButton(
                        text = if (pkg != null) "Enquire · ${pkg.name}" else "Send an enquiry",
                        onClick = { onEnquire(service.id, pkg?.id) },
                        modifier = Modifier.fillMaxWidth().testTag("enquire_button")
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            service == null -> ErrorState("This service is no longer available.", Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .koachGlow(service.category.tone.color(), 0.18f)
                            .padding(horizontal = gutter, vertical = 8.dp)
                    ) {
                        IconTile(service.category.icon, tone = service.category.tone, size = 64.dp)
                        Spacer(Modifier.height(18.dp))
                        Eyebrow(service.category.displayName)
                        Spacer(Modifier.height(6.dp))
                        Text(service.tagline, style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
                        Spacer(Modifier.height(12.dp))
                        Text(service.description, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary)
                        Spacer(Modifier.height(20.dp))
                        Eyebrow("What you get", color = c.textTertiary)
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            service.deliverables.forEach { StatusPill(it, Tone.Neutral, showDot = false) }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text("Choose a package", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                    }
                }
                items(service.packages, key = { it.id }) { pkg ->
                    PackageCard(
                        pkg,
                        selected = pkg.id == state.selectedPackageId,
                        onSelect = { viewModel.selectPackage(pkg.id) },
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                }
                item {
                    Text(
                        "Prices are starting points in INR. Final quotes depend on scope and are confirmed by the studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary,
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageCard(pkg: ServicePackage, selected: Boolean, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    val c = KoachTheme.colors
    KoachCard(
        highlighted = selected,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .testTag("package_${pkg.id}")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(pkg.name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                    if (pkg.recommended) {
                        Spacer(Modifier.width(8.dp))
                        StatusPill("Most popular", Tone.Brand, showDot = false)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(pkg.turnaround, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }
            Text(formatInr(pkg.priceInr), style = MaterialTheme.typography.headlineSmall, color = if (selected) c.brand else c.textPrimary)
            Spacer(Modifier.width(12.dp))
            RadioDot(selected)
        }
        Spacer(Modifier.height(14.dp))
        pkg.features.forEach { feature ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = c.brand, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(feature, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val c = KoachTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) c.brand else c.outline, CircleShape)
    ) {
        if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(c.brand))
    }
}

/** Explore tab for clients: the services catalogue and the sample portfolio. */
@Composable
fun ExploreScreen(
    servicesViewModel: ServicesViewModel,
    portfolioViewModel: PortfolioViewModel,
    onOpenService: (String) -> Unit,
    onOpenPortfolioItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val services by servicesViewModel.uiState.collectAsState()
    val portfolio by portfolioViewModel.uiState.collectAsState()
    var section by rememberSaveable { mutableIntStateOf(0) }
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    KoachBackground(modifier.fillMaxSize()) {
        if (services.isLoading || portfolio.isLoading) {
            LoadingState()
            return@KoachBackground
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .statusBarsPadding()
                .testTag("explore_screen")
        ) {
            item {
                Column(Modifier.padding(start = gutter, end = gutter, top = 16.dp, bottom = 4.dp)) {
                    Eyebrow("Explore")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (section == 0) "What can we make for you?" else "Work that moves.",
                        style = MaterialTheme.typography.displaySmall,
                        color = c.textPrimary
                    )
                    Spacer(Modifier.height(18.dp))
                    KoachSegmentedControl(
                        options = listOf("Services", "Work"),
                        selectedIndex = section,
                        onSelect = { section = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            if (section == 0) {
                servicesCatalog(services, servicesViewModel::selectCategory, onOpenService)
            } else {
                portfolioGrid(portfolio, portfolioViewModel::selectCategory, onOpenPortfolioItem)
            }
        }
    }
}
