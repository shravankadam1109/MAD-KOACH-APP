package com.koach.studio.contact

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactViewModelTest {

    @Test
    fun defaultState_containsStudioAgencyDetails() = runTest {
        val vm = ContactViewModel()
        val state = vm.uiState.first()

        assertEquals("KOACH Studio", state.contactInfo.studioName)
        assertTrue(state.contactInfo.cityAndPin.contains("Bandra West, Mumbai"))
        assertEquals("+91 98200 22000", state.contactInfo.deskPhone)
        assertEquals("+91 98200 22001", state.contactInfo.productionHotline)
        assertEquals("hello@koachstudio.example", state.contactInfo.generalEmail)
        assertEquals("production@koachstudio.example", state.contactInfo.bookingsEmail)
        assertNull(state.notificationMessage)
    }

    @Test
    fun onInfoCopied_setsNotificationMessage() = runTest {
        val vm = ContactViewModel()
        vm.onInfoCopied("Desk phone")

        val state = vm.uiState.first()
        assertEquals("Desk phone copied to clipboard", state.notificationMessage)

        vm.dismissNotification()
        val clearedState = vm.uiState.first()
        assertNull(clearedState.notificationMessage)
    }

    @Test
    fun customInfo_populatesStateCorrectly() = runTest {
        val customInfo = StudioContactInfo(
            studioName = "KOACH Studio Satellite",
            deskPhone = "+91 98200 99999"
        )
        val vm = ContactViewModel(customInfo)
        val state = vm.uiState.first()

        assertEquals("KOACH Studio Satellite", state.contactInfo.studioName)
        assertEquals("+91 98200 99999", state.contactInfo.deskPhone)
    }
}
