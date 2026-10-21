package com.koach.studio.payments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.designsystem.components.EmptyState
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.InfoRow
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachFilterChips
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.LoadingState
import com.koach.studio.core.designsystem.components.StatusPill
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.Client
import com.koach.studio.core.model.Payment
import com.koach.studio.core.model.PaymentStatus
import com.koach.studio.core.model.Role
import com.koach.studio.core.model.User
import com.koach.studio.core.ui.formatDate
import com.koach.studio.core.ui.formatInr
import com.koach.studio.clients.data.ClientRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Status shown to the user: a due invoice past its date displays as overdue. */
fun Payment.effectiveStatus(today: LocalDate): PaymentStatus =
    if (status == PaymentStatus.DUE && dueOn.isBefore(today)) PaymentStatus.OVERDUE else status

data class PaymentsUiState(
    val isLoading: Boolean = true,
    val payments: List<Payment> = emptyList(),
    val clientNames: Map<String, String> = emptyMap(),
    val filter: PaymentStatus? = null,
    val outstandingInr: Long = 0,
    val paidInr: Long = 0,
    val isClient: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    val message: String? = null
)

class PaymentsViewModel(
    user: User,
    private val payments: PaymentRepository,
    clients: ClientRepository,
    private val clock: Clock
) : ViewModel() {

    private val filter = MutableStateFlow<PaymentStatus?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val isClient = user.role == Role.CLIENT

    val uiState: StateFlow<PaymentsUiState> =
        combine(payments.observePayments(), clients.observeClients(), filter, message) { list, clientList: List<Client>, f, msg ->
            val today = LocalDate.now(clock)
            PaymentsUiState(
                isLoading = false,
                payments = list.filter { f == null || it.effectiveStatus(today) == f },
                clientNames = clientList.associate { it.id to it.name },
                filter = f,
                outstandingInr = list.filter { it.status != PaymentStatus.PAID }.sumOf { it.amountInr },
                paidInr = list.filter { it.status == PaymentStatus.PAID }.sumOf { it.amountInr },
                isClient = isClient,
                today = today,
                message = msg
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PaymentsUiState())

    fun setFilter(status: PaymentStatus?) = filter.update { status }

    fun paySimulated(paymentId: String) {
        viewModelScope.launch {
            message.value = payments.markPaidSimulated(paymentId)
                .fold({ "Marked as paid (simulated, nothing was charged)" }, { it.message ?: "Couldn't update invoice" })
        }
    }

    fun messageShown() = message.update { null }
}

private val PaymentStatus.tone: Tone
    get() = when (this) {
        PaymentStatus.PAID -> Tone.Success
        PaymentStatus.DUE -> Tone.Warning
        PaymentStatus.OVERDUE -> Tone.Danger
    }

@Composable
fun PaymentsScreen(viewModel: PaymentsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val gutter = KoachTheme.spacing.gutter
    val snackbar = remember { SnackbarHostState() }
    var confirmId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = { KoachTopBar(title = if (state.isClient) "Invoices & payments" else "Payments", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier
    ) { padding ->
        if (state.isLoading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.testTag("payments_list")
        ) {
            item {
                KoachCard(highlighted = true, modifier = Modifier.padding(horizontal = gutter).fillMaxWidth()) {
                    Eyebrow("Outstanding")
                    Spacer(Modifier.height(6.dp))
                    Text(formatInr(state.outstandingInr), style = MaterialTheme.typography.displaySmall, color = c.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("${formatInr(state.paidInr)} paid to date", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                }
            }
            item {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = gutter)) {
                    IconTile(Icons.Outlined.Info, tone = Tone.Info, size = 28.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Payments are simulated in this demo. No payment provider is connected and no money is ever charged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary
                    )
                }
            }
            item { KoachFilterChips(PaymentStatus.entries, state.filter, viewModel::setFilter, label = { it.displayName }) }
            if (state.payments.isEmpty()) {
                item { EmptyState(Icons.AutoMirrored.Outlined.ReceiptLong, "No invoices", "Invoices from the studio will appear here.") }
            }
            items(state.payments, key = { it.id }) { payment ->
                InvoiceCard(
                    payment = payment,
                    status = payment.effectiveStatus(state.today),
                    clientName = if (state.isClient) null else state.clientNames[payment.clientId],
                    canPay = state.isClient && payment.status != PaymentStatus.PAID,
                    onPay = { confirmId = payment.id },
                    modifier = Modifier.padding(horizontal = gutter)
                )
            }
        }
    }

    val toPay = state.payments.find { it.id == confirmId }
    if (toPay != null) {
        AlertDialog(
            onDismissRequest = { confirmId = null },
            containerColor = c.surfaceRaised,
            icon = { IconTile(Icons.Outlined.Payments) },
            title = { Text("Simulate payment?", color = c.textPrimary) },
            text = {
                Text(
                    "This marks ${toPay.invoiceNumber} (${formatInr(toPay.amountInr)}) as paid for demo purposes. No card is charged and no money moves.",
                    color = c.textSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.paySimulated(toPay.id); confirmId = null }, modifier = Modifier.testTag("confirm_simulated_payment")) {
                    Text("Mark as paid", color = c.brand)
                }
            },
            dismissButton = { TextButton(onClick = { confirmId = null }) { Text("Cancel", color = c.textSecondary) } }
        )
    }
}

@Composable
private fun InvoiceCard(
    payment: Payment,
    status: PaymentStatus,
    clientName: String?,
    canPay: Boolean,
    onPay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    KoachCard(modifier = modifier.fillMaxWidth().testTag("invoice_${payment.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(payment.invoiceNumber, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                Spacer(Modifier.height(2.dp))
                Text(payment.description, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                if (clientName != null) Text(clientName, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
            StatusPill(status.displayName, status.tone)
        }
        Spacer(Modifier.height(12.dp))
        Text(formatInr(payment.amountInr), style = MaterialTheme.typography.headlineSmall, color = if (status == PaymentStatus.PAID) c.textSecondary else c.textPrimary)
        Spacer(Modifier.height(8.dp))
        KoachDivider()
        InfoRow("Issued", formatDate(payment.issuedOn))
        InfoRow(if (payment.paidOn != null) "Paid" else "Due", formatDate(payment.paidOn ?: payment.dueOn))
        if (canPay) {
            Spacer(Modifier.height(8.dp))
            KoachPrimaryButton(
                text = "Pay now (simulated)",
                onClick = onPay,
                modifier = Modifier.fillMaxWidth().testTag("pay_${payment.id}")
            )
        }
    }
}
