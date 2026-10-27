package com.koach.studio.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.koach.studio.auth.LoginValidator.Field
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachTextButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.KoachTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    /** True once a valid-looking email was submitted; the screen then shows the confirmation. */
    val sent: Boolean = false
)

/**
 * UI PROTOTYPE ONLY. This screen demonstrates the password-recovery journey but
 * deliberately does NOT call Firebase's password-reset API and sends no email:
 * "Continue" only validates the address and switches to a confirmation state.
 * Wire [continueWithEmail] to AuthRepository.sendPasswordReset when real
 * recovery emails are wanted.
 */
class ForgotPasswordViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value.trim(), emailError = null) }

    /** Validates only; no network request is made (prototype). */
    fun continueWithEmail() {
        val email = _uiState.value.email
        val error = LoginValidator.validate(email, "x".repeat(LoginValidator.PASSWORD_MIN))[Field.EMAIL]
        _uiState.update { if (error != null) it.copy(emailError = error) else it.copy(sent = true, emailError = null) }
    }

    fun useDifferentEmail() = _uiState.update { it.copy(sent = false) }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val focus = LocalFocusManager.current

    KoachBackground(modifier.fillMaxSize()) {
        AuthBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { KoachTopBar(title = "", onBack = onBackToSignIn, transparent = true) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KoachTheme.spacing.xl, vertical = 8.dp)
                    .testTag("forgot_password_screen")
            ) {
                AuthHeader(
                    eyebrow = "Account recovery",
                    headline = "Forgot password?",
                    body = "Enter the email you use for KOACH and we'll help you get back in."
                )
                Spacer(Modifier.height(28.dp))
                AnimatedContent(
                    targetState = state.sent,
                    transitionSpec = { fadeIn(KoachMotion.enter()) togetherWith fadeOut(KoachMotion.exit()) },
                    label = "forgot-password-state"
                ) { sent ->
                    if (!sent) {
                        Column {
                            KoachTextField(
                                value = state.email,
                                onValueChange = viewModel::onEmailChange,
                                label = "Email",
                                leadingIcon = Icons.Outlined.Email,
                                error = state.emailError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.continueWithEmail() }),
                                modifier = Modifier.testTag("forgot_email")
                            )
                            Spacer(Modifier.height(20.dp))
                            KoachPrimaryButton(
                                text = "Continue",
                                onClick = { focus.clearFocus(); viewModel.continueWithEmail() },
                                modifier = Modifier.fillMaxWidth().testTag("forgot_continue")
                            )
                        }
                    } else {
                        KoachCard(
                            highlighted = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forgot_sent")
                                .semantics { liveRegion = LiveRegionMode.Polite }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.MarkEmailRead, contentDescription = null, tint = c.brand)
                                Spacer(Modifier.width(12.dp))
                                Text("Recovery link sent", style = MaterialTheme.typography.titleLarge, color = c.textPrimary)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "If an account exists for this email, recovery instructions would appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            KoachTextButton("Use a different email", onClick = viewModel::useDifferentEmail)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                    KoachTextButton(
                        text = "Back to Sign In",
                        onClick = onBackToSignIn,
                        modifier = Modifier.testTag("forgot_back_to_sign_in")
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
