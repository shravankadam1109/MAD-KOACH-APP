package com.koach.studio.bookings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.bookings.data.BookingRepository
import com.koach.studio.bookings.data.BookingRequest
import com.koach.studio.bookings.data.BookingValidator
import com.koach.studio.bookings.data.BookingValidator.Field
import com.koach.studio.catalog.data.CatalogRepository
import com.koach.studio.core.auth.Permission
import com.koach.studio.core.auth.can
import com.koach.studio.core.model.Booking
import com.koach.studio.core.model.BookingStatus
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class NewBookingUiState(
    val services: List<CreativeService> = emptyList(),
    val serviceId: String = "",
    val packageId: String? = null,
    val preferredDate: LocalDate? = null,
    val budget: String = "",
    val details: String = "",
    val contactName: String = "",
    val contactEmail: String = "",
    val contactPhone: String = "",
    val errors: Map<Field, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val submitted: Booking? = null,
    val errorMessage: String? = null,
    val today: LocalDate = LocalDate.now()
) {
    val selectedService: CreativeService? get() = services.find { it.id == serviceId }
}

class NewBookingViewModel(
    user: User,
    prefillServiceId: String?,
    prefillPackageId: String?,
    private val catalog: CatalogRepository,
    private val bookings: BookingRepository,
    private val clock: Clock
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NewBookingUiState(
            serviceId = prefillServiceId.orEmpty(),
            packageId = prefillPackageId?.takeIf { it.isNotBlank() },
            contactName = user.name,
            contactEmail = user.email,
            today = LocalDate.now(clock)
        )
    )
    val uiState: StateFlow<NewBookingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val services = catalog.observeServices().first()
            _uiState.update { it.copy(services = services) }
        }
    }

    fun onServiceSelected(serviceId: String) = _uiState.update {
        it.copy(serviceId = serviceId, packageId = null, errors = it.errors - Field.SERVICE)
    }

    fun onPackageSelected(packageId: String?) = _uiState.update { it.copy(packageId = packageId) }

    fun onDateSelected(date: LocalDate) = _uiState.update {
        it.copy(preferredDate = date, errors = it.errors.updated(Field.DATE, BookingValidator.dateError(date, it.today)))
    }

    fun onBudgetChanged(value: String) = _uiState.update {
        it.copy(budget = value.filter(Char::isDigit).take(9), errors = it.errors - Field.BUDGET)
    }

    fun onDetailsChanged(value: String) = _uiState.update {
        // Only re-validate once the user has seen an error, so typing isn't nagged.
        it.copy(details = value, errors = if (Field.DETAILS in it.errors) it.errors.updated(Field.DETAILS, BookingValidator.detailsError(value)) else it.errors)
    }

    fun onNameChanged(value: String) = _uiState.update {
        val err = if (Field.NAME in it.errors) BookingValidator.nameError(value) else null
        it.copy(contactName = value, errors = it.errors.updated(Field.NAME, err))
    }

    fun onEmailChanged(value: String) = _uiState.update {
        val err = if (Field.EMAIL in it.errors) BookingValidator.emailError(value) else null
        it.copy(contactEmail = value, errors = it.errors.updated(Field.EMAIL, err))
    }

    fun onPhoneChanged(value: String) = _uiState.update {
        val err = if (Field.PHONE in it.errors) BookingValidator.phoneError(value) else null
        it.copy(contactPhone = value, errors = it.errors.updated(Field.PHONE, err))
    }

    fun submit() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val date = state.preferredDate
        val request = BookingRequest(
            serviceId = state.serviceId,
            packageId = state.packageId,
            preferredDate = date ?: LocalDate.MIN,
            budgetInr = state.budget.toLongOrNull(),
            details = state.details,
            contactName = state.contactName,
            contactEmail = state.contactEmail,
            contactPhone = state.contactPhone
        )
        val errors = BookingValidator.validate(request, state.today).let {
            if (date == null) it + (Field.DATE to "Pick a preferred date") else it
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = bookings.createBooking(request)
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    submitted = result.getOrNull(),
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun Map<Field, String>.updated(field: Field, error: String?) = if (error == null) this - field else this + (field to error)
}

data class BookingListItem(val booking: Booking, val serviceName: String, val packageName: String?)

data class BookingsUiState(
    val isLoading: Boolean = true,
    val items: List<BookingListItem> = emptyList(),
    val statusFilter: BookingStatus? = null,
    val canManage: Boolean = false,
    val canCreate: Boolean = false,
    val pendingCount: Int = 0,
    val message: String? = null
)

class BookingsViewModel(
    user: User,
    private val bookings: BookingRepository,
    catalog: CatalogRepository
) : ViewModel() {

    private val filter = MutableStateFlow<BookingStatus?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val canManage = user.can(Permission.MANAGE_BOOKINGS)
    private val canCreate = user.can(Permission.CREATE_BOOKINGS)

    val uiState: StateFlow<BookingsUiState> =
        combine(bookings.observeBookings(), catalog.observeServices(), filter, message) { list, services, status, msg ->
            BookingsUiState(
                isLoading = false,
                items = list.filter { status == null || it.status == status }.map { booking ->
                    val service = services.find { it.id == booking.serviceId }
                    BookingListItem(
                        booking,
                        serviceName = service?.name ?: "Service",
                        packageName = service?.packages?.find { it.id == booking.packageId }?.name
                    )
                },
                statusFilter = status,
                canManage = canManage,
                canCreate = canCreate,
                pendingCount = list.count { it.status == BookingStatus.PENDING },
                message = msg
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookingsUiState())

    fun setFilter(status: BookingStatus?) = filter.update { status }

    fun updateStatus(bookingId: String, status: BookingStatus) {
        viewModelScope.launch {
            val result = bookings.updateStatus(bookingId, status)
            message.value = result.fold(
                onSuccess = { "Booking ${status.displayName.lowercase()}" },
                onFailure = { it.message ?: "Couldn't update booking" }
            )
        }
    }

    fun messageShown() = message.update { null }
}
