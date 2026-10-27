package com.koach.studio.auth

import com.koach.studio.core.auth.PHONE_AUTO_VERIFIED
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.auth.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OtpStep {
    PHONE_ENTRY,
    CODE_ENTRY
}

data class OtpUiState(
    val step: OtpStep = OtpStep.PHONE_ENTRY,
    val phone: String = "+91 ",
    val otpCode: String = "",
    val name: String = "",
    val verificationId: String? = null,
    val cooldownSeconds: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val signedIn: Boolean = false
)

class OtpViewModel(
    private val auth: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, errorMessage = null) }
    fun onOtpChange(value: String) = _uiState.update { it.copy(otpCode = value.filter { it.isDigit() }.take(6), errorMessage = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errorMessage = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun sendOtp() {
        val state = _uiState.value
        if (state.isLoading || state.cooldownSeconds > 0) return
        val error = LoginValidator.validatePhone(state.phone)
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = auth.requestPhoneOtp(state.phone)
            result.fold(
                onSuccess = { verId ->
                    if (verId == PHONE_AUTO_VERIFIED) {
                        // Android read the SMS itself; no code to type.
                        _uiState.update { it.copy(isLoading = false, verificationId = verId, infoMessage = "Number verified automatically") }
                        completeVerification(verId, "")
                        return@fold
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = OtpStep.CODE_ENTRY,
                            verificationId = verId,
                            infoMessage = "Verification code sent to ${state.phone}",
                            errorMessage = null
                        )
                    }
                    startCooldown(30)
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Failed to send OTP") }
                }
            )
        }
    }

    fun verifyOtp() {
        val state = _uiState.value
        if (state.isLoading) return
        val codeError = LoginValidator.validateOtp(state.otpCode)
        if (codeError != null) {
            _uiState.update { it.copy(errorMessage = codeError) }
            return
        }
        val verId = state.verificationId ?: return
        completeVerification(verId, state.otpCode)
    }

    private fun completeVerification(verId: String, code: String) {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = auth.verifyPhoneOtp(verId, code, state.name, "", state.phone)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, signedIn = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Invalid verification code") }
                }
            )
        }
    }

    fun changePhone() {
        _uiState.update { it.copy(step = OtpStep.PHONE_ENTRY, otpCode = "", errorMessage = null) }
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _uiState.update { it.copy(cooldownSeconds = i) }
                delay(1000)
            }
            _uiState.update { it.copy(cooldownSeconds = 0) }
        }
    }
}
