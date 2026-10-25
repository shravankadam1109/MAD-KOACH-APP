package com.koach.studio.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.auth.LoginValidator.Field
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val portal: AuthPortal,
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val errors: Map<Field, String> = emptyMap(),
    val isLoading: Boolean = false,
    val signingInDemoId: String? = null,
    val errorMessage: String? = null,
    val signedIn: Boolean = false,
    /** Sample accounts for this portal only. Empty when running against a real backend. */
    val demoAccounts: List<User> = emptyList(),
    val isDemo: Boolean = false,
    val demoPassword: String? = null,
    /** Neutral information, e.g. the result of a password reset request. */
    val infoMessage: String? = null
)

class LoginViewModel(
    portal: AuthPortal,
    private val auth: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            portal = portal,
            demoAccounts = auth.demoAccounts().filter { portal.accepts(it.role) },
            isDemo = auth.isDemo,
            demoPassword = auth.demoPassword
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value.trim(), errors = it.errors - Field.EMAIL, errorMessage = null) }
    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, errors = it.errors - Field.PASSWORD, errorMessage = null) }
    fun togglePasswordVisibility() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
    fun errorShown() = _uiState.update { it.copy(errorMessage = null) }
    fun infoShown() = _uiState.update { it.copy(infoMessage = null) }

    /** Asks the backend to email a reset link for the email in the form. */
    fun requestPasswordReset() {
        val email = _uiState.value.email
        LoginValidator.validate(email, "x".repeat(LoginValidator.PASSWORD_MIN))[Field.EMAIL]?.let { error ->
            _uiState.update { it.copy(errors = it.errors + (Field.EMAIL to error), infoMessage = null) }
            return
        }
        viewModelScope.launch {
            val result = auth.sendPasswordReset(email)
            _uiState.update {
                it.copy(infoMessage = result.fold({ "If $email has an account, a reset link is on its way." }, { e -> e.message }))
            }
        }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.isLoading) return
        val errors = LoginValidator.validate(state.email, state.password)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = auth.signInWithEmail(state.email, state.password)
            finish(result)
        }
    }

    /** Signs in with a sample account; only accounts belonging to this portal are offered. */
    fun signInWithDemo(userId: String) {
        val state = _uiState.value
        if (state.isLoading || state.demoAccounts.none { it.id == userId }) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, signingInDemoId = userId, errorMessage = null) }
            finish(auth.signInAsDemo(userId))
        }
    }

    private suspend fun finish(result: Result<User>) {
        val user = result.getOrNull()
        if (user != null && !_uiState.value.portal.accepts(user.role)) {
            // Never let the wrong door grant a session: sign straight back out.
            auth.signOut()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    signingInDemoId = null,
                    errorMessage = if (user.role.isAgency) "This is an agency team account. Use Agency team sign-in." else "This is a client account. Use Client sign-in."
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                signingInDemoId = null,
                signedIn = user != null,
                errorMessage = result.exceptionOrNull()?.message
            )
        }
    }
}
