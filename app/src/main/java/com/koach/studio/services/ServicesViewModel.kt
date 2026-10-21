package com.koach.studio.services

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.catalog.data.CatalogRepository
import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.ServiceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class ServicesUiState(
    val isLoading: Boolean = true,
    val services: List<CreativeService> = emptyList(),
    val categories: List<ServiceCategory> = ServiceCategory.entries,
    val selectedCategory: ServiceCategory? = null
)

class ServicesViewModel(catalog: CatalogRepository) : ViewModel() {
    private val selected = MutableStateFlow<ServiceCategory?>(null)

    val uiState: StateFlow<ServicesUiState> = combine(catalog.observeServices(), selected) { services, category ->
        ServicesUiState(
            isLoading = false,
            services = services.filter { category == null || it.category == category },
            categories = services.map { it.category }.distinct(),
            selectedCategory = category
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServicesUiState())

    fun selectCategory(category: ServiceCategory?) = selected.update { category }
}

data class ServiceDetailUiState(
    val isLoading: Boolean = true,
    val service: CreativeService? = null,
    val selectedPackageId: String? = null
)

class ServiceDetailViewModel(serviceId: String, catalog: CatalogRepository) : ViewModel() {
    private val selectedPackage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ServiceDetailUiState> =
        combine(catalog.observeService(serviceId), selectedPackage) { service, packageId ->
            ServiceDetailUiState(
                isLoading = false,
                service = service,
                // Default to the recommended package so the CTA is always meaningful.
                selectedPackageId = packageId
                    ?: service?.packages?.firstOrNull { it.recommended }?.id
                    ?: service?.packages?.firstOrNull()?.id
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceDetailUiState())

    fun selectPackage(packageId: String) = selectedPackage.update { packageId }
}
