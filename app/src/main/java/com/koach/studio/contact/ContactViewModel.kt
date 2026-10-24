package com.koach.studio.contact

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ContactUiState(
    val contactInfo: StudioContactInfo = StudioContactInfo(),
    val notificationMessage: String? = null
)

class ContactViewModel(
    initialInfo: StudioContactInfo = StudioContactInfo()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState(contactInfo = initialInfo))
    val uiState: StateFlow<ContactUiState> = _uiState.asStateFlow()

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(notificationMessage = null)
    }

    fun onInfoCopied(label: String) {
        _uiState.value = _uiState.value.copy(notificationMessage = "$label copied to clipboard")
    }
}
