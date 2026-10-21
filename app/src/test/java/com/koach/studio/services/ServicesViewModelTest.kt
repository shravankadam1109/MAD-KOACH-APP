package com.koach.studio.services

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
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val data = DemoData(LocalDate.of(2026, 10, 22))
    private val catalog = InMemoryCatalogRepository(data.services, data.portfolio)

    @Test
    fun categoryFilter_narrowsServices_andNullShowsAll() = runTest {
        val vm = ServicesViewModel(catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals(7, vm.uiState.first { !it.isLoading }.services.size)
        vm.selectCategory(ServiceCategory.VFX)
        assertEquals(listOf("svc-vfx"), vm.uiState.value.services.map { it.id })
        vm.selectCategory(null)
        assertEquals(7, vm.uiState.value.services.size)
    }

    @Test
    fun serviceDetail_preselectsRecommendedPackage_andAllowsChange() = runTest {
        val vm = ServiceDetailViewModel("svc-edit", catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        assertEquals("edit-2", vm.uiState.first { !it.isLoading }.selectedPackageId)
        vm.selectPackage("edit-3")
        assertEquals("edit-3", vm.uiState.value.selectedPackageId)
    }

    @Test
    fun unknownService_hasNoServiceOrPackage() = runTest {
        val vm = ServiceDetailViewModel("missing", catalog)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
        val state = vm.uiState.first { !it.isLoading }
        assertNull(state.service)
        assertNull(state.selectedPackageId)
    }
}
