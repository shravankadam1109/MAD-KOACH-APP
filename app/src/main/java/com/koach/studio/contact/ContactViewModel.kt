package com.koach.studio.contact

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ContactUiState(
    val contactInfo: StudioContactInfo = StudioContactInfo(),
    val notificationMessage: String? = null,
    val inquiryName: String = "",
    val inquiryEmail: String = "",
    val inquiryMessage: String = "",
    val inquiryNameError: String? = null,
    val inquiryEmailError: String? = null,
    val inquiryMessageError: String? = null,
    val isInquirySubmitting: Boolean = false,
    val isInquirySuccess: Boolean = false
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

    fun onInquiryNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            inquiryName = name,
            inquiryNameError = if (_uiState.value.inquiryNameError != null && name.isNotBlank()) null else _uiState.value.inquiryNameError
        )
    }

    fun onInquiryEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            inquiryEmail = email,
            inquiryEmailError = if (_uiState.value.inquiryEmailError != null && isValidEmail(email)) null else _uiState.value.inquiryEmailError
        )
    }

    fun onInquiryMessageChanged(message: String) {
        _uiState.value = _uiState.value.copy(
            inquiryMessage = message,
            inquiryMessageError = if (_uiState.value.inquiryMessageError != null && message.trim().length >= 10) null else _uiState.value.inquiryMessageError
        )
    }

    fun submitInquiry() {
        val current = _uiState.value
        val nameErr = if (current.inquiryName.isBlank()) "Name is required" else null
        val emailErr = if (!isValidEmail(current.inquiryEmail)) "Please enter a valid email address" else null
        val msgErr = if (current.inquiryMessage.trim().length < 10) "Message must be at least 10 characters" else null

        if (nameErr != null || emailErr != null || msgErr != null) {
            _uiState.value = current.copy(
                inquiryNameError = nameErr,
                inquiryEmailError = emailErr,
                inquiryMessageError = msgErr
            )
            return
        }

        _uiState.value = current.copy(
            isInquirySubmitting = false,
            isInquirySuccess = true,
            inquiryNameError = null,
            inquiryEmailError = null,
            inquiryMessageError = null,
            notificationMessage = "Inquiry sent successfully"
        )
    }

    fun resetInquiry() {
        _uiState.value = _uiState.value.copy(
            inquiryName = "",
            inquiryEmail = "",
            inquiryMessage = "",
            inquiryNameError = null,
            inquiryEmailError = null,
            inquiryMessageError = null,
            isInquirySubmitting = false,
            isInquirySuccess = false
        )
    }

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && trimmed.contains("@") && trimmed.contains(".") && !trimmed.contains(" ")
    }
}
