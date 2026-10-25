package com.koach.studio.app

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
import com.koach.studio.auth.LoginViewModel
import com.koach.studio.auth.TeamLoginScreen
import com.koach.studio.auth.WelcomeScreen
import com.koach.studio.core.di.appContainer
import com.koach.studio.core.di.koachViewModel

private object AuthRoutes {
    const val WELCOME = "welcome"
    const val CLIENT_LOGIN = "login/client"
    const val TEAM_LOGIN = "login/team"
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
            key(signedIn.id) { KoachShell(signedIn) }
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
                onBack = { nav.popBackStack() }
            )
        }
        composable(AuthRoutes.TEAM_LOGIN) {
            TeamLoginScreen(
                viewModel = koachViewModel(key = "login-team") { LoginViewModel(AuthPortal.AGENCY, it.authRepository) },
                onBack = { nav.popBackStack() }
            )
        }
    }
}
