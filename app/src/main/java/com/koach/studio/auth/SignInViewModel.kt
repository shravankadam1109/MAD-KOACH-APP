package com.koach.studio.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.koach.studio.core.auth.AuthRepository
import com.koach.studio.core.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val clientAccounts: List<User> = emptyList(),
    val agencyAccounts: List<User> = emptyList(),
    val isDemo: Boolean = true,
    val signingInId: String? = null,
    val errorMessage: String? = null
)

class SignInViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        auth.demoAccounts().let { accounts ->
            SignInUiState(
                clientAccounts = accounts.filterNot { it.role.isAgency },
                agencyAccounts = accounts.filter { it.role.isAgency }.sortedBy { it.role.ordinal * -1 },
                isDemo = auth.isDemo
            )
        }
    )
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun signIn(userId: String) {
        if (_uiState.value.signingInId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(signingInId = userId, errorMessage = null) }
            val result = auth.signInAsDemo(userId)
            _uiState.update {
                it.copy(signingInId = null, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }
}
