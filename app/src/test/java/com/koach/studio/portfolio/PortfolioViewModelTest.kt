package com.koach.studio.portfolio

import com.koach.studio.catalog.data.InMemoryCatalogRepository
import com.koach.studio.core.data.DemoData
import com.koach.studio.core.model.ServiceCategory
import com.koach.studio.projects.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val data = DemoData(LocalDate.of(2026, 10, 22))
    private val catalog = InMemoryCatalogRepository(data.services, data.portfolio)

    @Test
    fun allItems_areSamples_newestFirst() = runTest {
        val vm = PortfolioViewModel(catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val items = vm.uiState.first { !it.isLoading }.items
        assertEquals(data.portfolio.size, items.size)
        assertTrue(items.all { it.isSample })
        assertEquals(items.sortedByDescending { it.year }.map { it.year }, items.map { it.year })
    }

    @Test
    fun filter_matchesLeadCategoryOrServicesUsed() = runTest {
        val vm = PortfolioViewModel(catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        vm.uiState.first { !it.isLoading }
        vm.selectCategory(ServiceCategory.VFX)
        val ids = vm.uiState.value.items.map { it.id }.toSet()
        assertEquals(setOf("pf-2", "pf-6"), ids)
    }

    @Test
    fun detail_linksToServiceForItsCategory() = runTest {
        val vm = PortfolioDetailViewModel("pf-5", catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals("svc-motion", vm.uiState.first { !it.isLoading }.relatedServiceId)
    }

    @Test
    fun detail_resolvesRelatedServiceForAllSamplePieces() = runTest {
        val filmingVm = PortfolioDetailViewModel("pf-1", catalog)
        val brandingVm = PortfolioDetailViewModel("pf-4", catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            filmingVm.uiState.collect {}
            brandingVm.uiState.collect {}
        }
        assertEquals("svc-film", filmingVm.uiState.first { !it.isLoading }.relatedServiceId)
        assertEquals("svc-brand", brandingVm.uiState.first { !it.isLoading }.relatedServiceId)
    }
}
