package com.koach.studio.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.catalog.data.CatalogRepository
import com.koach.studio.core.model.PortfolioItem
import com.koach.studio.core.model.ServiceCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val items: List<PortfolioItem> = emptyList(),
    val categories: List<ServiceCategory> = emptyList(),
    val selectedCategory: ServiceCategory? = null
)

class PortfolioViewModel(catalog: CatalogRepository) : ViewModel() {
    private val selected = MutableStateFlow<ServiceCategory?>(null)

    val uiState: StateFlow<PortfolioUiState> = combine(catalog.observePortfolio(), selected) { items, category ->
        PortfolioUiState(
            isLoading = false,
            // A piece matches a filter if it's its lead category or one of the services used.
            items = items
                .filter { category == null || it.category == category || category in it.services }
                .sortedByDescending { it.year },
            categories = ServiceCategory.entries.filter { c -> items.any { it.category == c } },
            selectedCategory = category
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioUiState())

    fun selectCategory(category: ServiceCategory?) = selected.update { category }
}

data class PortfolioDetailUiState(
    val isLoading: Boolean = true,
    val item: PortfolioItem? = null,
    /** Service matching the piece's lead category, for "start a similar project". */
    val relatedServiceId: String? = null
)

class PortfolioDetailViewModel(itemId: String, catalog: CatalogRepository) : ViewModel() {
    val uiState: StateFlow<PortfolioDetailUiState> =
        combine(catalog.observePortfolioItem(itemId), catalog.observeServices()) { item, services ->
            PortfolioDetailUiState(
                isLoading = false,
                item = item,
                relatedServiceId = item?.let { piece -> services.find { it.category == piece.category }?.id }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PortfolioDetailUiState())
}
