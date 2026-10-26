package com.koach.studio.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "+91 ",
    val password: String = "",
    val company: String = "",
    val passwordVisible: Boolean = false,
    val errors: Map<LoginValidator.Field, String> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signedUp: Boolean = false
)

class SignUpViewModel(
    private val auth: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, errors = it.errors - LoginValidator.Field.NAME, errorMessage = null) }
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value.trim(), errors = it.errors - LoginValidator.Field.EMAIL, errorMessage = null) }
    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, errors = it.errors - LoginValidator.Field.PHONE, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errors = it.errors - LoginValidator.Field.PASSWORD, errorMessage = null) }
    fun onCompanyChange(value: String) = _uiState.update { it.copy(company = value, errorMessage = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun signUp() {
        val s = _uiState.value
        if (s.isLoading) return
        val errors = LoginValidator.validateSignUp(s.name, s.email, s.phone, s.password)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = auth.signUpClient(s.name, s.email, s.phone, s.password, s.company)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, signedUp = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Sign up failed") }
                }
            )
        }
    }

    fun signUpWithGoogle(idToken: String = "") {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = auth.signInWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, signedUp = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Google Sign-In failed") }
                }
            )
        }
    }
}
