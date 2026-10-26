package com.koach.studio.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import com.koach.studio.core.designsystem.components.PortfolioMosaic
import com.koach.studio.core.designsystem.theme.KoachMotion
import com.koach.studio.core.designsystem.theme.entrance
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.koach.studio.auth.LoginValidator.Field
import com.koach.studio.core.designsystem.components.DemoTag
import com.koach.studio.core.designsystem.components.Eyebrow
import com.koach.studio.core.designsystem.components.IconTile
import com.koach.studio.core.designsystem.components.KoachAvatar
import com.koach.studio.core.designsystem.components.KoachBackground
import com.koach.studio.core.designsystem.components.KoachCard
import com.koach.studio.core.designsystem.components.KoachDivider
import com.koach.studio.core.designsystem.components.KoachPrimaryButton
import com.koach.studio.core.designsystem.components.KoachSecondaryButton
import com.koach.studio.core.designsystem.components.KoachTextButton
import com.koach.studio.core.designsystem.components.KoachTextField
import com.koach.studio.core.designsystem.components.KoachTopBar
import com.koach.studio.core.designsystem.components.Tone
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.model.User

/** Client portal sign-in. */
@Composable
fun ClientLoginScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToOtp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LoginScreen(
        viewModel = viewModel,
        onBack = onBack,
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToOtp = onNavigateToOtp,
        copy = LoginCopy(
            eyebrow = "Client portal",
            headline = "Sign in.",
            body = "Sign in to see your projects, review cuts, and talk to your crew.",
            icon = Icons.Outlined.Movie,
            tone = Tone.Brand,
            createAccountLabel = "New to KOACH? Create account",
            screenTag = "client_login_screen"
        ),
        modifier = modifier
    )
}

/** Client portal sign-up. */
@Composable
fun ClientSignUpScreen(
    viewModel: SignUpViewModel,
    onBack: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val focus = LocalFocusManager.current

    KoachBackground(modifier.fillMaxSize()) {
        AuthBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { KoachTopBar(title = "", onBack = onBack, transparent = true) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KoachTheme.spacing.xl, vertical = 8.dp)
                    .testTag("client_signup_screen")
            ) {
                AuthHeader(
                    eyebrow = "Client registration",
                    headline = "Create account.",
                    body = "Register for KOACH Studio to manage project briefs, reviews, deliverables, and team chat."
                )
                Spacer(Modifier.height(24.dp))

                KoachTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Full name",
                    leadingIcon = Icons.Outlined.Person,
                    error = state.errors[Field.NAME],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.testTag("signup_name")
                )
                Spacer(Modifier.height(14.dp))

                KoachTextField(
                    value = state.company,
                    onValueChange = viewModel::onCompanyChange,
                    label = "Organisation / Brand (optional)",
                    leadingIcon = Icons.Outlined.Business,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.testTag("signup_company")
                )
                Spacer(Modifier.height(14.dp))

                KoachTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email address",
                    leadingIcon = Icons.Outlined.Email,
                    error = state.errors[Field.EMAIL],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.testTag("signup_email")
                )
                Spacer(Modifier.height(14.dp))

                KoachTextField(
                    value = state.phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Phone number with country code",
                    leadingIcon = Icons.Outlined.Phone,
                    error = state.errors[Field.PHONE],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.testTag("signup_phone")
                )
                Spacer(Modifier.height(14.dp))

                KoachTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Choose a password (min 6 chars)",
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.errors[Field.PASSWORD],
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (state.passwordVisible) "Hide password" else "Show password",
                                tint = c.textTertiary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.signUp() }),
                    modifier = Modifier.testTag("signup_password")
                )
                Spacer(Modifier.height(20.dp))

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.danger,
                        modifier = Modifier.padding(bottom = 12.dp).testTag("signup_error")
                    )
                }

                KoachPrimaryButton(
                    text = if (state.isLoading) "Creating account…" else "Create Client Account",
                    onClick = { focus.clearFocus(); viewModel.signUp() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth().testTag("signup_submit")
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Already have an account?", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.brand,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigateToSignIn() }.testTag("switch_to_signin")
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Client portal Phone OTP verification screen. */
@Composable
fun ClientOtpScreen(
    viewModel: OtpViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val focus = LocalFocusManager.current

    KoachBackground(modifier.fillMaxSize()) {
        AuthBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { KoachTopBar(title = "", onBack = onBack, transparent = true) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KoachTheme.spacing.xl, vertical = 8.dp)
                    .testTag("client_otp_screen")
            ) {
                AuthHeader(
                    eyebrow = "Phone Verification",
                    headline = if (state.step == OtpStep.PHONE_ENTRY) "OTP Verification" else "Enter code",
                    body = if (state.step == OtpStep.PHONE_ENTRY) "Sign in or register quickly using your phone number."
                    else "Enter the 6-digit verification code sent to ${state.phone}"
                )
                Spacer(Modifier.height(24.dp))

                if (state.step == OtpStep.PHONE_ENTRY) {
                    KoachTextField(
                        value = state.phone,
                        onValueChange = viewModel::onPhoneChange,
                        label = "Phone number with country code",
                        leadingIcon = Icons.Outlined.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.sendOtp() }),
                        modifier = Modifier.testTag("otp_phone_input")
                    )
                    Spacer(Modifier.height(18.dp))

                    if (state.errorMessage != null) {
                        Text(state.errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = c.danger, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    KoachPrimaryButton(
                        text = if (state.isLoading) "Sending OTP…" else "Get Verification Code",
                        onClick = { focus.clearFocus(); viewModel.sendOtp() },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("otp_send_button")
                    )
                } else {
                    KoachTextField(
                        value = state.otpCode,
                        onValueChange = viewModel::onOtpChange,
                        label = "6-Digit Code",
                        leadingIcon = Icons.Outlined.Pin,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus(); viewModel.verifyOtp() }),
                        modifier = Modifier.testTag("otp_code_input")
                    )
                    Spacer(Modifier.height(14.dp))

                    if (state.errorMessage != null) {
                        Text(state.errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = c.danger, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    KoachPrimaryButton(
                        text = if (state.isLoading) "Verifying…" else "Verify & Sign In",
                        onClick = { focus.clearFocus(); viewModel.verifyOtp() },
                        enabled = !state.isLoading && state.otpCode.length == 6,
                        modifier = Modifier.fillMaxWidth().testTag("otp_verify_button")
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = viewModel::changePhone) {
                            Text("Change phone number", color = c.textSecondary)
                        }
                        if (state.cooldownSeconds > 0) {
                            Text("Resend in ${state.cooldownSeconds}s", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                        } else {
                            TextButton(onClick = viewModel::sendOtp, enabled = !state.isLoading) {
                                Text("Resend OTP", color = c.brand, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Agency team sign-in. */
@Composable
fun TeamLoginScreen(viewModel: LoginViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LoginScreen(
        viewModel = viewModel,
        onBack = onBack,
        onNavigateToSignUp = {},
        onNavigateToOtp = {},
        copy = LoginCopy(
            eyebrow = "Agency team",
            headline = "Studio sign-in.",
            body = "For KOACH crew and admins. Client accounts can't sign in here.",
            icon = Icons.Outlined.Groups,
            tone = Tone.Info,
            createAccountLabel = null,
            screenTag = "team_login_screen"
        ),
        modifier = modifier
    )
}

private data class LoginCopy(
    val eyebrow: String,
    val headline: String,
    val body: String,
    val icon: ImageVector,
    val tone: Tone,
    val createAccountLabel: String?,
    val screenTag: String
)

@Composable
private fun LoginScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToOtp: () -> Unit,
    copy: LoginCopy,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val c = KoachTheme.colors
    val focus = LocalFocusManager.current
    var showRequestAccess by rememberSaveable { mutableStateOf(false) }

    KoachBackground(modifier.fillMaxSize()) {
        AuthBackdrop()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = { KoachTopBar(title = "", onBack = onBack, transparent = true) }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = KoachTheme.spacing.xl, vertical = 8.dp)
                    .testTag(copy.screenTag)
            ) {
                AuthHeader(eyebrow = copy.eyebrow, headline = copy.headline, body = copy.body)
                Spacer(Modifier.height(28.dp))

                KoachTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email",
                    leadingIcon = Icons.Outlined.Email,
                    error = state.errors[Field.EMAIL],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                    modifier = Modifier.testTag("login_email")
                )
                Spacer(Modifier.height(14.dp))
                KoachTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = "Password",
                    leadingIcon = Icons.Outlined.Lock,
                    error = state.errors[Field.PASSWORD],
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focus.clearFocus()
                        viewModel.signIn()
                    }),
                    visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailing = {
                        IconButton(onClick = viewModel::togglePasswordVisibility) {
                            Icon(
                                if (state.passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (state.passwordVisible) "Hide password" else "Show password",
                                tint = c.textTertiary
                            )
                        }
                    },
                    modifier = Modifier.testTag("login_password")
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    KoachTextButton(
                        text = "Forgot password?",
                        onClick = viewModel::requestPasswordReset,
                        modifier = Modifier.testTag("login_forgot")
                    )
                }

                if (state.errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.danger,
                        modifier = Modifier
                            .testTag("login_error")
                            .semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                }

                if (state.infoMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = state.infoMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.info,
                        modifier = Modifier
                            .testTag("login_info")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                Spacer(Modifier.height(20.dp))
                KoachPrimaryButton(
                    text = if (state.isLoading && state.signingInDemoId == null) "Signing in…" else "Sign In",
                    onClick = {
                        focus.clearFocus()
                        viewModel.signIn()
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_submit")
                )

                // Additional sign-in options for Client Portal
                if (state.portal == AuthPortal.CLIENT) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KoachDivider(modifier = Modifier.weight(1f))
                        Text(
                            "OR",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        KoachDivider(modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))

                    // Google Sign-In button
                    KoachSecondaryButton(
                        text = "Continue with Google",
                        onClick = { viewModel.signInWithGoogle() },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("login_google_btn")
                    )
                    Spacer(Modifier.height(10.dp))

                    // Phone OTP sign-in button
                    KoachSecondaryButton(
                        text = "Sign in with Phone (OTP)",
                        onClick = onNavigateToOtp,
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().testTag("login_phone_otp_btn")
                    )

                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Create account",
                            style = MaterialTheme.typography.labelLarge,
                            color = c.brand,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateToSignUp() }.testTag("switch_to_signup")
                        )
                    }
                } else if (copy.createAccountLabel != null) {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        KoachTextButton(
                            text = copy.createAccountLabel,
                            onClick = { showRequestAccess = true },
                            modifier = Modifier.testTag("login_request_access")
                        )
                    }
                }

                if (state.demoAccounts.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    DemoAccountsCard(
                        portal = state.portal,
                        accounts = state.demoAccounts,
                        signingInId = state.signingInDemoId,
                        onSelect = { userId ->
                            focus.clearFocus()
                            viewModel.signInWithDemo(userId)
                        }
                    )
                }

                if (state.isDemo && state.demoPassword != null) {
                    Spacer(Modifier.height(16.dp))
                    DemoPasswordBanner(state.demoPassword!!)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showRequestAccess) {
        RequestAccessDialog(onDismiss = { showRequestAccess = false })
    }
}

@Composable
private fun DemoAccountsCard(
    portal: AuthPortal,
    accounts: List<User>,
    signingInId: String?,
    onSelect: (String) -> Unit
) {
    val c = KoachTheme.colors
    KoachCard(modifier = Modifier.fillMaxWidth().testTag("demo_accounts_card")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Eyebrow("Sample accounts", color = c.brand)
            Spacer(Modifier.weight(1f))
            DemoTag()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Tap any ${if (portal == AuthPortal.CLIENT) "client" else "team"} persona to test the portal.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { user ->
                DemoAccountRow(
                    user = user,
                    signingIn = signingInId == user.id,
                    onClick = { onSelect(user.id) }
                )
            }
        }
    }
}

@Composable
private fun DemoAccountRow(user: User, signingIn: Boolean, onClick: () -> Unit) {
    val c = KoachTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .testTag("demo_account_${user.id}")
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Sign in as ${user.name}, ${user.title}"
            }
    ) {
        KoachAvatar(user.initials, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
            Text(user.title, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
        if (signingIn) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = c.brand, strokeWidth = 2.dp)
        } else {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DemoPasswordBanner(password: String) {
    val c = KoachTheme.colors
    Text(
        text = "Demo password for all sample accounts is $password",
        style = MaterialTheme.typography.labelSmall,
        color = c.textTertiary,
        modifier = Modifier.fillMaxWidth().testTag("demo_password_banner")
    )
}

@Composable
private fun RequestAccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request access") },
        text = {
            Text("KOACH is an invite-only studio portal. If you are a new client, get in touch with your producer or reach us at studio@koach.example to receive an invite link.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

/** Short band of KOACH artwork across the top of every auth screen, fading into the page. */
@Composable
private fun AuthBackdrop() {
    PortfolioMosaic(Modifier.fillMaxWidth().height(300.dp), dim = 0.6f)
}

/** Shared auth heading: brand, eyebrow, headline and supporting line, arriving in sequence. */
@Composable
private fun AuthHeader(eyebrow: String, headline: String, body: String) {
    val c = KoachTheme.colors
    Column(Modifier.fillMaxWidth()) {
        Wordmark(Modifier.entrance(0))
        Spacer(Modifier.height(96.dp))
        Eyebrow(eyebrow, modifier = Modifier.entrance(1))
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = headline,
            transitionSpec = { (fadeIn(KoachMotion.enter()) + slideInVertically(KoachMotion.enter()) { it / 3 }) togetherWith fadeOut(KoachMotion.exit()) },
            label = "auth-headline",
            modifier = Modifier.entrance(2)
        ) { text ->
            Text(text, style = MaterialTheme.typography.displayMedium, color = c.textPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textSecondary, modifier = Modifier.entrance(3).animateContentSize())
    }
}
