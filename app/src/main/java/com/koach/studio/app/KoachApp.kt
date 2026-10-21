package com.koach.studio.app

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import com.koach.studio.auth.SignInScreen
import com.koach.studio.core.di.appContainer

/** Root of the UI: the sign-in screen until a user is signed in, then their shell. */
@Composable
fun KoachApp() {
    val user by appContainer().authRepository.currentUser.collectAsState()
    Crossfade(targetState = user, label = "auth") { signedIn ->
        if (signedIn == null) {
            SignInScreen()
        } else {
            // Keyed so switching accounts starts with fresh navigation state.
            key(signedIn.id) { KoachShell(signedIn) }
        }
    }
}
