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
import org.junit.Assert.assertNull
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
    fun allItems_areRealWorkWithArtwork_datedFirst() = runTest {
        val vm = PortfolioViewModel(catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val items = vm.uiState.first { !it.isLoading }.items
        assertEquals(data.portfolio.size, items.size)
        assertTrue(items.none { it.isSample })
        assertTrue(items.all { it.cover != null && it.metrics.isEmpty() })
        assertEquals("pf-endura", items.first().id) // the only dated piece
    }

    @Test
    fun filter_offersOnlyCategoriesThatHaveWork() = runTest {
        val vm = PortfolioViewModel(catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        assertEquals(listOf(ServiceCategory.SOCIAL_MEDIA), vm.uiState.first { !it.isLoading }.categories)
        vm.selectCategory(ServiceCategory.VFX)
        assertTrue(vm.uiState.value.items.isEmpty())
        vm.selectCategory(ServiceCategory.SOCIAL_MEDIA)
        assertEquals(data.portfolio.size, vm.uiState.value.items.size)
    }

    @Test
    fun detail_linksToServiceForItsCategory() = runTest {
        val vm = PortfolioDetailViewModel("pf-peeyo", catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        assertEquals("Peeyo", state.item?.title)
        assertEquals("svc-social", state.relatedServiceId)
    }

    @Test
    fun detail_forUnknownPiece_hasNoItem() = runTest {
        val vm = PortfolioDetailViewModel("pf-missing", catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        assertNull(state.item)
        assertNull(state.relatedServiceId)
    }
}
