package com.koach.studio.catalog.data

import com.koach.studio.core.model.CreativeService
import com.koach.studio.core.model.PortfolioItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Read-only studio catalogue: services, packages and showcase work. Public to every role. */
interface CatalogRepository {
    fun observeServices(): Flow<List<CreativeService>>
    fun observeService(serviceId: String): Flow<CreativeService?>
    fun observePortfolio(): Flow<List<PortfolioItem>>
    fun observePortfolioItem(itemId: String): Flow<PortfolioItem?>
}

class InMemoryCatalogRepository(
    services: List<CreativeService>,
    portfolio: List<PortfolioItem>
) : CatalogRepository {
    private val services = MutableStateFlow(services)
    private val portfolio = MutableStateFlow(portfolio)

    override fun observeServices(): Flow<List<CreativeService>> = services
    override fun observeService(serviceId: String): Flow<CreativeService?> = services.map { list -> list.find { it.id == serviceId } }
    override fun observePortfolio(): Flow<List<PortfolioItem>> = portfolio
    override fun observePortfolioItem(itemId: String): Flow<PortfolioItem?> = portfolio.map { list -> list.find { it.id == itemId } }
}
