package com.koach.studio.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.koach.studio.auth.SignInScreen
import com.koach.studio.core.di.appContainer
import com.koach.studio.projects.navigation.ProjectsNavHost

/** Root of the UI: the sign-in screen until a user is signed in, then the app. */
@Composable
fun KoachApp() {
    val user by appContainer().authRepository.currentUser.collectAsState()
    if (user == null) {
        SignInScreen()
    } else {
        ProjectsNavHost()
    }
}
