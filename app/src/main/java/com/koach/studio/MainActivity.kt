package com.koach.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.koach.studio.app.KoachApp
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.di.appContainer
import com.koach.studio.core.settings.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val container = appContainer()
            val preferences by container.themePreferencesStore.preferences.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (preferences.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemDark
            }
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !isDark
                    controller.isAppearanceLightNavigationBars = !isDark
                }
            }
            KoachTheme(darkTheme = isDark) {
                KoachApp()
            }
        }
    }
}
