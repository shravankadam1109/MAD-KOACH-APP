package com.koach.studio.app

import com.koach.studio.auth.StudioAccessRequestViewModel
import com.koach.studio.auth.StudioAccessRequestScreen
import com.koach.studio.auth.ForgotPasswordViewModel
import com.koach.studio.auth.ForgotPasswordScreen
import com.koach.studio.core.designsystem.theme.KoachTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.koach.studio.auth.AuthPortal
import com.koach.studio.auth.ClientLoginScreen
import com.koach.studio.auth.ClientSignUpScreen
import com.koach.studio.auth.ClientOtpScreen
import com.koach.studio.auth.LoginViewModel
import com.koach.studio.auth.SignUpViewModel
import com.koach.studio.auth.OtpViewModel
import com.koach.studio.auth.TeamLoginScreen
import com.koach.studio.auth.WelcomeScreen
import com.koach.studio.core.di.appContainer
import com.koach.studio.core.di.koachViewModel

private object AuthRoutes {
    const val WELCOME = "welcome"
    const val CLIENT_LOGIN = "login/client"
    const val CLIENT_SIGN_UP = "signup/client"
    const val CLIENT_OTP = "login/otp"
    const val TEAM_LOGIN = "login/team"
    const val FORGOT_PASSWORD = "login/forgot"
    const val STUDIO_ACCESS_REQUEST = "login/team/request"
}

/** Root of the UI: the signed-out entry flow until a user is signed in, then their shell. */
@Composable
fun KoachApp() {
    val container = appContainer()
    val user by container.authRepository.currentUser.collectAsState()
    Crossfade(targetState = user, label = "auth") { signedIn ->
        if (signedIn == null) {
            SignedOutFlow(isDemo = container.authRepository.isDemo)
        } else {
            // Keyed so switching accounts starts with fresh navigation state.
            key(signedIn.id) {
                val notice = container.dataNotice
                if (notice == null) {
                    KoachShell(signedIn)
                } else {
                    Column(Modifier.fillMaxSize()) {
                        DataNoticeBar(notice)
                        Box(Modifier.weight(1f)) { KoachShell(signedIn) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedOutFlow(isDemo: Boolean) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = AuthRoutes.WELCOME,
        enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 } },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { it / 8 } }
    ) {
        composable(AuthRoutes.WELCOME) {
            WelcomeScreen(
                isDemo = isDemo,
                onClientLogin = { nav.navigate(AuthRoutes.CLIENT_LOGIN) },
                onTeamLogin = { nav.navigate(AuthRoutes.TEAM_LOGIN) }
            )
        }
        composable(AuthRoutes.CLIENT_LOGIN) {
            ClientLoginScreen(
                viewModel = koachViewModel(key = "login-client") { LoginViewModel(AuthPortal.CLIENT, it.authRepository) },
                onBack = { nav.popBackStack() },
                onNavigateToSignUp = { nav.navigate(AuthRoutes.CLIENT_SIGN_UP) },
                onNavigateToOtp = { nav.navigate(AuthRoutes.CLIENT_OTP) },
                onForgotPassword = { nav.navigate(AuthRoutes.FORGOT_PASSWORD) }
            )
        }
        composable(AuthRoutes.CLIENT_SIGN_UP) {
            ClientSignUpScreen(
                viewModel = koachViewModel(key = "signup-client") { SignUpViewModel(it.authRepository) },
                onBack = { nav.popBackStack() },
                onNavigateToSignIn = {
                    nav.navigate(AuthRoutes.CLIENT_LOGIN) {
                        popUpTo(AuthRoutes.WELCOME)
                    }
                }
            )
        }
        composable(AuthRoutes.CLIENT_OTP) {
            ClientOtpScreen(
                viewModel = koachViewModel(key = "otp-client") { OtpViewModel(it.authRepository) },
                onBack = { nav.popBackStack() }
            )
        }
        composable(AuthRoutes.TEAM_LOGIN) {
            TeamLoginScreen(
                viewModel = koachViewModel(key = "login-team") { LoginViewModel(AuthPortal.AGENCY, it.authRepository) },
                onBack = { nav.popBackStack() },
                onForgotPassword = { nav.navigate(AuthRoutes.FORGOT_PASSWORD) },
                onRequestAccess = { nav.navigate(AuthRoutes.STUDIO_ACCESS_REQUEST) }
            )
        }
        composable(AuthRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                viewModel = koachViewModel(key = "forgot-password") { ForgotPasswordViewModel() },
                onBackToSignIn = { nav.popBackStack() }
            )
        }
        composable(AuthRoutes.STUDIO_ACCESS_REQUEST) {
            StudioAccessRequestScreen(
                viewModel = koachViewModel(key = "studio-access-request") { StudioAccessRequestViewModel(it.authRepository) },
                onBackToSignIn = { nav.popBackStack() }
            )
        }
    }
}

/** Thin, always-visible notice that part of the backend is still sample data. */
@Composable
private fun DataNoticeBar(text: String) {
    val c = KoachTheme.colors
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = c.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .background(c.info.copy(alpha = 0.18f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("data_notice")
    )
}

