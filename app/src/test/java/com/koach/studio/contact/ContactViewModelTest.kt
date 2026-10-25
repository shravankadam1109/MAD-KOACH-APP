package com.koach.studio.contact

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun inquiryValidation_requiresNameEmailAndMessage() {
        val vm = ContactViewModel()

        // Submit empty form
        vm.submitInquiry()
        val state = vm.uiState.value
        assertEquals("Name is required", state.inquiryNameError)
        assertEquals("Please enter a valid email address", state.inquiryEmailError)
        assertEquals("Message must be at least 10 characters", state.inquiryMessageError)
        assertFalse(state.isInquirySuccess)

        // Enter valid info
        vm.onInquiryNameChanged("Rohan Varma")
        vm.onInquiryEmailChanged("rohan@example.com")
        vm.onInquiryMessageChanged("Need 3-day brand commercial shoot in Mumbai.")
        vm.submitInquiry()

        val successState = vm.uiState.value
        assertTrue(successState.isInquirySuccess)
        assertNull(successState.inquiryNameError)
        assertNull(successState.inquiryEmailError)
        assertNull(successState.inquiryMessageError)

        // Reset
        vm.resetInquiry()
        val resetState = vm.uiState.value
        assertFalse(resetState.isInquirySuccess)
        assertEquals("", resetState.inquiryName)
    }
}
