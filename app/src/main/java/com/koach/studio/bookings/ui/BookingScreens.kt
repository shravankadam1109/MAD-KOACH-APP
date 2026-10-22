package com.koach.studio.bookings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.koach.studio.bookings.data.BookingValidator
import com.koach.studio.bookings.data.BookingValidator.Field
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.KoachDateField
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.FormSection
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachDropdownField
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.components.koachGlow
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatInr

val BookingStatus.tone: Tone
    get() = when (this) {
        BookingStatus.PENDING -> Tone.Warning
        BookingStatus.CONFIRMED -> Tone.Brand
        BookingStatus.DECLINED -> Tone.Danger
        BookingStatus.COMPLETED -> Tone.Neutral
        BookingStatus.CANCELLED -> Tone.Neutral
    }

@Composable
fun NewBookingScreen(
    viewModel: NewBookingViewModel,
    onBack: () -> Unit,
    onViewBookings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = if (state.submitted == null) "Book a service" else "Request sent", onBack = onBack) },
        modifier = modifier
    ) { padding ->
        val submitted = state.submitted
        if (submitted != null) {
            BookingConfirmation(
                reference = submitted.id.uppercase(),
                serviceName = state.selectedService?.name.orEmpty(),
                date = formatDate(submitted.preferredDate, state.today),
                onViewBookings = onViewBookings,
                onDone = onBack,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = gutter, vertical = 8.dp)
                .testTag("new_booking_form")
        ) {
            Eyebrow("Tell us about your project")
            Spacer(Modifier.height(6.dp))
            Text("We'll confirm within one working day.", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
            Spacer(Modifier.height(20.dp))

            FormSection {
                KoachDropdownField(
                    label = "Service",
                    options = state.services,
                    selected = state.selectedService,
                    onSelected = { viewModel.onServiceSelected(it.id) },
                    optionLabel = { it.name },
                    error = state.errors[Field.SERVICE],
                    modifier = Modifier.testTag("booking_service")
                )
                val packages = state.selectedService?.packages.orEmpty()
                if (packages.isNotEmpty()) {
                    KoachDropdownField(
                        label = "Package (optional)",
                        options = packages,
                        selected = packages.find { it.id == state.packageId },
                        onSelected = { viewModel.onPackageSelected(it.id) },
                        optionLabel = { "${it.name} · ${formatInr(it.priceInr)}" },
                        placeholder = "Not sure yet"
                    )
                }
                KoachDateField(
                    label = "Preferred date",
                    value = state.preferredDate,
                    onValueChange = viewModel::onDateSelected,
                    error = state.errors[Field.DATE],
                    helper = "At least ${BookingValidator.MIN_LEAD_DAYS} days from today",
                    earliest = state.today.plusDays(BookingValidator.MIN_LEAD_DAYS),
                    latest = state.today.plusDays(BookingValidator.MAX_AHEAD_DAYS),
                    fieldModifier = Modifier.testTag("booking_date")
                )
                KoachTextField(
                    value = state.budget,
                    onValueChange = viewModel::onBudgetChanged,
                    label = "Budget in ₹ (optional)",
                    leadingIcon = Icons.Outlined.CurrencyRupee,
                    error = state.errors[Field.BUDGET],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                KoachTextField(
                    value = state.details,
                    onValueChange = viewModel::onDetailsChanged,
                    label = "Project details",
                    placeholder = "What are we making, for whom, and where will it live?",
                    error = state.errors[Field.DETAILS],
                    maxLength = BookingValidator.DETAILS_MAX,
                    singleLine = false,
                    minLines = 4,
                    modifier = Modifier.testTag("booking_details")
                )
                Spacer(Modifier.height(4.dp))
                Eyebrow("Contact", color = c.textTertiary)
                KoachTextField(
                    value = state.contactName,
                    onValueChange = viewModel::onNameChanged,
                    label = "Name",
                    leadingIcon = Icons.Outlined.Person,
                    error = state.errors[Field.NAME]
                )
                KoachTextField(
                    value = state.contactEmail,
                    onValueChange = viewModel::onEmailChanged,
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    error = state.errors[Field.EMAIL],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            state.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = c.danger, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
            KoachPrimaryButton(
                text = "Send booking request",
                onClick = viewModel::submit,
                loading = state.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("booking_submit")
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Demo build: requests are stored on this device only and no email is sent.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BookingConfirmation(
    reference: String,
    serviceName: String,
    date: String,
    onViewBookings: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .padding(KoachTheme.spacing.gutter)
            .testTag("booking_confirmation")
    ) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.koachGlow(c.brand, 0.35f).padding(24.dp)) {
            IconTile(Icons.Filled.CheckCircle, size = 88.dp)
        }
        Spacer(Modifier.height(20.dp))
        Text("Request received", style = MaterialTheme.typography.headlineLarge, color = c.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            "The studio will review your request and confirm availability. You'll get a notification when it's confirmed.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        KoachCard(modifier = Modifier.fillMaxWidth()) {
            InfoRow("Reference", reference)
            KoachDivider()
            InfoRow("Service", serviceName)
            KoachDivider()
            InfoRow("Preferred date", date)
            KoachDivider()
            InfoRow("Status", "Pending")
        }
        Spacer(Modifier.weight(1f))
        KoachPrimaryButton("View my bookings", onClick = onViewBookings, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        KoachSecondaryButton("Done", onClick = onDone, modifier = Modifier.fillMaxWidth().navigationBarsPadding())
    }
}

@Composable
fun BookingsScreen(
    viewModel: BookingsViewModel,
    onBack: () -> Unit,
    onNewBooking: () -> Unit,
    onOpenBooking: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = {
            KoachTopBar(
                title = if (state.canManage) "Booking inbox" else "My bookings",
                subtitle = if (state.canManage) "${state.pendingCount} awaiting a reply" else null,
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (state.canCreate) {
                ExtendedFloatingActionButton(
                    onClick = onNewBooking,
                    containerColor = c.brand,
                    contentColor = c.onBrand,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New booking") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("bookings_list")
        ) {
            item {
                KoachFilterChips(
                    options = BookingStatus.entries,
                    selected = state.statusFilter,
                    onSelected = viewModel::setFilter,
                    label = { it.displayName }
                )
            }
            if (state.items.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.EventNote,
                        title = "No bookings",
                        message = if (state.canCreate) "When you request a service it shows up here." else "Nothing matches this filter.",
                        actionLabel = if (state.canCreate) "Book a service" else null,
                        onAction = if (state.canCreate) onNewBooking else null
                    )
                }
            }
            items(state.items, key = { it.booking.id }) { item ->
                BookingCard(
                    item,
                    canManage = state.canManage,
                    onConfirm = { viewModel.updateStatus(item.booking.id, BookingStatus.CONFIRMED) },
                    onOpen = { onOpenBooking(item.booking.id) },
                    modifier = Modifier.padding(horizontal = KoachTheme.spacing.gutter)
                )
            }
        }
    }
}

@Composable
private fun BookingCard(
    item: BookingListItem,
    canManage: Boolean,
    onConfirm: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    val booking = item.booking
    KoachCard(onClick = onOpen, modifier = modifier.fillMaxWidth().testTag("booking_${booking.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.serviceName, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                Text(item.packageName ?: "Package to be discussed", style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            StatusPill(booking.status.displayName, booking.status.tone)
        }
        Spacer(Modifier.height(12.dp))
        Text(booking.details, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, maxLines = 3)
        Spacer(Modifier.height(12.dp))
        KoachDivider()
        InfoRow("Preferred date", formatDate(booking.preferredDate), icon = Icons.Outlined.CalendarMonth)
        booking.budgetInr?.let { InfoRow("Budget", formatInr(it), icon = Icons.Outlined.CurrencyRupee) }
        if (canManage) InfoRow("Requested by", booking.contactName, icon = Icons.Outlined.Person)
        if (canManage && booking.status == BookingStatus.PENDING) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KoachSecondaryButton("Review", onClick = onOpen, modifier = Modifier.weight(1f).testTag("review_${booking.id}"))
                KoachPrimaryButton("Confirm", onClick = onConfirm, modifier = Modifier.weight(1f).testTag("confirm_${booking.id}"))
            }
        }
    }
}
