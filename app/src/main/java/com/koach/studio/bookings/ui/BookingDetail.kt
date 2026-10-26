package com.koach.studio.bookings.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.bookings.data.BookingRepository
import com.koach.studio.bookings.data.BookingRules
import com.koach.studio.catalog.data.CatalogRepository
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.designsystem.components.ErrorState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Booking
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.User
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatInr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookingDetailUiState(
    val isLoading: Boolean = true,
    val booking: Booking? = null,
    val service: CreativeService? = null,
    val isAgency: Boolean = false,
    /** Status moves offered to this user right now. */
    val agencyActions: Set<BookingStatus> = emptySet(),
    val canCancel: Boolean = false,
    val isWorking: Boolean = false,
    val message: String? = null
)

class BookingDetailViewModel(
    private val bookingId: String,
    private val user: User,
    private val bookings: BookingRepository,
    catalog: CatalogRepository
) : ViewModel() {

    private val working = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BookingDetailUiState> =
        combine(bookings.observeBooking(bookingId), catalog.observeServices(), working, message) { booking, services, busy, msg ->
            BookingDetailUiState(
                isLoading = false,
                booking = booking,
                service = booking?.let { b -> services.find { it.id == b.serviceId } },
                isAgency = user.role.isAgency,
                agencyActions = if (booking != null && user.can(Permission.MANAGE_BOOKINGS)) BookingRules.agencyTransitions(booking.status) else emptySet(),
                canCancel = booking != null && booking.requestedBy == user.id && BookingRules.clientCanCancel(booking.status),
                isWorking = busy,
                message = msg
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookingDetailUiState())

    fun setStatus(status: BookingStatus, note: String? = null) = run({ bookings.updateStatus(bookingId, status, note) }, "Booking ${status.displayName.lowercase()}")
    fun cancel() = run({ bookings.cancelBooking(bookingId) }, "Request cancelled")
    fun messageShown() = message.update { null }

    private fun run(action: suspend () -> Result<Unit>, success: String) {
        if (working.value) return
        viewModelScope.launch {
            working.value = true
            message.value = action().fold({ success }, { it.message ?: "Something went wrong" })
            working.value = false
        }
    }
}

@Composable
fun BookingDetailScreen(viewModel: BookingDetailViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val snackbar = remember { SnackbarHostState() }
    var declining by rememberSaveable { mutableStateOf(false) }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val booking = state.booking
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { KoachTopBar(title = state.service?.name ?: "Booking", subtitle = booking?.id?.uppercase(), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            booking == null -> ErrorState("This booking isn't available.", Modifier.padding(padding))
            else -> LazyColumn(
                contentPadding = PaddingValues(start = KoachTheme.spacing.gutter, end = KoachTheme.spacing.gutter, top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.testTag("booking_detail")
            ) {
                item {
                    KoachCard(highlighted = booking.status == BookingStatus.PENDING, modifier = Modifier.fillMaxWidth()) {
                        StatusPill(booking.status.displayName, booking.status.tone, modifier = Modifier.testTag("booking_status"))
                        Spacer(Modifier.height(14.dp))
                        Text(state.service?.name ?: "Service", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
                        val pkg = state.service?.packages?.find { it.id == booking.packageId }
                        Text(pkg?.let { "${it.name} · from ${formatInr(it.priceInr)}" } ?: "Package to be discussed", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        booking.statusNote?.let {
                            Spacer(Modifier.height(12.dp))
                            Eyebrow("Note from the studio", color = c.textTertiary)
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary)
                        }
                    }
                }
                item {
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        InfoRow("Preferred date", formatDate(booking.preferredDate), icon = Icons.Outlined.CalendarMonth)
                        KoachDivider()
                        InfoRow("Budget", booking.budgetInr?.let(::formatInr) ?: "Not given", icon = Icons.Outlined.CurrencyRupee)
                        KoachDivider()
                        InfoRow("Requested", formatDate(booking.createdOn), icon = Icons.Outlined.Schedule)
                        if (state.isAgency) {
                            KoachDivider()
                            InfoRow("Contact", booking.contactName, icon = Icons.Outlined.Person)
                            KoachDivider()
                            InfoRow("Email", booking.contactEmail, icon = Icons.Outlined.Email)
                        }
                        state.service?.let {
                            KoachDivider()
                            InfoRow("Category", it.category.displayName, icon = Icons.Outlined.Inventory2)
                        }
                    }
                }
                item {
                    KoachCard(modifier = Modifier.fillMaxWidth()) {
                        Eyebrow("Brief", color = c.textTertiary)
                        Spacer(Modifier.height(6.dp))
                        Text(booking.details, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                    }
                }
                if (state.agencyActions.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (BookingStatus.CONFIRMED in state.agencyActions) {
                                KoachPrimaryButton("Confirm booking", onClick = { viewModel.setStatus(BookingStatus.CONFIRMED) }, loading = state.isWorking, modifier = Modifier.fillMaxWidth().testTag("detail_confirm"))
                            }
                            if (BookingStatus.COMPLETED in state.agencyActions) {
                                KoachPrimaryButton("Mark as completed", onClick = { viewModel.setStatus(BookingStatus.COMPLETED) }, loading = state.isWorking, modifier = Modifier.fillMaxWidth().testTag("detail_complete"))
                            }
                            if (BookingStatus.DECLINED in state.agencyActions) {
                                KoachSecondaryButton("Decline with a note", onClick = { declining = true }, destructive = true, modifier = Modifier.fillMaxWidth().testTag("detail_decline"))
                            }
                        }
                    }
                }
                if (state.canCancel) {
                    item {
                        KoachSecondaryButton("Cancel request", onClick = { confirmCancel = true }, destructive = true, modifier = Modifier.fillMaxWidth().testTag("detail_cancel"))
                        Spacer(Modifier.height(6.dp))
                        Text("You can cancel until the studio confirms.", style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                    }
                }
            }
        }
    }

    if (declining) {
        var reason by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { declining = false },
            containerColor = c.surfaceRaised,
            title = { Text("Decline booking", color = c.textPrimary) },
            text = {
                Column {
                    Text("The client will see this note.", color = c.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    KoachTextField(reason, { reason = it }, "Reason", singleLine = false, minLines = 3, modifier = Modifier.testTag("decline_reason"))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.setStatus(BookingStatus.DECLINED, reason); declining = false },
                    enabled = reason.isNotBlank(),
                    modifier = Modifier.testTag("confirm_decline")
                ) { Text("Decline", color = if (reason.isNotBlank()) c.danger else c.textTertiary) }
            },
            dismissButton = { TextButton(onClick = { declining = false }) { Text("Back", color = c.textSecondary) } }
        )
    }
    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            containerColor = c.surfaceRaised,
            title = { Text("Cancel this request?", color = c.textPrimary) },
            text = { Text("The studio will be told you no longer need this booking.", color = c.textSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.cancel(); confirmCancel = false }, modifier = Modifier.testTag("confirm_cancel")) { Text("Cancel request", color = c.danger) } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep it", color = c.textSecondary) } }
        )
    }
}
